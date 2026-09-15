# PLAN: Gatear asignaciones de rol — solo quien administra la aplicación asigna o revoca

## Metadata

- **ID:** HU-018
- **Slice:** `assignments` (pierde dos escrituras) y `authorization` (las recibe, orquestadas) — mismo patrón que HU-016/HU-017
- **Tipo:** Escritura
- **Fecha:** 2026-09-14
- **Rama sugerida:** `feature/HU-018-gatear-asignaciones-administracion`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-018.md` (dictada), `ADR-023-application-administration-model.md`, código real: `assignments/*`, `authorization/*` (HU-016/HU-017 como precedente exacto de cableado)
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 21, 22 — sin 13/14/20 nuevos (se mueven DTOs existentes, no se crean formas nuevas), sin 5/6 nuevos (rutas y códigos de éxito sin cambio)

## 1. Resumen funcional

`AssignRoleUseCase` y `RevokeAssignmentUseCase` (slice `assignments`) hoy están abiertos a cualquier
usuario autenticado del tenant. Esta historia exige que quien asigna o revoca un rol sea
administrador de la aplicación involucrada — mismo mecanismo de HU-009, mismo cableado que HU-016.

A diferencia de HU-016 (donde `DefineRole` admite un rol `TENANT` sin aplicación, y por eso el gate
es condicional), **aquí el gate es siempre incondicional**: `AssignRoleRequest.applicationId()` es
obligatorio desde que el agregado se envió (HU-005) — toda asignación pertenece a una aplicación, sin
excepción. Mismo argumento que HU-017 usó para `RegisterProtectedResourceUseCase`.

No cubre `roles`/`resources` (ya cerradas, HU-016/HU-017), `profiles` (HU-019),
`AssignApplicationAdministratorUseCase` (operación distinta, ya existe desde HU-015, esta historia no
la toca), autoservicio de administradores (HU-020) ni auditoría administrativa (HU-021).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Asignar un rol como administrador de la aplicación | `201`, asignación creada, igual que hoy |
| 2 | Asignar un rol **sin** ser administrador de esa aplicación | `400 NOT_AUTHORIZED_TO_ADMINISTER`, la asignación **no** se crea |
| 3 | Revocar una asignación como administrador de la aplicación dueña de la asignación | `200`, revocada, igual que hoy |
| 4 | Revocar una asignación **sin** ser administrador de esa aplicación | `400 NOT_AUTHORIZED_TO_ADMINISTER`, la asignación **no** se revoca |
| 5 | `GET /api/v1/roles/{roleId}/assignments` (listar) | Sin cambios — no gateado, sigue en `assignments` |
| 6 | Revocar una asignación inexistente | `AssignmentNotFoundException`, igual que hoy — el lookup de aplicación no lo enmascara |
| 7 | `AssignApplicationAdministratorUseCase` (asignar/quitar administrador, HU-015) | Sin cambios — no es esta historia |
| 8 | Suite completa | `verificar.ps1` en verde |

## 3. Reglas de negocio

Ninguna regla nueva: se reutiliza el mecanismo de HU-009 vía
`PrincipalMustBeApplicationAdministratorValidator`, ya implementado y probado.

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| R1 | Quien asigna un rol debe administrar la aplicación de la asignación | Reutiliza `PrincipalMustBeApplicationAdministratorValidator` | `AuthorizeAdministrationUseCase` | `NotAuthorizedToAdministerException` → 400 |
| R2 | Quien revoca una asignación debe administrar la aplicación **de la asignación existente** | Ídem R1 | Ídem, más `AssignmentApplicationLookupValidator` **[N]** para resolver el `applicationId` de la asignación | Ídem |
| R3 | Gate siempre incondicional | Decisión de alcance: `AssignRoleRequest`/toda `Assignment` tiene `applicationId` obligatorio — no hay caso "sin aplicación" que excluir, a diferencia de HU-016 | — | — |

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguno nuevo. `Assignment` no cambia.

### Value objects

Ninguno nuevo. Se reutilizan `ApplicationId`, `TenantId`, `UserId`, `RoleId`, `AssignmentId`.

### DTOs nuevos

| DTO | Componentes | Invariantes | Vive en |
|---|---|---|---|
| `AssignmentOwnershipQuery` | `AssignmentId assignmentId, TenantId tenantId` | Ambos `requireNonNull` — mismo patrón que `RoleOwnershipQuery` | `assignments/application/primaryport/request/` |
| `AdministerAssignmentCreationRequest` | `AdministrationRequest administration, AssignRoleRequest assignment` | Ambos `requireNonNull` — **sin `Optional`**, mismo criterio que HU-017 (siempre hay aplicación) | `authorization/application/primaryport/request/` |
| `AdministerAssignmentRevocationRequest` | `AdministrationRequest administration, RevokeAssignmentRequest revocation` | Ídem | `authorization/application/primaryport/request/` |

## 5. Persistencia

Sin cambios. `AssignmentApplicationLookupValidator` reutiliza `AssignmentRepository.findByIdForTenant`,
que ya existe.

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| `POST` | `/api/v1/roles/{roleId}/assignments` | `201` (sin cambio) | `AssignRoleRawRequest` (sin cambio de forma) | `AssignmentAdministrationWebResponse` (misma forma que `AssignmentWebResponse`, nueva clase — mismo criterio que HU-016/HU-017: no reutiliza el nombre del DTO de la respuesta de la otra clase para no acoplar módulos por el nombre de una clase) |
| `DELETE` | `/api/v1/roles/{roleId}/assignments/{assignmentId}` | `200` (sin cambio) | — | — |
| `GET` | `/api/v1/roles/{roleId}/assignments` | `200` (sin cambio) | — | — (permanece en `assignments`, sin tocar) |

**Las dos rutas de escritura se mueven de `AssignmentController` (`assignments`) a un nuevo
`AssignmentAdministrationController` (`authorization`).** Mismo razonamiento de Modulith que
HU-016/HU-017: `authorization` ya depende de `assignments`; `assignments` no puede depender de
`authorization` sin ciclo.

- **Autorización:** requiere token. El inquilino sale del principal. La aplicación contra la que se
  gatea sale de `AssignRoleRequest.applicationId()` (creación, directo del cuerpo) o de
  `AssignmentApplicationLookupValidator` (revocación, resuelto de la asignación existente).
- **Errores esperados:** `NotAuthorizedToAdministerException` → 400 (ya existe);
  `AssignmentNotFoundException` → 400-equivalente (ya existe, sin cambio); el resto de rechazos de
  `AssignRoleRulesValidator`/`RevokeAssignmentRulesValidator` no cambian.

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/assignments/application/rule/validator/AssignmentApplicationLookupValidator.java
/**
 * Resuelve a qué aplicación pertenece una asignación (HU-018). A diferencia de
 * RoleApplicationLookupValidator (HU-016), nunca vacío: Assignment.applicationId es obligatorio
 * desde HU-005 (ver ADR-024, hallazgo de por qué "administrador global" no es representable hoy).
 * Rechaza con AssignmentNotFoundException si la asignación no existe para ese inquilino.
 */
public interface AssignmentApplicationLookupValidator
        extends ReactiveOperation<AssignmentOwnershipQuery, ApplicationId> {
}
```

```java
// pdp/authorization/application/usecase/AdministerAssignmentCreationUseCase.java
public interface AdministerAssignmentCreationUseCase
        extends ReactiveOperation<AdministerAssignmentCreationRequest, AssignmentResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerAssignmentRevocationUseCase.java
public interface AdministerAssignmentRevocationUseCase
        extends ReactiveOperation<AdministerAssignmentRevocationRequest, Void> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/AdministerAssignmentCreationInteractor.java
public interface AdministerAssignmentCreationInteractor
        extends ReactiveOperation<AssignRoleRawRequest, AssignmentAdministrationWebResponse> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/AdministerAssignmentRevocationInteractor.java
public interface AdministerAssignmentRevocationInteractor
        extends ReactiveOperation<RevokeAssignmentRawRequest, Void> {
}
```

> `AssignRoleRawRequest` y `RevokeAssignmentRawRequest` **se mueven** de
> `assignments/infrastructure/adapter/primary/web/dto/request/raw/` a la misma ruta relativa dentro
> de `authorization` — misma forma exacta, solo cambia el paquete (precedente HU-016).

### Firmas de DTOs y validador

```java
// pdp/assignments/application/primaryport/request/AssignmentOwnershipQuery.java
public record AssignmentOwnershipQuery(AssignmentId assignmentId, TenantId tenantId) { }
```

```java
// pdp/authorization/application/primaryport/request/AdministerAssignmentCreationRequest.java
public record AdministerAssignmentCreationRequest(AdministrationRequest administration, AssignRoleRequest assignment) { }
```

```java
// pdp/authorization/application/primaryport/request/AdministerAssignmentRevocationRequest.java
public record AdministerAssignmentRevocationRequest(AdministrationRequest administration, RevokeAssignmentRequest revocation) { }
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/response/AssignmentAdministrationWebResponse.java
public record AssignmentAdministrationWebResponse(String id, String userId, String tenantId,
        String applicationId, String roleId, String validFrom, String validUntil) { }
// misma forma exacta que assignments/.../AssignmentWebResponse
```

### Firmas nuevas en `RequiredArgumentMessages`

```java
public static final String ASSIGNMENT_APPLICATION_LOOKUP_VALIDATOR = "se requiere el validador de aplicación de la asignación";
public static final String ADMINISTER_ASSIGNMENT_CREATION_USE_CASE = "se requiere el caso de uso de administración de creación de asignación";
public static final String ADMINISTER_ASSIGNMENT_REVOCATION_USE_CASE = "se requiere el caso de uso de administración de revocación de asignación";
public static final String ASSIGNMENT_CREATION_REQUEST = "se requiere la solicitud de asignación de rol";
public static final String ASSIGNMENT_REVOCATION_REQUEST = "se requiere la solicitud de revocación de asignación";
```

`ADMINISTRATION_REQUEST` ya existe (HU-017) — se reutiliza tal cual, sin agregar una constante nueva.

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado · `[D]` eliminado (movido a otra ruta, ver flecha).

```
pdp/assignments/
├── application/
│   ├── primaryport/request/
│   │   └── AssignmentOwnershipQuery.java                                   [N]
│   └── rule/validator/
│       ├── AssignmentApplicationLookupValidator.java                      [N]
│       └── impl/AssignmentApplicationLookupValidatorImpl.java             [N]
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/AssignmentController.java                           [M] quita assign()/revoke(), deja list()
    │   ├── dto/request/raw/AssignRoleRawRequest.java                      [D] → authorization (misma forma)
    │   ├── dto/request/raw/RevokeAssignmentRawRequest.java                [D] → authorization (misma forma)
    │   ├── mapper/AssignRoleRequestMapper.java                            [D] → authorization (sin cambios de lógica)
    │   ├── interactor/AssignRoleInteractor.java                           [D] → authorization (renombrado)
    │   ├── interactor/impl/AssignRoleInteractorImpl.java                  [D] → authorization (renombrado, lógica nueva)
    │   ├── interactor/RevokeAssignmentInteractor.java                     [D] → authorization (renombrado)
    │   └── interactor/impl/RevokeAssignmentInteractorImpl.java            [D] → authorization (renombrado, lógica nueva)
    └── config/AssignmentsConfiguration.java                               [M] quita 2 beans de interactor, agrega 1 bean de validador

pdp/authorization/
├── application/
│   ├── primaryport/request/
│   │   ├── AdministerAssignmentCreationRequest.java                       [N]
│   │   └── AdministerAssignmentRevocationRequest.java                     [N]
│   └── usecase/
│       ├── AdministerAssignmentCreationUseCase.java                       [N]
│       ├── AdministerAssignmentRevocationUseCase.java                     [N]
│       └── impl/
│           ├── AdministerAssignmentCreationUseCaseImpl.java               [N]
│           └── AdministerAssignmentRevocationUseCaseImpl.java             [N]
├── infrastructure/
│   ├── adapter/primary/web/
│   │   ├── controller/AssignmentAdministrationController.java             [N] rutas movidas de AssignmentController
│   │   ├── dto/request/raw/AssignRoleRawRequest.java                      [N] ← assignments (misma forma)
│   │   ├── dto/request/raw/RevokeAssignmentRawRequest.java                [N] ← assignments (misma forma)
│   │   ├── dto/response/AssignmentAdministrationWebResponse.java          [N]
│   │   ├── mapper/AssignRoleRequestMapper.java                            [N] ← assignments (sin cambios)
│   │   ├── mapper/AssignmentAdministrationResponseMapper.java             [N]
│   │   ├── interactor/AdministerAssignmentCreationInteractor.java         [N] ← AssignRoleInteractor
│   │   ├── interactor/impl/AdministerAssignmentCreationInteractorImpl.java [N]
│   │   ├── interactor/AdministerAssignmentRevocationInteractor.java       [N] ← RevokeAssignmentInteractor
│   │   └── interactor/impl/AdministerAssignmentRevocationInteractorImpl.java [N]
│   └── config/AuthorizationConfiguration.java                             [M] agrega 6 beans + registra el controller
└── package-info.java                                                      [M] agrega "assignments :: usecase", "assignments :: model", "assignments :: dto" si no están ya

shared/message/RequiredArgumentMessages.java                                [M] agrega 5 constantes (§7)
```

**Trampa de Modulith:** `assignments` ya tiene NamedInterfaces con consumidores desde HU-015
(`usecase`, `model`, `dto` — ver PLAN-HU-015.md §13/§14). Agregar un consumidor nuevo
(`authorization`) a interfaces ya nombradas no dispara la trampa de "primer `@NamedInterface`".
Verificar igual compilando.

## 9. Casos de prueba esperados

> Los tests de `AssignRoleRequestMapperTests` (si existen; verificar si `AssignRoleRawRequest` tenía
> mapper propio o si el interactor construía el request directo) se mueven a `authorization` — trabajo
> del tester/implementador.

| Capa | Clase de prueba | Casos |
|---|---|---|
| `application` (`assignments`) | `AssignmentApplicationLookupValidatorImplTests` | asignación existente → `ApplicationId` correcto; asignación inexistente → `AssignmentNotFoundException` |
| `application` (`authorization`) | `AdministerAssignmentCreationUseCaseImplTests` | decisión `ALLOW` → delega y devuelve `AssignmentResponse`; rechazo → `NotAuthorizedToAdministerException`, `AssignRoleUseCase` no se invoca |
| `application` (`authorization`) | `AdministerAssignmentRevocationUseCaseImplTests` | mismos dos casos, sobre `RevokeAssignmentUseCase` |
| `infrastructure` (`authorization`) | `AdministerAssignmentCreationInteractorImplTests` | construye `AdministrationRequest` directo desde `assignment.applicationId()`, sin lookup |
| `infrastructure` (`authorization`) | `AdministerAssignmentRevocationInteractorImplTests` | resuelve el `applicationId` vía `AssignmentApplicationLookupValidator`; propaga `AssignmentNotFoundException` si no existe |
| `infrastructure` (`authorization`) | `AssignmentAdministrationControllerTests` | delega a cada interactor, responde 201/200 |
| `infrastructure` (`assignments`) | `AssignmentControllerTests` (ajustado) | ya no prueba `assign`/`revoke`; sigue probando `list` |

Presupuesto total estimado: **12–15 pruebas**.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-14 |
| Contrato aprobado (gate 1) | ✅ Aprobado | 2026-09-14 |
| Pruebas en rojo | ✅ Confirmado (8 casos, `UnsupportedOperationException`) | 2026-09-14 |
| Implementación en verde | ✅ Verde (675 pruebas) | 2026-09-15 |
| Validación | ✅ APROBADO — ver `REPORTE-HU-018.md` | 2026-09-15 |
| Entrega (gate 2) | ⏳ Pendiente de confirmación para commit y push | |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. Las dos preguntas que `HU-018.md` dejaba abiertas se resolvieron por
investigación de código, con el mismo precedente que HU-016/HU-017 ya validó:

1. **¿Se gatea la asignación/revocación del rol administrador en sí?** No: `AssignRoleUseCase`/
   `RevokeAssignmentUseCase` son use cases distintos de `AssignApplicationAdministratorUseCase`
   (HU-015) — esta historia no los toca, confirmado leyendo `assignments/application/usecase/`.
2. **¿Cuántos fakes rompe el cambio?** Dimensionado en §9 — el tester/implementador lo confirma al
   ejecutar `verificar.ps1 -Rapido` tras aplicar las firmas `[M]`.
