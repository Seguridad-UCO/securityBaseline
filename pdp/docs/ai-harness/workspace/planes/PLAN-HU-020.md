# PLAN: Autoservicio de administradores — agregar, quitar y listar sin el canal interno

## Metadata

- **ID:** HU-020
- **Slice:** `assignments` (dos casos de uso nuevos: quitar y listar; reutiliza `AssignApplicationAdministratorUseCase` de HU-015 para agregar) + `authorization` (los gatea y expone en público)
- **Tipo:** Escritura + consulta
- **Fecha:** 2026-09-15
- **Rama sugerida:** `feature/HU-020-autoservicio-administradores`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-020.md` (dictada), `ADR-023-application-administration-model.md`, código real: `AssignApplicationAdministratorUseCaseImpl`/`InternalApplicationAdministratorController` (HU-015), `ApplicationOwnerLookupValidator` (resuelve tenant desde `applicationId` sin pasar por el principal), `AssignmentRepository`/`AssignmentCriteria` (consulta por rol+tenant ya existe y alcanza para listar), `RevokeAssignmentUseCase` (ya gateado por HU-018)
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 21, 22
- **Decisión de Sebastián (gate 1, vía `AskUserQuestion`):** el último administrador activo de una aplicación **no puede ser revocado** — nueva regla de negocio, no una decisión de OPA (es una restricción de integridad del catálogo, igual que "no duplicar una asignación activa" en HU-005).

## 1. Resumen funcional

Hoy la única forma de nombrar o quitar un administrador es `InternalApplicationAdministratorController`
(`/internal/v1/**`, mTLS) — pensado para backfill, no para uso cotidiano. Esta historia agrega tres
rutas públicas bajo `/api/v1/applications/{applicationId}/administrators`, gateadas de forma
incondicional por el mismo mecanismo de HU-009/ADR-023 (hace falta ya ser administrador de la
aplicación para agregar, quitar o listar a otros). Agregar reutiliza
`AssignApplicationAdministratorUseCase` (HU-015) tal cual — el gate es nuevo, el caso de uso no
cambia. Quitar y listar son casos de uso nuevos en `assignments`, junto con la regla que impide dejar
una aplicación sin ningún administrador.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Agregar un administrador, siendo administrador de la aplicación | `201`, misma forma que el backfill interno |
| 2 | Agregar un administrador, sin ser administrador de la aplicación | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 3 | Listar administradores, siendo administrador | `200`, lista de administradores activos |
| 4 | Listar administradores, sin ser administrador | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 5 | Quitar un administrador que **no** es el único activo, siendo administrador | `200`, revocado |
| 6 | Quitar un administrador sin ser administrador de la aplicación | `400 NOT_AUTHORIZED_TO_ADMINISTER` |
| 7 | Quitar al **único** administrador activo (incluido a sí mismo) | `400`, código nuevo `CANNOT_REMOVE_LAST_ADMINISTRATOR`, la asignación **no** se revoca |
| 8 | El endpoint interno mTLS de backfill (HU-015) sigue funcionando sin cambios | Cero regresión |
| 9 | Suite completa | `verificar.ps1` en verde |

## 3. Reglas de negocio

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| R1 | Quien agrega/quita/lista administradores debe administrar la aplicación | Reutiliza `PrincipalMustBeApplicationAdministratorValidator` (ya existe) | `AuthorizeAdministrationUseCase` | `NotAuthorizedToAdministerException` → 400 (ya existe) |
| R2 | No se puede revocar el rol `ADMIN` si es la única asignación activa de ese rol para la aplicación | **Nueva** — `LastAdministratorMustNotBeRevokedRule` **[N]**, pura y síncrona (recibe el conteo ya resuelto, no consulta nada) | `RemoveApplicationAdministratorUseCaseImpl` resuelve el conteo vía `AssignmentRepository.findBy(AssignmentCriteria.of(adminRoleId, tenantId), window)` antes de invocar la regla | `CannotRemoveLastAdministratorException` **[N]** → 400 |

> **Por qué R2 es una `Rule` nueva y no una condición inline en el use case:** decide, con excepción
> propia, si el catálogo queda en un estado inválido (sin dueño) — exactamente el criterio que separa
> una `Rule` de un `if` de control de flujo (mismo criterio que `AssignmentMustNotDuplicateActiveRule`,
> HU-005).

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguno nuevo. `Assignment`/`Role` (existentes) no cambian.

### Value objects / hechos de regla

| Tipo | Componentes | Vive en |
|---|---|---|
| `AdministratorRevocationEligibility` (hecho de `LastAdministratorMustNotBeRevokedRule`) | `ApplicationId applicationId, int activeAdministratorCount` | `assignments/domain/rule/model/` |

### Excepciones

| Excepción | Código | Vive en |
|---|---|---|
| `CannotRemoveLastAdministratorException` | `CANNOT_REMOVE_LAST_ADMINISTRATOR` | `assignments/domain/exception/` |

## 5. Persistencia

Sin cambios. `AssignmentRepository.findBy(AssignmentCriteria.of(roleId, tenantId), window)` ya existe
y alcanza para listar/contar los administradores activos de una aplicación (el `roleId` del rol
`ADMIN` de esa aplicación ya es único por `RoleScope.ofApplication`).

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| `POST` | `/api/v1/applications/{applicationId}/administrators` | `201` | `{"userId": "..."}` | `ApplicationAdministratorWebResponse` |
| `DELETE` | `/api/v1/applications/{applicationId}/administrators/{userId}` | `200` | — | — |
| `GET` | `/api/v1/applications/{applicationId}/administrators` | `200` | — | `List<ApplicationAdministratorWebResponse>` (sin paginación — el conteo esperado de administradores por aplicación es pequeño; si eso deja de ser cierto, es una historia futura, no una suposición a la que valga la pena diseñar hoy) |

Vive en un controller nuevo, `ApplicationAdministratorController` (`authorization`) — no se fusiona
con `ApplicationAdministrationController` (remove/rotate de HU-015, capacidad distinta) ni con
`InternalApplicationAdministratorController` (`assignments`, canal interno, sigue existiendo sin
cambios).

- **Autorización:** requiere token. El inquilino sale del principal para el `subject`, pero el
  `tenantId`/`applicationId` de la `AdministrationRequest` salen de `ApplicationOwnerLookupValidator`
  resuelto desde el `applicationId` de la ruta — mismo patrón que `AssignApplicationAdministratorInteractorImpl`
  (HU-015), nunca del principal ni del cuerpo.
- **Errores esperados:** `NotAuthorizedToAdministerException` → 400 (ya existe);
  `CannotRemoveLastAdministratorException` → 400 (nuevo); `ApplicationNotFoundException` → 400
  (ya existe, vía `ApplicationOwnerLookupValidator` si `applicationId` no existe).

## 7. SPEC — el contrato

### `assignments` — nuevos `[N]`

```java
// pdp/assignments/domain/rule/LastAdministratorMustNotBeRevokedRule.java
public interface LastAdministratorMustNotBeRevokedRule
        extends OperationWithoutResult<AdministratorRevocationEligibility> {
}
```

```java
// pdp/assignments/domain/rule/model/AdministratorRevocationEligibility.java
// Corregido en FASE 1 de 2-tester-spec: gana applicationId, que la excepción necesita.
public record AdministratorRevocationEligibility(ApplicationId applicationId, int activeAdministratorCount) {

    public AdministratorRevocationEligibility {
    }
}
```

```java
// pdp/assignments/domain/exception/CannotRemoveLastAdministratorException.java
public final class CannotRemoveLastAdministratorException extends BusinessRuleViolationException {
    public CannotRemoveLastAdministratorException(ApplicationId applicationId) {
        super("CANNOT_REMOVE_LAST_ADMINISTRATOR", AssignmentsMessages.cannotRemoveLastAdministrator(applicationId.value().toString()));
    }
}
```

```java
// pdp/assignments/application/primaryport/request/RemoveApplicationAdministratorRequest.java
public record RemoveApplicationAdministratorRequest(TenantId tenantId, ApplicationId applicationId, UserId userId) { }
```

```java
// pdp/assignments/application/primaryport/request/ListApplicationAdministratorsRequest.java
public record ListApplicationAdministratorsRequest(TenantId tenantId, ApplicationId applicationId) { }
```

```java
// pdp/assignments/application/usecase/RemoveApplicationAdministratorUseCase.java
public interface RemoveApplicationAdministratorUseCase
        extends ReactiveOperationWithoutResult<RemoveApplicationAdministratorRequest> {
}
```

```java
// pdp/assignments/application/usecase/ListApplicationAdministratorsUseCase.java
public interface ListApplicationAdministratorsUseCase
        extends ReactiveOperation<ListApplicationAdministratorsRequest, java.util.List<AssignmentResponse>> {
}
```

> **Colaboradores de los Impl (fijado en FASE 1 de `2-tester-spec`, no estaba en la SPEC original):**
> ambos casos de uso necesitan resolver el `RoleId` del rol `ADMIN` de la aplicación
> (`RoleLookupByNameInScopeValidator`, mismo patrón que `AssignApplicationAdministratorUseCaseImpl`),
> consultar `AssignmentRepository.findBy(AssignmentCriteria.of(adminRoleId, tenantId), window)` y
> filtrar por vigencia con `TimeProvider` (ninguna consulta del repositorio filtra por fecha). Además,
> `RemoveApplicationAdministratorUseCaseImpl` recibe `LastAdministratorMustNotBeRevokedRule` y
> `RevokeAssignmentUseCase` para completar R2:
>
> ```java
> public RemoveApplicationAdministratorUseCaseImpl(RoleLookupByNameInScopeValidator roleLookup,
>         AssignmentRepository repository, LastAdministratorMustNotBeRevokedRule mustNotBeLastAdministrator,
>         RevokeAssignmentUseCase revokeAssignment, TimeProvider time)
> ```
>
> ```java
> public ListApplicationAdministratorsUseCaseImpl(RoleLookupByNameInScopeValidator roleLookup,
>         AssignmentRepository repository, TimeProvider time)
> ```

> `ListApplicationAdministratorsUseCase` devuelve `List<AssignmentResponse>` (ya existe, HU-005) — sin
> DTO de respuesta nuevo en `assignments`: cada elemento ya trae `userId`/`validFrom`/`validUntil`,
> que es todo lo que HU-020 pidió exponer (ver `HU-020.md` pregunta 2). El aplanado a web ocurre en
> `authorization`, igual que toda respuesta de este proyecto.

### `authorization` — nuevos `[N]`

```java
// pdp/authorization/application/primaryport/request/AdministerApplicationAdministratorAssignmentRequest.java
public record AdministerApplicationAdministratorAssignmentRequest(
        AdministrationRequest administration, AssignApplicationAdministratorRequest assignment) { }
```

```java
// pdp/authorization/application/primaryport/request/AdministerApplicationAdministratorRemovalRequest.java
public record AdministerApplicationAdministratorRemovalRequest(
        AdministrationRequest administration, RemoveApplicationAdministratorRequest removal) { }
```

```java
// pdp/authorization/application/primaryport/request/AdministerApplicationAdministratorListRequest.java
public record AdministerApplicationAdministratorListRequest(
        AdministrationRequest administration, ListApplicationAdministratorsRequest query) { }
```

Los tres sin `Optional` — igual que HU-018/019 con las operaciones de asignación: `applicationId`
siempre está presente (viene de la ruta), el gate siempre se evalúa.

> **Constructores de los Impl (fijado en FASE 1 de `2-tester-spec`):** mismo patrón exacto que
> `AdministerAssignmentRevocationUseCaseImpl` (HU-018) — `(PrincipalMustBeApplicationAdministratorValidator
> mustBeAdministrator, {UseCase delegado})`.

```java
// pdp/authorization/application/usecase/AdministerApplicationAdministratorAssignmentUseCase.java
public interface AdministerApplicationAdministratorAssignmentUseCase
        extends ReactiveOperation<AdministerApplicationAdministratorAssignmentRequest, AssignmentResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerApplicationAdministratorRemovalUseCase.java
public interface AdministerApplicationAdministratorRemovalUseCase
        extends ReactiveOperationWithoutResult<AdministerApplicationAdministratorRemovalRequest> {
}
```

```java
// pdp/authorization/application/usecase/AdministerApplicationAdministratorListUseCase.java
public interface AdministerApplicationAdministratorListUseCase
        extends ReactiveOperation<AdministerApplicationAdministratorListRequest, java.util.List<AssignmentResponse>> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/request/raw/AssignApplicationAdministratorRawRequest.java
public record AssignApplicationAdministratorRawRequest(String applicationId, String userId) { }
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/response/ApplicationAdministratorWebResponse.java
public record ApplicationAdministratorWebResponse(String userId, String validFrom, String validUntil) { }
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/
// AdministerApplicationAdministratorAssignmentInteractor, ...RemovalInteractor, ...ListInteractor
// — misma forma que sus equivalentes de HU-015/018: raw request de entrada, web response de salida.
// El de "quitar" recibe applicationId+userId (ambos de la ruta, sin cuerpo); el de "listar" recibe
// solo applicationId (de la ruta, sin cuerpo ni query params).
```

### Firmas nuevas en `RequiredArgumentMessages`

```java
public static final String LAST_ADMINISTRATOR_MUST_NOT_BE_REVOKED_RULE = "se requiere la regla de último administrador";
public static final String REMOVE_APPLICATION_ADMINISTRATOR_USE_CASE = "se requiere el caso de uso de remoción de administrador de aplicación";
public static final String LIST_APPLICATION_ADMINISTRATORS_USE_CASE = "se requiere el caso de uso de listado de administradores de aplicación";
public static final String ADMINISTER_APPLICATION_ADMINISTRATOR_ASSIGNMENT_USE_CASE = "se requiere el caso de uso de administración de alta de administrador";
public static final String ADMINISTER_APPLICATION_ADMINISTRATOR_REMOVAL_USE_CASE = "se requiere el caso de uso de administración de remoción de administrador";
public static final String ADMINISTER_APPLICATION_ADMINISTRATOR_LIST_USE_CASE = "se requiere el caso de uso de administración de listado de administradores";
```

### Mensaje nuevo en `AssignmentsMessages`

```java
public static String cannotRemoveLastAdministrator(String applicationId) {
    return "no se puede quitar al único administrador activo de la aplicación " + applicationId;
}
```

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado.

```
pdp/assignments/
├── domain/
│   ├── rule/
│   │   ├── LastAdministratorMustNotBeRevokedRule.java                       [N]
│   │   ├── impl/LastAdministratorMustNotBeRevokedRuleImpl.java              [N]
│   │   └── model/AdministratorRevocationEligibility.java                    [N]
│   └── exception/CannotRemoveLastAdministratorException.java                [N]
├── application/
│   ├── primaryport/request/
│   │   ├── RemoveApplicationAdministratorRequest.java                       [N]
│   │   └── ListApplicationAdministratorsRequest.java                       [N]
│   └── usecase/
│       ├── RemoveApplicationAdministratorUseCase.java                      [N]
│       ├── ListApplicationAdministratorsUseCase.java                       [N]
│       └── impl/
│           ├── RemoveApplicationAdministratorUseCaseImpl.java              [N]
│           └── ListApplicationAdministratorsUseCaseImpl.java               [N]
└── infrastructure/config/AssignmentsConfiguration.java                      [M] agrega 2 beans de use case + la regla

pdp/authorization/
├── application/
│   ├── primaryport/request/ (3 records nuevos, §7)                          [N]
│   └── usecase/ (3 interfaces + 3 impls, §7)                                [N]
├── infrastructure/
│   ├── adapter/primary/web/
│   │   ├── controller/ApplicationAdministratorController.java              [N]
│   │   ├── dto/request/raw/AssignApplicationAdministratorRawRequest.java   [N]
│   │   ├── dto/response/ApplicationAdministratorWebResponse.java           [N]
│   │   └── interactor/ (3 interfaces + 3 impls, §7)                        [N]
│   └── config/AuthorizationConfiguration.java                              [M] agrega 6 beans + registra el controller
└── package-info.java                                                       [M] si `assignments :: usecase` no cubre ya los dos nuevos (ya está declarado desde HU-015 — sin cambio esperado)

shared/message/RequiredArgumentMessages.java                                 [M] agrega 6 constantes (§7)
pdp/assignments/domain/message/AssignmentsMessages.java                      [M] agrega 1 método (§7)
```

**Nota de Modulith:** `assignments :: usecase` ya está en `allowedDependencies` de `authorization`
desde HU-015 — los dos casos de uso nuevos (`RemoveApplicationAdministratorUseCase`,
`ListApplicationAdministratorsUseCase`) quedan cubiertos sin declarar nada nuevo. Verificar igual
compilando: si `ModulithStructureTests` marca algo, es que `AssignmentResponse` (usado como tipo de
retorno de `ListApplicationAdministratorsUseCase`) necesita algo que `assignments :: dto` no cubre
todavía — no debería, porque ya se usa así desde HU-005/015, pero es la comprobación barata antes de
asumirlo.

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `domain` (`assignments`) | `LastAdministratorMustNotBeRevokedRuleImplTests` | conteo > 1 → no lanza; conteo == 1 → `CannotRemoveLastAdministratorException`; conteo == 0 → decisión explícita del tester/implementador (no debería ocurrir en la práctica — documentarlo, no silenciarlo) |
| `application` (`assignments`) | `RemoveApplicationAdministratorUseCaseImplTests` | revoca cuando hay más de un administrador; rechaza cuando es el único, sin llegar a `RevokeAssignmentUseCase` |
| `application` (`assignments`) | `ListApplicationAdministratorsUseCaseImplTests` | devuelve la lista de `AssignmentResponse` que el repositorio resuelve |
| `application` (`authorization`) | `AdministerApplicationAdministratorAssignmentUseCaseImplTests` | ALLOW → delega en `AssignApplicationAdministratorUseCase`; DENY → `NotAuthorizedToAdministerException`, sin invocarlo |
| `application` (`authorization`) | `AdministerApplicationAdministratorRemovalUseCaseImplTests` | mismos dos casos, sobre `RemoveApplicationAdministratorUseCase` |
| `application` (`authorization`) | `AdministerApplicationAdministratorListUseCaseImplTests` | mismos dos casos, sobre `ListApplicationAdministratorsUseCase` |
| `infrastructure` (`authorization`) | 3 `*InteractorImplTests` | construyen `AdministrationRequest` resolviendo `tenantId` vía `ApplicationOwnerLookupValidator` desde el `applicationId` de la ruta |
| `infrastructure` (`authorization`) | `ApplicationAdministratorControllerTests` | delega a cada interactor, responde 201/200/200 |

Presupuesto total estimado: **20-24 pruebas**.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-15 |
| Contrato aprobado (gate 1) | ✅ Aprobado | 2026-09-15 |
| Pruebas en rojo | ✅ Confirmado | 2026-09-15 |
| Implementación en verde | ✅ Verde | 2026-09-15 |
| Validación | ✅ APROBADO — ver REPORTE-HU-020.md | 2026-09-15 |
| Entrega (gate 2) | ⏳ Pendiente — confirmar commit/push | |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. La única decisión que requería a Sebastián (último administrador)
ya se tomó vía `AskUserQuestion` antes de escribir este plan — ver Metadata.
