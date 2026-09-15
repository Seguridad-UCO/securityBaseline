# PLAN: Gatear perfiles — solo quien administra la aplicación define, compone y asigna perfiles

## Metadata

- **ID:** HU-019
- **Slice:** `profiles` (pierde dos escrituras), `assignments` (pierde dos escrituras) y `authorization` (las recibe, orquestadas) — mismo patrón que HU-016/HU-017/HU-018
- **Tipo:** Escritura
- **Fecha:** 2026-09-14
- **Rama sugerida:** `feature/HU-019-gatear-perfiles-administracion`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-019.md` (dictada), `ADR-023-application-administration-model.md`, código real: `profiles/*`, `assignments/*`, `authorization/*` (HU-016/017/018 como precedente exacto)
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 21, 22

## 1. Resumen funcional

`DefineProfileUseCase`, `AddRoleToProfileUseCase` (slice `profiles`) y `AssignProfileUseCase`,
`RevokeProfileAssignmentUseCase` (slice `assignments`) hoy están abiertos a cualquier usuario
autenticado del tenant. Esta historia cierra el mismo hueco que HU-016/HU-018, pero para perfiles —
necesaria porque un perfil es un paquete de roles: dejarlo sin gatear sería un atajo para esquivar el
gate que HU-018 ya puso sobre la asignación directa de roles.

**El hallazgo que preocupaba a `HU-019.md` (perfiles multi-aplicación) no aplica**: `Profile` tiene su
propio `RoleScope` (igual que `Role`), independiente de los roles que contiene — no hay ambigüedad de
"¿contra cuál aplicación se gatea?". El gate de `DefineProfile`/`AddRoleToProfile` es **condicional**
(como HU-016, vacío para alcance `TENANT`); el de `AssignProfile`/`RevokeProfileAssignment` es
**incondicional** (como HU-017/HU-018: `AssignProfileRequest`/`ProfileAssignment` siempre tienen
`applicationId`).

No cubre `roles`/`resources`/`assignments` de rol (ya cerradas), autoservicio de administradores
(HU-020) ni auditoría administrativa (HU-021).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Definir un perfil `APPLICATION` como administrador de esa aplicación | `201`, perfil creado |
| 2 | Definir un perfil `APPLICATION` **sin** ser administrador | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 3 | Definir un perfil `TENANT` sin ser administrador de ninguna aplicación | `201`, sin gate — cero regresión |
| 4 | Agregar un rol a un perfil `APPLICATION` como administrador de esa aplicación | `200`, rol agregado |
| 5 | Agregar un rol a un perfil `APPLICATION` **sin** ser administrador | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 6 | Agregar un rol a un perfil `TENANT` | `200`, sin gate |
| 7 | Asignar un perfil como administrador de la aplicación | `201`, asignado |
| 8 | Asignar un perfil **sin** ser administrador de esa aplicación | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 9 | Revocar una asignación de perfil como administrador de la aplicación dueña | `200`, revocada |
| 10 | Revocar una asignación de perfil **sin** ser administrador | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 11 | `GET /api/v1/profiles` (listar) | Sin cambios — no gateado |
| 12 | Perfil o asignación inexistente | Excepción de dominio existente, sin cambio, no enmascarada por el lookup |
| 13 | Suite completa | `verificar.ps1` en verde |

## 3. Reglas de negocio

Ninguna nueva: mismo mecanismo de HU-009.

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| R1 | Quien define un perfil `APPLICATION` debe administrar esa aplicación | `PrincipalMustBeApplicationAdministratorValidator` | `AuthorizeAdministrationUseCase` | `NotAuthorizedToAdministerException` → 400 |
| R2 | Quien agrega un rol a un perfil `APPLICATION` debe administrar la aplicación **dueña del perfil** | Ídem R1 | Ídem, más `ProfileApplicationLookupValidator` **[N]** | Ídem |
| R3 | Un perfil `TENANT` no se gatea (define/agrega rol) | Decisión de alcance, no una `Rule` — mismo criterio que HU-016 R3 | — | — |
| R4 | Quien asigna un perfil debe administrar la aplicación de la asignación | `PrincipalMustBeApplicationAdministratorValidator` | Ídem | Ídem |
| R5 | Quien revoca una asignación de perfil debe administrar la aplicación **de la asignación existente** | Ídem R4 | Ídem, más `ProfileAssignmentApplicationLookupValidator` **[N]** | Ídem |
| R6 | Asignar/revocar perfil siempre incondicional | `AssignProfileRequest`/`ProfileAssignment` siempre tienen `applicationId` — mismo criterio que HU-017/HU-018 | — | — |

## 4. Modelo de dominio afectado

Ninguna entidad ni value object nuevo. `Profile`, `ProfileAssignment` no cambian.

### DTOs nuevos

| DTO | Componentes | Vive en |
|---|---|---|
| `ProfileOwnershipQuery` | `ProfileId profileId, TenantId tenantId` | `profiles/application/primaryport/request/` |
| `ProfileAssignmentOwnershipQuery` | `ProfileAssignmentId profileAssignmentId, TenantId tenantId` | `assignments/application/primaryport/request/` |
| `AdministerProfileDefinitionRequest(Optional<AdministrationRequest>, DefineProfileRequest)` | condicional, espejo de `AdministerRoleDefinitionRequest` | `authorization/application/primaryport/request/` |
| `AdministerProfileRoleAdditionRequest(Optional<AdministrationRequest>, AddRoleToProfileRequest)` | condicional | ídem |
| `AdministerProfileAssignmentCreationRequest(AdministrationRequest, AssignProfileRequest)` | incondicional, espejo de `AdministerAssignmentCreationRequest` | ídem |
| `AdministerProfileAssignmentRevocationRequest(AdministrationRequest, RevokeProfileAssignmentRequest)` | incondicional | ídem |

## 5. Persistencia

Sin cambios. `ProfileApplicationLookupValidator` reutiliza `ProfileRepository.findByIdForTenant`;
`ProfileAssignmentApplicationLookupValidator` reutiliza `ProfileAssignmentRepository.findByIdForTenant`
— ambos ya existen.

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Observación |
|---|---|---|---|
| `POST` | `/api/v1/profiles` | `201` | Movido de `ProfileController` a `ProfileAdministrationController` (`authorization`) |
| `POST` | `/api/v1/profiles/{profileId}/roles` | `200` | Ídem |
| `GET` | `/api/v1/profiles` | `200` | Permanece en `profiles` |
| `POST` | `/api/v1/profiles/{profileId}/assignments` | `201` | Movido de `ProfileAssignmentController` a `ProfileAssignmentAdministrationController` (`authorization`) |
| `DELETE` | `/api/v1/profiles/{profileId}/assignments/{profileAssignmentId}` | `200` | Ídem |

**Dos controllers nuevos, no uno**, mismo criterio que el código real ya separa `ProfileController` de
`ProfileAssignmentController` (comentario de `ProfileAssignmentController`: "capacidad nueva, aditiva,
con su propio controller") — esta historia respeta esa misma separación en `authorization`.

- **Autorización:** la aplicación sale de `scope.applicationId()` (definir perfil/agregar rol,
  vía `ProfileApplicationLookupValidator` para agregar rol) o de `assignment.applicationId()`
  (asignar, directo) / `ProfileAssignmentApplicationLookupValidator` (revocar).

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/profiles/application/rule/validator/ProfileApplicationLookupValidator.java
/** Resuelve a qué aplicación pertenece un perfil, si a alguna — vacío para alcance TENANT.
 * Espejo exacto de RoleApplicationLookupValidator (HU-016), aplicado a Profile.scope(). */
public interface ProfileApplicationLookupValidator
        extends ReactiveOperation<ProfileOwnershipQuery, Optional<ApplicationId>> {
}
```

```java
// pdp/assignments/application/rule/validator/ProfileAssignmentApplicationLookupValidator.java
/** Resuelve a qué aplicación pertenece una asignación de perfil. Nunca vacío — espejo exacto de
 * AssignmentApplicationLookupValidator (HU-018), aplicado a ProfileAssignment. */
public interface ProfileAssignmentApplicationLookupValidator
        extends ReactiveOperation<ProfileAssignmentOwnershipQuery, ApplicationId> {
}
```

```java
// pdp/authorization/application/usecase/AdministerProfileDefinitionUseCase.java
public interface AdministerProfileDefinitionUseCase
        extends ReactiveOperation<AdministerProfileDefinitionRequest, ProfileResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerProfileRoleAdditionUseCase.java
public interface AdministerProfileRoleAdditionUseCase
        extends ReactiveOperation<AdministerProfileRoleAdditionRequest, ProfileResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerProfileAssignmentCreationUseCase.java
public interface AdministerProfileAssignmentCreationUseCase
        extends ReactiveOperation<AdministerProfileAssignmentCreationRequest, ProfileAssignmentResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerProfileAssignmentRevocationUseCase.java
public interface AdministerProfileAssignmentRevocationUseCase
        extends ReactiveOperation<AdministerProfileAssignmentRevocationRequest, Void> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/
// AdministerProfileDefinitionInteractor, AdministerProfileRoleAdditionInteractor,
// AdministerProfileAssignmentCreationInteractor, AdministerProfileAssignmentRevocationInteractor
// — misma forma que sus equivalentes de HU-016/018 (raw request de entrada, web response de salida).
```

> `DefineProfileRawRequest`, `AddRoleToProfileRawRequest`, `AssignProfileRawRequest`,
> `RevokeProfileAssignmentRawRequest` se mueven a `authorization`, misma forma exacta.

### Firmas de DTOs

```java
public record ProfileOwnershipQuery(ProfileId profileId, TenantId tenantId) { }
public record ProfileAssignmentOwnershipQuery(ProfileAssignmentId profileAssignmentId, TenantId tenantId) { }
public record AdministerProfileDefinitionRequest(Optional<AdministrationRequest> administration, DefineProfileRequest profile) { }
public record AdministerProfileRoleAdditionRequest(Optional<AdministrationRequest> administration, AddRoleToProfileRequest addition) { }
public record AdministerProfileAssignmentCreationRequest(AdministrationRequest administration, AssignProfileRequest assignment) { }
public record AdministerProfileAssignmentRevocationRequest(AdministrationRequest administration, RevokeProfileAssignmentRequest revocation) { }
```

Dos respuestas web nuevas en `authorization`, mismas formas que `ProfileWebResponse` y
`ProfileAssignmentWebResponse`: `ProfileAdministrationWebResponse`, `ProfileAssignmentAdministrationWebResponse`.

### Firmas nuevas en `RequiredArgumentMessages`

```java
public static final String PROFILE_APPLICATION_LOOKUP_VALIDATOR = "se requiere el validador de aplicación del perfil";
public static final String PROFILE_ASSIGNMENT_APPLICATION_LOOKUP_VALIDATOR = "se requiere el validador de aplicación de la asignación de perfil";
public static final String ADMINISTER_PROFILE_DEFINITION_USE_CASE = "se requiere el caso de uso de administración de definición de perfil";
public static final String ADMINISTER_PROFILE_ROLE_ADDITION_USE_CASE = "se requiere el caso de uso de administración de adición de rol a perfil";
public static final String ADMINISTER_PROFILE_ASSIGNMENT_CREATION_USE_CASE = "se requiere el caso de uso de administración de asignación de perfil";
public static final String ADMINISTER_PROFILE_ASSIGNMENT_REVOCATION_USE_CASE = "se requiere el caso de uso de administración de revocación de asignación de perfil";
public static final String DEFINE_PROFILE_REQUEST = "se requiere la solicitud de definición de perfil";
public static final String ADD_ROLE_TO_PROFILE_REQUEST = "se requiere la solicitud de adición de rol a perfil";
public static final String PROFILE_ASSIGNMENT_CREATION_REQUEST = "se requiere la solicitud de asignación de perfil";
public static final String PROFILE_ASSIGNMENT_REVOCATION_REQUEST = "se requiere la solicitud de revocación de asignación de perfil";
```

### Mensajes de dominio

`ProfilesMessages` puede perder los métodos que solo usaba el mapper movido (mismo criterio que
`RolesMessages` en HU-016) — el implementador confirma qué migra a `AuthorizationMessages`.

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado · `[D]` eliminado (movido, ver flecha).

```
pdp/profiles/
├── application/
│   ├── primaryport/request/ProfileOwnershipQuery.java                     [N]
│   └── rule/validator/
│       ├── ProfileApplicationLookupValidator.java                         [N]
│       └── impl/ProfileApplicationLookupValidatorImpl.java                [N]
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/ProfileController.java                              [M] quita define()/addRole(), deja list()
    │   ├── dto/request/raw/DefineProfileRawRequest.java                   [D] → authorization
    │   ├── dto/request/raw/AddRoleToProfileRawRequest.java                [D] → authorization
    │   ├── mapper/DefineProfileRequestMapper.java                         [D] → authorization
    │   ├── mapper/AddRoleToProfileRequestMapper.java                      [D] → authorization (si existe como mapper propio)
    │   ├── interactor/DefineProfileInteractor(+Impl).java                 [D] → authorization (renombrado)
    │   └── interactor/AddRoleToProfileInteractor(+Impl).java              [D] → authorization (renombrado)
    └── config/ProfilesConfiguration.java                                  [M] quita 2 beans de interactor, agrega 1 de validador

pdp/assignments/
├── application/
│   ├── primaryport/request/ProfileAssignmentOwnershipQuery.java           [N]
│   └── rule/validator/
│       ├── ProfileAssignmentApplicationLookupValidator.java               [N]
│       └── impl/ProfileAssignmentApplicationLookupValidatorImpl.java      [N]
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/ProfileAssignmentController.java                    [D] → authorization (sin lectura que conservar: sin GET propio)
    │   ├── dto/request/raw/AssignProfileRawRequest.java                   [D] → authorization
    │   ├── dto/request/raw/RevokeProfileAssignmentRawRequest.java         [D] → authorization
    │   ├── interactor/AssignProfileInteractor(+Impl).java                 [D] → authorization (renombrado)
    │   └── interactor/RevokeProfileAssignmentInteractor(+Impl).java       [D] → authorization (renombrado)
    └── config/AssignmentsConfiguration.java                               [M] quita 2 beans de interactor, agrega 1 de validador

pdp/authorization/
├── application/
│   ├── primaryport/request/ (4 records nuevos, §7)                        [N]
│   └── usecase/ (4 interfaces + 4 impls, §7)                              [N]
├── infrastructure/
│   ├── adapter/primary/web/
│   │   ├── controller/ProfileAdministrationController.java                [N] rutas de ProfileController
│   │   ├── controller/ProfileAssignmentAdministrationController.java      [N] rutas de ProfileAssignmentController
│   │   ├── dto/request/raw/ (4 raw requests movidos)                      [N]
│   │   ├── dto/response/ProfileAdministrationWebResponse.java             [N]
│   │   ├── dto/response/ProfileAssignmentAdministrationWebResponse.java   [N]
│   │   ├── mapper/ (movidos + 2 response mappers nuevos)                  [N]
│   │   └── interactor/ (4 interactors + 4 impls, §7)                      [N]
│   └── config/AuthorizationConfiguration.java                             [M] agrega 12 beans + registra 2 controllers
└── package-info.java                                                      [M] agrega "profiles :: usecase/model/dto", "assignments :: usecase/model/dto" si faltan

shared/message/RequiredArgumentMessages.java                                [M] agrega 10 constantes (§7)
```

**Trampa de Modulith:** verificar si `profiles` ya tiene `@NamedInterface` con consumidores (HU-011)
— si `authorization` es su **primer** consumidor externo, hace falta declarar las interfaces
nombradas nuevas (no solo agregar a `allowedDependencies`). A diferencia de `roles`/`assignments`
(ya consumidos por `authorization` desde HU-016/018), `profiles` puede necesitar este paso — el
implementador lo confirma compilando, tal como advierte el protocolo del planificador.

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `application` (`profiles`) | `ProfileApplicationLookupValidatorImplTests` | perfil `APPLICATION` → `Optional` presente; `TENANT` → vacío; inexistente → excepción |
| `application` (`assignments`) | `ProfileAssignmentApplicationLookupValidatorImplTests` | asignación existente → `ApplicationId`; inexistente → `ProfileAssignmentNotFoundException` |
| `application` (`authorization`) | `AdministerProfileDefinitionUseCaseImplTests` | 3 casos, espejo de `AdministerRoleDefinitionUseCaseImplTests` |
| `application` (`authorization`) | `AdministerProfileRoleAdditionUseCaseImplTests` | 3 casos, espejo de `AdministerResourceGrantUseCaseImplTests` |
| `application` (`authorization`) | `AdministerProfileAssignmentCreationUseCaseImplTests` | 2 casos, espejo de HU-018 |
| `application` (`authorization`) | `AdministerProfileAssignmentRevocationUseCaseImplTests` | 2 casos, espejo de HU-018 |
| `infrastructure` (`authorization`) | 4 `*InteractorImplTests` | resolución de `applicationId` según el caso (directo/lookup/condicional) |
| `infrastructure` (`authorization`) | `ProfileAdministrationControllerTests`, `ProfileAssignmentAdministrationControllerTests` | delegan, responden código esperado |
| `infrastructure` (`profiles`/`assignments`) | Controllers ajustados | ya no prueban las operaciones movidas |

Presupuesto total estimado: **24–28 pruebas** (el doble de HU-016/HU-018: cuatro operaciones, no dos).

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-14 |
| Contrato aprobado (gate 1) | ✅ Aprobado | 2026-09-14 |
| Pruebas en rojo | ✅ Confirmado (22 casos, `UnsupportedOperationException`) | 2026-09-14 |
| Implementación en verde | ✅ Verde (675 pruebas) | 2026-09-15 |
| Validación | ✅ APROBADO — ver `REPORTE-HU-019.md` | 2026-09-15 |
| Entrega (gate 2) | ⏳ Pendiente de confirmación para commit y push | |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. El hallazgo que `HU-019.md` señalaba como potencial bloqueante
(perfiles multi-aplicación) se descartó leyendo `Profile.java`: tiene su propio `RoleScope`, espejo
exacto de `Role` — no hay ambigüedad de contra cuál aplicación gatear.
