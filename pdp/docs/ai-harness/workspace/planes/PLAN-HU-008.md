# PLAN: HU-008 — Identidad de usuario en el canal BFF, para que `subject.roles` llegue a OPA

## Metadata

- **ID:** HU-008
- **Slice:** `authorization` (existente), con cambios en `shared/security`, `roles`, `assignments`
- **Tipo:** Escritura de orquestación (ningún endpoint nuevo, ninguna tabla nueva)
- **Fecha:** 2026-09-12
- **Rama sugerida:** `feature/HU-008-identidad-bff-roles-opa`
- **Fuentes:**
    - `PLAN-HU-006.md` §0 — el hallazgo que abrió esta historia ("HU-008 — identidad de usuario final
      en el canal interno", ahora acotada al canal BFF, ver §0 abajo)
    - Código real: `PdpPrincipal`, `SecurityContext`, `LocalUserPrincipal`, `AccessRequest`,
      `AuthorizeUseCaseImpl`, `AuthorizeRequestMapper`, `AuthorizeInteractorImpl`,
      `ResolveActiveRolesUseCase`/`ActiveRolesResponse` (ya existían, ver hallazgo 2 de §0),
      `RoleRepository.findById`, `OpaSubject`, `OpaPolicyDecisionAdapter`
- **Criterios de la línea base que toca:** 1, 2, 4, 9, 11, 12, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — el alcance real es solo el canal BFF, no "identidad de usuario" en general

`PLAN-HU-006.md` dejó dos huecos: el canal BFF (`subject` es el `sub` crudo de Keycloak) y el canal
interno del PEP (`subject` es el JWT de *evidencia* que autentica al PEP como llamador — el
`SolicitudAcceso` de `contracts/pep-pdp/v1/request.schema.json` no tiene campo de usuario final en
absoluto). El segundo requiere cambiar ese contrato — es trabajo de otra persona (el PEP), no algo
que el PDP resuelva solo. **Esta historia resuelve solo el canal BFF.** El canal interno sigue
enviando `subject.roles` ausente a OPA, exactamente como hoy — no es una regresión, es el mismo
comportamiento, documentado como fuera de alcance.

### Hallazgo 2 — la mitad del trabajo ya existía, sin usar

`ResolveActiveRolesUseCase`/`ActiveRolesResponse` (HU-005) tienen el javadoc literal: *"Sin
interactor ni controller en esta historia: lo invocará HU-006 en proceso"* / *"El contexto resuelto
que HU-006 enviará a OPA"*. HU-006 nunca lo conectó — ese fue el hueco real, no que faltara el caso
de uso. Esta historia lo cablea, no lo reconstruye.

### Hallazgo 3 — el `UserId` real ya viaja en la sesión, y se estaba descartando

`ProvisionIdentityUseCaseImpl.toPrincipal(...)` ya arma `LocalUserPrincipal(userId, subject,
tenantId, email, name)` con el `UserId` interno resuelto en el primer login — pero
`SecurityContext.currentPrincipal()` lo mete en el campo `tokenId` de `PdpPrincipal`
(`new PdpPrincipal(user.tenantId(), user.subject(), user.userId())` — el tercer argumento posicional
es `tokenId`, no `userId`), y `tokenId` está marcado en su propio javadoc como *"no se usa todavía"*.
El dato correcto ya existe en cada petición autenticada por sesión; solo hay que dejar de tirarlo.
**No hace falta resolver `issuer`+`subject` contra `SecurityUserRepository.findIdentity(...)` en esta
historia** — ese camino (que sí necesitaría el `issuer`, ausente hoy en `PdpPrincipal`) queda sin
usar porque el dato ya resuelto está más cerca.

### Hallazgo 4 — un rol borrado no debe tumbar la autorización

Si un usuario tiene una asignación vigente pero el rol fue borrado (no hay caso de uso que borre
roles hoy, pero el puerto no lo impide a futuro), la resolución de nombres debe **omitir** ese id,
no fallar toda la petición. No es una `Rule` que rechaza — es un filtro de un dato de enriquecimiento
que, si falta, hace la política más restrictiva (menos roles → menos probable un `ALLOW`), nunca
menos segura.

## 1. Resumen funcional

Cuatro piezas encadenadas:

1. `PdpPrincipal` gana un campo `Optional<UserId> userId` — poblado desde `LocalUserPrincipal`
   (canal BFF), vacío para `Jwt`/`OidcUser` (los casos transitorios documentados en su propio
   javadoc como "compatibilidad temporal").
2. `AccessRequest` gana `Optional<UserId> subjectUserId` (lo pone el mapper, viene del principal) y
   `Set<String> subjectRoles` (lo llena `AuthorizeUseCaseImpl`, arranca vacío).
3. `AuthorizeUseCaseImpl`, después de validar aplicación y recurso, resuelve los roles activos del
   usuario en esa aplicación — vía un nuevo validador de `authorization` que orquesta
   `assignments.ResolveActiveRolesUseCase` (ya existente) y un nuevo validador que `roles` publica
   para traducir `RoleId` → nombre — y enriquece el `AccessRequest` antes de llamar a
   `PolicyDecisionPort`.
4. `OpaPolicyDecisionAdapter` copia `subjectRoles` al campo `roles` de `OpaSubject`, que hasta hoy
   no existía.

**No cubre:** el canal interno (PEP) — sigue mandando `subject.roles` ausente, sin cambios. No
publica ninguna política Rego real (sigue siendo trabajo de quien escriba
`policies/applications/*.rego`, HU-006 §0). No toca `profiles` ni `entitlements` — solo `roles`, que
es lo único que el catálogo del PDP modela hoy (HU-004/HU-005).

## 2. Criterios de aceptación

| # | Criterio                                                                                                                                                                                    | Resultado esperado                                                               |
|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| 1 | Un login por el canal BFF deja `PdpPrincipal.userId()` presente                                                                                                                             | Prueba directa sobre `SecurityContext`/el mapeo desde `LocalUserPrincipal`       |
| 2 | `AuthorizeUseCaseImpl`, con un usuario que tiene un rol vigente en esa aplicación, llama a `PolicyDecisionPort.execute` con `AccessRequest.subjectRoles()` conteniendo el nombre de ese rol | `AuthorizeUseCaseImplTests` con fakes de `ActiveRoleNamesLookupValidator`        |
| 3 | Sin `subjectUserId` (canal interno, o BFF sin sesión resuelta), `subjectRoles` queda vacío y no se intenta ninguna resolución                                                               | `AuthorizeUseCaseImplTests` — poison pill sobre `ActiveRoleNamesLookupValidator` |
| 4 | Un rol activo que ya no existe en el catálogo se omite del resultado, sin error                                                                                                             | `RoleNamesLookupValidatorImplTests`                                              |
| 5 | `OpaPolicyDecisionAdapter` envía `subject.roles` con los nombres resueltos                                                                                                                  | `OpaPolicyDecisionAdapterTests` — inspecciona el cuerpo POST capturado           |
| 6 | El canal interno (`EvaluateInternalAccessUseCaseImpl`) sigue construyendo `AccessRequest` sin `subjectUserId`, comportamiento idéntico a hoy                                                | Prueba existente de ese caso de uso, sin cambios de expectativa                  |
| 7 | `AuthorizationHttpTests`/`InternalSecurityChainIntegrationTests` (e2e) siguen en verde sin cambiar sus aserciones                                                                           | Regresión — ver sección 9                                                        |

## 3. Reglas de negocio

Ninguna regla nueva de dominio. Todo lo nuevo son **validadores de enriquecimiento** (consultan y
traducen, no rechazan) — misma categoría que `ApplicationOwnerLookupValidator` de HU-003.

| # | Regla           | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|-----------------|------------|-------------------------|------------------|
| — | (ninguna nueva) | —          | —                       | —                |

## 4. Modelo de dominio afectado

Ninguno nuevo. `AccessRequest`, `PdpPrincipal` son contratos existentes que ganan componentes
(marcados `[M]` en la sección 7 — no son value objects de dominio, son DTOs de aplicación/seguridad).

## 5. Persistencia

No aplica — sin sección 5. No hay tabla ni columna nueva.

## 6. Endpoint

No aplica — sin endpoint nuevo. `POST /api/v1/authorize` no cambia su contrato HTTP de entrada ni
de salida.

## 7. SPEC — el contrato

### Firmas nuevas en `roles` — `application/rule/validator/` [N]

```java
// pdp/roles/application/rule/validator/RoleNamesLookupValidator.java
public interface RoleNamesLookupValidator extends ReactiveOperation<Set<RoleId>, Set<String>> {
}
```

> Nota para el implementador (no es lógica, es el contrato ya decidido): por cada `RoleId`,
> `RoleRepository.findById(...)`; si no existe, se omite (hallazgo 4 — nunca un error). Devuelve los
> `RoleName.value()` de los que sí existen. Publicado bajo la interfaz nombrada `"rule"` que `roles`
> ya tiene (misma que `RoleScopeMustCoverApplicationValidator`) — no hace falta una nueva.

### Firmas nuevas en `assignments` — primer `@NamedInterface` del módulo [N]

```java
// pdp/assignments/application/usecase/package-info.java
@org.springframework.modulith.NamedInterface("usecase")
package co.edu.uco.seguridad.pdp.assignments.application.usecase;

// pdp/assignments/application/primaryport/request/package-info.java
@org.springframework.modulith.NamedInterface("dto")
package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

// pdp/assignments/application/primaryport/response/package-info.java
@org.springframework.modulith.NamedInterface("dto")
package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;
```

> **Trampa de Modulith, primera vez que `assignments` publica algo** (`1-planificador.md`, FASE 5):
> hasta hoy nadie consume `assignments` desde fuera, así que no tiene ningún `@NamedInterface`. En
> cuanto declare estos tres, Modulith deja de exponer implícitamente el resto de sus paquetes por el
> nombre plano `"assignments"` — si algo más necesitara cruzar, hay que nombrarlo explícito. Hoy solo
> `authorization` consume `assignments :: usecase` y `assignments :: dto`, así que basta con esos dos
> en su `allowedDependencies` (sección 8).

### Firmas nuevas en `authorization` — `application/rule/validator/` [N]

```java
// pdp/authorization/application/rule/validator/ActiveRoleNamesLookupValidator.java
public interface ActiveRoleNamesLookupValidator
        extends ReactiveOperation<co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest, Set<String>> {
}
```

> Nota para el implementador: orquesta `assignments.ResolveActiveRolesUseCase.execute(input)` →
> `.map(ActiveRolesResponse::roleIds)` → `roles.RoleNamesLookupValidator.execute(roleIds)`. Reutiliza
> `ResolveActiveRolesRequest` (ya publicado por `assignments`, ver arriba) como su propia entrada en
> vez de inventar un DTO — un mismo dato, un solo tipo.

### Firmas modificadas [M]

```java
// shared/security/PdpPrincipal.java — nuevo componente al final
public record PdpPrincipal(TenantId tenantId, String subject, String tokenId, Optional<UserId> userId) { }
// from(Jwt) y from(OidcUser): Optional.empty(). El caso LocalUserPrincipal en SecurityContext
// pasa Optional.of(UserId.of(user.userId())) — user.userId() ya es el UUID interno (hallazgo 3).
```

```java
// pdp/authorization/application/primaryport/request/AccessRequest.java — dos componentes nuevos
public record AccessRequest(TenantId tenantId, String subject, ApplicationId applicationId,
        ResourcePath resourcePath, HttpVerb action, String requestId, String correlationId,
        Optional<UserId> subjectUserId, Set<String> subjectRoles) {
    // + método de instancia:
    public AccessRequest withSubjectRoles(Set<String> roles) { /* nuevo AccessRequest con ese campo reemplazado */ }
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/mapper/AuthorizeRequestMapper.java
// un parametro nuevo al final; subjectRoles arranca en Set.of()
public static AccessRequest toRequest(AuthorizeRawRequest raw, TenantId tenantId, String subject,
        String requestId, String correlationId, java.util.Optional<UserId> subjectUserId) { ... }
```

```java
// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaSubject.java
public record OpaSubject(String id, String type, String tenantId, java.util.List<String> roles) {
}
```

## 8. Árbol de archivos

```
pdp/roles/application/rule/validator/
├── RoleNamesLookupValidator.java                                    [N]
└── impl/RoleNamesLookupValidatorImpl.java                           [N]
pdp/roles/infrastructure/config/RolesConfiguration.java              [M] — nuevo @Bean

pdp/assignments/application/usecase/package-info.java                [N] — @NamedInterface("usecase")
pdp/assignments/application/primaryport/request/package-info.java    [N] — @NamedInterface("dto")
pdp/assignments/application/primaryport/response/package-info.java   [N] — @NamedInterface("dto")

pdp/authorization/application/rule/validator/
├── ActiveRoleNamesLookupValidator.java                              [N]
└── impl/ActiveRoleNamesLookupValidatorImpl.java                     [N]
pdp/authorization/application/primaryport/request/AccessRequest.java [M] — +subjectUserId, +subjectRoles, +withSubjectRoles(...)
pdp/authorization/application/usecase/impl/AuthorizeUseCaseImpl.java [M] — +dependencia, +paso de resolución
pdp/authorization/application/usecase/impl/EvaluateInternalAccessUseCaseImpl.java [M] — construye AccessRequest con Optional.empty()/Set.of()
pdp/authorization/infrastructure/adapter/primary/web/mapper/AuthorizeRequestMapper.java [M] — +parámetro
pdp/authorization/infrastructure/adapter/primary/web/interactor/impl/AuthorizeInteractorImpl.java [M] — pasa principal.userId()
pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaSubject.java        [M] — +roles
pdp/authorization/infrastructure/adapter/secondary/policy/OpaPolicyDecisionAdapter.java [M] — copia subjectRoles → OpaSubject.roles
pdp/authorization/infrastructure/config/AuthorizationConfiguration.java              [M] — nuevo @Bean, nueva dependencia en authorizeUseCase(...)
pdp/authorization/package-info.java                                                 [M] — +"assignments :: usecase", +"assignments :: dto", +"roles :: rule"

shared/security/PdpPrincipal.java                                    [M] — +userId
shared/security/SecurityContext.java                                 [M] — resuelve userId para LocalUserPrincipal
shared/message/RequiredArgumentMessages.java                         [M] — constantes nuevas (SUBJECT_ROLES, ROLE_NAMES_LOOKUP_VALIDATOR, ACTIVE_ROLE_NAMES_LOOKUP_VALIDATOR, PRINCIPAL_USER_ID)
```

## 9. Casos de prueba esperados

> `AccessRequest` y `PdpPrincipal` son contratos muy consumidos: **todo test existente que los
> construya con `new` deja de compilar** hasta que el tester le agregue los componentes nuevos. Es
> intencional (ver `sb-testing`, "al cambiar la firma de un puerto") — no son casos nuevos que
> inventar, son los mismos tests con dos argumentos más.

| Capa                                   | Clase de prueba                                                             | Casos                                                                                                                                                                                                                                                                                                 |
|----------------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| application (roles)                    | `RoleNamesLookupValidatorImplTests`                                         | resuelve nombres de un conjunto de ids existentes; omite un id que no existe sin lanzar; conjunto vacío → conjunto vacío                                                                                                                                                                              |
| application (authorization)            | `ActiveRoleNamesLookupValidatorImplTests`                                   | delega en `ResolveActiveRolesUseCase` y `RoleNamesLookupValidator` en cadena; conjunto vacío de `assignments` → conjunto vacío de nombres                                                                                                                                                             |
| application (authorization, existente) | `AuthorizeUseCaseImplTests`                                                 | camino feliz ahora arma `AccessRequest` con `subjectRoles` no vacío cuando `subjectUserId` está presente; con `subjectUserId` ausente, poison pill sobre `rolesLookup` (nunca se llama); los casos de rechazo ya existentes (aplicación/recurso) siguen sin alcanzar `rolesLookup` — otro poison pill |
| infrastructure (adaptador, existente)  | `OpaPolicyDecisionAdapterTests`                                             | el caso "sends the input wrapped..." ahora también afirma `input.subject.roles` con los nombres del `AccessRequest` de prueba                                                                                                                                                                         |
| infrastructure (mapper)                | `AuthorizeRequestMapperTests` (si no existe, créala; si existe, extiéndela) | `subjectUserId` se propaga tal cual al `AccessRequest`                                                                                                                                                                                                                                                |
| shared (seguridad)                     | `SecurityContextTests`/equivalente                                          | `LocalUserPrincipal` → `PdpPrincipal.userId()` presente con el valor correcto; `Jwt`/`OidcUser` → ausente                                                                                                                                                                                             |
| e2e (regresión, existente)             | `AuthorizationHttpTests`, `InternalSecurityChainIntegrationTests`           | sin casos nuevos — deben seguir en verde con las mismas aserciones; confirman que enriquecer con roles no cambia el resultado cuando OPA sigue sin política (`DENY`/`NO_APPLICABLE_POLICY`)                                                                                                           |

Presupuesto estimado: **12-16 pruebas nuevas**, más los ajustes mecánicos de firma en los archivos
existentes que ya construían `AccessRequest`/`PdpPrincipal` a mano.

## 10. Trazabilidad

| Fase                       | Estado                                                                         | Fecha      |
|----------------------------|--------------------------------------------------------------------------------|------------|
| Plan                       | ✅ Generado                                                                     | 2026-09-12 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                                                     | 2026-09-12 |
| Pruebas en rojo            | ✅ Rojo confirmado (478 pruebas, 5 errores por `UnsupportedOperationException`) | 2026-09-12 |
| Implementación en verde    | ✅ Verde (478 pruebas, 0 fallos)                                                | 2026-09-12 |
| Validación                 | ✅ APROBADO                                                                     | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                                                                    |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee. Una decisión de alcance documentada en §0: el canal interno (PEP) queda
explícitamente fuera — requiere un cambio de contrato con `contracts/pep-pdp/v1/` que no se puede
decidir unilateralmente desde el PDP. Cuando esa historia exista (necesitaría acordarse con el
dueño del PEP), reutilizará el mismo `ActiveRoleNamesLookupValidator` que esta historia ya deja
publicado — no hay que repetir esa parte.
