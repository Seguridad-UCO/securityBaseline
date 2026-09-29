# PLAN: HU-011 — Perfiles: agrupación de roles

## Metadata

- **ID:** HU-011
- **Slice:** `nuevo: profiles` (BC-05), con `[M]` en `roles` y `assignments` (BC-08)
- **Tipo:** Mixto (catálogo de escritura + consulta, más una escritura de orquestación en `assignments`)
- **Fecha:** 2026-09-12
- **Rama sugerida:** `feature/HU-011-perfiles-agrupacion-roles`
- **Fuentes:**
    - `pdp/docs/ai-harness/workspace/HU-011.md` (historia dictada; renumerada desde HU-008 el
      2026-09-12 por colisión con la historia de identidad ya fusionada bajo ese ID — ver la nota en
      el propio archivo)
    - `security-platform-architecture/docs/02-domain/03-bounded-contexts.md` (BC-05 Perfiles, BC-08
      Asignaciones) y `10-entities.md` (MVP vs. diferido: `Perfil`/`PerfilRol` y `UsuarioPerfil` en
      Etapa 2)
    - Diagramas `docs/02-domain/diagrams/models/05-perfiles-full.html` (Perfil 1 → PerfilRol 1..* →
      Rol; depende de Roles y Tenants) y `08-asignaciones-full.html` ("UsuarioPerfil ... generate
      UsuarioAplicacionRol records" — confirma materialización, no resolución en consulta)
    - Código real: `Role`/`RoleScope`/`RoleRepository`/`DefineRoleUseCaseImpl`/
      `GrantResourceToRoleUseCaseImpl` (patrón exacto a espejar para el catálogo), `Assignment`/
      `AssignRoleUseCaseImpl`/`RevokeAssignmentUseCaseImpl`/`AssignmentRepository` (reutilizados
      directamente, sin duplicar su lógica, para materializar y revocar)
    - Tres decisiones confirmadas por Sebastián el 2026-09-12 (ver §11): alcance propio (`RoleScope`),
      `applicationId` explícito al asignar, revocación en cascada
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 7, 9, 11, 12, 13, 14, 15, 16, 17, 18, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — el reparto de responsabilidad ya está en la arquitectura, no hay que inventarlo

BC-05 (Perfiles) y BC-08 (Asignaciones) son bounded contexts distintos, igual que `roles` y
`assignments` ya son slices separados hoy. El diagrama de Asignaciones dice literalmente que
`UsuarioPerfil` **genera** registros `UsuarioAplicacionRol` — confirma la respuesta de Sebastián
(materializar, no resolver en consulta) con una fuente independiente de su respuesta. Por eso esta
historia reparte así:

- **`profiles` (nuevo slice, BC-05):** el catálogo — `Perfil` agrupa `Rol`es. Espeja `roles` (BC-04)
  clase por clase: `Profile`/`Role`, `ProfileName`/`RoleName`, `DefineProfileUseCase`/
  `DefineRoleUseCase`, etc.
- **`assignments` (BC-08, ya existe): gana capacidad nueva.** Asignar un perfil a un usuario
  **reutiliza `AssignRoleUseCase` una vez por cada rol del perfil** (mismo módulo, sin cruzar
  frontera) y persiste un nuevo agregado `ProfileAssignment` que recuerda qué `Assignment` generó,
  para poder revocarlos en cascada. No se reimplementa ninguna regla de asignación ya escrita.

### Hallazgo 2 — el contenido del perfil no necesita una regla de cobertura nueva

Se consideró una regla "el alcance del perfil debe cubrir el alcance de cada rol que agrupa" (por
simetría con `RoleScopeMustCoverResourceRule`, que sí es necesaria porque protege contra fuga de
alcance). No hace falta: cuando el perfil se **asigne** a un usuario, cada rol se asigna con
`AssignRoleUseCase`, que **ya** aplica `RoleScopeMustCoverApplicationRule` por su cuenta con el
`applicationId` real de la asignación. Añadir la regla en el catálogo sería defensa en profundidad
redundante, no una necesidad — y "no inventes una regla que nadie pidió" (`sb-fuentes`). No se
declara ninguna regla nueva de este tipo.

### Hallazgo 3 — `RoleScope` cruza de módulo por primera vez: exportarlo, no moverlo

Sebastián confirmó que el perfil tiene **su propio** `RoleScope` (mismo tipo, no uno nuevo
paralelo). Eso significa que `profiles.domain.Profile` referencia
`co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope` directamente — el primer consumidor externo
de ese paquete. Por la regla de commons ("VO usado por 2+ slices → `pdp/commons/model/`") lo
correcto en principio sería mover `RoleScope`/`RoleScopeLevel` a `commons` — pero eso reescribiría
el paquete de una clase que ya usan `RoleTests`, `RoleScopeTests`, `RoleScopeLevelTests`,
`RolePersistenceMapperTests` y otros, y el planificador (y el implementador) **no tocan
`pdp/src/test`**. La alternativa correcta y ya documentada en `1-planificador.md` ("trampa de
Modulith al cruzar un módulo por primera vez") es **exportar**, no mover: `roles.domain.model` gana
un `package-info.java` nuevo con `@NamedInterface("model")`, y `profiles` declara
`"roles :: model"` en su `allowedDependencies`. Cero archivos existentes tocados.

### Hallazgo 4 — falta un validador de existencia de rol publicado por `roles`, y no existe todavía

`profiles` necesita comprobar "¿este rol existe para este inquilino?" al agregar un rol a un
perfil — la misma pregunta que `GrantResourceRulesValidatorImpl` ya resuelve **dentro** de `roles`
con `RoleRepository.findByIdForTenant` + `RoleMustExistForTenantRule`, pero nunca se publicó como
validador a otros módulos (a diferencia de `RoleScopeMustCoverApplicationValidator`, que sí). Esta
historia añade `RoleMustExistForTenantValidator`/`Impl` en `roles` — envoltorio nuevo sobre una
regla que ya existe, cero lógica nueva — y lo publica por `roles :: rule` (ya es interfaz nombrada).

## 1. Resumen funcional

**Catálogo (`profiles`):** un administrador define un `Perfil` (nombre + alcance propio, mismo
`RoleScope` que un `Rol`) y le agrega roles existentes uno a uno — espejo exacto de cómo se define
un `Rol` y se le concede un recurso. Se puede listar el catálogo visible para un inquilino (los
suyos más los globales), igual que `ListRolesUseCase`.

**Asignación (`assignments`):** asignar un perfil a un usuario, para una aplicación dada,
materializa una `Assignment` por cada rol del perfil (reutilizando `AssignRoleUseCase` tal cual) y
persiste un `ProfileAssignment` que recuerda esos identificadores generados. Revocar el
`ProfileAssignment` revoca en cascada cada `Assignment` que generó (reutilizando
`RevokeAssignmentUseCase`), y luego se revoca a sí mismo.

**No cubre:** herencia entre perfiles, denegaciones explícitas de perfil (excluido en
`HU-011.md`), listar las asignaciones de perfil por separado (las `Assignment` que genera ya
aparecen en `GET /api/v1/roles/{roleId}/assignments`), ni compensación automática si falla la
materialización a mitad de camino (mismo límite ya aceptado y documentado por HU-010 para el resto
del proyecto — no se inventa una saga aquí).

## 2. Criterios de aceptación

| # | Criterio (derivado de HU-011.md + decisiones confirmadas)                                                    | Resultado esperado                                                                                                                                                            |
|---|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Un perfil agrupa uno o más roles                                                                             | `Profile.roles()` es un `Set<RoleId>`; `AddRoleToProfileUseCase` los agrega uno a uno, idempotente                                                                            |
| 2 | El nombre del perfil es único en su alcance exacto                                                           | `ProfileNameMustBeUniqueInScopeRule` — mismo patrón que `RoleNameMustBeUniqueInScopeRule`                                                                                     |
| 3 | El perfil tiene su propio alcance (decisión confirmada)                                                      | `Profile.scope(): RoleScope`, validado igual que en `Role` (si es `APPLICATION`, la aplicación debe existir para el inquilino)                                                |
| 4 | Asignar un perfil materializa una asignación por cada rol que agrupa                                         | `AssignProfileUseCaseImpl` llama a `AssignRoleUseCase.execute(...)` una vez por `RoleId` del perfil, dentro del mismo módulo                                                  |
| 5 | El `applicationId` de la materialización es explícito (decisión confirmada)                                  | `AssignProfileRequest` lo recibe como componente, igual que `AssignRoleRequest`                                                                                               |
| 6 | No se puede asignar dos veces el mismo perfil activo al mismo usuario+aplicación                             | `ProfileAssignmentMustNotDuplicateActiveRule`, mismo patrón que `AssignmentMustNotDuplicateActiveRule`                                                                        |
| 7 | Revocar la asignación de un perfil revoca en cascada lo que generó (decisión confirmada)                     | `RevokeProfileAssignmentUseCaseImpl` llama a `RevokeAssignmentUseCase.execute(...)` por cada id en `ProfileAssignment.generatedAssignmentIds()` antes de revocarse a sí mismo |
| 8 | Un perfil o un rol que no existen para el inquilino se rechazan, no se materializan a medias silenciosamente | `ProfileNotFoundException` / `RoleNotFoundException` (ya existente) antes de tocar `AssignmentRepository`                                                                     |

## 3. Reglas de negocio

| #  | Regla                                                                              | Dónde vive (VO / Rule)                                                                                                                               | Puerto que trae el dato                                            | Excepción → HTTP                             |
|----|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|----------------------------------------------|
| P1 | El nombre de perfil es único en su alcance exacto (nivel + inquilino + aplicación) | `profiles/domain/rule/ProfileNameMustBeUniqueInScopeRule` (síncrona)                                                                                 | `ProfileRepository.existsByNameInScope`                            | `DuplicateProfileNameException` → 409        |
| P2 | El perfil existe para el inquilino que pregunta                                    | `profiles/domain/rule/ProfileMustExistForTenantRule` (síncrona)                                                                                      | `ProfileRepository.findByIdForTenant`                              | `ProfileNotFoundException` → 400             |
| P3 | El rol que se agrega a un perfil existe para el inquilino                          | `roles/domain/rule/RoleMustExistForTenantRule` (ya existe, síncrona) — envuelta en el nuevo `RoleMustExistForTenantValidator` publicado a `profiles` | `RoleRepository.findByIdForTenant` (prestado de `roles`)           | `RoleNotFoundException` (ya existente) → 400 |
| A1 | No hay ya una asignación de este perfil activa para (usuario, aplicación)          | `assignments/domain/rule/ProfileAssignmentMustNotDuplicateActiveRule` (síncrona)                                                                     | `ProfileAssignmentRepository.existsActiveByUserApplicationProfile` | `DuplicateProfileAssignmentException` → 409  |
| A2 | El `ProfileAssignment` existe para el inquilino que revoca                         | `assignments/domain/rule/ProfileAssignmentMustExistForTenantRule` (síncrona)                                                                         | `ProfileAssignmentRepository.findByIdForTenant`                    | `ProfileAssignmentNotFoundException` → 400   |

Las reglas ya existentes que se reutilizan sin cambio (`RoleNameMustBeUniqueInScopeRule` no aplica
aquí — es una regla distinta, de `roles`, no reutilizada; sí se reutilizan tal cual
`AssignmentMustNotDuplicateActiveRule` y `RoleScopeMustCoverApplicationRule`, ambas **dentro** de
`AssignRoleUseCase`, invocado como caja negra) no se listan de nuevo: no cambian de forma ni de
sitio.

## 4. Modelo de dominio afectado

### Entidad / agregado

- **`Profile`** (nuevo, `profiles/domain/Profile.java`): agrega roles a un catálogo, con nombre y
  alcance propio. Factoría `define(id, name, scope, registeredAt)` (roles vacío) y comportamiento
  `withRole(RoleId)` (idempotente) — espejo exacto de `Role.define`/`Role.withResource`.
- **`ProfileAssignment`** (nuevo, `assignments/domain/ProfileAssignment.java`): liga un usuario, una
  aplicación y un perfil, con una vigencia propia y el conjunto de `AssignmentId` que generó al
  materializarse. Factoría `grant(id, userId, tenantId, applicationId, profileId,
  generatedAssignmentIds, now)` y comportamiento `revoke(now)` — espejo de `Assignment`, con el
  campo adicional que hace posible la cascada.

### Value objects

| VO                           | Nuevo o existente               | Invariantes                            | Vive en                                                                                              |
|------------------------------|---------------------------------|----------------------------------------|------------------------------------------------------------------------------------------------------|
| `ProfileId`                  | Nuevo                           | UUID no nulo; `of(String)` para parseo | `pdp/commons/model/` (2 consumidores: `profiles` y `assignments`, mismo motivo que `RoleId`)         |
| `ProfileName`                | Nuevo                           | 3 a 60 caracteres tras `trim()`        | `profiles/domain/model/` (un solo consumidor)                                                        |
| `ProfileAssignmentId`        | Nuevo                           | UUID no nulo; `of(String)` para parseo | `assignments/domain/model/` (un solo consumidor, igual que `AssignmentId`)                           |
| `RoleScope`/`RoleScopeLevel` | Existente, reutilizado tal cual | Sin cambio                             | `roles/domain/model/` — gana `@NamedInterface("model")` nuevo (hallazgo 3), cero cambio de contenido |

## 5. Persistencia

### Tabla `profile` (slice `profiles`)

- **Campos:** `id`, `name`, `scopeLevel`, `tenantId`, `applicationId`, `roleIds` (lista de String),
  `registeredAt` — plano, como todo `{X}Entity`.
- **Consultas nuevas en el puerto `ProfileRepository`:**
    - `Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope)`
    - `Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId)`
    - `Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window)`
    - `Mono<Profile> save(Profile profile)`
- **Índice:** de respaldo, no único, sobre `(name)` — la unicidad real la decide la regla contra
  `scopeLevel`/`tenantId`/`applicationId` juntos, igual que en `role`; no se define un índice
  compuesto único nuevo, mismo criterio que ya usa `RoleSchema` hoy (verificar en el propio archivo
  al implementar; si `RoleSchema` sí define uno compuesto, `ProfileSchema` lo espeja igual).
- **Inicializador de esquema:** nuevo, `SurrealProfileSchemaInitializer` (mismo patrón que
  `SurrealRoleSchemaInitializer`).

### Tabla `profile_assignment` (slice `assignments`)

- **Campos:** `id`, `userId`, `tenantId`, `applicationId`, `profileId`, `generatedAssignmentIds`
  (lista de String), `validFrom`, `validUntil` — plano.
- **Consultas nuevas en el puerto `ProfileAssignmentRepository`:**
    -
    `Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId, ProfileId profileId, Instant now)`
    - `Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId id, TenantId tenantId)`
    - `Mono<ProfileAssignment> save(ProfileAssignment profileAssignment)`
- **Índice:** ninguno nuevo obligatorio — la tabla es pequeña y se consulta por id o por la tripleta
  (usuario, aplicación, perfil), igual que `assignment` hoy.
- **Inicializador de esquema:** nuevo, `SurrealProfileAssignmentSchemaInitializer` (mismo patrón que
  `SurrealAssignmentSchemaInitializer`).

## 6. Endpoint

| Verbo  | Ruta                                                             | Código de éxito | Cuerpo de entrada                        | Cuerpo de salida                   |
|--------|------------------------------------------------------------------|-----------------|------------------------------------------|------------------------------------|
| POST   | `/api/v1/profiles`                                               | 201             | `{name, scope, applicationId?}`          | `ProfileWebResponse`               |
| POST   | `/api/v1/profiles/{profileId}/roles`                             | 200             | `{roleId}`                               | `ProfileWebResponse`               |
| GET    | `/api/v1/profiles`                                               | 200             | `page`/`size` u `offset`/`limit` (query) | `PageResponse<ProfileWebResponse>` |
| POST   | `/api/v1/profiles/{profileId}/assignments`                       | 201             | `{userId, applicationId}`                | `ProfileAssignmentWebResponse`     |
| DELETE | `/api/v1/profiles/{profileId}/assignments/{profileAssignmentId}` | 200             | —                                        | (sin cuerpo de datos)              |

- **Autorización:** requiere token; el inquilino sale del principal, nunca del body — mismo patrón
  que `RoleController`/`AssignmentController`.
- **Errores esperados:** `InvalidProfileNameException`/`InvalidRoleScopeException` → 400 (VO);
  `DuplicateProfileNameException`/`DuplicateProfileAssignmentException` → 409; `ProfileNotFoundException`/
  `RoleNotFoundException`/`ProfileAssignmentNotFoundException`/`ApplicationNotFoundException` → 400
  (regla de negocio); manejados todos por el único `ApiErrorHandler`, sin handler propio del slice.
- **Scope GLOBAL bloqueado por ahora:** igual que `DefineRoleRequestMapper` rechaza `GLOBAL` con
  `RolesMessages.globalScopeNotAdministrableYet()` hasta HU-009, `DefineProfileRequestMapper` hace
  lo mismo con `ProfilesMessages.globalScopeNotAdministrableYet()` — mismo motivo, mismo límite.

## 7. SPEC — el contrato

### `profiles` — dominio `[N]`

```java
// pdp/profiles/domain/Profile.java
public record Profile(ProfileId id, ProfileName name, co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope,
        java.util.Set<co.edu.uco.seguridad.pdp.commons.model.RoleId> roles, java.time.Instant registeredAt) {
    // constructor compacto vacío (el implementador añade Objects.requireNonNull por componente,
    // y `roles = Set.copyOf(...)`, igual que Role)
    public static Profile define(ProfileId id, ProfileName name,
            co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope, java.time.Instant registeredAt) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
    public Profile withRole(co.edu.uco.seguridad.pdp.commons.model.RoleId roleId) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
}

// pdp/profiles/domain/model/ProfileName.java
public record ProfileName(String value) { }   // invariantes: 3 a 60 caracteres tras trim, como RoleName

// pdp/profiles/domain/ProfileCriteria.java
public record ProfileCriteria(co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId) {
    public static ProfileCriteria ofTenant(co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
    public boolean matches(Profile profile) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
}

// pdp/profiles/domain/rule/ProfileNameMustBeUniqueInScopeRule.java
public interface ProfileNameMustBeUniqueInScopeRule
        extends co.edu.uco.seguridad.shared.contract.OperationWithoutResult<ProfileNameAvailability> { }

// pdp/profiles/domain/rule/model/ProfileNameAvailability.java
public record ProfileNameAvailability(ProfileName name, co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope,
        boolean taken) { }

// pdp/profiles/domain/rule/ProfileMustExistForTenantRule.java
public interface ProfileMustExistForTenantRule
        extends co.edu.uco.seguridad.shared.contract.OperationWithoutResult<ProfileExistence> { }

// pdp/profiles/domain/rule/model/ProfileExistence.java
public record ProfileExistence(ProfileId profileId, co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        boolean registered) { }

// pdp/profiles/domain/exception/InvalidProfileNameException.java
public final class InvalidProfileNameException extends co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException { }

// pdp/profiles/domain/exception/DuplicateProfileNameException.java
public final class DuplicateProfileNameException extends co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException { }

// pdp/profiles/domain/exception/ProfileNotFoundException.java
public final class ProfileNotFoundException extends co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException { }

// pdp/profiles/domain/message/ProfilesMessages.java
public final class ProfilesMessages {
    public static String profileNameTaken(String name, String scope) { throw new UnsupportedOperationException("pendiente: HU-011"); }
    public static String profileNotFound(String profileId) { throw new UnsupportedOperationException("pendiente: HU-011"); }
    public static String globalScopeNotAdministrableYet() { throw new UnsupportedOperationException("pendiente: HU-011"); }
    public static String applicationIdNotApplicableForTenantScope() { throw new UnsupportedOperationException("pendiente: HU-011"); }
}
```

### `profiles` — aplicación `[N]`

```java
// pdp/profiles/application/secondaryport/repository/ProfileRepository.java
public interface ProfileRepository {
    reactor.core.publisher.Mono<Boolean> existsByNameInScope(ProfileName name, co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope);
    reactor.core.publisher.Mono<Profile> findByIdForTenant(ProfileId profileId, co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId);
    reactor.core.publisher.Mono<co.edu.uco.seguridad.pdp.commons.model.ResultPage<Profile>> findBy(ProfileCriteria criteria, co.edu.uco.seguridad.pdp.commons.model.PageWindow window);
    reactor.core.publisher.Mono<Profile> save(Profile profile);
}

// pdp/profiles/application/primaryport/request/DefineProfileRequest.java
public record DefineProfileRequest(ProfileName name, co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope) { }

// pdp/profiles/application/primaryport/request/AddRoleToProfileRequest.java
public record AddRoleToProfileRequest(co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, ProfileId profileId,
        co.edu.uco.seguridad.pdp.commons.model.RoleId roleId) { }

// pdp/profiles/application/primaryport/request/ListProfilesRequest.java
public record ListProfilesRequest(ProfileCriteria criteria, co.edu.uco.seguridad.pdp.commons.model.PageWindow window) { }

// pdp/profiles/application/primaryport/request/ProfileOwnershipQuery.java  -- publicado como `profiles :: dto`
public record ProfileOwnershipQuery(co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, ProfileId profileId) { }

// pdp/profiles/application/primaryport/response/ProfileResponse.java
public record ProfileResponse(ProfileId id, ProfileName name, co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope scope,
        java.util.Set<co.edu.uco.seguridad.pdp.commons.model.RoleId> roles, java.time.Instant registeredAt) { }

// pdp/profiles/application/usecase/DefineProfileUseCase.java
public interface DefineProfileUseCase extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<DefineProfileRequest, ProfileResponse> { }

// pdp/profiles/application/usecase/AddRoleToProfileUseCase.java
public interface AddRoleToProfileUseCase extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AddRoleToProfileRequest, ProfileResponse> { }

// pdp/profiles/application/usecase/ListProfilesUseCase.java
public interface ListProfilesUseCase extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<ListProfilesRequest, co.edu.uco.seguridad.pdp.commons.model.ResultPage<ProfileResponse>> { }

// pdp/profiles/application/rule/validator/DefineProfileRulesValidator.java
public interface DefineProfileRulesValidator extends co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult<DefineProfileRequest> { }

// pdp/profiles/application/rule/validator/AddRoleToProfileRulesValidator.java  -- devuelve el Profile encontrado
public interface AddRoleToProfileRulesValidator extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AddRoleToProfileRequest, Profile> { }

// pdp/profiles/application/rule/validator/ProfileRolesLookupValidator.java  -- publicado como `profiles :: rule`
public interface ProfileRolesLookupValidator
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<ProfileOwnershipQuery, java.util.Set<co.edu.uco.seguridad.pdp.commons.model.RoleId>> { }
```

### `profiles` — infraestructura `[N]`

```java
// pdp/profiles/infrastructure/adapter/secondary/persistence/entity/ProfileEntity.java
public record ProfileEntity(String id, String name, String scopeLevel, String tenantId, String applicationId,
        java.util.List<String> roleIds, String registeredAt) { }
```

Mapper (`ProfilePersistenceMapper`), adaptador (`SurrealProfileRepository`), esquema
(`ProfileSchema`/`SurrealProfileSchemaInitializer`), controller (`ProfileController`), raw
requests/response web (`DefineProfileRawRequest(name, scope, applicationId)`,
`AddRoleToProfileRawRequest(roleId)`, `ListProfilesRawRequest(page, size, offset, limit)`,
`ProfileWebResponse(id, name, scope, tenantId, applicationId, roleIds, registeredAt)`),
interactores y mappers web: mismas firmas que sus espejos en `roles`, con `Profile`/`ProfileId`/
`ProfileName` en vez de `Role`/`RoleId`/`RoleName`. No se repiten aquí letra por letra — el
tester/implementador copia `RoleController`/`DefineRoleRequestMapper`/`RoleResponseMapper` y
sustituye los tipos, tal como pide `sb-arquitectura` ("antes de escribir un adaptador... abre el
equivalente en `tenants`" — aquí el equivalente más cercano es `roles`, no `tenants`).

### `roles` — `[N]` (aditivo, ningún archivo existente cambia)

```java
// pdp/roles/domain/model/package-info.java  -- NUEVO, exporta lo que ya existía
@org.springframework.modulith.NamedInterface("model")
package co.edu.uco.seguridad.pdp.roles.domain.model;

// pdp/roles/application/primaryport/request/RoleOwnershipQuery.java  -- ya está bajo `roles :: dto`
public record RoleOwnershipQuery(co.edu.uco.seguridad.pdp.commons.model.RoleId roleId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId) { }

// pdp/roles/application/rule/validator/RoleMustExistForTenantValidator.java  -- ya está bajo `roles :: rule`
public interface RoleMustExistForTenantValidator
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult<RoleOwnershipQuery> { }
```

### `assignments` — dominio `[N]`

```java
// pdp/assignments/domain/model/ProfileAssignmentId.java
public record ProfileAssignmentId(java.util.UUID value) { }   // mismo patrón que AssignmentId

// pdp/assignments/domain/ProfileAssignment.java
public record ProfileAssignment(ProfileAssignmentId id, co.edu.uco.seguridad.pdp.commons.model.UserId userId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
        co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId,
        java.util.Set<co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId> generatedAssignmentIds,
        co.edu.uco.seguridad.pdp.assignments.domain.model.Validity validity) {
    public static ProfileAssignment grant(ProfileAssignmentId id, co.edu.uco.seguridad.pdp.commons.model.UserId userId,
            co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
            co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId,
            java.util.Set<co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId> generatedAssignmentIds,
            java.time.Instant now) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
    public ProfileAssignment revoke(java.time.Instant now) {
        throw new UnsupportedOperationException("pendiente: HU-011");
    }
}

// pdp/assignments/domain/rule/ProfileAssignmentMustNotDuplicateActiveRule.java
public interface ProfileAssignmentMustNotDuplicateActiveRule
        extends co.edu.uco.seguridad.shared.contract.OperationWithoutResult<ActiveProfileAssignmentAvailability> { }

// pdp/assignments/domain/rule/model/ActiveProfileAssignmentAvailability.java
public record ActiveProfileAssignmentAvailability(co.edu.uco.seguridad.pdp.commons.model.UserId userId,
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
        co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId, boolean taken) { }

// pdp/assignments/domain/rule/ProfileAssignmentMustExistForTenantRule.java
public interface ProfileAssignmentMustExistForTenantRule
        extends co.edu.uco.seguridad.shared.contract.OperationWithoutResult<ProfileAssignmentExistence> { }

// pdp/assignments/domain/rule/model/ProfileAssignmentExistence.java
public record ProfileAssignmentExistence(ProfileAssignmentId profileAssignmentId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, boolean registered) { }

// pdp/assignments/domain/exception/DuplicateProfileAssignmentException.java
public final class DuplicateProfileAssignmentException extends co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException { }

// pdp/assignments/domain/exception/ProfileAssignmentNotFoundException.java
public final class ProfileAssignmentNotFoundException extends co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException { }
```

> Nota de dependencia: `assignments` necesita `ProfileId` (tipo de `profiles`). Como `ProfileId` vive
> en `commons` (ver sección 4), esto no exige ninguna frontera nueva de Modulith para el tipo en sí
> — solo `"profiles"`, `"profiles :: rule"` y `"profiles :: dto"` en `allowedDependencies` de
> `assignments`, para `ProfileRolesLookupValidator` y `ProfileOwnershipQuery`.

### `assignments` — aplicación `[N]`

```java
// pdp/assignments/application/secondaryport/repository/ProfileAssignmentRepository.java
public interface ProfileAssignmentRepository {
    reactor.core.publisher.Mono<Boolean> existsActiveByUserApplicationProfile(co.edu.uco.seguridad.pdp.commons.model.UserId userId,
            co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId, co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId,
            java.time.Instant now);
    reactor.core.publisher.Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId id, co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId);
    reactor.core.publisher.Mono<ProfileAssignment> save(ProfileAssignment profileAssignment);
}

// pdp/assignments/application/primaryport/request/AssignProfileRequest.java
public record AssignProfileRequest(co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        co.edu.uco.seguridad.pdp.commons.model.UserId userId, co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
        co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId) { }

// pdp/assignments/application/primaryport/request/RevokeProfileAssignmentRequest.java
public record RevokeProfileAssignmentRequest(ProfileAssignmentId profileAssignmentId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId) { }

// pdp/assignments/application/primaryport/response/ProfileAssignmentResponse.java
public record ProfileAssignmentResponse(ProfileAssignmentId id, co.edu.uco.seguridad.pdp.commons.model.UserId userId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId, co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
        co.edu.uco.seguridad.pdp.profiles.domain.ProfileId profileId,
        java.util.Set<co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId> generatedAssignmentIds,
        java.time.Instant validFrom, java.util.Optional<java.time.Instant> validUntil) { }

// pdp/assignments/application/usecase/AssignProfileUseCase.java
public interface AssignProfileUseCase extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AssignProfileRequest, ProfileAssignmentResponse> { }

// pdp/assignments/application/usecase/RevokeProfileAssignmentUseCase.java
public interface RevokeProfileAssignmentUseCase extends co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult<RevokeProfileAssignmentRequest> { }

// pdp/assignments/application/rule/validator/AssignProfileRulesValidator.java  -- devuelve el conjunto de roles ya validado
public interface AssignProfileRulesValidator
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AssignProfileRequest, java.util.Set<co.edu.uco.seguridad.pdp.commons.model.RoleId>> { }

// pdp/assignments/application/rule/validator/RevokeProfileAssignmentRulesValidator.java  -- devuelve el ProfileAssignment encontrado
public interface RevokeProfileAssignmentRulesValidator
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<RevokeProfileAssignmentRequest, ProfileAssignment> { }
```

### `assignments` — infraestructura `[N]`

```java
// pdp/assignments/infrastructure/adapter/secondary/persistence/entity/ProfileAssignmentEntity.java
public record ProfileAssignmentEntity(String id, String userId, String tenantId, String applicationId, String profileId,
        java.util.List<String> generatedAssignmentIds, String validFrom, String validUntil) { }
```

Mapper (`ProfileAssignmentPersistenceMapper`), adaptador (`SurrealProfileAssignmentRepository`),
esquema (`ProfileAssignmentSchema`/`SurrealProfileAssignmentSchemaInitializer`), controller nuevo
`ProfileAssignmentController` (no se toca `AssignmentController` existente — aditivo, evita
inflar un controller ya usado por HU-005), raw requests (`AssignProfileRawRequest(userId,
applicationId)`), respuesta web (`ProfileAssignmentWebResponse(id, userId, tenantId, applicationId,
profileId, generatedAssignmentIds, validFrom, validUntil)`), interactores
(`AssignProfileInteractor`/`RevokeProfileAssignmentInteractor`) y mappers: mismas firmas que sus
espejos en `assignments` para `Assignment`, sustituyendo tipos.

## 8. Árbol de archivos

```
pdp/profiles/                                                          (slice nuevo)
├── domain/
│   ├── Profile.java                                                   [N]
│   ├── ProfileCriteria.java                                           [N]
│   ├── model/ProfileName.java                                         [N]
│   ├── message/ProfilesMessages.java                                  [N]
│   ├── exception/InvalidProfileNameException.java                     [N]
│   ├── exception/DuplicateProfileNameException.java                   [N]
│   ├── exception/ProfileNotFoundException.java                        [N]
│   ├── rule/ProfileNameMustBeUniqueInScopeRule.java                   [N]
│   ├── rule/impl/ProfileNameMustBeUniqueInScopeRuleImpl.java          [N]
│   ├── rule/model/ProfileNameAvailability.java                        [N]
│   ├── rule/ProfileMustExistForTenantRule.java                        [N]
│   ├── rule/impl/ProfileMustExistForTenantRuleImpl.java               [N]
│   └── rule/model/ProfileExistence.java                               [N]
├── application/
│   ├── secondaryport/repository/ProfileRepository.java                [N]
│   ├── primaryport/request/DefineProfileRequest.java                  [N]
│   ├── primaryport/request/AddRoleToProfileRequest.java               [N]
│   ├── primaryport/request/ListProfilesRequest.java                  [N]
│   ├── primaryport/request/ProfileOwnershipQuery.java                 [N]  -- @NamedInterface("dto")
│   ├── primaryport/response/ProfileResponse.java                      [N]
│   ├── usecase/DefineProfileUseCase.java                              [N]
│   ├── usecase/impl/DefineProfileUseCaseImpl.java                     [N]
│   ├── usecase/AddRoleToProfileUseCase.java                           [N]
│   ├── usecase/impl/AddRoleToProfileUseCaseImpl.java                  [N]
│   ├── usecase/ListProfilesUseCase.java                               [N]
│   ├── usecase/impl/ListProfilesUseCaseImpl.java                      [N]
│   ├── rule/validator/DefineProfileRulesValidator.java                [N]
│   ├── rule/validator/impl/DefineProfileRulesValidatorImpl.java       [N]
│   ├── rule/validator/AddRoleToProfileRulesValidator.java             [N]
│   ├── rule/validator/impl/AddRoleToProfileRulesValidatorImpl.java    [N]
│   ├── rule/validator/ProfileRolesLookupValidator.java                [N]  -- @NamedInterface("rule")
│   └── rule/validator/impl/ProfileRolesLookupValidatorImpl.java       [N]
└── infrastructure/
    ├── adapter/primary/web/controller/ProfileController.java          [N]
    ├── adapter/primary/web/dto/request/raw/DefineProfileRawRequest.java [N]
    ├── adapter/primary/web/dto/request/raw/AddRoleToProfileRawRequest.java [N]
    ├── adapter/primary/web/dto/request/raw/ListProfilesRawRequest.java [N]
    ├── adapter/primary/web/dto/response/ProfileWebResponse.java        [N]
    ├── adapter/primary/web/interactor/DefineProfileInteractor.java     [N]
    ├── adapter/primary/web/interactor/impl/DefineProfileInteractorImpl.java [N]
    ├── adapter/primary/web/interactor/AddRoleToProfileInteractor.java  [N]
    ├── adapter/primary/web/interactor/impl/AddRoleToProfileInteractorImpl.java [N]
    ├── adapter/primary/web/interactor/ListProfilesInteractor.java      [N]
    ├── adapter/primary/web/interactor/impl/ListProfilesInteractorImpl.java [N]
    ├── adapter/primary/web/mapper/DefineProfileRequestMapper.java      [N]
    ├── adapter/primary/web/mapper/AddRoleToProfileRequestMapper.java   [N]
    ├── adapter/primary/web/mapper/ListProfilesRequestMapper.java       [N]
    ├── adapter/primary/web/mapper/ProfileResponseMapper.java           [N]
    ├── adapter/secondary/persistence/entity/ProfileEntity.java         [N]
    ├── adapter/secondary/persistence/mapper/ProfilePersistenceMapper.java [N]
    ├── adapter/secondary/persistence/repository/SurrealProfileRepository.java [N]
    ├── adapter/secondary/persistence/schema/ProfileSchema.java         [N]
    ├── adapter/secondary/persistence/schema/SurrealProfileSchemaInitializer.java [N]
    └── config/ProfilesConfiguration.java                               [N]

pdp/roles/
├── domain/model/package-info.java                                     [N] -- @NamedInterface("model"), ningún archivo existente cambia
├── application/primaryport/request/RoleOwnershipQuery.java             [N]
├── application/rule/validator/RoleMustExistForTenantValidator.java     [N]
├── application/rule/validator/impl/RoleMustExistForTenantValidatorImpl.java [N]
└── infrastructure/config/RolesConfiguration.java                       [M] -- +1 bean

pdp/assignments/
├── domain/model/ProfileAssignmentId.java                               [N]
├── domain/ProfileAssignment.java                                       [N]
├── domain/rule/ProfileAssignmentMustNotDuplicateActiveRule.java        [N]
├── domain/rule/impl/ProfileAssignmentMustNotDuplicateActiveRuleImpl.java [N]
├── domain/rule/model/ActiveProfileAssignmentAvailability.java          [N]
├── domain/rule/ProfileAssignmentMustExistForTenantRule.java            [N]
├── domain/rule/impl/ProfileAssignmentMustExistForTenantRuleImpl.java   [N]
├── domain/rule/model/ProfileAssignmentExistence.java                   [N]
├── domain/exception/DuplicateProfileAssignmentException.java           [N]
├── domain/exception/ProfileAssignmentNotFoundException.java            [N]
├── application/secondaryport/repository/ProfileAssignmentRepository.java [N]
├── application/primaryport/request/AssignProfileRequest.java           [N]
├── application/primaryport/request/RevokeProfileAssignmentRequest.java [N]
├── application/primaryport/response/ProfileAssignmentResponse.java     [N]
├── application/usecase/AssignProfileUseCase.java                       [N]
├── application/usecase/impl/AssignProfileUseCaseImpl.java              [N]
├── application/usecase/RevokeProfileAssignmentUseCase.java             [N]
├── application/usecase/impl/RevokeProfileAssignmentUseCaseImpl.java   [N]
├── application/rule/validator/AssignProfileRulesValidator.java         [N]
├── application/rule/validator/impl/AssignProfileRulesValidatorImpl.java [N]
├── application/rule/validator/RevokeProfileAssignmentRulesValidator.java [N]
├── application/rule/validator/impl/RevokeProfileAssignmentRulesValidatorImpl.java [N]
├── infrastructure/adapter/primary/web/controller/ProfileAssignmentController.java [N]
├── infrastructure/adapter/primary/web/dto/request/raw/AssignProfileRawRequest.java [N]
├── infrastructure/adapter/primary/web/dto/response/ProfileAssignmentWebResponse.java [N]
├── infrastructure/adapter/primary/web/interactor/AssignProfileInteractor.java [N]
├── infrastructure/adapter/primary/web/interactor/impl/AssignProfileInteractorImpl.java [N]
├── infrastructure/adapter/primary/web/interactor/RevokeProfileAssignmentInteractor.java [N]
├── infrastructure/adapter/primary/web/interactor/impl/RevokeProfileAssignmentInteractorImpl.java [N]
├── infrastructure/adapter/primary/web/mapper/AssignProfileRequestMapper.java [N]
├── infrastructure/adapter/primary/web/mapper/ProfileAssignmentResponseMapper.java [N]
├── infrastructure/adapter/secondary/persistence/entity/ProfileAssignmentEntity.java [N]
├── infrastructure/adapter/secondary/persistence/mapper/ProfileAssignmentPersistenceMapper.java [N]
├── infrastructure/adapter/secondary/persistence/repository/SurrealProfileAssignmentRepository.java [N]
├── infrastructure/adapter/secondary/persistence/schema/ProfileAssignmentSchema.java [N]
├── infrastructure/adapter/secondary/persistence/schema/SurrealProfileAssignmentSchemaInitializer.java [N]
├── infrastructure/config/AssignmentsConfiguration.java                 [M] -- +9 beans, +dependencia en AssignProfileUseCase/RevokeProfileAssignmentUseCase sobre AssignRoleUseCase/RevokeAssignmentUseCase ya existentes
└── package-info.java                                                   [M] -- +"profiles", "profiles :: rule", "profiles :: dto" en allowedDependencies

pdp/commons/model/
└── ProfileId.java                                                      [N]

shared/message/
└── RequiredArgumentMessages.java                                       [M] -- constantes nuevas (ver lista en §11)
```

## 9. Casos de prueba esperados

| Capa                         | Clase de prueba                                                                                                                                                                                                        | Casos                                                                                                                                                                                                                                                                         |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `profiles` domain            | `ProfileTests`                                                                                                                                                                                                         | `define` deja roles vacío; `withRole` es idempotente (agregar dos veces el mismo no duplica)                                                                                                                                                                                  |
| `profiles` domain            | `ProfileNameTests`                                                                                                                                                                                                     | rechaza null/vacío/corto/largo, igual que `RoleNameTests`                                                                                                                                                                                                                     |
| `profiles` domain            | `ProfileCriteriaTests`                                                                                                                                                                                                 | perfil del tenant coincide; perfil global coincide; perfil de otro tenant no                                                                                                                                                                                                  |
| `profiles` domain            | `ProfileNameMustBeUniqueInScopeRuleImplTests`, `ProfileMustExistForTenantRuleImplTests`                                                                                                                                | 1 camino feliz + 1 rechazo cada una, calco de sus equivalentes en `roles`                                                                                                                                                                                                     |
| `profiles` application       | `DefineProfileUseCaseImplTests`                                                                                                                                                                                        | camino feliz (APPLICATION y TENANT); rechazo por nombre duplicado; rechazo por aplicación inexistente cuando scope es APPLICATION                                                                                                                                             |
| `profiles` application       | `AddRoleToProfileUseCaseImplTests`                                                                                                                                                                                     | camino feliz; rechazo por perfil inexistente; rechazo por rol inexistente para el tenant                                                                                                                                                                                      |
| `profiles` application       | `ListProfilesUseCaseImplTests`                                                                                                                                                                                         | página con los del tenant + globales                                                                                                                                                                                                                                          |
| `profiles` application       | `ProfileRolesLookupValidatorImplTests`                                                                                                                                                                                 | perfil existente devuelve su conjunto de roles; perfil inexistente lanza `ProfileNotFoundException`                                                                                                                                                                           |
| `profiles` infrastructure    | `ProfilePersistenceMapperTests`, `DefineProfileRequestMapperTests`, `AddRoleToProfileRequestMapperTests`, `ListProfilesRequestMapperTests`, `ProfileResponseMapperTests`, `ProfileControllerTests`, `ProfileHttpTests` | mismo presupuesto que sus equivalentes en `roles` (campo ausente/mal formado/válido; delega al interactor; scope GLOBAL rechazado con 400)                                                                                                                                    |
| `roles` application          | `RoleMustExistForTenantValidatorImplTests`                                                                                                                                                                             | rol existente no lanza; rol inexistente lanza `RoleNotFoundException`                                                                                                                                                                                                         |
| `assignments` domain         | `ProfileAssignmentTests`                                                                                                                                                                                               | `grant` deja vigencia sin fin; `revoke` la cierra; conserva `generatedAssignmentIds`                                                                                                                                                                                          |
| `assignments` domain         | `ProfileAssignmentMustNotDuplicateActiveRuleImplTests`, `ProfileAssignmentMustExistForTenantRuleImplTests`                                                                                                             | 1 camino feliz + 1 rechazo cada una                                                                                                                                                                                                                                           |
| `assignments` application    | `AssignProfileUseCaseImplTests`                                                                                                                                                                                        | camino feliz: N roles del perfil generan N `Assignment` (fake de `AssignRoleUseCase` capturando cuántas veces y con qué `AssignRoleRequest` se llamó); rechazo por perfil inexistente (nunca llega a `AssignRoleUseCase`, poison-pill); rechazo por perfil ya asignado activo |
| `assignments` application    | `RevokeProfileAssignmentUseCaseImplTests`                                                                                                                                                                              | camino feliz: revoca cada `AssignmentId` generado (fake de `RevokeAssignmentUseCase` capturando cuántas veces) y luego el propio `ProfileAssignment`; rechazo por `ProfileAssignment` inexistente                                                                             |
| `assignments` infrastructure | `ProfileAssignmentPersistenceMapperTests`, `AssignProfileRequestMapperTests`, `ProfileAssignmentResponseMapperTests`, `ProfileAssignmentControllerTests`, `ProfileAssignmentHttpTests`                                 | mismo presupuesto que `AssignmentController`/`AssignRoleRequestMapper`                                                                                                                                                                                                        |
| `shared/persistence`         | `SurrealRepositoryIntegrationTests` (extendida)                                                                                                                                                                        | `ProfileRepository`: guarda y encuentra por nombre en alcance; `ProfileAssignmentRepository`: guarda y encuentra activo por (usuario, aplicación, perfil)                                                                                                                     |

Presupuesto estimado: **35-42 pruebas nuevas** — historia grande por diseño (dos slices), no por
alcance inflado; cada pieza es un espejo 1:1 de algo que ya existe y ya está probado.

## 10. Trazabilidad

| Fase                       | Estado                                        | Fecha      |
|----------------------------|-----------------------------------------------|------------|
| Plan                       | ✅ Generado                                    | 2026-09-12 |
| Contrato aprobado (gate 1) | ⏳ Pendiente                                   |            |
| Pruebas en rojo            | ⏳ Pendiente                                   |            |
| Implementación en verde    | ⏳ Pendiente                                   |            |
| Validación                 | ✅ Aprobado — ver `reportes/REPORTE-HU-011.md` | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                                   |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee — las tres decisiones de diseño que la propia `HU-011.md` (y mi pregunta
adicional sobre el origen del `applicationId`) marcaban como necesarias antes de planificar ya las
confirmó Sebastián el 2026-09-12:

1. **Alcance del perfil:** propio, mismo tipo `RoleScope` que `Role` (no heredado de sus roles).
2. **Origen del `applicationId` al asignar:** parámetro explícito en `AssignProfileRequest`, igual
   que `AssignRoleRequest` ya hace hoy.
3. **Revocación:** en cascada — revocar un `ProfileAssignment` revoca automáticamente cada
   `Assignment` que generó.

Una lista de nombres de constantes nuevas de `RequiredArgumentMessages` (para que el implementador
no tenga que inventarlas): `PROFILE_ID`, `PROFILE_NAME`, `PROFILE_SCOPE`, `PROFILE_ROLES`,
`PROFILE`, `PROFILE_CRITERIA`, `PROFILE_REPOSITORY`, `PROFILE_NAME_UNIQUE_RULE`,
`PROFILE_EXISTS_RULE`, `DEFINE_PROFILE_RULES_VALIDATOR`, `ADD_ROLE_TO_PROFILE_RULES_VALIDATOR`,
`PROFILE_ROLES_LOOKUP_VALIDATOR`, `DEFINE_PROFILE_USE_CASE`, `ADD_ROLE_TO_PROFILE_USE_CASE`,
`LIST_PROFILES_USE_CASE`, `DEFINE_PROFILE_INTERACTOR`, `ADD_ROLE_TO_PROFILE_INTERACTOR`,
`LIST_PROFILES_INTERACTOR`, `ROLE_MUST_EXIST_FOR_TENANT_VALIDATOR`, `PROFILE_ASSIGNMENT_ID`,
`PROFILE_ASSIGNMENT`, `PROFILE_ASSIGNMENT_REPOSITORY`, `GENERATED_ASSIGNMENT_IDS`,
`PROFILE_ASSIGNMENT_NOT_DUPLICATE_RULE`, `PROFILE_ASSIGNMENT_EXISTS_RULE`,
`ASSIGN_PROFILE_RULES_VALIDATOR`, `REVOKE_PROFILE_ASSIGNMENT_RULES_VALIDATOR`,
`ASSIGN_PROFILE_USE_CASE`, `REVOKE_PROFILE_ASSIGNMENT_USE_CASE`, `ASSIGN_PROFILE_INTERACTOR`,
`REVOKE_PROFILE_ASSIGNMENT_INTERACTOR`. Y en `ValueObjectMessages`: `ProfileName.LENGTH` (nested
class, mismo patrón que `RoleName`).
