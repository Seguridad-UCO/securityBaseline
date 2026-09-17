# PLAN: MFA como step-up para operaciones administrativas

## Metadata

- **ID:** HU-024
- **Slice:** `shared` (evidencia de autenticación, capacidad técnica transversal) + `authorization`
  (el único punto de invocación — decora el gate de ADR-023)
- **Tipo:** Mixto (infraestructura de evidencia de claims + decorador de un validador existente)
- **Fecha:** 2026-09-16
- **Rama sugerida:** `feature/HU-024-mfa-step-up-administrativo`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-024.md`,
  `security-platform-architecture/docs/01-governance/adr/ADR-027-mfa-step-up-administrative-operations.md`,
  código real: `PrincipalMustBeApplicationAdministratorValidatorImpl.java`, `AdministrationRequest.java`,
  `ApplicationAdministrationRequestMapper.java`, `AuthorizationConfiguration.java`,
  `PdpPrincipal.java`, `SecurityContext.java`, `LocalUserPrincipal.java`,
  `OidcAuthenticationSuccessHandler.java`, `NotAuthorizedToAdministerException.java`,
  `AuthorizationMessages.java`. No hay event storming propio de esta capacidad en
  `artefactos-referencia`. Decisiones de diseño confirmadas con Sebastián el 2026-09-16 (ver §11 —
  resueltas, no pendientes).
- **Criterios de la línea base que toca:** 1, 2, 9, 11, 12, 21, 22, 23

## 1. Resumen funcional

Exige evidencia de MFA (`acr`/`amr` del JWT) para toda operación administrativa gateada por
`AuthorizeAdministrationUseCase` (ADR-023), como *step-up* adicional a la autorización de rol —
nunca en catálogos ni en `/api/v1/authorize`. Se cablea decorando la ÚNICA interfaz que los 12
`Administer*UseCaseImpl` ya inyectan (`PrincipalMustBeApplicationAdministratorValidator`), sin tocar
ninguno de esos 12 archivos ni sus pruebas.

**No cubre:** configuración del realm de Keycloak (prerrequisito externo, documentado como tal);
el flujo de reautenticación del lado de `securityBaseline-vue` — decisión explícita: el rechazo por
falta de MFA queda distinguido en el código (`code: MFA_REQUIRED`), pero disparar la redirección al
login de Keycloak con el parámetro de step-up es trabajo del frontend, historia aparte.

### 1.1 Dónde vive `acr`/`amr` — hallazgo de esta planificación

Para el canal que de verdad importa (perfil `keycloak`, panel administrativo vía BFF), el principal
por request es `LocalUserPrincipal` — reconstruido desde `WebSessionServerSecurityContextRepository`,
**sin JWT vivo por petición**. El JWT completo (con `acr`/`amr`) solo existe una vez, en
`OidcAuthenticationSuccessHandler.startLocalSession`, en el momento del login
(`oidc.getIdToken()`). Leer el claim ahí y guardarlo en la sesión no es "una llamada adicional a
Keycloak" (lo que ADR-027 prohíbe) — es leer un token que el propio flujo OIDC ya entregó. Un
*step-up* real, en consecuencia, exige que el usuario repita el login completo de Keycloak (sesión
nueva); no hay forma de refrescar `acr`/`amr` de una sesión BFF ya abierta sin eso.

Para el perfil de desarrollo (`!keycloak`, `Jwt` directo) el claim sí está fresco en cada request —
sin cambio de diseño necesario ahí, solo de extracción.

**Decisión de dato transportado:** ni `PdpPrincipal` ni `LocalUserPrincipal` ni `AdministrationRequest`
guardan un `boolean` ya resuelto — guardan la evidencia cruda (`AuthenticationContextEvidence`:
`acr` + `amr`, ver §4). La decisión de qué claim mirar y qué valores aceptar depende de
`MfaEvidenceProperties`, que es configuración de Spring — y `PdpPrincipal`/`SecurityContext`/
`ApplicationAdministrationRequestMapper` son utilidades **estáticas, sin inyección**, a propósito
(`SecurityContext` es "el único camino... sin `@AuthenticationPrincipal`", sin un solo colaborador
inyectado). Meterles una dependencia de configuración para resolver la política habría exigido
convertirlas en beans, con onda expansiva sobre cada uno de sus muchos consumidores. La evaluación
real (`MfaEvidenceProperties.satisfiedBy(evidence)`) vive en el único componente de este diseño que
ya es un bean de Spring: el decorador (§1.2). Transportar solo `acr`/`amr` (no el mapa completo de
claims) es deliberado: son los dos claims estándar OIDC que describen *cómo* se autenticó la sesión,
no datos personales — un mapa de claims completo arriesgaría filtrar algo sensible a
`AdministrationRequest`, que ya viaja hacia la auditoría (HU-021).

### 1.2 Cómo se cablea — decorador, no un segundo validador por caso de uso

Los 12 `Administer*UseCaseImpl` inyectan la **interfaz** `PrincipalMustBeApplicationAdministratorValidator`,
nunca la clase concreta. `MfaAwareApplicationAdministratorValidator` implementa esa misma interfaz,
delega en la implementación actual (autorización vía OPA) y, solo si esa autorización permite, exige
evidencia de MFA — mismo patrón que `RevocationAwareJwtDecoder` en HU-022 (decorar un contrato
existente para que todos sus consumidores queden cubiertos sin tocarlos). El bean se cablea en
`AuthorizationConfiguration`; los 12 casos de uso y sus 12 archivos de prueba no cambian.

**Orden: autorización primero, MFA después** — ADR-027 dice "además de la autorización de rol,
evidencia de MFA": el step-up es adicional a una autorización que ya debe existir, no un sustituto.
Evita además una consulta a OPA innecesaria si el sujeto ni siquiera tiene el rol.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Rechazo distinguible sin evidencia de MFA | Un administrador autorizado por rol pero sin `acr`/`amr` aceptado recibe `MfaEvidenceRequiredException` (400, code `MFA_REQUIRED`) — nunca `NOT_AUTHORIZED_TO_ADMINISTER` |
| 2 | Con evidencia, sin fricción | Un administrador autorizado por rol y con `acr`/`amr` aceptado completa la operación exactamente igual que hoy — cero cambio de comportamiento observable |
| 3 | Sin MFA en no-administrativo | Catálogos y `/api/v1/authorize` no invocan `MfaAwareApplicationAdministratorValidator` en absoluto — no hay cambio en esas rutas (no se tocan) |
| 4 | Sin excepción de operación | Los 12 `Administer*UseCaseImpl` (hoy) y cualquiera futuro que use `PrincipalMustBeApplicationAdministratorValidator` quedan cubiertos por construcción, al ser un decorador de la interfaz que todos comparten |
| 5 | Fail-closed sin configuración | Con `pdp.security.mfa.claim`/`accepted-values` sin configurar (antes de que el realm esté listo), `MfaEvidenceProperties.satisfiedBy` nunca es `true` — toda operación administrativa queda bloqueada hasta configurar el realm. Es la consecuencia esperada y documentada de ADR-027 (ver Alcance de HU-024.md), no un defecto |
| 6 | `verificar.ps1` (suite completa) sigue en verde | `mvnw -f pdp/pom.xml verify`, cobertura ≥ 50 % por paquete nuevo |

## 3. Reglas de negocio

No hay un value object nuevo con invariantes de formato que rechacen algo — `AuthenticationContextEvidence`
transporta lo que el JWT ya trae, sin validarlo (un `acr` ausente es un estado válido: "sin
evidencia", no un error de formato). La única decisión es de comportamiento del decorador, mismo
criterio que `PrincipalMustBeApplicationAdministratorValidatorImpl` ya aplica hoy para
`NOT_AUTHORIZED_TO_ADMINISTER` sin una `Rule` separada (la decisión ya viene resuelta, sin I/O
adicional que justifique un validador con Rule propia).

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| 1 | Autorizado por rol pero sin evidencia de MFA aceptada → se rechaza, distinguible de "no autorizado" | `MfaAwareApplicationAdministratorValidator` (comparación sobre `AuthenticationContextEvidence` ya resuelta, vía `MfaEvidenceProperties.satisfiedBy`) | — (dato ya viaja en `AdministrationRequest`) | `MfaEvidenceRequiredException` → **400**, code `MFA_REQUIRED` |

## 4. Modelo de dominio afectado

### Value objects

| VO | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|
| `AuthenticationContextEvidence` | Nuevo | Ninguno de formato — transporta lo que el JWT trae, incluida la ausencia | `shared/security/mfa/` |
| `MfaEvidenceProperties` | Nuevo | Ninguno que bloquee el arranque — ver nota en §7 sobre por qué **no** lleva `requireNonNull` como el resto de `@ConfigurationProperties` del proyecto | `shared/security/mfa/` |

## 5. Persistencia

*(No aplica — esta historia no toca ninguna base de datos.)*

## 6. Endpoint

*(Sección eliminada — no expone ni modifica ningún endpoint HTTP. Decora un validador ya inyectado
por los 12 endpoints administrativos existentes, de forma transparente para sus controllers e
interactors.)*

## 7. SPEC — el contrato

### Contratos y value objects nuevos

```java
// shared/security/mfa/AuthenticationContextEvidence.java
public record AuthenticationContextEvidence(Optional<String> acr, List<String> amr) { }
```

```java
// shared/security/mfa/MfaEvidenceProperties.java
@ConfigurationProperties(prefix = "pdp.security.mfa")
public record MfaEvidenceProperties(String claim, Set<String> acceptedValues) {
    public boolean satisfiedBy(AuthenticationContextEvidence evidence) { }
    // implementa: claim="acr" → evidence.acr() está en acceptedValues; claim="amr" → alguno de
    // evidence.amr() está en acceptedValues; claim nulo/vacío o acceptedValues vacío → false
    // siempre (fail-closed mientras el realm no esté configurado, PLAN-HU-024.md §2 criterio 5).
}
```

> **`MfaEvidenceProperties` NO lleva `Objects.requireNonNull` en su constructor compacto**, a
> diferencia de `RevocationRetentionProperties`/`ActiveRolesCacheRetentionProperties`: un TTL ausente
> siempre es un error de configuración, pero un `claim`/`accepted-values` ausente es un **estado
> operativo válido y esperado** antes de que el realm de Keycloak tenga el flujo de MFA configurado
> (criterio de aceptación 5). Bloquear el arranque del PDP por esto contradice el propio ADR-027, que
> documenta esa dependencia externa como conocida. El implementador no debe aplicar aquí la regla
> general de "toda `@ConfigurationProperties` valida sus componentes" — es la excepción deliberada,
> documentada para que no se corrija por reflejo.

### Firma de la pieza de infraestructura

```java
// pdp/authorization/application/rule/validator/impl/MfaAwareApplicationAdministratorValidator.java
public final class MfaAwareApplicationAdministratorValidator implements PrincipalMustBeApplicationAdministratorValidator {
    public MfaAwareApplicationAdministratorValidator(PrincipalMustBeApplicationAdministratorValidator delegate,
            MfaEvidenceProperties mfaProperties) { }
    // execute(AdministrationRequest): delega en `delegate.execute(input)`; si permite, evalúa
    // mfaProperties.satisfiedBy(input.authenticationContext()); si no satisface, erroa con
    // MfaEvidenceRequiredException(input.tenantId(), input.applicationId()). Orden: autorización
    // primero (§1.2) — nunca evalúa MFA si el delegado ya rechazó.
}
```

### Excepción nueva

```java
// pdp/authorization/domain/exception/MfaEvidenceRequiredException.java
public final class MfaEvidenceRequiredException extends BusinessRuleViolationException {
    public MfaEvidenceRequiredException(TenantId tenantId, ApplicationId applicationId) {
        super("MFA_REQUIRED", AuthorizationMessages.mfaEvidenceRequired(applicationId));
    }
}
```

> Mismo patrón exacto que `NotAuthorizedToAdministerException` — `BusinessRuleViolationException` →
> 400 vía `ApiErrorHandler` (enganche por jerarquía base, no se toca). El `code` distinto
> (`MFA_REQUIRED` vs `NOT_AUTHORIZED_TO_ADMINISTER`) es el mecanismo con el que
> `securityBaseline-vue` distingue "reautentica con tu segundo factor" de "pide otro rol" — decisión
> de la sección de preguntas, criterio de aceptación 1.

### Firmas nuevas en puertos existentes

Ninguna — no hay un puerto de salida nuevo ni modificado.

### [M] Firmas que cambian (el implementador las aplica, no se generan como esqueleto)

```java
// shared/security/PdpPrincipal.java — [M]: gana el componente `AuthenticationContextEvidence
// authenticationContext`. `from(Jwt jwt)` lo resuelve con
// jwt.getClaimAsString("acr")/jwt.getClaimAsStringList("amr") (con null-safety: amr ausente →
// List.of(), no null). `from(OidcUser user)` igual, vía user.getClaimAsString/getClaims().
```

```java
// shared/security/LocalUserPrincipal.java — [M]: gana el componente `AuthenticationContextEvidence
// authenticationContext`.
```

```java
// shared/security/SecurityContext.java — [M]: la rama `LocalUserPrincipal` de currentPrincipal()
// pasa `user.authenticationContext()` al construir el PdpPrincipal (hoy construye con
// Optional.of(UserId.of(user.userId())) y nada más al final — agrega el nuevo componente).
```

```java
// shared/auth/service/OidcAuthenticationSuccessHandler.java — [M]: en startLocalSession, tras
// provisionIdentity.execute(request), reconstruye el LocalUserPrincipal recibido con la
// AuthenticationContextEvidence extraída de oidc.getIdToken() (mismo método de extracción que
// PdpPrincipal.from(Jwt) — no dupliques la lógica, factoriza si hace falta) antes de persistir la
// sesión. ProvisionIdentityUseCaseImpl/ProvisionIdentityRequest NO cambian: el use case de
// aprovisionamiento de identidad no sabe de MFA, construye con evidencia vacía por defecto y el
// success handler la completa — es lo único que tiene el ID token en la mano.
```

```java
// pdp/authorization/application/primaryport/request/AdministrationRequest.java — [M]: gana el
// componente `AuthenticationContextEvidence authenticationContext`. `withSubjectRoles(...)` (el
// único método propio del record) pasa el valor existente sin cambiarlo.
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/mapper/ApplicationAdministrationRequestMapper.java
// — [M]: toAdministrationRequest pasa principal.authenticationContext() al nuevo componente de
// AdministrationRequest.
```

```java
// pdp/authorization/domain/message/AuthorizationMessages.java — [M]: agrega
// mfaEvidenceRequired(ApplicationId), mismo estilo que notAuthorizedToAdminister(ApplicationId).
```

```java
// pdp/authorization/infrastructure/config/AuthorizationConfiguration.java — [M], CORTE ATÓMICO:
// el bean principalMustBeApplicationAdministratorValidator pasa de devolver
// new PrincipalMustBeApplicationAdministratorValidatorImpl(useCase) a devolver
// new MfaAwareApplicationAdministratorValidator(new PrincipalMustBeApplicationAdministratorValidatorImpl(useCase), mfaProperties).
```

> **Este `[M]` NO se cablea hasta que `MfaAwareApplicationAdministratorValidator.execute` tenga su
> cuerpo real.** Verificado durante esta misma planificación: envolver el bean de producción con el
> decorador todavía-esqueleto rompe en rojo `ApplicationHttpTests` (500 en vez de 201/200 —
> `UnsupportedOperationException` sin capturar en el camino real de administración, que varias
> pruebas HTTP existentes ya ejercitan). Mismo patrón exacto que `InternalSecurityConfiguration` en
> HU-022: el planificador ya materializó el `[N]` (`MfaAwareApplicationAdministratorValidator`,
> `MfaEvidenceProperties` — ambos compilando y con `verificar.ps1 -Rapido` en verde, 750/750, sin
> tocar este bean), pero el cableado real en `AuthorizationConfiguration` es tarea de
> `@3-implementador`, atómico junto con la lógica de `execute`/`satisfiedBy`. `@EnableConfigurationProperties`
> sí quedó ya extendido con `MfaEvidenceProperties.class` — registrar la properties bean es inocuo,
> nadie la consume todavía.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
├── security/
│   ├── mfa/
│   │   ├── AuthenticationContextEvidence.java             [N]
│   │   └── MfaEvidenceProperties.java                      [N]
│   ├── PdpPrincipal.java                                    [M]
│   ├── LocalUserPrincipal.java                              [M]
│   └── SecurityContext.java                                 [M]
└── auth/service/
    └── OidcAuthenticationSuccessHandler.java                 [M]

pdp/src/main/resources/application.properties                [M] — pdp.security.mfa.claim,
                                                                    pdp.security.mfa.accepted-values
                                                                    (ambas vacías: ver criterio 5)

pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/
├── application/
│   ├── rule/validator/impl/
│   │   └── MfaAwareApplicationAdministratorValidator.java    [N]
│   └── primaryport/request/
│       └── AdministrationRequest.java                        [M]
├── domain/
│   ├── exception/
│   │   └── MfaEvidenceRequiredException.java                 [N]
│   └── message/
│       └── AuthorizationMessages.java                        [M]
└── infrastructure/
    ├── adapter/primary/web/mapper/
    │   └── ApplicationAdministrationRequestMapper.java        [M]
    └── config/
        └── AuthorizationConfiguration.java                    [M]
```

No se toca `identity` (salvo el success handler, que ya vive en `shared/auth`, no en el slice),
`roles`, `profiles`, `resources`, `tenants`, `applications`, `assignments`, ni ninguno de los 12
`Administer*UseCaseImpl` — es exactamente el punto de §1.2.

## 9. Casos de prueba esperados

> `pdp/src/test` no lo toco yo (regla del planificador). Lo que sigue es lo que `@2-tester-spec`
> debe escribir — descripción del caso, no el código. Nota aparte: 4 archivos de prueba existentes
> ya construyen `PdpPrincipal`/`LocalUserPrincipal` directamente
> (`ApplicationAdministrationRequestMapperTests`, `OidcAuthenticationSuccessHandlerTests`,
> `PdpPrincipalSecurityContextTests`, `SessionControllerTests`) — dejarán de compilar con el nuevo
> componente y hay que arreglarlos (agregar el argumento), igual que en HU-022/023.

| Capa | Clase de prueba | Casos |
|---|---|---|
| `infrastructure` (unitaria, sin Spring) | `MfaEvidencePropertiesTests` | (a) `claim="acr"`, `acr` presente y en `acceptedValues` → `true`; (b) `claim="acr"`, presente pero no en `acceptedValues` → `false`; (c) `claim="acr"`, `acr` ausente (`Optional.empty()`) → `false`; (d) `claim="amr"`, algún valor de `amr` está en `acceptedValues` → `true`; (e) `claim="amr"`, `amr` vacío o sin intersección → `false`; (f) `claim`/`acceptedValues` sin configurar (cadena vacía / conjunto vacío) → `false` siempre, incluso con evidencia presente (fail-closed, criterio 5) |
| `application` (unitaria, sin Spring) | `MfaAwareApplicationAdministratorValidatorTests` | (a) delegado rechaza (`NotAuthorizedToAdministerException`) → el error se propaga tal cual, `MfaEvidenceProperties.satisfiedBy` nunca se evalúa (delegado *unreachable* para MFA — verificar con un fake que lanzaría si se llamara); (b) delegado permite + evidencia satisface → `Mono.empty()`; (c) delegado permite + evidencia no satisface → `MfaEvidenceRequiredException` con el `tenantId`/`applicationId` correctos |
| `infrastructure` (unitaria) | `ApplicationAdministrationRequestMapperTests` (extiende existente) | + un caso: el `authenticationContext` del `PdpPrincipal` viaja intacto al `AdministrationRequest` resultante |
| `infrastructure` (unitaria) | `OidcAuthenticationSuccessHandlerTests` (extiende existente) | + un caso: el `LocalUserPrincipal` persistido en sesión lleva la `AuthenticationContextEvidence` extraída del `oidc.getIdToken()` (con `acr`/`amr` sintéticos en el ID token de prueba) |
| `infrastructure` (unitaria) | `PdpPrincipalSecurityContextTests` (extiende existente) | + casos: `PdpPrincipal.from(Jwt)` con `acr`/`amr` presentes y ausentes; `SecurityContext.currentPrincipal()` para un `LocalUserPrincipal` propaga su `authenticationContext()` sin alterarlo |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-16 |
| Contrato aprobado (gate 1) | ✅ Aprobado | 2026-09-17 |
| Pruebas en rojo | ✅ Confirmado (9 casos, `UnsupportedOperationException`) | 2026-09-17 |
| Implementación en verde | ✅ Verde en las pruebas de la historia | 2026-09-17 |
| Validación | ✅ Aprobada — ver REPORTE-HU-024.md | 2026-09-17 |
| Entrega (gate 2) | ⏳ Pendiente — confirmación humana para commit/push | |

## 11. Ambigüedades pendientes

**Resueltas con Sebastián el 2026-09-16** (no reabrir):

1. **Dónde se captura `acr`/`amr`** → una sola vez, en `OidcAuthenticationSuccessHandler` (login),
   guardado en `LocalUserPrincipal`/sesión — nunca releído por request. Un *step-up* exige un login
   nuevo. Ver §1.1.
2. **Mecanismo de composición** → decorador (`MfaAwareApplicationAdministratorValidator`) sobre la
   interfaz `PrincipalMustBeApplicationAdministratorValidator` ya inyectada por los 12 casos de uso
   administrativos — cero cambios en esos 12 archivos. Ver §1.2.
3. **Claim y valor exactos** → configurables por propiedad (`pdp.security.mfa.claim`,
   `pdp.security.mfa.accepted-values`), sin valor por defecto — se fijan cuando el realm de Keycloak
   tenga el flujo de MFA configurado, sin tocar código. Ver §7, nota sobre por qué el `record` no
   valida su ausencia al arrancar.
4. **Distinción HTTP "falta MFA" vs "no autorizado"** → excepción hermana de
   `NotAuthorizedToAdministerException`, mismo patrón (`BusinessRuleViolationException` → 400), code
   distinto (`MFA_REQUIRED`). Sin tocar `ApiErrorHandler`.

**Decidida por el planificador, con base explícita en el propio HU-024.md** (el borrador permite
que el planificador la resuelva y la declare, no la deja abierta):

5. **Disparo del step-up desde `securityBaseline-vue`** → fuera de alcance de esta historia. El
   código del PDP deja lista la distinción (`code: MFA_REQUIRED`); redirigir al flujo de Keycloak con
   el parámetro que fuerza el nivel de autenticación es trabajo de frontend, historia aparte — mismo
   criterio que HU-022 dejó fuera el hosting de Redis en Azure.

Sin ambigüedades pendientes de respuesta.
