# Reporte de validacion — HU-004

## Metadata

- **Slice:** `roles` (nuevo) + `[M]` en `resources`, `pdp/commons`, `shared`
- **Fecha:** 2026-09-12
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-004.md`
- **Rama:** `feature/HU-004-catalogo-roles`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. La primera corrida arrastraba `OPENSSL_CONF` apuntando
> a `C:\Program Files\PostgreSQL\psqlODBC\etc\openssl.cnf` (variable de entorno de la máquina, ajena a
> este cambio — ya diagnosticado y con corrección entregada durante HU-003). Confirmado limpiando la
> variable para esta corrida: mismo código, build en verde. No es un fallo del código; se reporta como
> lo que es, un error de entorno.

```
ESTADO: ROJO  (mvnw clean verify, 41,4s, exit 1)   ← con OPENSSL_CONF de sistema aún apuntando al archivo inexistente
PRUEBAS: Tests run: 391, Failures: 0, Errors: 1, Skipped: 0
  InternalSecurityChainIntegrationTests: java.lang.IllegalStateException: openssl no pudo generar
  el certificado de prueba para /CN=localhost  ← error de entorno, no de código (módulo authorization,
  fuera del alcance de HU-004)

--- repetido con OPENSSL_CONF limpio para esta sesión ---
ESTADO: VERDE  (mvnw clean verify, 49,8s, exit 0)
PRUEBAS: Tests run: 393, Failures: 0, Errors: 0, Skipped: 0
```

```
consistencia.ps1 → CONSISTENTE: todos los slices siguen la misma forma.
drift.ps1        → SIN DERIVA: todos los enlaces resuelven y toda clase citada existe.
```

| Comprobacion                   | Resultado                                                  |
|--------------------------------|------------------------------------------------------------|
| Compilacion                    | ✅                                                          |
| Pruebas                        | ✅ 393 pruebas (83 nuevas/tocadas de `roles` + `resources`) |
| Cobertura (≥ 50 % por paquete) | ✅ (parte de `clean verify` en verde)                       |
| `LayeredArchitectureTests`     | ✅                                                          |
| `ModulithStructureTests`       | ✅                                                          |
| `consistencia.ps1`             | ✅                                                          |
| `drift.ps1`                    | ✅ (0 hallazgos nuevos)                                     |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 4 / criterio 7] — Interpolación directa de literales SurrealQL en `SurrealRoleRepository`

- **Archivo:**
  `pdp/src/main/java/co/edu/uco/seguridad/pdp/roles/infrastructure/adapter/secondary/persistence/repository/SurrealRoleRepository.java`
- **Observación:** `existsByNameInScope` y `save` interpolan directamente `"NONE"` (cuando el componente
  opcional del alcance está ausente) y el arreglo literal de `resourceIds` (`['uuid1','uuid2']`) en el
  cuerpo del SurQL, en vez de pasarlos como parámetros ligados.
- **Por qué no es bloqueante:** los valores interpolados no son texto de usuario — `"NONE"` es una
  constante fija y cada elemento del arreglo es un `UUID.toString()` ya validado por su value object
  (solo hex y guiones). Es la misma técnica que `SurrealApplicationRepository.findBy` usa para
  `LIMIT`/`START` (enteros ya validados por `PageWindow`), documentada allí como excepción deliberada.
- **Sugerencia:** si una futura historia añade un campo de texto libre al arreglo interpolado, esa
  excepción deja de sostenerse y debe pasar a parámetro ligado.

### [Plan §11.2] — Ambigüedad de `NONE` en el índice único, resuelta

El plan dejaba abierto si `tenantId`/`applicationId` ausentes (`NONE`) se comportan correctamente
dentro del índice único `role_scope_name`. `SurrealRepositoryIntegrationTests
.role_repository_lists_the_tenant_catalog_including_globals_and_excluding_other_tenants` guarda un rol
`GLOBAL` junto a roles de dos tenants distintos sin colisión de índice — la ambigüedad queda resuelta
a favor de "funciona como se esperaba"; no requiere acción.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                      |
|---|-----------------------------------------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅         | Ver tabla siguiente                                                                                                                                                                            |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol)  | ✅         | Clases/métodos en inglés (`RoleName`, `DefineRoleUseCaseImpl`, `granting_the_same_resource_twice_does_not_duplicate_it`); `RolesMessages`, `ValueObjectMessages.RoleName/RoleScope` en español |
| 3 | ¿Introdujo deriva doc↔codigo?                                   | ✅         | `drift.ps1` en verde, 0 hallazgos nuevos (14 excepciones preexistentes sin cambio)                                                                                                             |
| 4 | ¿La logica quedo en la capa correcta?                           | ✅         | Ver detalle abajo                                                                                                                                                                              |

**Detalle criterio 1 (aceptación) — criterio del plan → evidencia:**

| # criterio                           | Evidencia                                                                                                                                                                                                                                                            |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 (alta TENANT)                      | `RoleHttpTests.defines_a_tenant_scoped_role` (201)                                                                                                                                                                                                                   |
| 2 (alta APPLICATION)                 | `RoleHttpTests.refuses_granting_a_resource_from_another_application_to_an_application_scoped_role` (define previo, 201) + `DefineRoleRulesValidatorImplTests.rejects_an_application_scoped_role_when_the_application_does_not_exist`                                 |
| 3 (nombre único por alcance)         | `RoleNameMustBeUniqueInScopeRuleImplTests` + `DefineRoleRulesValidatorImplTests.rejects_a_name_already_taken_in_the_scope`                                                                                                                                           |
| 4 (recurso coherente con alcance)    | `RoleScopeMustCoverResourceRuleImplTests` + `GrantResourceRulesValidatorImplTests.rejects_a_resource_outside_the_role_scope` + `RoleHttpTests.refuses_granting_a_resource_from_another_application_to_an_application_scoped_role`                                    |
| 5 (recurso inexistente)              | `ProtectedResourceOwnerLookupValidatorImplTests.reports_not_found_when_no_application_owns_the_resource` + `GrantResourceRulesValidatorImplTests.rejects_when_the_resource_does_not_exist`                                                                           |
| 6 (consulta por tenant + paginación) | `ListRolesUseCaseImplTests` (4 casos) + `SurrealRepositoryIntegrationTests.role_repository_lists_the_tenant_catalog_including_globals_and_excluding_other_tenants` + `RoleHttpTests.the_catalog_never_shows_roles_of_another_tenant` + `ListRolesRequestMapperTests` |
| 7 (GLOBAL no creable)                | `DefineRoleRequestMapperTests.rejects_a_global_scope_on_this_channel` + `RoleHttpTests.refuses_a_global_scope_on_this_channel`                                                                                                                                       |
| 8 (aislamiento al conceder)          | `GrantResourceRulesValidatorImplTests.rejects_when_the_role_does_not_exist_for_the_tenant` (con poison pill) + `RoleMustExistForTenantRuleImplTests`                                                                                                                 |
| 9 (conceder es idempotente)          | `RoleTests.granting_the_same_resource_twice_does_not_duplicate_it` + `GrantResourceToRoleUseCaseImplTests.saves_the_role_with_the_resource_added_and_returns_it`                                                                                                     |

**Detalle criterio 4 (capa correcta):**

- Cero `if/throw` de negocio en los tres use cases: `DefineRoleUseCaseImpl` y
  `GrantResourceToRoleUseCaseImpl` delegan toda decisión al validador (`rules.execute(...)`) antes de
  construir/guardar; `ListRolesUseCaseImpl` solo proyecta.
- El único `if` de orquestación (¿el alcance trae `applicationId`?) en
  `DefineRoleRulesValidatorImpl.execute` se resuelve con `scope.applicationId().map(...).orElse(Mono.empty())`,
  tal como exige el plan — no hay una rama `if/else` de negocio.
- `RoleController` es `package-private`, `final`, y delega en los tres interactores sin importar nada
  de `application`.
- Los tres mappers de request delegan el formato a los value objects (`RoleName::new`,
  `RoleScopeLevel::parse`, `ApplicationId::of`, `RoleId::of`, `ResourceId::of`) vía `RequestFieldParser`;
  ninguno valida formato a mano.
- `SurrealRoleRepository` no decide nada de negocio: traduce `Role` ↔ fila y delega la reconstrucción
  en `RolePersistenceMapper`.
- Las tres reglas de dominio (`RoleNameMustBeUniqueInScopeRuleImpl`, `RoleMustExistForTenantRuleImpl`,
  `RoleScopeMustCoverResourceRuleImpl`) son síncronas, sin puertos ni `Mono`, tal como exige `sb-arquitectura`.
- Cero anotaciones de Spring en `roles/domain` y `roles/application` (verificado por
  `LayeredArchitectureTests`, parte del build en verde).
- `allowedDependencies` de `roles` (`package-info.java`) no fue relajado para compilar: se declaró una
  vez, con las interfaces nombradas que el plan fijó (`applications :: rule`, `resources :: rule`, etc.).

## Criterios de la linea base

> Solo los que el plan declaro. Los marcados 🤖 los resuelve el build, no la lectura.

| #  | Criterio                    | Resultado                 | Punto de control comprobado                                                                                                                                                                                                                                                                      |
|----|-----------------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture          | ✅ 🤖                      | `LayeredArchitectureTests` + `ModulithStructureTests` en verde; cero Spring en `domain`/`application` de `roles`                                                                                                                                                                                 |
| 2  | Contratos de servicios      | ✅                         | `DefineRoleUseCase`, `GrantResourceToRoleUseCase`, `ListRolesUseCase`, las 3 `Rule`, los 2 `Validator` y los 3 `Interactor` son interfaces vacías que extienden un contrato de `shared/contract`; `RoleRepository` es el puerto de salida explícito                                              |
| 3  | Reglas e integridad         | ✅                         | 3 `Rule` puras y síncronas en `domain/rule/impl`, sin puertos; los 2 `Validator` de `application/rule/validator` hacen la E/S; cero `if/throw` de negocio en los use cases                                                                                                                       |
| 5  | Manejo de mensajes          | ✅                         | `RoleController` envuelve con `ApiResponse.success(code, WebContractMessages.xxx(), data, context)`; errores nuevos cuelgan de la jerarquía ya traducida por `ApiErrorHandler` (no se tocó)                                                                                                      |
| 6  | Manejo de parámetros        | ✅                         | Los 3 raw requests son `String`; `RequestFieldParser` en los 3 mappers                                                                                                                                                                                                                           |
| 7  | Adaptadores de persistencia | ✅ (ver observación menor) | `SurrealRoleRepository` implementa el puerto sin decidir negocio; tabla en `RoleSchema.TABLE`; valores de usuario como parámetros ligados                                                                                                                                                        |
| 9  | Excepciones                 | ✅                         | `InvalidRoleNameException`/`InvalidRoleScopeException` → `InvalidValueException`; `DuplicateRoleNameException` → `ConflictBusinessRuleException`; `RoleNotFoundException`/`ResourceOutsideRoleScopeException` → `BusinessRuleViolationException`; ninguna excepción nueva tocó `ApiErrorHandler` |
| 11 | Interacción entre capas     | ✅                         | `RoleController` → interactor → use case → rules/validators → `RoleRepository`; controller no importa `application`                                                                                                                                                                              |
| 12 | SOLID                       | ✅                         | Contratos de una operación; dependencias inyectadas por constructor contra interfaces en los 13 beans de `RolesConfiguration`                                                                                                                                                                    |
| 13 | DTOs                        | ✅                         | Raw (`String`) → mapper con `RequestFieldParser` → DTO tipado con value objects (`DefineRoleRequest`, `GrantResourceRequest`, `ListRolesRequest`)                                                                                                                                                |
| 14 | DTOs seguros                | ✅                         | Los 4 DTOs de `application/primaryport` validan con `Objects.requireNonNull` en su constructor compacto (incluye `Set.copyOf` en `RoleResponse`); `RoleWebResponse` sale plana                                                                                                                   |
| 15 | Validación de dominio       | ✅                         | `RoleName` (3–60, trim), `RoleScope` (coherencia L2), `Role` (requireNonNull + `Set.copyOf`) validan en su constructor compacto                                                                                                                                                                  |
| 16 | Repositorios dinámicos      | ✅                         | `RoleRepository.findBy(RoleCriteria, PageWindow)` — un método, no uno por combinación de filtros                                                                                                                                                                                                 |
| 17 | Consultas dinámicas         | ✅                         | `RoleCriteria(TenantId)` con `matches(Role)`, specification siguiendo el patrón de `ApplicationCriteria`                                                                                                                                                                                         |
| 18 | Paginación                  | ✅                         | `ListRolesRequestMapper` reutiliza `PageWindow` (máx. 100)                                                                                                                                                                                                                                       |
| 19 | Rangos                      | ✅                         | `ListRolesRequestMapper` es copia literal de la lógica de `ListApplicationsRequestMapper` (page/size vs. offset/limit, `ConflictingRequestParametersException` en mezcla); probado en `ListRolesRequestMapperTests`                                                                              |
| 20 | Adaptadores limpios         | ✅                         | Controller, mappers y `SurrealRoleRepository` sin reglas de negocio; formato delegado a los VOs                                                                                                                                                                                                  |
| 21 | Modelo refinado             | ✅                         | `Role`/`RoleCriteria`/`RoleScope` son `record` inmutables, sin Lombok, con factorías con nombre (`define`, `ofTenant`, `ofApplication`) y comportamiento (`withResource`, `isGlobal`, `matches`)                                                                                                 |
| 22 | Arquitectura reactiva       | ✅                         | `Mono` en toda la cadena de `roles`; único `.block()` del módulo está en `SurrealRoleSchemaInitializer` vía `SurrealSchemaInitializer` (arranque, excepción documentada)                                                                                                                         |

## Desviaciones respecto al plan

| Archivo                                                                                           | Plan decia                                                                                                                                   | Codigo hace                                                                                                                                                                                          | ¿Justificado?                                                                                                                                                                                                                                                                                                                                                           |
|---------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pdp/roles/domain/exception/ResourceOutsideRoleScopeException.java`                               | Un solo constructor `(ResourceId)`                                                                                                           | Constructor adicional sobrecargado `(ApplicationId)` para el uso real de `RoleScopeMustCoverResourceRuleImpl`, que solo dispone de `resourceApplicationId`/`resourceTenantId` vía `ResourceCoverage` | Sí — el constructor original `(ResourceId)` sigue intacto (lo exige el fake de `GrantResourceRulesValidatorImplTests`); la sobrecarga no cambia ningún contrato ya fijado, solo añade uno. Documentado por el implementador en su cierre de fase                                                                                                                        |
| `pdp/roles/infrastructure/adapter/secondary/persistence/schema/SurrealRoleSchemaInitializer.java` | "Esqueleto de HU-004: `run()` queda vacío... patrón de `SurrealProtectedResourceSchemaInitializer`" (implementa `ApplicationRunner` directo) | Se reescribió para extender `SurrealSchemaInitializer` (la base común de todos los módulos, con timeout y no derriba el contexto si SurrealDB no responde)                                           | Sí — es el patrón real vigente en el resto del proyecto (`SurrealApplicationSchemaInitializer`, `SurrealProtectedResourceSchemaInitializer`, `SurrealTenantSchemaInitializer`); el esqueleto del tester no lo había adoptado, y quedarse con `ApplicationRunner` directo habría sido la única inconsistencia del slice frente a `consistencia.ps1` (que sigue en verde) |
| `pdp/roles/domain/message/RolesMessages.java`                                                     | 6 métodos (5 excepciones + `globalScopeNotAdministrableYet`)                                                                                 | +1 método `applicationIdNotApplicableForTenantScope()`                                                                                                                                               | Sí — necesario para la barrera C2 (`TENANT` prohíbe `applicationId`); el plan no fijaba el texto exacto de esa barrera, solo la excepción (`MalformedRequestFieldException`), y el catálogo de mensajes es el único lugar permitido para el texto (regla invariante de `sb-estandares`)                                                                                 |

## Datos para la entrega

- **Mensaje de commit:** `feat(roles): agregar catálogo de roles con alcance y recursos autorizados (HU-004)`
- **Cuerpo:** Nuevo slice `roles` (dominio, aplicación, infraestructura completos): agregado `Role`
  con alcance `GLOBAL`/`TENANT`/`APPLICATION`, 3 reglas de negocio (nombre único por alcance, existencia
  para el tenant, cobertura de alcance sobre el recurso), 3 casos de uso (definir rol, conceder recurso,
  listar catálogo paginado) y 3 endpoints (`POST /api/v1/roles`, `POST /api/v1/roles/{roleId}/resources`,
  `GET /api/v1/roles`). `[M]` en `resources`: nuevo puerto/validador `ProtectedResourceOwnerLookupValidator`
  publicado como `resources :: rule`. `[M]` en `pdp/commons`: `RoleId`. 393 pruebas, `clean verify` en
  verde (con el `OPENSSL_CONF` de sistema corregido).
- **Rama:** `feature/HU-004-catalogo-roles`
- **Archivos a incluir:** todo `pdp/src/main/java/co/edu/uco/seguridad/pdp/roles/**`,
  `pdp/src/test/java/co/edu/uco/seguridad/pdp/roles/**`, los `[M]` en `pdp/resources/**` (main y test),
  `pdp/commons/model/RoleId.java`, `pdp/commons/message/ValueObjectMessages.java`,
  `shared/message/RequiredArgumentMessages.java`, `shared/web/message/WebContractMessages.java`,
  `pdp/src/test/java/co/edu/uco/seguridad/shared/persistence/surrealdb/SurrealRepositoryIntegrationTests.java`,
  y las 3 clases de prueba de HU-001 corregidas por el nuevo método del puerto
  (`ProtectedResourceMustExistValidatorTests`, `RegisterProtectedResourceRulesValidatorTests`,
  `RegisterProtectedResourceUseCaseImplTests`). El plan y este reporte se versionan aparte, en
  `docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
