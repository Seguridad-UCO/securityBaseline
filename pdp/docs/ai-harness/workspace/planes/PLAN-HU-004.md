# PLAN: Catálogo de roles — alcance y recursos que autoriza

## Metadata

- **ID:** HU-004
- **Slice:** `nuevo: roles` + firmas nuevas en `resources` y en `pdp/commons`
- **Tipo:** Mixto (dos escrituras + una consulta paginada)
- **Fecha:** 2026-09-11
- **Rama sugerida:** `feature/HU-004-catalogo-roles`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-004.md` (decisiones de Sebastián, 2026-09-11) ·
  `security-platform-architecture/docs/02-domain/03-bounded-contexts.md` (BC-04: «catálogo RBAC de roles
  y su vínculo a recursos/acciones»; *no es responsable de* asignar ni evaluar) ·
  `06-invariants.md` (`INV-DAT-01`: un rol es de tenant/aplicación o global de forma inequívoca) ·
  `07-business-rules.md` (`RB-14` herencia/denegaciones: **pendiente en arquitectura → fuera**) ·
  `10-entities.md` (`Rol` + autorización recurso: **Sí, etapa 1**) · `12-value-objects.md`.
  **No existe event storming de este contexto** en `artefactos-referencia` (los que hay son de otro
  dominio) — anotado, no inventado. Código real consultado: `tenants/*` (patrón), `applications/*`
  (consulta paginada, validador compuesto, `ApplicationOwnershipQuery`), `resources/*` (puerto,
  agregado, interfaces nombradas), `ListApplicationsRequestMapper`, `ValueObjectMessages`.
- **Criterios de la línea base que toca:** 1, 2, 3, 5, 6, 7, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22

## 1. Resumen funcional

Nuevo slice `roles`: un **rol** es una entrada de catálogo con un nombre, un **alcance** (global, de
tenant o de aplicación — `INV-DAT-01`) y el conjunto de **recursos protegidos** que declara
autorizar. Tres operaciones: definir un rol, concederle un recurso, y listar el catálogo visible
para un tenant (los suyos más los globales), paginado como `applications` (criterios 16–19).

**Lo que este slice NO hace, a propósito:** no decide nada. El vínculo rol→recurso es **dato de
catálogo que HU-006 enviará a OPA como evidencia**; ningún caso de uso lee esa lista para conceder o
negar. La única lógica es de integridad del catálogo (unicidad, existencia, coherencia de alcance).

**No cubre:** asignar roles a usuarios (HU-005), perfiles (HU-008), quién puede administrar
(HU-009 — consecuencia: los roles **globales** existen en el modelo y la persistencia pero **no se
crean ni modifican por HTTP** todavía), herencia/denegaciones (`RB-14`, pendiente en arquitectura).

## 2. Criterios de aceptación

| # | Criterio                           | Resultado esperado                                                                                                                                                         |
|---|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Alta de rol de tenant              | `POST /api/v1/roles` con `scope=TENANT` → 201; el rol queda con el tenant del principal                                                                                    |
| 2 | Alta de rol de aplicación          | `scope=APPLICATION` + `applicationId` → 201 si la aplicación existe y es del tenant del principal; si no → 400 `APPLICATION_NOT_FOUND`                                     |
| 3 | Nombre único dentro del alcance    | Mismo nombre y mismo alcance → 409 `ROLE_NAME_TAKEN`; mismo nombre en otro tenant, o en tenant vs. aplicación → permitido                                                  |
| 4 | Recursos coherentes con el alcance | Conceder un recurso de una aplicación de otro tenant a un rol de tenant, o de otra aplicación a un rol de aplicación → 400 `RESOURCE_OUTSIDE_ROLE_SCOPE` (`INV-DAT-01`)    |
| 5 | Recurso inexistente                | `resourceId` que no está en el catálogo → 400 `PROTECTED_RESOURCE_NOT_FOUND`                                                                                               |
| 6 | Consulta por tenant                | `GET /api/v1/roles` devuelve los roles del tenant del principal **más** los globales; nunca los de otro tenant. Paginado (`page`/`size` o `offset`/`limit`, ambiguo → 400) |
| 7 | Global no creable aún              | `scope=GLOBAL` por HTTP → 400 `MALFORMED_REQUEST_FIELD` en `scope`, con mensaje que diga por qué                                                                           |
| 8 | Aislamiento al conceder            | Conceder un recurso a un rol de otro tenant (o a uno global) → 400 `ROLE_NOT_FOUND` — el rol «no existe» para ese tenant                                                   |
| 9 | Conceder es idempotente            | Conceder dos veces el mismo recurso deja el conjunto igual (es un `Set`), 200 ambas veces                                                                                  |

## 3. Reglas de negocio

| #  | Regla                                                                                                                              | Dónde vive (VO / Rule)                                                                                            | Puerto que trae el dato                                                                                | Excepción → HTTP                                                                |
|----|------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| L1 | Nombre: 3–60 caracteres tras `trim`, no vacío                                                                                      | VO `RoleName` (**síncrona**)                                                                                      | —                                                                                                      | `InvalidRoleNameException` → 400                                                |
| L2 | Alcance coherente: `GLOBAL` sin ids, `TENANT` solo tenant, `APPLICATION` tenant + aplicación                                       | VO `RoleScope` (**síncrona**)                                                                                     | —                                                                                                      | `InvalidRoleScopeException` → 400                                               |
| R1 | El nombre es único dentro del alcance exacto (nivel + tenant + aplicación)                                                         | `RoleNameMustBeUniqueInScopeRule` (**síncrona**, recibe `RoleNameAvailability`)                                   | `RoleRepository.existsByNameInScope`                                                                   | `DuplicateRoleNameException` → **409**                                          |
| R2 | Para `APPLICATION`, la aplicación existe y pertenece al tenant                                                                     | **Prestada**: `ApplicationMustExistForTenantValidator` (`applications :: rule`)                                   | (interno de `applications`)                                                                            | `ApplicationNotFoundException` → 400 (ya existe)                                |
| R3 | El rol existe **para ese tenant** (un rol de otro tenant o uno global no existe para él)                                           | `RoleMustExistForTenantRule` (**síncrona**, recibe `RoleExistence`)                                               | `RoleRepository.findByIdForTenant`                                                                     | `RoleNotFoundException` → 400                                                   |
| R4 | El recurso existe                                                                                                                  | **Prestada, nueva**: `ProtectedResourceOwnerLookupValidator` (`resources :: rule`) — resuelve la aplicación dueña | `ProtectedResourceRepository.findApplicationIdById` **[M]**                                            | `ProtectedResourceNotFoundException` → 400 (constructor nuevo por `ResourceId`) |
| R5 | El alcance del rol cubre el recurso: global cubre todo; tenant cubre recursos de sus aplicaciones; aplicación cubre solo los suyos | `RoleScopeMustCoverResourceRule` (**síncrona**, recibe `ResourceCoverage`)                                        | tenant del recurso vía **prestado** `ApplicationOwnerLookupValidator` (`applications :: rule`, HU-003) | `ResourceOutsideRoleScopeException` → 400                                       |

Barreras de contrato del borde HTTP (no son reglas de negocio — validación de forma, como C1–C4 de
HU-003):

| #  | Barrera                                                                                          | Dónde                                                               | Excepción → HTTP                                                                                             |
|----|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| C1 | `scope` debe ser `TENANT` o `APPLICATION`. `GLOBAL` se rechaza **en este canal** hasta HU-009    | `DefineRoleRequestMapper`                                           | `MalformedRequestFieldException("scope", RolesMessages.globalScopeNotAdministrableYet())` → 400              |
| C2 | `scope=APPLICATION` exige `applicationId`; `scope=TENANT` lo prohíbe (ambigüedad, no se adivina) | `DefineRoleRequestMapper`                                           | `MissingRequestFieldException("applicationId")` / `MalformedRequestFieldException("applicationId", …)` → 400 |
| C3 | Ventana de paginación: `page`/`size` y `offset`/`limit` no se mezclan; rangos válidos            | `ListRolesRequestMapper` (copia de `ListApplicationsRequestMapper`) | `ConflictingRequestParametersException` / `MalformedRequestFieldException` → 400                             |

**Ninguna `if/throw` de negocio en los use cases.** El único `if` de orquestación (¿tiene
`applicationId` el alcance?) se resuelve con `Mono.justOrEmpty(scope.applicationId())` en el
validador, no con una rama.

## 4. Modelo de dominio afectado

### Entidad / agregado

`Role` **[N]** —
`record Role(RoleId id, RoleName name, RoleScope scope, Set<ResourceId> resources, Instant registeredAt)`.
Factoría con nombre `define(id, name, scope, registeredAt)` (recursos vacíos). Transición
`withResource(ResourceId)` → nuevo `Role` con el recurso añadido (`Set.copyOf`, idempotente).
Sin `authorizes(...)`: eso sería decidir. **Sin eventos de dominio** en esta historia (no hay
consumidor; HU-007 los añadirá cuando exista auditoría).

`RoleCriteria` **[N]** — specification de consulta, junto al agregado (como `ApplicationCriteria`):
`record RoleCriteria(TenantId tenantId)`; `matches(Role)` = el rol es global **o** su alcance es de
ese tenant.

### Value objects

| VO                                        | Nuevo o existente | Invariantes                                                                                                                                                                                                                                                                       | Vive en                                                        |
|-------------------------------------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `RoleId`                                  | **Nuevo**         | UUID no nulo; `of(String)` → `InvalidIdentifierException("ROLE_ID", raw)`                                                                                                                                                                                                         | `pdp/commons/model/` — HU-005 lo consume, dos slices → commons |
| `RoleName`                                | **Nuevo**         | `trim`; no nulo/vacío; 3–60 caracteres                                                                                                                                                                                                                                            | `roles/domain/model/`                                          |
| `RoleScope`                               | **Nuevo**         | `Optional<TenantId>` y `Optional<ApplicationId>` presentes **exactamente** según el nivel (L2). Factorías `global()`, `ofTenant(TenantId)`, `ofApplication(TenantId, ApplicationId)`. `isGlobal()`. `Optional` como componente tiene precedente aceptado en `ApplicationCriteria` | `roles/domain/model/`                                          |
| `ResourceId`, `TenantId`, `ApplicationId` | Existentes        | —                                                                                                                                                                                                                                                                                 | `pdp/commons/model/`                                           |

### Enums

| Enum                     | Valores                           | Comportamiento que expone                                                                                           |
|--------------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `RoleScopeLevel` **[N]** | `GLOBAL`, `TENANT`, `APPLICATION` | `requiresTenant()`, `requiresApplication()`, `parse(String)` → `InvalidRoleScopeException` si no es uno de los tres |

### Hechos de regla (`roles/domain/rule/model/`)

`RoleNameAvailability(RoleName name, RoleScope scope, boolean taken)` ·
`RoleExistence(RoleId roleId, TenantId tenantId, boolean registered)` ·
`ResourceCoverage(RoleScope scope, ApplicationId resourceApplicationId, TenantId resourceTenantId)`.

## 5. Persistencia

- **Tabla:** `role` (constante en `RoleSchema.TABLE`)
- **Campos:** `id`, `name`, `level` (`GLOBAL|TENANT|APPLICATION`), `tenantId` (ausente si global),
  `applicationId` (ausente salvo `APPLICATION`), `resourceIds` (array de strings), `registeredAt`
- **Índice:** `role_scope_name` UNIQUE sobre `(level, tenantId, applicationId, name)` — cierra la
  ventana de carrera de R1; la regla sigue siendo quien produce el mensaje.
- **Consultas nuevas en el puerto:** ver §7 (`RoleRepository` completo, es nuevo)
- **Inicializador de esquema:** **nuevo** `SurrealRoleSchemaInitializer` (patrón de `resources`)
- **[M] en `resources`:** `ProtectedResourceRepository.findApplicationIdById(ResourceId)` + su
  implementación Surreal (`SELECT applicationId FROM type::record('protected_resource', $id)`) —
  mismo patrón que `findTenantIdById` de HU-003.

## 6. Endpoint

| Verbo | Ruta                               | Código de éxito             | Cuerpo de entrada                                 | Cuerpo de salida                             |
|-------|------------------------------------|-----------------------------|---------------------------------------------------|----------------------------------------------|
| POST  | `/api/v1/roles`                    | 201 `ROLE_DEFINED`          | `DefineRoleRawRequest`                            | `ApiResponse<RoleWebResponse>`               |
| POST  | `/api/v1/roles/{roleId}/resources` | 200 `ROLE_RESOURCE_GRANTED` | `GrantResourceRawRequest` (+ `roleId` de la ruta) | `ApiResponse<RoleWebResponse>`               |
| GET   | `/api/v1/roles`                    | 200 `ROLES_LISTED`          | query `page`/`size`/`offset`/`limit`              | `ApiResponse<PageResponse<RoleWebResponse>>` |

- **Autorización:** canal BFF (`SecurityConfiguration`/`KeycloakSecurityConfiguration`, ya
  cubre `/api/**`). **El tenant sale del principal** (`SecurityContext.currentPrincipal()`), nunca del
  cuerpo. Sin restricción de rol administrador (HU-009).
- **Errores esperados:** ver §2 y §3. Ninguno nuevo en `ApiErrorHandler`: todas las excepciones
  nuevas cuelgan de `InvalidValueException`, `BusinessRuleViolationException` o
  `ConflictBusinessRuleException`, que ya se traducen.

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/roles/domain/rule/RoleNameMustBeUniqueInScopeRule.java                                    [N]
public interface RoleNameMustBeUniqueInScopeRule extends OperationWithoutResult<RoleNameAvailability> { }

// pdp/roles/domain/rule/RoleMustExistForTenantRule.java                                          [N]
public interface RoleMustExistForTenantRule extends OperationWithoutResult<RoleExistence> { }

// pdp/roles/domain/rule/RoleScopeMustCoverResourceRule.java                                      [N]
public interface RoleScopeMustCoverResourceRule extends OperationWithoutResult<ResourceCoverage> { }

// pdp/roles/application/rule/validator/DefineRoleRulesValidator.java                             [N]
public interface DefineRoleRulesValidator extends ReactiveOperationWithoutResult<DefineRoleRequest> { }

// pdp/roles/application/rule/validator/GrantResourceRulesValidator.java                          [N]
// Finder con rechazo (como ApplicationOwnerLookupValidator): valida R3, R4, R5 y devuelve el rol
// encontrado para el tenant, que el caso de uso transforma y guarda.
public interface GrantResourceRulesValidator extends ReactiveOperation<GrantResourceRequest, Role> { }

// pdp/roles/application/usecase/DefineRoleUseCase.java                                           [N]
public interface DefineRoleUseCase extends ReactiveOperation<DefineRoleRequest, RoleResponse> { }

// pdp/roles/application/usecase/GrantResourceToRoleUseCase.java                                  [N]
public interface GrantResourceToRoleUseCase extends ReactiveOperation<GrantResourceRequest, RoleResponse> { }

// pdp/roles/application/usecase/ListRolesUseCase.java                                            [N]
public interface ListRolesUseCase extends ReactiveOperation<ListRolesRequest, ResultPage<RoleResponse>> { }

// pdp/roles/application/secondaryport/repository/RoleRepository.java                             [N]
public interface RoleRepository {
    Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope);
    Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId);     // vacío si no existe o no es de ese tenant (global tampoco)
    Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window);
    Mono<Role> save(Role role);
}

// pdp/resources/application/rule/validator/ProtectedResourceOwnerLookupValidator.java            [N]
// Publicado en `resources :: rule` (la interfaz nombrada ya existe). Vacío del puerto → ProtectedResourceNotFoundException.
public interface ProtectedResourceOwnerLookupValidator extends ReactiveOperation<ResourceId, ApplicationId> { }

// pdp/roles/infrastructure/adapter/primary/web/interactor/DefineRoleInteractor.java              [N]
public interface DefineRoleInteractor extends ReactiveOperation<DefineRoleRawRequest, RoleWebResponse> { }

// pdp/roles/infrastructure/adapter/primary/web/interactor/GrantResourceToRoleInteractor.java     [N]
public interface GrantResourceToRoleInteractor extends ReactiveOperation<GrantResourceRawRequest, RoleWebResponse> { }

// pdp/roles/infrastructure/adapter/primary/web/interactor/ListRolesInteractor.java               [N]
public interface ListRolesInteractor extends ReactiveOperation<ListRolesRawRequest, PageResponse<RoleWebResponse>> { }
```

### Constructores de las implementaciones (fijados aquí para que el tester no adivine)

```java
RoleNameMustBeUniqueInScopeRuleImpl()
RoleMustExistForTenantRuleImpl()
RoleScopeMustCoverResourceRuleImpl()
DefineRoleRulesValidatorImpl(ApplicationMustExistForTenantValidator applicationMustExist,
                             RoleNameMustBeUniqueInScopeRule nameMustBeUnique, RoleRepository repository)
GrantResourceRulesValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule roleMustExist,
                                ProtectedResourceOwnerLookupValidator resourceOwner,
                                ApplicationOwnerLookupValidator applicationOwner,
                                RoleScopeMustCoverResourceRule scopeMustCover)
DefineRoleUseCaseImpl(DefineRoleRulesValidator rules, RoleRepository repository,
                      IdentifierGenerator identifiers, TimeProvider time)     // construye un Role desde cero → ambos desde el día uno
GrantResourceToRoleUseCaseImpl(GrantResourceRulesValidator rules, RoleRepository repository)
ListRolesUseCaseImpl(RoleRepository repository)
ProtectedResourceOwnerLookupValidatorImpl(ProtectedResourceRepository repository)
DefineRoleInteractorImpl(DefineRoleUseCase useCase)
GrantResourceToRoleInteractorImpl(GrantResourceToRoleUseCase useCase)
ListRolesInteractorImpl(ListRolesUseCase useCase)
SurrealRoleRepository(SurrealDbClient client)
SurrealRoleSchemaInitializer(SurrealDbClient client)
```

### Firmas de value objects y entidades

```java
// pdp/commons/model/RoleId.java                                                                  [N]
public record RoleId(UUID value) { public static RoleId of(String raw) }     // invariantes: no nulo; of → InvalidIdentifierException("ROLE_ID", raw)

// pdp/roles/domain/model/RoleName.java                                                           [N]
public record RoleName(String value) { }                                     // invariantes: trim, requerido, 3–60 → InvalidRoleNameException

// pdp/roles/domain/model/RoleScopeLevel.java                                                     [N]
public enum RoleScopeLevel { GLOBAL, TENANT, APPLICATION;
    public boolean requiresTenant(); public boolean requiresApplication();
    public static RoleScopeLevel parse(String raw); }                        // parse → InvalidRoleScopeException

// pdp/roles/domain/model/RoleScope.java                                                          [N]
public record RoleScope(RoleScopeLevel level, Optional<TenantId> tenantId, Optional<ApplicationId> applicationId) {
    public static RoleScope global();
    public static RoleScope ofTenant(TenantId tenantId);
    public static RoleScope ofApplication(TenantId tenantId, ApplicationId applicationId);
    public boolean isGlobal(); }                                             // invariante L2 → InvalidRoleScopeException

// pdp/roles/domain/Role.java                                                                     [N]
public record Role(RoleId id, RoleName name, RoleScope scope, Set<ResourceId> resources, Instant registeredAt) {
    public static Role define(RoleId id, RoleName name, RoleScope scope, Instant registeredAt);
    public Role withResource(ResourceId resourceId); }                       // resources = Set.copyOf(...)

// pdp/roles/domain/RoleCriteria.java                                                             [N]
public record RoleCriteria(TenantId tenantId) {
    public static RoleCriteria ofTenant(TenantId tenantId);
    public boolean matches(Role role); }                                     // global || scope.tenantId == tenantId

// pdp/roles/domain/rule/model/RoleNameAvailability.java                                          [N]
public record RoleNameAvailability(RoleName name, RoleScope scope, boolean taken) { }
// pdp/roles/domain/rule/model/RoleExistence.java                                                 [N]
public record RoleExistence(RoleId roleId, TenantId tenantId, boolean registered) { }
// pdp/roles/domain/rule/model/ResourceCoverage.java                                              [N]
public record ResourceCoverage(RoleScope scope, ApplicationId resourceApplicationId, TenantId resourceTenantId) { }
```

### Excepciones nuevas (`pdp/roles/domain/exception/`) y mensajes

```java
InvalidRoleNameException(String reason)            extends InvalidValueException          // "INVALID_ROLE_NAME"
InvalidRoleScopeException(String reason)           extends InvalidValueException          // "INVALID_ROLE_SCOPE"
DuplicateRoleNameException(RoleName, RoleScope)    extends ConflictBusinessRuleException  // "ROLE_NAME_TAKEN"   → 409
RoleNotFoundException(RoleId)                      extends BusinessRuleViolationException // "ROLE_NOT_FOUND"    → 400
ResourceOutsideRoleScopeException(ResourceId)      extends BusinessRuleViolationException // "RESOURCE_OUTSIDE_ROLE_SCOPE" → 400
// pdp/roles/domain/message/RolesMessages.java  [N]: métodos estáticos en español para las cinco, más globalScopeNotAdministrableYet()
// pdp/resources/domain/exception/ProtectedResourceNotFoundException.java  [M]: + constructor (ResourceId) y su mensaje en ResourcesMessages
// pdp/commons/message/ValueObjectMessages.java  [M]: + RoleName.LENGTH, RoleScope.INCOHERENT, RoleScope.UNSUPPORTED_LEVEL
```

### Firmas de DTOs

```java
// pdp/roles/application/primaryport/request/DefineRoleRequest.java                               [N]
public record DefineRoleRequest(RoleName name, RoleScope scope) { }
// pdp/roles/application/primaryport/request/GrantResourceRequest.java                            [N]
public record GrantResourceRequest(TenantId tenantId, RoleId roleId, ResourceId resourceId) { }
// pdp/roles/application/primaryport/request/ListRolesRequest.java                                [N]
public record ListRolesRequest(RoleCriteria criteria, PageWindow window) { }
// pdp/roles/application/primaryport/response/RoleResponse.java                                   [N]
public record RoleResponse(RoleId id, RoleName name, RoleScope scope, Set<ResourceId> resources, Instant registeredAt) { }

// pdp/roles/infrastructure/adapter/primary/web/dto/request/raw/DefineRoleRawRequest.java         [N]
public record DefineRoleRawRequest(String name, String scope, String applicationId) { }
// .../raw/GrantResourceRawRequest.java                                                           [N]  (roleId lo pone el controller desde la ruta)
public record GrantResourceRawRequest(String roleId, String resourceId) { }
// .../raw/ListRolesRawRequest.java                                                               [N]
public record ListRolesRawRequest(String page, String size, String offset, String limit) { }
// .../dto/response/RoleWebResponse.java                                                          [N]  (plana; tenantId/applicationId null cuando no aplican)
public record RoleWebResponse(String id, String name, String scope, String tenantId, String applicationId,
        List<String> resourceIds, String registeredAt) { }

// .../persistence/entity/RoleEntity.java                                                         [N]
public record RoleEntity(String id, String name, String level, String tenantId, String applicationId,
        List<String> resourceIds, String registeredAt) { }
```

### Mappers (estáticos, `final class` con constructor privado)

```java
DefineRoleRequestMapper.toRequest(DefineRoleRawRequest raw, TenantId tenantId): DefineRoleRequest        // C1, C2
GrantResourceRequestMapper.toRequest(GrantResourceRawRequest raw, TenantId tenantId): GrantResourceRequest
ListRolesRequestMapper.toRequest(ListRolesRawRequest raw, TenantId tenantId): ListRolesRequest          // C3, copia de ListApplicationsRequestMapper
RoleResponseMapper.toResponse(RoleResponse response): RoleWebResponse
RolePersistenceMapper.toDomain(RoleEntity entity): Role
```

### Firmas nuevas en puertos existentes

```java
// pdp/resources/application/secondaryport/repository/ProtectedResourceRepository.java             [M]
Mono<ApplicationId> findApplicationIdById(ResourceId resourceId);   // vacío si no existe
```

## 8. Árbol de archivos

> Rutas desde `pdp/src/main/java/co/edu/uco/seguridad/`. `[N]` nuevo, `[M]` modificado.

```
pdp/commons/
├── model/RoleId.java                                                          [N]
└── message/ValueObjectMessages.java                                           [M] +RoleName, +RoleScope

pdp/resources/
├── application/secondaryport/repository/ProtectedResourceRepository.java     [M] +findApplicationIdById
├── application/rule/validator/ProtectedResourceOwnerLookupValidator.java     [N]  (paquete ya es `resources :: rule`)
├── application/rule/validator/impl/ProtectedResourceOwnerLookupValidatorImpl.java [N]
├── domain/exception/ProtectedResourceNotFoundException.java                   [M] +constructor(ResourceId)
├── domain/message/ResourcesMessages.java                                      [M] +protectedResourceNotFoundById
├── infrastructure/adapter/secondary/persistence/repository/SurrealProtectedResourceRepository.java [M]
└── infrastructure/config/ResourcesConfiguration.java                          [M] registra el validador

pdp/roles/                                                                     [N] slice completo
├── package-info.java        @ApplicationModule(allowedDependencies = {"commons",
│                              "applications", "applications :: rule", "applications :: dto", "applications :: exception",
│                              "resources", "resources :: rule", "resources :: exception"})
├── domain/
│   ├── Role.java
│   ├── RoleCriteria.java
│   ├── model/{RoleName, RoleScope, RoleScopeLevel}.java
│   ├── rule/{RoleNameMustBeUniqueInScopeRule, RoleMustExistForTenantRule, RoleScopeMustCoverResourceRule}.java
│   ├── rule/impl/{…Impl}.java (×3)
│   ├── rule/model/{RoleNameAvailability, RoleExistence, ResourceCoverage}.java
│   ├── exception/{InvalidRoleNameException, InvalidRoleScopeException, DuplicateRoleNameException,
│   │              RoleNotFoundException, ResourceOutsideRoleScopeException}.java
│   └── message/RolesMessages.java
├── application/
│   ├── primaryport/request/{DefineRoleRequest, GrantResourceRequest, ListRolesRequest}.java
│   ├── primaryport/response/RoleResponse.java
│   ├── secondaryport/repository/RoleRepository.java
│   ├── rule/validator/package-info.java        @NamedInterface("rule")  ← HU-005 lo consumirá; se declara ya para no caer en la trampa de Modulith
│   ├── rule/validator/{DefineRoleRulesValidator, GrantResourceRulesValidator}.java
│   ├── rule/validator/impl/{…Impl}.java (×2)
│   ├── usecase/{DefineRoleUseCase, GrantResourceToRoleUseCase, ListRolesUseCase}.java
│   └── usecase/impl/{…Impl}.java (×3)
└── infrastructure/
    ├── adapter/primary/web/controller/RoleController.java
    ├── adapter/primary/web/dto/request/raw/{DefineRoleRawRequest, GrantResourceRawRequest, ListRolesRawRequest}.java
    ├── adapter/primary/web/dto/response/RoleWebResponse.java
    ├── adapter/primary/web/interactor/{DefineRoleInteractor, GrantResourceToRoleInteractor, ListRolesInteractor}.java
    ├── adapter/primary/web/interactor/impl/{…Impl}.java (×3)
    ├── adapter/primary/web/mapper/{DefineRoleRequestMapper, GrantResourceRequestMapper, ListRolesRequestMapper, RoleResponseMapper}.java
    ├── adapter/secondary/persistence/entity/RoleEntity.java
    ├── adapter/secondary/persistence/mapper/RolePersistenceMapper.java
    ├── adapter/secondary/persistence/repository/SurrealRoleRepository.java
    ├── adapter/secondary/persistence/schema/{RoleSchema, SurrealRoleSchemaInitializer}.java
    └── config/RolesConfiguration.java           registra: repositorio, inicializador, 3 reglas, 2 validadores, 3 casos de uso, 3 interactores

shared/
├── message/RequiredArgumentMessages.java                                      [M] +constantes del slice
└── web/message/WebContractMessages.java                                       [M] +successRoleDefined, +successRoleResourceGranted, +successRolesListed
```

`ModulithStructureTests`: `roles` es un módulo nuevo sin consumidores → ningún `allowedDependencies`
ajeno cambia. `resources :: rule` y `applications :: rule` ya existen como interfaces nombradas.

## 9. Casos de prueba esperados

| Capa                            | Clase de prueba                                        | Casos                                                                                                                                                                                                                                                                                                         |
|---------------------------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain`                        | `RoleNameTests`                                        | trim; null → `InvalidRoleNameException`; 2 chars → rechaza; 61 chars → rechaza; 3 y 60 → acepta                                                                                                                                                                                                               |
| `domain`                        | `RoleScopeTests`                                       | `global()` sin ids; `ofTenant`; `ofApplication`; constructor `TENANT` sin tenant → `InvalidRoleScopeException`; `GLOBAL` con tenant → rechaza; `APPLICATION` sin aplicación → rechaza                                                                                                                         |
| `domain`                        | `RoleScopeLevelTests`                                  | `parse("tenant")` case-insensitive; `parse("x")` → `InvalidRoleScopeException`; `requiresTenant/Application` por nivel                                                                                                                                                                                        |
| `domain`                        | `RoleTests`                                            | `define` deja recursos vacíos; `withResource` añade y no muta el original; `withResource` repetido no duplica                                                                                                                                                                                                 |
| `domain`                        | `RoleCriteriaTests`                                    | rol del tenant → true; global → true; otro tenant → false                                                                                                                                                                                                                                                     |
| `domain`                        | `RoleNameMustBeUniqueInScopeRuleImplTests`             | `taken=true` → `DuplicateRoleNameException`; `false` → no lanza                                                                                                                                                                                                                                               |
| `domain`                        | `RoleMustExistForTenantRuleImplTests`                  | `registered=false` → `RoleNotFoundException`; `true` → no lanza                                                                                                                                                                                                                                               |
| `domain`                        | `RoleScopeMustCoverResourceRuleImplTests`              | global cubre todo; tenant cubre recurso de su tenant; tenant **no** cubre otro tenant; aplicación cubre solo la suya                                                                                                                                                                                          |
| `application`                   | `DefineRoleRulesValidatorImplTests`                    | nombre tomado → `DuplicateRoleNameException`; `APPLICATION` con app inexistente → `ApplicationNotFoundException` (fake del validador prestado); `TENANT` **no** invoca el validador de aplicación (poison pill); todo ok → completa                                                                           |
| `application`                   | `GrantResourceRulesValidatorImplTests`                 | rol no encontrado para el tenant → `RoleNotFoundException` y **no** consulta recursos (poison pill); recurso inexistente → `ProtectedResourceNotFoundException`; fuera de alcance → `ResourceOutsideRoleScopeException`; ok → devuelve el rol                                                                 |
| `application`                   | `DefineRoleUseCaseImplTests`                           | camino feliz: usa `IdentifierGenerator`/`TimeProvider` fijos, guarda un `Role` con recursos vacíos y devuelve `RoleResponse`; el validador que lanza corta antes de guardar (lista capturadora vacía)                                                                                                         |
| `application`                   | `GrantResourceToRoleUseCaseImplTests`                  | guarda el rol con el recurso añadido y devuelve la respuesta; el validador que lanza no guarda                                                                                                                                                                                                                |
| `application`                   | `ListRolesUseCaseImplTests`                            | proyecta `ResultPage<Role>` → `ResultPage<RoleResponse>` conservando total y ventana                                                                                                                                                                                                                          |
| `application` (resources)       | `ProtectedResourceOwnerLookupValidatorImplTests`       | existe → `ApplicationId`; vacío → `ProtectedResourceNotFoundException`                                                                                                                                                                                                                                        |
| `infrastructure` — mapper       | `DefineRoleRequestMapperTests`                         | `name` ausente → Missing; `scope` ausente → Missing; `scope=GLOBAL` → Malformed(`scope`); `scope=otro` → Malformed(`scope`); `APPLICATION` sin `applicationId` → Missing; `TENANT` con `applicationId` → Malformed(`applicationId`); `applicationId` no UUID → Malformed; feliz `TENANT`; feliz `APPLICATION` |
| `infrastructure` — mapper       | `GrantResourceRequestMapperTests`                      | `resourceId` ausente → Missing; no UUID → Malformed; `roleId` no UUID → Malformed; feliz                                                                                                                                                                                                                      |
| `infrastructure` — mapper       | `ListRolesRequestMapperTests`                          | sin parámetros → ventana por defecto; `page`+`offset` → `ConflictingRequestParametersException`; `size=0` → Malformed(`size`) — el resto de combinaciones ya las cubre `ListApplicationsRequestMapperTests` sobre la misma lógica                                                                             |
| `infrastructure` — mapper       | `RoleResponseMapperTests`                              | rol de aplicación → ambos ids; global → `tenantId`/`applicationId` nulos; recursos en orden estable                                                                                                                                                                                                           |
| `infrastructure` — mapper       | `RolePersistenceMapperTests`                           | entidad → dominio para los tres niveles; `resourceIds` vacío → `Set` vacío                                                                                                                                                                                                                                    |
| `infrastructure` — controller   | `RoleControllerTests`                                  | `POST /roles` → 201 `ROLE_DEFINED`; `POST /{id}/resources` → 200 y el `roleId` de la ruta llega al interactor; `GET` → 200 con `PageResponse`                                                                                                                                                                 |
| `infrastructure` — persistencia | `SurrealRepositoryIntegrationTests` **[M]** (+3 casos) | `save` + `findByIdForTenant` devuelve el rol con sus recursos; `findByIdForTenant` con otro tenant → vacío; `findBy(ofTenant)` incluye globales y excluye otro tenant; `findApplicationIdById` de `resources` resuelve y vacío                                                                                |
| `infrastructure` — HTTP         | `RoleHttpTests`                                        | define `TENANT` → 201; define `GLOBAL` → 400 `scope`; concede recurso de otra aplicación a rol de aplicación → 400 `RESOURCE_OUTSIDE_ROLE_SCOPE`; lista del tenant no muestra roles de otro tenant                                                                                                            |

Presupuesto estimado: **~48 pruebas.** Es la historia más grande hasta ahora, y lo es porque son
tres casos de uso, tres reglas y tres value objects — proporcional al presupuesto de `sb-testing`
(≈15 por caso de uso con reglas), no duplicación. Ya se partió una vez (asignaciones → HU-005). Si
al tester le resulta inmanejable, el corte natural es «conceder recurso» (R3–R5 y su endpoint) como
historia aparte; el plan está escrito para que eso sea un recorte limpio, no una reescritura.

## 10. Trazabilidad

| Fase                       | Estado                           | Fecha      |
|----------------------------|----------------------------------|------------|
| Plan                       | ✅ Generado                       | 2026-09-11 |
| Contrato aprobado (gate 1) | ✅ Aprobado                       | 2026-09-11 |
| Pruebas en rojo            | ✅ Completado                     | 2026-09-11 |
| Implementación en verde    | ✅ Completado                     | 2026-09-12 |
| Validación                 | ✅ Aprobado — `REPORTE-HU-004.md` | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                      |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. Tres notas para quien siga:

1. **`RoleScope` con `Optional` como componentes** es una elección deliberada con precedente
   (`ApplicationCriteria`, decisión registrada en `CHECKPOINT.md`). La alternativa —una jerarquía
   `sealed` de tres records— sería más idiomática en Java 25 pero no tiene precedente en el proyecto
   y complicaría la persistencia sin ganar nada. Si el equipo prefiere `sealed`, es un cambio
   local al VO y a `RoleScopeMustCoverResourceRuleImpl`, antes de escribir pruebas.
2. **Índice único con `NONE`**: en SurrealDB, `tenantId`/`applicationId` ausentes participan en el
   índice como `NONE`, lo que hace único `(GLOBAL, NONE, NONE, name)` correctamente. El
   implementador lo confirma en `SurrealRepositoryIntegrationTests`; si no se comporta así, la
   regla R1 sigue garantizando la unicidad y el índice pasa a ser solo defensa en profundidad.
3. **El orden del listado** lo fija el adaptador (`registeredAt DESC`, como `applications`); no es
   parte del contrato.
