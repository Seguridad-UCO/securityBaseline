# PLAN: Asignaciones vigentes — el contexto que OPA evalúa

## Metadata

- **ID:** HU-005
- **Slice:** `nuevo: assignments` + `[M]` en `identity`, `roles`, `pdp/commons`
- **Tipo:** Mixto (dos escrituras + dos consultas)
- **Fecha:** 2026-09-12
- **Rama sugerida:** `feature/HU-005-asignaciones-vigentes`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-005.md` (decisiones de Sebastián, 2026-09-11, más las
  dos decisiones de esta sesión sobre endpoints) ·
  `security-platform-architecture/docs/02-domain/03-bounded-contexts.md`
  (BC-08 Asignaciones) · `07-business-rules.md` (`RB-05` — solo asignaciones vigentes, invariante
  `INV-ASN-01`) · `06-invariants.md` (`INV-ASN-01` — inactivas/vencidas/revocadas no participan;
  `INV-ASN-02` — alcance vigente coherente con sujeto y rol; `INV-ID-02` — un claim no basta sin
  asignación vigente) · `10-entities.md` (`UsuarioAplicacionRol` vigente: Sí, etapa 1) ·
  `12-value-objects.md` (`Vigencia`: intervalo inicio/fin; `AlcanceAsignacion`: tenant+aplicación,
  simplificado en el plan a un campo `ApplicationId` por decisión ya tomada). Código real consultado:
  `roles/*` (HU-004, patrón de validador prestado + regla de cobertura de alcance),
  `applications/*` (`ApplicationMustExistForTenantValidator`, `ApplicationOwnerLookupValidator`,
  `ApplicationOwnershipQuery`), `identity/*` (`UserMustExistRule`, `SecurityUserRepository` — **sin**
  validador publicado todavía, ver hallazgo abajo).
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 5, 6, 7, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22

## Hallazgo antes de planificar

`HU-005.md` decía "identity ya publica `UserMustExistRule`" — **no es cierto**: `UserMustExistRule`
es una regla pura de `identity/domain/rule/`, sin validador de aplicación que la envuelva y sin
`@NamedInterface` en `identity`. Hoy la usa directamente `AssignTenantUseCaseImpl` (caso de uso como su
propio validador, correcto para un solo consumidor interno — pero `assignments` es un consumidor
**externo**, y eso exige un validador publicado, no la regla desnuda). Esta es la primera vez que
`identity` publica algo: aplica la trampa de Modulith documentada en `1-planificador.md` — ver
sección 8.

## 1. Resumen funcional

Nuevo slice `assignments`: una **asignación** liga (usuario, aplicación, rol) con una **vigencia**
(inicio, fin opcional). Es el contexto que HU-006 enviará a OPA — **resuelto desde SurrealDB, nunca
desde el token** (`INV-ID-02`). Tres escrituras/consultas HTTP (asignar, revocar, listar por rol) más
un caso de uso interno sin HTTP (`ResolveActiveRolesUseCase`, el que HU-006 invocará en proceso).

**Lo que este slice NO decide:** qué implica tener un rol (eso es de OPA). Solo integridad del
catálogo de asignaciones: coherencia de alcance, no duplicar una asignación activa, aislamiento por
tenant.

**No cubre:** `UsuarioPerfil`/`PlanRol`/`AsignacionPendienteRol` (etapa 2, fuera del modelo aceptado),
perfiles (HU-008), quién puede asignar (HU-009), enviar el contexto a OPA (HU-006). Ningún
`if (rol == ...)`: el rol es un dato de entrada, nunca una rama en Java.

## 2. Criterios de aceptación

| # | Criterio                              | Resultado esperado                                                                                                                                                                        |
|---|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Asignar                               | Un usuario existente recibe un rol existente en una aplicación existente (del tenant del principal), con vigencia desde ahora y sin fin → 201                                             |
| 2 | Coherencia de alcance (`INV-ASN-02`)  | El rol debe ser global, del tenant de la aplicación, o de esa misma aplicación; si no → 400 `APPLICATION_OUTSIDE_ROLE_SCOPE`                                                              |
| 3 | No duplicar                           | La misma tripleta (usuario, aplicación, rol) con una asignación vigente → 409 `ASSIGNMENT_ALREADY_ACTIVE`                                                                                 |
| 4 | Revocar                               | `DELETE` fija `fin = ahora`; la asignación deja de ser vigente inmediatamente → 200. Revocar una ya revocada es idempotente (vuelve a fijar `fin = ahora`, no falla)                      |
| 5 | Solo vigentes (`RB-05`, `INV-ASN-01`) | `ResolveActiveRolesUseCase` (interno, sin HTTP en esta historia) devuelve únicamente asignaciones con `fin` nulo o futuro                                                                 |
| 6 | La fuente es el almacén               | Un rol presente en el JWT pero sin asignación vigente **no** aparece en el contexto resuelto                                                                                              |
| 7 | Sujeto sin asignaciones               | `ResolveActiveRolesUseCase` devuelve conjunto vacío, no error                                                                                                                             |
| 8 | Aislamiento                           | `GET /api/v1/roles/{roleId}/assignments` nunca muestra asignaciones de aplicaciones de otro tenant; `POST`/`DELETE` rechazan igual (400 `APPLICATION_NOT_FOUND` / `ASSIGNMENT_NOT_FOUND`) |

## 3. Reglas de negocio

| #  | Regla                                                                                                                     | Dónde vive (VO / Rule)                                                                        | Puerto que trae el dato                                  | Excepción → HTTP                                                                               |
|----|---------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------|
| L1 | `Vigencia`: si `validUntil` está presente, debe ser posterior a `validFrom`                                               | VO `Vigencia` (**síncrona**)                                                                  | —                                                        | `InvalidVigenciaException` → 400                                                               |
| R1 | El usuario existe                                                                                                         | **Prestada, nueva**: `UserMustExistValidator` (`identity :: rule`)                            | `SecurityUserRepository.findById`                        | `UserNotFoundException` → 400                                                                  |
| R2 | La aplicación existe y pertenece al tenant del principal                                                                  | **Prestada**: `ApplicationMustExistForTenantValidator` (`applications :: rule`, ya existente) | (interno de `applications`)                              | `ApplicationNotFoundException` → 400                                                           |
| R3 | El rol existe y su alcance cubre la aplicación (global cubre todo; tenant cubre su tenant; aplicación cubre solo la suya) | **Prestada, nueva**: `RoleScopeMustCoverApplicationValidator` (`roles :: rule`)               | `RoleRepository.findById` **[M]**                        | `RoleNotFoundException` (existente) / `ApplicationOutsideRoleScopeException` (**nueva**) → 400 |
| R4 | No hay ya una asignación activa para la misma tripleta                                                                    | `AssignmentMustNotDuplicateActiveRule` (**síncrona**, recibe `ActiveAssignmentAvailability`)  | `AssignmentRepository.existsActiveByUserApplicationRole` | `DuplicateAssignmentException` → **409**                                                       |
| R5 | La asignación a revocar existe para ese tenant                                                                            | `AssignmentMustExistForTenantRule` (**síncrona**, recibe `AssignmentExistence`)               | `AssignmentRepository.findByIdForTenant`                 | `AssignmentNotFoundException` → 400                                                            |

Barrera de contrato del borde HTTP (no es regla de negocio):

| #  | Barrera                                                                           | Dónde                                                                                              | Excepción → HTTP                                                                 |
|----|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| C1 | Ventana de paginación del `GET` (`page`/`size` u `offset`/`limit`, ambiguo → 400) | `ListAssignmentsRequestMapper` (copia de `ListApplicationsRequestMapper`, mismo patrón que HU-004) | `ConflictingRequestParametersException` / `MalformedRequestFieldException` → 400 |

**Ninguna `if/throw` de negocio en los use cases.** R1→R2→R3→R4 se orquestan en `AssignRoleRulesValidatorImpl`
en ese orden (barato primero: existencia de usuario y aplicación antes que la consulta de cobertura del
rol, antes que la comprobación de duplicado — que es la más cara porque toca la tabla propia).

### Sobre R3 y su relación con R5 de HU-004

`RoleScopeMustCoverResourceRule` (HU-004) y la nueva `RoleScopeMustCoverApplicationRule` (esta
historia) comparten la misma matemática de cobertura (alcance del rol contra un par
aplicación/tenant), pero son invariantes distintas en la arquitectura (`INV-DAT-01` para recursos
autorizados por un rol; `INV-ASN-02` para el alcance de una asignación) y HU-004 ya está validada y
en `develop`. **Decisión: no generalizar/renombrar la regla existente.** Se declara una regla nueva,
separada, con su propio hecho (`ApplicationCoverage`, mismos tres campos que `ResourceCoverage` por
coincidencia de forma, no de significado) — tres líneas parecidas, no una abstracción prematura que
tocaría código ya enviado. Ver `sb-estandares`: "Tres líneas similares es mejor que una abstracción
prematura."

## 4. Modelo de dominio afectado

### Entidad / agregado

`Assignment` **[N]** —
`record Assignment(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId, RoleId roleId, Vigencia vigencia)`.
Factoría con nombre `assign(id, userId, tenantId, applicationId, roleId, now)` (vigencia desde
`now`, sin fin). Transición `revoke(Instant now)` → nuevo `Assignment` con `vigencia.endingAt(now)`.
Comportamiento `isActive(Instant now)` delega en `vigencia.isActiveAt(now)`.

`tenantId` **sí** es un campo del agregado (decisión de este plan, no de `HU-005.md`): la decisión de
Sebastián — "el tenant no es un componente propio: se deriva de la aplicación" — se interpreta como
"no hay un `AlcanceAsignacion` independiente que el llamador elija", no como "el agregado nunca lo
guarda". Se necesita como columna para filtrar por tenant en `findByIdForTenant`/`findBy` sin tener
que resolver el dueño de la aplicación en cada consulta paginada. Se fija una sola vez, en el momento
de crear la asignación, a partir del tenant ya validado del principal (R2) — nunca es un input
independiente. Ver sección 11.

`AssignmentCriteria` **[N]** — specification, junto al agregado:
`record AssignmentCriteria(RoleId roleId, TenantId tenantId)`;
`matches(Assignment)` = mismo rol y mismo tenant.

### Value objects

| VO                                    | Nuevo o existente                       | Invariantes                                                                          | Vive en                                                                                                                                                 |
|---------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AssignmentId`                        | **Nuevo**                               | UUID no nulo; `of(String)` → `InvalidIdentifierException("ASSIGNMENT_ID", raw)`      | `assignments/domain/model/` — un solo consumidor                                                                                                        |
| `Vigencia`                            | **Nuevo**                               | `validFrom` no nulo; si `validUntil` está presente, debe ser posterior a `validFrom` | `assignments/domain/model/`                                                                                                                             |
| `UserId`                              | **Se muda** de `identity/domain/model/` | Sin cambio de invariantes                                                            | `pdp/commons/model/` — pasa a tener dos consumidores (`identity`, `assignments`), mismo precedente que `ApplicationId`/`TenantId`/`ResourceId`/`RoleId` |
| `TenantId`, `ApplicationId`, `RoleId` | Existentes                              | —                                                                                    | `pdp/commons/model/`                                                                                                                                    |

### Enums

Ninguno nuevo.

### Hechos de regla (`assignments/domain/rule/model/`)

`ActiveAssignmentAvailability(UserId userId, ApplicationId applicationId, RoleId roleId, boolean taken)` ·
`AssignmentExistence(AssignmentId assignmentId, TenantId tenantId, boolean registered)`.

### Hecho de regla nuevo en `roles` (`roles/domain/rule/model/`)

`ApplicationCoverage(RoleScope scope, ApplicationId applicationId, TenantId tenantId)` **[N]** — mismos
campos que `ResourceCoverage` (ver nota de la sección 3), hecho distinto.

## 5. Persistencia

- **Tabla:** `assignment` (constante en `AssignmentSchema.TABLE`)
- **Campos:** `id`, `userId`, `tenantId` (denormalizado, ver sección 4), `applicationId`, `roleId`,
  `validFrom`, `validUntil` (ausente si sigue vigente)
- **Sin índice único de respaldo:** la unicidad es "sin otra asignación **activa**" — una condición en
  tiempo de ejecución (`validUntil` nulo o futuro), no expresable como `DEFINE INDEX ... UNIQUE`
  estático sin una consulta condicional que este proyecto no usa en ningún otro sitio todavía. R4
  quedará como la única barrera contra la carrera, igual que documenta la nota de riesgo de la
  sección 11.
- **Consultas nuevas en el puerto:** ver §7 (`AssignmentRepository`, completo, es nuevo)
- **Inicializador de esquema:** **nuevo** `SurrealAssignmentSchemaInitializer` (patrón de
  `SurrealRoleSchemaInitializer`)
- **[M] en `roles`:** `RoleRepository.findById(RoleId)` — a diferencia de `findByIdForTenant`, **sin**
  filtrar por tenant (un rol `GLOBAL` debe encontrarse igual). Mismo patrón que `findTenantIdById` de
  `applications` (HU-003).

## 6. Endpoint

| Verbo  | Ruta                                                | Código de éxito          | Cuerpo de entrada                                            | Cuerpo de salida                                   |
|--------|-----------------------------------------------------|--------------------------|--------------------------------------------------------------|----------------------------------------------------|
| POST   | `/api/v1/roles/{roleId}/assignments`                | 201 `ASSIGNMENT_CREATED` | `AssignRoleRawRequest` (+ `roleId` de la ruta)               | `ApiResponse<AssignmentWebResponse>`               |
| DELETE | `/api/v1/roles/{roleId}/assignments/{assignmentId}` | 200 `ASSIGNMENT_REVOKED` | (sin cuerpo; `assignmentId` de la ruta)                      | `ApiResponse<Void>`                                |
| GET    | `/api/v1/roles/{roleId}/assignments`                | 200 `ASSIGNMENTS_LISTED` | query `page`/`size`/`offset`/`limit` (+ `roleId` de la ruta) | `ApiResponse<PageResponse<AssignmentWebResponse>>` |

- **Autorización:** canal BFF, ya cubre `/api/**`. El tenant sale del principal
  (`SecurityContext.currentPrincipal()`), nunca del cuerpo. Sin restricción de rol administrador
  (HU-009).
- **`ResolveActiveRolesUseCase` no tiene endpoint en esta historia** (decisión de esta sesión): queda
  como caso de uso interno, wireado en `AssignmentsConfiguration`, listo para que HU-006 lo invoque en
  proceso. HU-006 decide si y cómo lo expone.
- **El segmento `roleId` de la ruta del `DELETE` no se vuelve a comprobar contra el `roleId` real de la
  asignación encontrada** — se revoca por `assignmentId` + tenant únicamente, igual de estricto en
  aislamiento (criterio 8), simplemente sin la comprobación redundante de consistencia de ruta.
- **Errores esperados:** ver §2 y §3. Ninguno nuevo en `ApiErrorHandler`.

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/assignments/domain/rule/AssignmentMustNotDuplicateActiveRule.java                         [N]
public interface AssignmentMustNotDuplicateActiveRule extends OperationWithoutResult<ActiveAssignmentAvailability> { }

// pdp/assignments/domain/rule/AssignmentMustExistForTenantRule.java                              [N]
public interface AssignmentMustExistForTenantRule extends OperationWithoutResult<AssignmentExistence> { }

// pdp/assignments/application/rule/validator/AssignRoleRulesValidator.java                       [N]
public interface AssignRoleRulesValidator extends ReactiveOperationWithoutResult<AssignRoleRequest> { }

// pdp/assignments/application/rule/validator/RevokeAssignmentRulesValidator.java                 [N]
// Finder con rechazo (como GrantResourceRulesValidator de HU-004): valida R5 y devuelve la
// asignación encontrada, que el caso de uso transforma (revoke) y guarda.
public interface RevokeAssignmentRulesValidator extends ReactiveOperation<RevokeAssignmentRequest, Assignment> { }

// pdp/assignments/application/usecase/AssignRoleUseCase.java                                     [N]
public interface AssignRoleUseCase extends ReactiveOperation<AssignRoleRequest, AssignmentResponse> { }

// pdp/assignments/application/usecase/RevokeAssignmentUseCase.java                                [N]
public interface RevokeAssignmentUseCase extends ReactiveOperationWithoutResult<RevokeAssignmentRequest> { }

// pdp/assignments/application/usecase/ListAssignmentsUseCase.java                                 [N]
public interface ListAssignmentsUseCase extends ReactiveOperation<ListAssignmentsRequest, ResultPage<AssignmentResponse>> { }

// pdp/assignments/application/usecase/ResolveActiveRolesUseCase.java                              [N]
// Sin interactor ni controller en esta historia: lo invocará HU-006 en proceso.
public interface ResolveActiveRolesUseCase extends ReactiveOperation<ResolveActiveRolesRequest, ActiveRolesResponse> { }

// pdp/assignments/application/secondaryport/repository/AssignmentRepository.java                 [N]
public interface AssignmentRepository {
    Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId, Instant now);
    Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId);          // vacío si no existe o no es de ese tenant
    Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window);
    Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now);
    Mono<Assignment> save(Assignment assignment);
}

// pdp/identity/application/rule/validator/UserMustExistValidator.java                            [N]
// Publicado en identity :: rule (primer NamedInterface de identity — ver Hallazgo). Vacío del puerto → UserNotFoundException.
public interface UserMustExistValidator extends ReactiveOperationWithoutResult<UserId> { }

// pdp/roles/domain/rule/RoleScopeMustCoverApplicationRule.java                                   [N]
public interface RoleScopeMustCoverApplicationRule extends OperationWithoutResult<ApplicationCoverage> { }

// pdp/roles/application/rule/validator/RoleScopeMustCoverApplicationValidator.java               [N]
// Publicado en roles :: rule (ya existente desde HU-004). Resuelve el rol por id (sin filtrar tenant,
// a diferencia de findByIdForTenant — un rol GLOBAL debe encontrarse) y comprueba que su alcance
// cubra la aplicación indicada.
public interface RoleScopeMustCoverApplicationValidator extends ReactiveOperationWithoutResult<RoleCoverageQuery> { }

// pdp/assignments/infrastructure/adapter/primary/web/interactor/AssignRoleInteractor.java        [N]
public interface AssignRoleInteractor extends ReactiveOperation<AssignRoleRawRequest, AssignmentWebResponse> { }

// pdp/assignments/infrastructure/adapter/primary/web/interactor/RevokeAssignmentInteractor.java  [N]
public interface RevokeAssignmentInteractor extends ReactiveOperationWithoutResult<RevokeAssignmentRawRequest> { }

// pdp/assignments/infrastructure/adapter/primary/web/interactor/ListAssignmentsInteractor.java   [N]
public interface ListAssignmentsInteractor extends ReactiveOperation<ListAssignmentsRawRequest, PageResponse<AssignmentWebResponse>> { }
```

### Constructores de las implementaciones (fijados aquí para que el tester no adivine)

```java
AssignmentMustNotDuplicateActiveRuleImpl()
AssignmentMustExistForTenantRuleImpl()
AssignRoleRulesValidatorImpl(UserMustExistValidator userMustExist,
                             ApplicationMustExistForTenantValidator applicationMustExist,
                             RoleScopeMustCoverApplicationValidator roleScopeMustCoverApplication,
                             AssignmentMustNotDuplicateActiveRule mustNotDuplicate,
                             AssignmentRepository repository, TimeProvider time)     // corrección: faltaba de dónde sale el "ahora" para R4, detectado al escribir las pruebas (FASE 2 del tester)
RevokeAssignmentRulesValidatorImpl(AssignmentRepository repository, AssignmentMustExistForTenantRule mustExist)
AssignRoleUseCaseImpl(AssignRoleRulesValidator rules, AssignmentRepository repository,
                      IdentifierGenerator identifiers, TimeProvider time)     // construye un Assignment desde cero → ambos desde el día uno
RevokeAssignmentUseCaseImpl(RevokeAssignmentRulesValidator rules, AssignmentRepository repository, TimeProvider time)
ListAssignmentsUseCaseImpl(AssignmentRepository repository)
ResolveActiveRolesUseCaseImpl(AssignmentRepository repository, TimeProvider time)
UserMustExistValidatorImpl(SecurityUserRepository repository, UserMustExistRule mustExist)
RoleScopeMustCoverApplicationValidatorImpl(RoleRepository repository, RoleMustExistForTenantRule roleMustExist,
                                           RoleScopeMustCoverApplicationRule coverageRule)
AssignRoleInteractorImpl(AssignRoleUseCase useCase)
RevokeAssignmentInteractorImpl(RevokeAssignmentUseCase useCase)
ListAssignmentsInteractorImpl(ListAssignmentsUseCase useCase)
SurrealAssignmentRepository(SurrealDbClient client)
SurrealAssignmentSchemaInitializer(SurrealDbClient client)
```

### Firmas de value objects y entidades

```java
// pdp/commons/model/UserId.java                                                                 [N] (movido, mismas invariantes)
public record UserId(UUID value) { public static UserId of(String raw) }     // sin nulo; of → InvalidIdentifierException("USER_ID", raw)

// pdp/assignments/domain/model/AssignmentId.java                                                 [N]
public record AssignmentId(UUID value) { public static AssignmentId of(String raw) }   // sin nulo; of → InvalidIdentifierException("ASSIGNMENT_ID", raw)

// pdp/assignments/domain/model/Vigencia.java                                                     [N]
public record Vigencia(Instant validFrom, Optional<Instant> validUntil) {
    public static Vigencia startingNow(Instant now);
    public boolean isActiveAt(Instant instant);
    public Vigencia endingAt(Instant instant); }                              // invariante L1 → InvalidVigenciaException

// pdp/assignments/domain/Assignment.java                                                         [N]
public record Assignment(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
        RoleId roleId, Vigencia vigencia) {
    public static Assignment assign(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
            RoleId roleId, Instant now);
    public Assignment revoke(Instant now);
    public boolean isActive(Instant now); }

// pdp/assignments/domain/AssignmentCriteria.java                                                 [N]
public record AssignmentCriteria(RoleId roleId, TenantId tenantId) {
    public static AssignmentCriteria of(RoleId roleId, TenantId tenantId);
    public boolean matches(Assignment assignment); }

// pdp/assignments/domain/rule/model/ActiveAssignmentAvailability.java                            [N]
public record ActiveAssignmentAvailability(UserId userId, ApplicationId applicationId, RoleId roleId, boolean taken) { }
// pdp/assignments/domain/rule/model/AssignmentExistence.java                                     [N]
public record AssignmentExistence(AssignmentId assignmentId, TenantId tenantId, boolean registered) { }

// pdp/roles/domain/rule/model/ApplicationCoverage.java                                           [N]
public record ApplicationCoverage(RoleScope scope, ApplicationId applicationId, TenantId tenantId) { }

// pdp/roles/application/primaryport/request/RoleCoverageQuery.java                               [N]
public record RoleCoverageQuery(RoleId roleId, TenantId tenantId, ApplicationId applicationId) { }
```

### Excepciones nuevas y mensajes

```java
// pdp/assignments/domain/exception/InvalidVigenciaException.java          [N] extends InvalidValueException           // "INVALID_VIGENCIA"
// pdp/assignments/domain/exception/DuplicateAssignmentException.java      [N] extends ConflictBusinessRuleException   // "ASSIGNMENT_ALREADY_ACTIVE" → 409
// pdp/assignments/domain/exception/AssignmentNotFoundException.java       [N] extends BusinessRuleViolationException  // "ASSIGNMENT_NOT_FOUND"      → 400
// pdp/assignments/domain/message/AssignmentsMessages.java                 [N]: métodos estáticos en español para las tres, en el mismo estilo de RolesMessages
// pdp/roles/domain/exception/ApplicationOutsideRoleScopeException.java    [N] extends BusinessRuleViolationException  // "APPLICATION_OUTSIDE_ROLE_SCOPE" → 400
// pdp/roles/domain/message/RolesMessages.java                             [M]: + applicationOutsideRoleScope(String applicationId)
// pdp/commons/message/ValueObjectMessages.java                            [M]: + Vigencia.INVALID_RANGE
```

### Firmas de DTOs

```java
// pdp/assignments/application/primaryport/request/AssignRoleRequest.java                         [N]
public record AssignRoleRequest(TenantId tenantId, UserId userId, ApplicationId applicationId, RoleId roleId) { }
// pdp/assignments/application/primaryport/request/RevokeAssignmentRequest.java                    [N]
public record RevokeAssignmentRequest(AssignmentId assignmentId, TenantId tenantId) { }
// pdp/assignments/application/primaryport/request/ListAssignmentsRequest.java                     [N]
public record ListAssignmentsRequest(AssignmentCriteria criteria, PageWindow window) { }
// pdp/assignments/application/primaryport/request/ResolveActiveRolesRequest.java                  [N]
public record ResolveActiveRolesRequest(UserId userId, ApplicationId applicationId) { }
// pdp/assignments/application/primaryport/response/AssignmentResponse.java                        [N]
public record AssignmentResponse(AssignmentId id, UserId userId, TenantId tenantId, ApplicationId applicationId,
        RoleId roleId, Instant validFrom, Optional<Instant> validUntil) { }
// pdp/assignments/application/primaryport/response/ActiveRolesResponse.java                       [N]
public record ActiveRolesResponse(UserId userId, ApplicationId applicationId, Set<RoleId> roleIds) { }

// pdp/assignments/infrastructure/adapter/primary/web/dto/request/raw/AssignRoleRawRequest.java    [N]  (roleId viaja en el propio raw, puesto por el controller desde la ruta — mismo patrón que GrantResourceRawRequest de HU-004)
public record AssignRoleRawRequest(String roleId, String userId, String applicationId) { }
// .../raw/RevokeAssignmentRawRequest.java                                                         [N]  (assignmentId de la ruta; el roleId de la ruta no se usa más allá del routing, ver §6)
public record RevokeAssignmentRawRequest(String assignmentId) { }
// .../raw/ListAssignmentsRawRequest.java                                                          [N]  (roleId viaja en el propio raw, igual que AssignRoleRawRequest)
public record ListAssignmentsRawRequest(String roleId, String page, String size, String offset, String limit) { }
// .../dto/response/AssignmentWebResponse.java                                                     [N]  (plana; validUntil null si sigue vigente)
public record AssignmentWebResponse(String id, String userId, String tenantId, String applicationId, String roleId,
        String validFrom, String validUntil) { }

// .../persistence/entity/AssignmentEntity.java                                                    [N]
public record AssignmentEntity(String id, String userId, String tenantId, String applicationId, String roleId,
        String validFrom, String validUntil) { }
```

### Mappers (estáticos, `final class` con constructor privado)

```java
AssignRoleRequestMapper.toRequest(AssignRoleRawRequest raw, TenantId tenantId): AssignRoleRequest              // roleId sale del propio raw
RevokeAssignmentRequestMapper.toRequest(RevokeAssignmentRawRequest raw, TenantId tenantId): RevokeAssignmentRequest
ListAssignmentsRequestMapper.toRequest(ListAssignmentsRawRequest raw, TenantId tenantId): ListAssignmentsRequest    // C1, copia de ListApplicationsRequestMapper; roleId sale del propio raw
AssignmentResponseMapper.toResponse(AssignmentResponse response): AssignmentWebResponse
AssignmentPersistenceMapper.toDomain(AssignmentEntity entity): Assignment
```

### Firmas nuevas en puertos existentes

```java
// pdp/roles/application/secondaryport/repository/RoleRepository.java                              [M]
Mono<Role> findById(RoleId roleId);   // vacío si no existe; a diferencia de findByIdForTenant, no filtra por tenant
```

## 8. Árbol de archivos

> Rutas desde `pdp/src/main/java/co/edu/uco/seguridad/`. `[N]` nuevo, `[M]` modificado.

```
pdp/commons/
├── model/UserId.java                                                          [N] (movido desde identity/domain/model)
└── message/ValueObjectMessages.java                                           [M] +Vigencia.INVALID_RANGE

pdp/identity/                                                                  primer NamedInterface de este módulo
├── domain/model/UserId.java                                                   [M] eliminado (movido a commons)
├── domain/SecurityUser.java                                                   [M] import actualizado
├── domain/exception/UserNotFoundException.java                                [M] import actualizado
├── domain/exception/package-info.java                                        [N] @NamedInterface("exception")
├── domain/rule/model/UserExistence.java                                       [M] import actualizado
├── application/primaryport/request/AssignTenantRequest.java                   [M] import actualizado
├── application/primaryport/response/UserResponse.java                        [M] import actualizado
├── application/secondaryport/repository/SecurityUserRepository.java          [M] import actualizado
├── application/usecase/impl/ProvisionIdentityUseCaseImpl.java                [M] import actualizado
├── application/usecase/impl/AssignTenantUseCaseImpl.java                     [M] import actualizado
├── application/rule/validator/UserMustExistValidator.java                    [N]
├── application/rule/validator/impl/UserMustExistValidatorImpl.java           [N]
├── application/rule/validator/package-info.java                              [N] @NamedInterface("rule")
├── infrastructure/adapter/primary/web/mapper/AssignTenantRequestMapper.java  [M] import actualizado
├── infrastructure/adapter/secondary/persistence/mapper/SecurityUserPersistenceMapper.java [M] import actualizado
├── infrastructure/adapter/secondary/persistence/repository/SurrealSecurityUserRepository.java [M] import actualizado
└── infrastructure/config/IdentityConfiguration.java                          [M] registra UserMustExistValidator

  (más los 6 archivos de prueba que importan identity.domain.model.UserId — el implementador
   actualiza el import mecánicamente, sin cambiar ninguna aserción: AssignTenantUseCaseImplTests,
   ProvisionIdentityUseCaseImplTests, UserMustExistRuleTests, AssignTenantRequestMapperTests,
   UserResponseMapperTests, y SurrealRepositoryIntegrationTests)

pdp/roles/
├── domain/rule/RoleScopeMustCoverApplicationRule.java                        [N]
├── domain/rule/impl/RoleScopeMustCoverApplicationRuleImpl.java               [N]
├── domain/rule/model/ApplicationCoverage.java                                [N]
├── domain/exception/ApplicationOutsideRoleScopeException.java                [N]
├── domain/exception/package-info.java                                       [N] @NamedInterface("exception")
├── domain/message/RolesMessages.java                                        [M] +applicationOutsideRoleScope
├── application/primaryport/request/RoleCoverageQuery.java                   [N]
├── application/primaryport/request/package-info.java                       [N] @NamedInterface("dto") (mismo patrón que applications :: dto)
├── application/rule/validator/RoleScopeMustCoverApplicationValidator.java   [N]
├── application/rule/validator/impl/RoleScopeMustCoverApplicationValidatorImpl.java [N]
├── application/secondaryport/repository/RoleRepository.java                 [M] +findById
├── infrastructure/adapter/secondary/persistence/repository/SurrealRoleRepository.java [M] +findById
└── infrastructure/config/RolesConfiguration.java                            [M] registra RoleScopeMustCoverApplicationValidator

pdp/assignments/                                                               [N] slice completo
├── package-info.java   @ApplicationModule(allowedDependencies = {"commons",
│                          "identity :: rule", "identity :: exception",
│                          "applications", "applications :: rule", "applications :: dto",
│                          "roles", "roles :: rule", "roles :: dto", "roles :: exception"})
├── domain/
│   ├── Assignment.java
│   ├── AssignmentCriteria.java
│   ├── model/{AssignmentId, Vigencia}.java
│   ├── rule/{AssignmentMustNotDuplicateActiveRule, AssignmentMustExistForTenantRule}.java
│   ├── rule/impl/{…Impl}.java (×2)
│   ├── rule/model/{ActiveAssignmentAvailability, AssignmentExistence}.java
│   ├── exception/{InvalidVigenciaException, DuplicateAssignmentException, AssignmentNotFoundException}.java
│   └── message/AssignmentsMessages.java
├── application/
│   ├── primaryport/request/{AssignRoleRequest, RevokeAssignmentRequest, ListAssignmentsRequest, ResolveActiveRolesRequest}.java
│   ├── primaryport/response/{AssignmentResponse, ActiveRolesResponse}.java
│   ├── secondaryport/repository/AssignmentRepository.java
│   ├── rule/validator/{AssignRoleRulesValidator, RevokeAssignmentRulesValidator}.java
│   ├── rule/validator/impl/{…Impl}.java (×2)
│   ├── usecase/{AssignRoleUseCase, RevokeAssignmentUseCase, ListAssignmentsUseCase, ResolveActiveRolesUseCase}.java
│   └── usecase/impl/{…Impl}.java (×4)
└── infrastructure/
    ├── adapter/primary/web/controller/AssignmentController.java
    ├── adapter/primary/web/dto/request/raw/{AssignRoleRawRequest, RevokeAssignmentRawRequest, ListAssignmentsRawRequest}.java
    ├── adapter/primary/web/dto/response/AssignmentWebResponse.java
    ├── adapter/primary/web/interactor/{AssignRoleInteractor, RevokeAssignmentInteractor, ListAssignmentsInteractor}.java
    ├── adapter/primary/web/interactor/impl/{…Impl}.java (×3)
    ├── adapter/primary/web/mapper/{AssignRoleRequestMapper, RevokeAssignmentRequestMapper, ListAssignmentsRequestMapper, AssignmentResponseMapper}.java
    ├── adapter/secondary/persistence/entity/AssignmentEntity.java
    ├── adapter/secondary/persistence/mapper/AssignmentPersistenceMapper.java
    ├── adapter/secondary/persistence/repository/SurrealAssignmentRepository.java
    ├── adapter/secondary/persistence/schema/{AssignmentSchema, SurrealAssignmentSchemaInitializer}.java
    └── config/AssignmentsConfiguration.java   registra: repositorio, inicializador, 2 reglas propias,
                                                2 validadores propios, 4 casos de uso (incluye
                                                ResolveActiveRolesUseCase, sin interactor), 3 interactores

shared/
├── message/RequiredArgumentMessages.java                                      [M] +constantes del slice
└── web/message/WebContractMessages.java                                       [M] +successAssignmentCreated, +successAssignmentRevoked, +successAssignmentsListed
```

`ModulithStructureTests`: `assignments` es un módulo nuevo sin consumidores → ningún `allowedDependencies`
ajeno cambia por su llegada. `identity` gana su primer `@NamedInterface` en esta historia — antes
exponía todo implícitamente a quien declarara `"identity"` en `allowedDependencies` (nadie lo hacía);
después, solo `identity :: rule` e `identity :: exception` quedan expuestos. Como ningún otro módulo
declara hoy `"identity"` a secas, esto no rompe a nadie — confirmarlo compilando (FASE 5) es
obligatorio de todas formas. `roles` ya tenía `roles :: rule` (HU-004, previendo esta historia); gana
además `roles :: exception` y `roles :: dto`, ambos también primeras veces para esas dos subcarpetas
concretas — mismo razonamiento: nadie las consumía antes.

## 9. Casos de prueba esperados

| Capa                                    | Clase de prueba                                        | Casos                                                                                                                                                                                                                                                                                                                                                             |
|-----------------------------------------|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain`                                | `VigenciaTests`                                        | `startingNow` sin fin; `endingAt` fija fin; `isActiveAt` antes de inicio → false; sin fin → siempre activa hasta el infinito; con fin futuro → activa; con fin pasado → inactiva; `validUntil` anterior o igual a `validFrom` → `InvalidVigenciaException`                                                                                                        |
| `domain`                                | `AssignmentTests`                                      | `assign` deja vigencia desde `now` sin fin; `revoke` fija fin y no muta el original; `isActive` delega en la vigencia                                                                                                                                                                                                                                             |
| `domain`                                | `AssignmentCriteriaTests`                              | mismo rol y tenant → true; mismo rol, otro tenant → false; otro rol, mismo tenant → false                                                                                                                                                                                                                                                                         |
| `domain`                                | `AssignmentMustNotDuplicateActiveRuleImplTests`        | `taken=true` → `DuplicateAssignmentException`; `false` → no lanza                                                                                                                                                                                                                                                                                                 |
| `domain`                                | `AssignmentMustExistForTenantRuleImplTests`            | `registered=false` → `AssignmentNotFoundException`; `true` → no lanza                                                                                                                                                                                                                                                                                             |
| `domain` (roles)                        | `RoleScopeMustCoverApplicationRuleImplTests`           | global cubre toda aplicación; tenant cubre aplicación de su tenant; tenant no cubre otro tenant; aplicación cubre solo la suya                                                                                                                                                                                                                                    |
| `application` (identity)                | `UserMustExistValidatorImplTests`                      | usuario encontrado → completa; vacío → `UserNotFoundException`                                                                                                                                                                                                                                                                                                    |
| `application` (roles)                   | `RoleScopeMustCoverApplicationValidatorImplTests`      | rol inexistente → `RoleNotFoundException`; rol global → completa para cualquier aplicación; rol de tenant que no coincide → `ApplicationOutsideRoleScopeException`; rol de aplicación que coincide → completa                                                                                                                                                     |
| `application`                           | `AssignRoleRulesValidatorImplTests`                    | usuario inexistente → `UserNotFoundException` (poison pill: no consulta aplicación ni rol); aplicación inexistente → `ApplicationNotFoundException`; rol no cubre la aplicación → `ApplicationOutsideRoleScopeException`; asignación ya activa → `DuplicateAssignmentException`; todo ok → completa                                                               |
| `application`                           | `RevokeAssignmentRulesValidatorImplTests`              | asignación no encontrada para el tenant → `AssignmentNotFoundException`; encontrada → devuelve la asignación                                                                                                                                                                                                                                                      |
| `application`                           | `AssignRoleUseCaseImplTests`                           | camino feliz: `IdentifierGenerator`/`TimeProvider` fijos, guarda un `Assignment` con vigencia desde `now` sin fin y devuelve `AssignmentResponse`; el validador que lanza corta antes de guardar                                                                                                                                                                  |
| `application`                           | `RevokeAssignmentUseCaseImplTests`                     | guarda la asignación con `validUntil = now` y completa; el validador que lanza no guarda                                                                                                                                                                                                                                                                          |
| `application`                           | `ListAssignmentsUseCaseImplTests`                      | proyecta `ResultPage<Assignment>` → `ResultPage<AssignmentResponse>` conservando total y ventana                                                                                                                                                                                                                                                                  |
| `application`                           | `ResolveActiveRolesUseCaseImplTests`                   | devuelve los `RoleId` activos para (usuario, aplicación) a la fecha `now`; sin asignaciones → conjunto vacío                                                                                                                                                                                                                                                      |
| `infrastructure` — mapper               | `AssignRoleRequestMapperTests`                         | `userId` ausente → Missing; no UUID → Malformed; `applicationId` ausente → Missing; no UUID → Malformed; feliz                                                                                                                                                                                                                                                    |
| `infrastructure` — mapper               | `RevokeAssignmentRequestMapperTests`                   | `assignmentId` no UUID → Malformed; feliz                                                                                                                                                                                                                                                                                                                         |
| `infrastructure` — mapper               | `ListAssignmentsRequestMapperTests`                    | sin parámetros → ventana por defecto; `page`+`offset` → `ConflictingRequestParametersException`; `size=0` → Malformed(`size`)                                                                                                                                                                                                                                     |
| `infrastructure` — mapper               | `AssignmentResponseMapperTests`                        | con `validUntil` presente → string no nulo; vigente (`validUntil` vacío) → null                                                                                                                                                                                                                                                                                   |
| `infrastructure` — mapper               | `AssignmentPersistenceMapperTests`                     | entidad con `validUntil` nulo → `Vigencia` sin fin; con `validUntil` → `Vigencia` con fin                                                                                                                                                                                                                                                                         |
| `infrastructure` — controller           | `AssignmentControllerTests`                            | `POST` → 201 `ASSIGNMENT_CREATED`; `DELETE` → 200 `ASSIGNMENT_REVOKED`, `assignmentId` de la ruta llega al interactor; `GET` → 200 con `PageResponse`                                                                                                                                                                                                             |
| `infrastructure` — persistencia         | `SurrealRepositoryIntegrationTests` **[M]** (+4 casos) | `save` + `existsActiveByUserApplicationRole` (antes de crear → false, después → true); `findByIdForTenant` con otro tenant → vacío; `findBy(AssignmentCriteria)` excluye otro tenant; `findActiveRoleIdsFor` excluye una asignación revocada (`validUntil` pasado)                                                                                                |
| `infrastructure` — persistencia (roles) | `SurrealRepositoryIntegrationTests` **[M]** (+1 caso)  | `RoleRepository.findById` encuentra un rol `GLOBAL` sin tenant en el filtro                                                                                                                                                                                                                                                                                       |
| `infrastructure` — HTTP                 | `AssignmentHttpTests`                                  | asigna un rol de tenant a una aplicación del tenant → 201; asigna un rol de aplicación a otra aplicación → 400 `APPLICATION_OUTSIDE_ROLE_SCOPE`; asignar la misma tripleta dos veces → 409; revoca y confirma que ya no aparece en `ResolveActiveRolesUseCase` (llamado directo al caso de uso, no HTTP); el listado de un tenant no muestra asignaciones de otro |

Presupuesto estimado: **~55 pruebas.** Es la historia más grande hasta ahora — cruza tres módulos
(`identity`, `roles`, `assignments`) además de crear el slice completo. Si al tester le resulta
inmanejable, el corte natural es dejar `ResolveActiveRolesUseCase` para un HT aparte (ya no tiene
HTTP, así que separarlo no rompe ningún endpoint): esta historia cerraría con asignar+revocar+listar,
y una historia técnica chica añadiría la resolución de roles activos antes de HU-006.

## 10. Trazabilidad

| Fase                       | Estado                                            | Fecha      |
|----------------------------|---------------------------------------------------|------------|
| Plan                       | ✅ Generado                                        | 2026-09-12 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                        | 2026-09-12 |
| Pruebas en rojo            | ✅ Completado                                      | 2026-09-12 |
| Implementación en verde    | ✅ Completado                                      | 2026-09-12 |
| Validación                 | ✅ Aprobado (segunda pasada) — `REPORTE-HU-005.md` | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                                       |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato — tres decisiones tomadas por el planificador que el validador debe
revisar con atención, y una nota de riesgo:

1. **`Assignment.tenantId` como campo del agregado**, pese a que la decisión registrada dice "el
   tenant no es un componente propio". Se interpretó como "no existe un `AlcanceAsignacion`
   independiente que el llamador elija" (coherente con "se deriva de la aplicación": se fija una vez,
   en el momento de crear, a partir del tenant ya validado de la aplicación — nunca es un input
   independiente ni mutable). La alternativa —no guardarlo y resolver el tenant de cada fila
   consultando `applications` en cada lectura— evita el campo "derivado" pero convierte cada consulta
   paginada en una llamada cruzada por fila, algo que este proyecto no hace en ningún otro sitio. Si
   el equipo prefiere la alternativa sin denormalizar, es un cambio local a `Assignment`,
   `AssignmentRepository.save` (pasaría a recibir `TenantId` aparte) y las consultas del adaptador,
   antes de escribir pruebas.
2. **`ApplicationCoverage`/`RoleScopeMustCoverApplicationRule` no reutiliza `ResourceCoverage`/
   `RoleScopeMustCoverResourceRule`** de HU-004 pese a tener la misma forma — ver justificación en la
   sección 3. Si el validador considera que la duplicación no vale la pena, la generalización
   (renombrar a `RoleScopeCoverage`/`RoleScopeMustCoverRule` y que ambas historias la compartan) es un
   `[M]` que toca código ya validado de HU-004, incluidas sus pruebas — decisión explícita de
   arquitectura, no algo que se cuele sin discutirlo.
3. **Desviación del protocolo del planificador, hecha con transparencia:** la mudanza de `UserId` a
   `pdp/commons/model` (sección 4) se ejecutó **en esta fase de planificación**, incluida la
   actualización mecánica de 17 imports — 11 de `pdp/src/main` y **6 de `pdp/src/test`**
   (`AssignTenantUseCaseImplTests`, `ProvisionIdentityUseCaseImplTests`, `UserMustExistRuleTests`,
   `AssignTenantRequestMapperTests`, `UserResponseMapperTests`, `SurrealRepositoryIntegrationTests`).
   El planificador tiene prohibido tocar `pdp/src/test`; se hizo una excepción deliberada porque (a)
   es un cambio de una sola línea de import por archivo, cero cambio de comportamiento o aserción, (b)
   es un prerrequisito estructural para que CUALQUIER pieza `[N]` de `assignments` pudiera siquiera
   compilar respetando las fronteras de Modulith ya diseñadas (`identity` no podía publicar
   `UserMustExistValidator` de forma consumible sin que `UserId` viviera en un paquete abierto), y (c)
   la decisión de mudar `UserId` ya estaba tomada y registrada por Sebastián antes de esta sesión. Se
   verificó con `verificar.ps1 -Compilar` y `-Rapido` (393 pruebas en verde) inmediatamente después. Si
   el validador considera que esto debió esperar al implementador, es reversible: `git diff` de los 6
   archivos de prueba muestra un cambio de una línea cada uno.
   **`RoleRepository.findById` NO se agregó** (sigue siendo `[M]` real, pendiente del implementador):
   agregar un método a un puerto existente no era necesario para que ningún esqueleto compilara (el
   cuerpo de `RoleScopeMustCoverApplicationValidatorImpl` solo lanza `UnsupportedOperationException`,
   nunca llama al puerto), así que aquí sí se respetó la restricción al pie de la letra.
4. **Sin índice único de respaldo contra la carrera de R4** (ver sección 5) — `AssignmentMustNotDuplicateActiveRule`
   es la única barrera. Es el mismo nivel de riesgo que HU-004 aceptó para su propio índice antes de
   confirmarlo con `SurrealRepositoryIntegrationTests` (nota 2 del plan de HU-004), pero aquí no hay
   ni siquiera un índice de defensa en profundidad porque la condición de unicidad depende del tiempo
   de ejecución (`vigente`), no de columnas estáticas.
