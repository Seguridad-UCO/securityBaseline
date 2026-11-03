# Reporte de validacion — HU-011

## Metadata

- **Slice:** `profiles` (nuevo, BC-05), con `[M]` en `roles` y `assignments` (BC-08)
- **Fecha:** 2026-09-12
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-011.md`
- **Rama:** `feature/HU-011-perfiles-agrupacion-roles`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 80s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 579, Failures: 0, Errors: 0, Skipped: 0

Log completo: pdp/target/verificar-ultimo.log (707 lineas)
```

`jacoco:check` — "All coverage checks have been met." (410 clases analizadas, bundle `seguridad`).

| Comprobacion                   | Resultado                        |
|--------------------------------|----------------------------------|
| Compilacion                    | ✅                                |
| Pruebas                        | ✅ 579 pruebas, 0 fallos          |
| Cobertura (≥ 50 % por paquete) | ✅                                |
| `LayeredArchitectureTests`     | ✅ (incluida en la corrida verde) |
| `ModulithStructureTests`       | ✅ (incluida en la corrida verde) |

Comprobaciones adicionales ejecutables:

```
consistencia.ps1 → CONSISTENTE: todos los slices siguen la misma forma (applications, assignments,
                    authorization, identity, profiles, resources, roles, tenants)
drift.ps1        → 1 hallazgo: PepRegistrationProperties (MAPA-PLATAFORMA-SEGURIDAD.md) —
                    preexistente (commit 5c5fce7, 2026-09-11, previo a esta historia y sin relación
                    con `profiles`/`assignments`); no lo introdujo este cambio. Observación, no
                    bloqueante.
```

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 3 — deriva] Hallazgo preexistente de `drift.ps1`, no introducido por esta historia

- **Archivo:** `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md`
- **Problema:** cita `PepRegistrationProperties`, una clase que `drift.ps1` no encuentra.
- **Referencia:** el propio archivo no fue tocado por HU-011 (`git log` lo fecha en 2026-09-11,
  commit `5c5fce7`, sobre observabilidad/seguridad del PEP — fuera del alcance de perfiles). No se
  corrige aquí porque no es responsabilidad de esta historia y su corrección no debe mezclarse con
  un cambio no relacionado.
- **Correccion esperada:** una historia o tarea aparte que sanee `MAPA-PLATAFORMA-SEGURIDAD.md`.

### [Documentación interna del plan] Inconsistencia menor entre §4 y §7 de `PLAN-HU-011.md`

- **Archivo:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-011.md`
- **Problema:** la sección 4 (Modelo de dominio) decide correctamente que `ProfileId` vive en
  `pdp/commons/model/` (2 consumidores: `profiles` y `assignments`), pero la SPEC de la sección 7
  para `ProfileAssignment` referencia el tipo por la ruta `co.edu.uco.seguridad.pdp.profiles.domain.ProfileId`
  — inconsistente con la propia sección 4 del mismo plan.
- **Referencia:** `sb-estandares` — "VO usado por 2+ slices → `pdp/commons/model/`". El código
  implementado (`pdp/commons/model/ProfileId.java`, consumido por ambos slices) sigue la decisión
  correcta de la sección 4, no la ruta desactualizada de la sección 7. Poner `ProfileId` dentro de
  `profiles.domain` habría exigido además una frontera Modulith `"profiles"` extra en
  `assignments`, contradiciendo la propia nota de la sección 7 que dice que "esto no exige ninguna
  frontera nueva... porque `ProfileId` vive en `commons`".
- **Correccion esperada:** ninguna en código. Es una nota para quien lea el plan más adelante: la
  ruta correcta de `ProfileId` es la de la sección 4, la mención de la sección 7 tiene una ruta
  obsoleta de una iteración anterior del diseño.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|---|-----------------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅         | Ver tabla de trazabilidad de criterios más abajo — cada fila de §2 del plan tiene una prueba concreta que la ejercita (nombradas por clase)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol)  | ✅         | `ProfilesMessages`/`AssignmentsMessages` en español (`profileNameTaken`, `profileNotFound`, `globalScopeNotAdministrableYet`...); identificadores de clases/métodos en inglés (`Profile`, `ProfileAssignment`, `AssignProfileUseCase`...); Javadoc en español (`ProfileController`, `DefineProfileRequestMapper`, `RevokeProfileAssignmentUseCaseImpl`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 3 | ¿Introdujo deriva doc↔codigo?                                   | ✅         | `drift.ps1` muestra 1 hallazgo, preexistente y sin relación (ver Observaciones). Ningún archivo de `pdp/docs/` fue renombrado/movido por esta historia salvo `HU-008.md` → `HU-011.md`, ya referenciado correctamente en su propio archivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| 4 | ¿La logica quedo en la capa correcta?                           | ✅         | `grep -rl "org.springframework" .../profiles/{domain,application} .../assignments/{domain,application}` → vacío (fuera de `package-info.java`, que son metadatos de Modulith, no lógica). Reglas puras y síncronas (`ProfileNameMustBeUniqueInScopeRuleImpl`, `ProfileAssignmentMustNotDuplicateActiveRuleImpl`) sin I/O. Los validadores hacen la E/S y delegan la decisión a la regla (`DefineProfileRulesValidatorImpl`, `AddRoleToProfileRulesValidatorImpl`). Los controllers (`ProfileController`, `ProfileAssignmentController`) solo obtienen contexto, delegan al interactor y envuelven — cero DTOs de aplicación construidos a mano, cero decisiones. Ningún `allowedDependencies` se relajó para compilar: cada entrada nueva (`"roles :: model"`, `"roles :: dto"`, `"profiles :: rule"`, `"profiles :: dto"`) corresponde a una interfaz nombrada real, publicada en su `package-info.java` |

## Criterios de la línea base

| #     | Criterio                                           | Resultado | Punto de control comprobado                                                                                                                                                                                                                                                                                                                        |
|-------|----------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | Clean Architecture                                 | ✅ 🤖      | `LayeredArchitectureTests`/`ModulithStructureTests` verdes en la corrida; cero anotaciones Spring en `domain`/`application` de `profiles` y `assignments` (grep vacío)                                                                                                                                                                             |
| 2     | Contratos de servicios                             | ✅         | `DefineProfileUseCase`, `AssignProfileUseCase`, `ProfileRolesLookupValidator`, etc. son interfaces vacías que extienden contratos de `shared/contract`; puertos explícitos en `application/secondaryport/` (`ProfileRepository`, `ProfileAssignmentRepository`)                                                                                    |
| 3     | Reglas e integridad                                | ✅         | `ProfileNameMustBeUniqueInScopeRuleImpl`/`ProfileMustExistForTenantRuleImpl`/`ProfileAssignmentMustNotDuplicateActiveRuleImpl`/`ProfileAssignmentMustExistForTenantRuleImpl` — puras, síncronas, if/throw únicamente; cero `if/throw` de negocio en los use cases (`AssignProfileUseCaseImpl`/`RevokeProfileAssignmentUseCaseImpl` solo orquestan) |
| 4     | Capacidades transversales                          | ✅         | `IdentifierGenerator`/`TimeProvider` inyectados en `DefineProfileUseCaseImpl`/`AssignProfileUseCaseImpl`; sin `Instant.now()`/`UUID.randomUUID()` en línea (grep sobre `profiles`/`assignments` application sin resultados fuera de los puertos inyectados)                                                                                        |
| 5     | Manejo de mensajes                                 | ✅         | `ApiResponse.success("PROFILE_DEFINED", WebContractMessages.successProfileDefined(), ...)` con códigos estables en los 5 endpoints nuevos                                                                                                                                                                                                          |
| 6     | Manejo de parámetros                               | ✅         | `DefineProfileRawRequest`/`AssignProfileRawRequest` son `String` desnudos; `RequestFieldParser.parse` en los mappers                                                                                                                                                                                                                               |
| 7     | Adaptadores de persistencia                        | ✅         | `SurrealProfileRepository`/`SurrealProfileAssignmentRepository` implementan el puerto vía `Entity` + `{X}PersistenceMapper`, sin decidir negocio; nombre de tabla en `ProfileSchema.TABLE`/`ProfileAssignmentSchema.TABLE`                                                                                                                         |
| 9     | Excepciones                                        | ✅         | `InvalidProfileNameException`→`InvalidValueException`, `DuplicateProfileNameException`/`DuplicateProfileAssignmentException`→`ConflictBusinessRuleException`, `ProfileNotFoundException`/`ProfileAssignmentNotFoundException`→`BusinessRuleViolationException`; ningún `@RestControllerAdvice` nuevo                                               |
| 11    | Interacción entre capas                            | ✅         | Controller → Interactor → UseCase → Rules, verificado leyendo `ProfileController`/`ProfileAssignmentController` (no importan `application`)                                                                                                                                                                                                        |
| 12    | SOLID                                              | ✅         | Constructores con `Objects.requireNonNull` por dependencia; reglas sustituibles por bean (`ProfilesConfiguration`/`AssignmentsConfiguration`)                                                                                                                                                                                                      |
| 13    | DTOs                                               | ✅         | Dos niveles: `DefineProfileRawRequest`(Strings) → `DefineProfileRequestMapper` → `DefineProfileRequest`(VOs)                                                                                                                                                                                                                                       |
| 14    | DTOs seguros                                       | ✅         | Sin Jakarta Validation; 3 barreras verificadas en `DefineProfileRequestMapper` (campo presente vía `RequestFieldParser`, VO válido, `requireNonNull` en los records — los 12 records corregidos en la fase de implementación)                                                                                                                      |
| 15    | Validación de dominio                              | ✅         | `ProfileName`(3-60 chars trim), `Profile`/`ProfileAssignment` con `requireNonNull` completo en constructor compacto                                                                                                                                                                                                                                |
| 16-19 | Repositorios/consultas/paginación/rangos dinámicos | ✅         | `ProfileCriteria.ofTenant`/`matches`, `ProfileRepository.findBy(criteria, window)`, `ListProfilesRequestMapper` acepta `page`/`size` u `offset`/`limit` — mismo patrón que HU-001                                                                                                                                                                  |
| 21    | Modelo refinado                                    | ✅         | `Profile`/`ProfileAssignment` son `record` inmutables con factorías con nombre (`define`, `grant`) y comportamiento (`withRole`, `revoke`, `isActive`)                                                                                                                                                                                             |
| 22    | Arquitectura reactiva                              | ✅         | `Mono`/`Flux` en toda la cadena de `AssignProfileUseCaseImpl` (`Flux.fromIterable(roleIds).concatMap(...)`); sin `block()` en el camino de una petición (grep confirmado en la fase de implementación, solo en el `ApplicationRunner` de esquema, que es la excepción documentada)                                                                 |
| 23    | Arquitectura antes del negocio                     | ✅ 🤖      | `mvnw -f pdp/pom.xml verify` en verde, cobertura ≥ 50 % por paquete confirmada por `jacoco-check`                                                                                                                                                                                                                                                  |

## Desviaciones respecto al plan

| Archivo                                     | Plan decia                                                                         | Codigo hace                                                            | ¿Justificado?                                                                                                                                                                  |
|---------------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PLAN-HU-011.md` §7 (SPEC de `assignments`) | `ProfileId` referenciado como `co.edu.uco.seguridad.pdp.profiles.domain.ProfileId` | `ProfileId` vive en `co.edu.uco.seguridad.pdp.commons.model.ProfileId` | Sí — sigue la decisión correcta de la propia sección 4 del plan (VO con 2+ consumidores va en `commons`), no la ruta desactualizada de la sección 7. Ver Observaciones menores |

Ninguna otra desviación: el árbol de archivos de la sección 8 coincide 1:1 con lo existente (0
archivos faltantes, 0 archivos huérfanos sin registrar en su `{Slice}Configuration`), y ningún
archivo del plan quedó lanzando `UnsupportedOperationException`.

## Datos para la entrega

- **Mensaje de commit:**
  `feat(profiles,assignments): agrega HU-011 — perfiles como agrupación de roles, con asignación y revocación en cascada`
- **Cuerpo:** Nuevo slice `profiles` (BC-05): catálogo de perfiles con nombre único por alcance,
  agregado de roles y listado paginado (`POST /api/v1/profiles`, `POST
  /api/v1/profiles/{profileId}/roles`, `GET /api/v1/profiles`). Extiende `assignments` (BC-08) con
  `ProfileAssignment`: asignar un perfil materializa una `Assignment` por cada rol que agrupa
  (reutilizando `AssignRoleUseCase`), y revocarlo revoca en cascada cada `Assignment` generada
  (reutilizando `RevokeAssignmentUseCase`) — `POST
  /api/v1/profiles/{profileId}/assignments`, `DELETE
  /api/v1/profiles/{profileId}/assignments/{profileAssignmentId}`. Aditivo en `roles`: publica
  `RoleScope` como interfaz nombrada (`roles :: model`) y un `RoleMustExistForTenantValidator`
  (`roles :: rule`) para que `profiles` los consuma sin duplicar lógica. 579 pruebas, cobertura y
  arquitectura en verde.
- **Rama:** `feature/HU-011-perfiles-agrupacion-roles`
- **Archivos a incluir:** todo lo nuevo/modificado bajo `pdp/src/main` y `pdp/src/test` listado en
  `git status` para este cambio (slices `profiles`, `assignments`, `roles`, `commons`, `shared`),
  más `pdp/docs/ai-harness/workspace/HU-011.md` (renombrado desde `HU-008.md`) y
  `pdp/docs/ai-harness/PROJECT-MAP.md` (regenerado). El plan y este reporte se versionan en
  `pdp/docs/ai-harness/workspace/` como el resto de historias.

## Proximos pasos

Listo para el gate 2 (entrega).
