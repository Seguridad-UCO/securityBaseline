# PLAN: HU-013 — El PDP valida la credencial de una aplicación

## Metadata

- **ID:** HU-013
- **Slice:** `applications` (existente), con `[M]` en `shared/port` y `shared/config`
- **Tipo:** Consulta (valida, no escribe)
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-013-validacion-credencial-aplicacion`
- **Fuentes:**
  - `pdp/docs/ai-harness/workspace/HU-013.md` (historia dictada por Sebastián, dependiente de HU-012)
  - Código real leído antes de planificar: `InternalAccessDecisionController.java` +
    `InternalSecurityConfiguration.java` (HU-003 — el canal `/internal/v1/**` ya protege con mTLS +
    evidencia JWT cualquier ruta bajo ese prefijo, sin tocar la cadena de seguridad para sumar una
    ruta nueva), `ApplicationOwnerLookupValidator`/`Impl` (patrón de "resolver por id, rechazar con
    una sola excepción si no existe"), `ApplicationRepository.java`, `CredentialHasher.java`,
    `SharedPortsConfiguration.java`, `ApplicationNotFoundException.java` + `ApplicationsMessages.java`
    (patrón de excepción + catálogo a espejar)
  - Decisiones ya fijadas por la propia `HU-013.md`: canal interno mTLS (mismo que HU-003), el
    endpoint recibe el secreto en texto plano, responde inválida sin distinguir "no existe" de
    "no coincide", y no migra el `pep/starter` (eso es una historia del lado del PEP)
- **Criterios de la línea base que toca:** 1, 2, 4, 9, 11, 12, 13, 14, 15, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — el canal interno ya está protegido; no hace falta tocar la cadena de seguridad

`InternalSecurityConfiguration` usa `securityMatcher("/internal/v1/**")` (comodín), así que
cualquier ruta nueva bajo ese prefijo hereda mTLS + evidencia JWT automáticamente. A diferencia de
HU-003, este endpoint **no necesita leer el sujeto del JWT de evidencia** — no hay "quién pregunta",
solo "esta aplicación con este secreto, ¿es válida?". Por eso el interactor no replica el patrón de
`currentEvidenceSubject()` de `InternalAccessDecisionInteractorImpl`: es más simple, mapea el path y
el cuerpo, y ya.

### Hallazgo 2 — "inválida" es una sola excepción para dos causas, no dos rutas de código

La propia historia pide no distinguir "la aplicación no existe" de "el secreto no coincide". Eso se
resuelve en una sola regla pura (`ApplicationCredentialMustBeValidRule`) que recibe un booleano ya
resuelto (`ApplicationCredentialValidity.valid()`) y lanza siempre la misma excepción
(`InvalidApplicationCredentialException`) si es `false` — el validador decide `valid` como
"la aplicación existe **y** el hash coincide" en un solo paso, nunca como dos condiciones que el
use case tendría que combinar con un `if`.

### Hallazgo 3 — `CredentialHasher` necesita `matches`, tal como HU-012 ya avisó

HU-012 dejó una nota explícita: *"HU-013 le añadirá `matches(...)` como `[M]` cuando exista quien
valide."* Ese momento es ahora. Añadir un segundo método obliga a quitar `@FunctionalInterface` del
puerto — sigue siendo la misma interfaz, con una operación más.

### Hallazgo 4 — no hace falta una entidad ni un puerto nuevos, solo una consulta más angosta

Ya existe `ApplicationRepository.findTenantIdById` (HU-003) para "¿existe, y de quién es?". Falta el
equivalente para el hash: `findCredentialHashById`. Igual que `findTenantIdById`, responde **solo**
el dato que la regla necesita — nunca el agregado completo — y una ausencia se traduce en
`Mono.empty()`, no en una excepción del puerto: la excepción la decide la regla, no el repositorio.

## 1. Resumen funcional

Un nuevo endpoint del canal interno (`POST /internal/v1/applications/{applicationId}/credential-validations`)
recibe un secreto en texto plano y responde con el `tenantId` de la aplicación si el secreto
coincide con el hash guardado (HU-012); si la aplicación no existe o el secreto no coincide, responde
el mismo error en ambos casos, sin distinguir la causa.

**No cubre:** migrar `pep/starter`/el PEP para que use este endpoint en vez de su propio
`integrations.json` (historia aparte, del lado del PEP), auditar las validaciones (se declara fuera
de alcance — `AccessAuditRepository` de HU-007 es para decisiones de autorización de usuario, no
para autenticación de aplicación; añadirlo aquí sería una responsabilidad nueva no pedida), ni
rotar la credencial (HU-014).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Credencial válida | El secreto correcto para una aplicación existente responde 200 con el `tenantId` de esa aplicación |
| 2 | Credencial inválida por secreto incorrecto | Responde el mismo error que el caso 3, sin distinguir |
| 3 | Credencial inválida por aplicación inexistente | Responde exactamente el mismo cuerpo/código que el caso 2 |
| 4 | Canal interno, no BFF | La ruta vive bajo `/internal/v1/**`; no requiere ni acepta un token de usuario final |
| 5 | Sin fuga de datos | La respuesta de éxito solo trae `tenantId` — nunca el hash, nunca el secreto, nunca otros campos de `Application` |

## 3. Reglas de negocio

| # | Regla | Dónde vive (VO / Rule) | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| C1 | La credencial (aplicación + secreto) debe ser válida — existe y el secreto coincide con su hash | `applications/domain/rule/ApplicationCredentialMustBeValidRule` (síncrona) | `ApplicationRepository.findCredentialHashById` + `CredentialHasher.matches` (resueltos por el validador) | `InvalidApplicationCredentialException` → 400 |

Ninguna otra regla nueva: no hay unicidad que verificar, no hay estado de tenant que consultar (a
diferencia del registro, aquí no importa si el tenant está activo — es una pregunta de identidad de
la aplicación, no de autorización).

## 4. Modelo de dominio afectado

### Value objects

Ninguno nuevo. El secreto en tránsito sigue siendo `String` (mismo criterio que
`RegisterApplicationRequest.description`: sin invariante de dominio propio, vive un instante).

### "Hecho ya resuelto" de la regla

| Registro | Vive en | Campos |
|---|---|---|
| `ApplicationCredentialValidity` | `applications/domain/rule/model/` | `ApplicationId applicationId, boolean valid` |

## 5. Persistencia

No se toca el esquema (`application` sigue `SCHEMALESS`, sin campos nuevos). Solo una consulta nueva
en el puerto existente:

- **Consulta nueva en el puerto:** `Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId)`
  — vacío si no existe, igual que `findTenantIdById`.

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| POST | `/internal/v1/applications/{applicationId}/credential-validations` | 200 | `{secret}` | `{tenantId}` (plano, sin `ApiResponse` — mismo criterio D6 de HU-003: es un canal máquina-a-máquina) |

- **Autorización:** canal interno — mTLS + evidencia JWT, heredado automáticamente de
  `InternalSecurityConfiguration` (comodín `/internal/v1/**`). Sin token de usuario final.
- **Errores esperados:** `InvalidApplicationCredentialException` → 400, idéntico para "no existe" y
  "no coincide". Ningún otro código de negocio nuevo.

## 7. SPEC — el contrato

### `applications/domain` — nuevos `[N]`

```java
// pdp/applications/domain/exception/InvalidApplicationCredentialException.java
public final class InvalidApplicationCredentialException extends co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException { }
// constructor(): sin argumentos — el mensaje es fijo, nunca revela la causa. code "INVALID_APPLICATION_CREDENTIAL".

// pdp/applications/domain/rule/ApplicationCredentialMustBeValidRule.java
public interface ApplicationCredentialMustBeValidRule
        extends co.edu.uco.seguridad.shared.contract.OperationWithoutResult<ApplicationCredentialValidity> { }

// pdp/applications/domain/rule/model/ApplicationCredentialValidity.java
public record ApplicationCredentialValidity(
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId, boolean valid) { }
```

### `applications/domain/message` — `[M]`, aditivo

```java
// ApplicationsMessages.java gana:
public static String invalidApplicationCredential() {
    // fijo, sin parámetros — nunca dice si "no existe" o "no coincide"
}
```

### `applications/application` — nuevos `[N]`

```java
// pdp/applications/application/primaryport/request/ValidateApplicationCredentialRequest.java
public record ValidateApplicationCredentialRequest(
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId, String secret) { }

// pdp/applications/application/usecase/ValidateApplicationCredentialUseCase.java
public interface ValidateApplicationCredentialUseCase
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<ValidateApplicationCredentialRequest,
                co.edu.uco.seguridad.pdp.commons.model.TenantId> { }
```

### Firmas modificadas `[M]` — el tester las aplica, el planificador no las toca

```java
// shared/port/CredentialHasher.java — deja de ser @FunctionalInterface
public interface CredentialHasher {
    String hash(String plaintext);
    boolean matches(String plaintext, String hash);
}

// applications/application/secondaryport/repository/ApplicationRepository.java — método nuevo, aditivo
Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId);
```

### `applications/infrastructure` — nuevos `[N]`

```java
// pdp/applications/infrastructure/adapter/primary/web/dto/request/raw/ValidateApplicationCredentialRawRequest.java
public record ValidateApplicationCredentialRawRequest(String applicationId, String secret) { }

// pdp/applications/infrastructure/adapter/primary/web/dto/response/ApplicationCredentialValidationWebResponse.java
public record ApplicationCredentialValidationWebResponse(String tenantId) { }
```

Controller (`InternalApplicationCredentialController`, package-private, `@RequestMapping
("/internal/v1/applications")`, un solo `@PostMapping("/{applicationId}/credential-validations")`
que arma `new ValidateApplicationCredentialRawRequest(applicationId, body.secret())` y delega al
interactor — mismo patrón de "el path completa el raw" que `ProfileAssignmentController.assign`),
interactor (`ValidateApplicationCredentialInteractor` + `Impl`, sin leer `SecurityContext` ni el JWT
de evidencia — ver Hallazgo 1) y mappers (`ValidateApplicationCredentialRequestMapper.toRequest(raw)`
parseando `applicationId` con `RequestFieldParser.parse("applicationId", raw.applicationId(),
ApplicationId::of)` y dejando `secret` tal cual; `ApplicationCredentialValidationResponseMapper
.toResponse(TenantId): ApplicationCredentialValidationWebResponse`): mismas firmas que sus espejos
en `authorization`/`profiles`, sustituyendo tipos — no se repiten letra por letra.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/
├── shared/
│   ├── port/CredentialHasher.java                                          [M] -- +matches(...); NO lo toca el planificador
│   ├── config/SharedPortsConfiguration.java                                [M] -- credentialHasher() implementa los 2 métodos; NO lo toca el planificador
│   └── message/RequiredArgumentMessages.java                               [M] -- +3 constantes (ver §11)
└── pdp/
    └── applications/
        ├── domain/
        │   ├── exception/InvalidApplicationCredentialException.java        [N]
        │   ├── message/ApplicationsMessages.java                           [M] -- +1 método, aditivo
        │   ├── rule/ApplicationCredentialMustBeValidRule.java              [N]
        │   ├── rule/impl/ApplicationCredentialMustBeValidRuleImpl.java     [N]
        │   └── rule/model/ApplicationCredentialValidity.java               [N]
        ├── application/
        │   ├── primaryport/request/ValidateApplicationCredentialRequest.java [N]
        │   ├── secondaryport/repository/ApplicationRepository.java         [M] -- +findCredentialHashById; NO lo toca el planificador
        │   ├── usecase/ValidateApplicationCredentialUseCase.java           [N]
        │   └── usecase/impl/ValidateApplicationCredentialUseCaseImpl.java  [N]
        └── infrastructure/
            ├── adapter/primary/web/controller/InternalApplicationCredentialController.java [N]
            ├── adapter/primary/web/dto/request/raw/ValidateApplicationCredentialRawRequest.java [N]
            ├── adapter/primary/web/dto/response/ApplicationCredentialValidationWebResponse.java [N]
            ├── adapter/primary/web/interactor/ValidateApplicationCredentialInteractor.java [N]
            ├── adapter/primary/web/interactor/impl/ValidateApplicationCredentialInteractorImpl.java [N]
            ├── adapter/primary/web/mapper/ValidateApplicationCredentialRequestMapper.java [N]
            ├── adapter/primary/web/mapper/ApplicationCredentialValidationResponseMapper.java [N]
            ├── adapter/secondary/persistence/repository/SurrealApplicationRepository.java [M] -- +findCredentialHashById; NO lo toca el planificador
            └── infrastructure/config/ApplicationsConfiguration.java        [M] -- +regla, +caso de uso, +interactor (el planificador SÍ puede
                                                                                    cablear esto: son beans para clases [N] nuevas, no acopladas
                                                                                    a ninguna firma [M] todavía sin aplicar)
```

> **Por qué esta vez sí se cablea parte del `[M]` de `ApplicationsConfiguration`.** A diferencia de
> HU-012 (donde el bean de `registerApplicationUseCase` dependía de un constructor que todavía no
> existía), aquí `ValidateApplicationCredentialUseCaseImpl` es una clase enteramente **nueva**: su
> constructor se define en este mismo plan y no cambia después. Cablear su `@Bean` no depende de que
> el tester aplique ningún `[M]` primero — por eso el planificador sí lo materializa en la FASE 5,
> igual que hizo con `ProfilesConfiguration` completa en HU-011.

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `applications` domain | `ApplicationCredentialMustBeValidRuleImplTests` (nueva) | `valid=true` no lanza; `valid=false` lanza `InvalidApplicationCredentialException` |
| `applications` application | `ValidateApplicationCredentialUseCaseImplTests` (nueva) | secreto correcto para aplicación existente devuelve su `tenantId`; secreto incorrecto lanza `InvalidApplicationCredentialException` (fake `CredentialHasher.matches` devuelve `false`); aplicación inexistente lanza la **misma** excepción (fake `findCredentialHashById` devuelve `Mono.empty()`, nunca se llega a invocar `matches` — poison-pill) |
| `applications` infrastructure | `ValidateApplicationCredentialRequestMapperTests` (nueva) | `applicationId` ausente → `MissingRequestFieldException`; mal formado → `MalformedRequestFieldException`; válido → `ApplicationId` correcto; `secret` pasa tal cual sin transformar |
| `applications` infrastructure | `ApplicationCredentialValidationResponseMapperTests` (nueva) | `toResponse` aplana el `TenantId` a `String` |
| `applications` infrastructure | `InternalApplicationCredentialControllerTests` (nueva) | delega al interactor con `applicationId` del path (no del cuerpo, si el cuerpo llegase a traerlo) y responde 200 con el cuerpo plano — mismo presupuesto que `ApplicationControllerTests` |
| `applications` infrastructure (E2E) | `InternalApplicationCredentialHttpTests` (nueva, `@SpringBootTest` + mTLS como `PdpTlsIntegrationTests`/`ApplicationHttpTests`) | registra una aplicación (reutilizando HU-012), valida con el secreto correcto → 200 + `tenantId` correcto; valida con un secreto incorrecto → 400 `INVALID_APPLICATION_CREDENTIAL`; valida contra un `applicationId` que no existe → mismo 400, mismo código |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-13 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ✅ Aprobado — ver `reportes/REPORTE-HU-013.md` | 2026-09-13 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes

Ninguna que bloquee — todas las decisiones de diseño que la propia `HU-013.md` marcaba como
necesarias ya venían resueltas en su propio texto (canal interno, secreto en claro como entrada,
error uniforme) o se resuelven por precedente directo de código ya existente (§0). Dos decisiones que
sí tomé sin preguntar, por ser de bajo riesgo y reversibles, quedan documentadas aquí para que
cualquiera pueda objetar antes de implementar:

1. **Auditoría fuera de alcance.** No se reutiliza `AccessAuditRepository` (HU-007): audita
   decisiones de autorización de usuario, no autenticación de aplicación. Si se quiere auditar esto,
   es una historia propia.
2. **Ruta del endpoint.** `POST /internal/v1/applications/{applicationId}/credential-validations` —
   sustantivo en plural sobre el resultado de la operación, mismo estilo que
   `/internal/v1/access-decisions` (HU-003).

Lista de constantes nuevas de `RequiredArgumentMessages`: `APPLICATION_CREDENTIAL_MUST_BE_VALID_RULE`,
`VALIDATE_APPLICATION_CREDENTIAL_USE_CASE`, `VALIDATE_APPLICATION_CREDENTIAL_INTERACTOR`.
