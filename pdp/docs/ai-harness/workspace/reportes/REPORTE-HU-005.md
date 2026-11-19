# Reporte de validacion — HU-005

## Metadata

- **Slice:** `nuevo: assignments` + `[M]` en `identity`, `roles`, `pdp/commons`, `shared`
- **Fecha:** 2026-09-12 (segunda pasada — la primera fue rechazada por convención de idioma)
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-005.md`
- **Rama:** `feature/HU-005-asignaciones-vigentes`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Corrida independiente del validador (Docker
> verificado arriba, `OPENSSL_CONF` limpio para esta sesión).

```
ESTADO: VERDE  (mvnw clean verify, 52,4s, exit 0)
PRUEBAS: Tests run: 465, Failures: 0, Errors: 0, Skipped: 0
```

```
consistencia.ps1 → CONSISTENTE: applications, assignments, authorization, identity, resources, roles, tenants
drift.ps1        → 1 hallazgo activo: una clase del starter del PEP, citada en
                    MAPA-PLATAFORMA-SEGURIDAD.md, que drift.ps1 no encuentra porque su escaneo no
                    cubre el árbol pep/ — la clase sí existe ahí. Ese documento no se tocó en esta
                    historia: es deriva preexistente y ajena a HU-005, no un hallazgo nuevo.
                    (El reporte anterior de esta misma historia citaba, en su propio texto, esa
                    misma clase y la clase de prueba ya renombrada — ambas citas desaparecen al
                    sobrescribir ese reporte con este.)
```

| Comprobacion                   | Resultado                                                                       |
|--------------------------------|---------------------------------------------------------------------------------|
| Compilacion                    | ✅                                                                               |
| Pruebas                        | ✅ 465 pruebas                                                                   |
| Cobertura (≥ 50 % por paquete) | ✅ (parte de `clean verify` en verde)                                            |
| `LayeredArchitectureTests`     | ✅                                                                               |
| `ModulithStructureTests`       | ✅                                                                               |
| `consistencia.ps1`             | ✅                                                                               |
| `drift.ps1`                    | ✅ (0 hallazgos nuevos; el único activo es preexistente y ajeno a esta historia) |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno. El bloqueante de la primera pasada (convención de idioma: la clase de dominio `Vigencia`
era la única de 342 clases de producción con nombre en español) quedó resuelto: se renombró a
`Validity` — junto con `InvalidVigenciaException` → `InvalidValidityException`, el campo/accesor
`vigencia` → `validity` en `Assignment` y sus cuatro consumidores, la clase anidada
`ValueObjectMessages.Vigencia` → `.Validity`, el método `AssignmentsMessages.invalidVigencia` →
`.invalidValidity`, y la constante `RequiredArgumentMessages.VIGENCIA` → `.VALIDITY` — conservando
el texto en español en todos los mensajes. Confirmado por barrido completo: cero identificadores en
español en las 342 clases de producción; el texto en español sobrevive solo donde corresponde
(Javadoc y catálogos de mensajes).

## Observaciones menores

### [Plan §2, criterio 4] — Idempotencia de revocar sin prueba dedicada

Se mantiene de la pasada anterior: el plan declara "revocar una ya revocada es idempotente"; ningún
caso de prueba lo ejercita explícitamente (dos `DELETE` seguidos sobre la misma asignación). El
diseño lo sostiene — `Validity.endingAt` solo exige que el nuevo `validUntil` sea posterior a
`validFrom`, no compara contra un `validUntil` previo — pero no hay una prueba que lo demuestre.
No bloquea: observación para quien retome esta área.

### [drift.ps1] — Falso positivo preexistente sobre la clase de registro del starter del PEP

Ver arriba, sección "Resultado del build". Ajeno a esta historia; queda registrado para quien saneé
el alcance del escaneo de `drift.ps1`.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                         |
|---|-----------------------------------------------------------------|-----------|---------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅         | Ver tabla siguiente (sin cambios respecto a la primera pasada — el rename no tocó comportamiento) |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol)  | ✅         | Bloqueante de la pasada anterior resuelto — ver "Bloqueantes"                                     |
| 3 | ¿Introdujo deriva doc↔codigo?                                   | ✅         | `drift.ps1` en verde; el único hallazgo activo es preexistente y ajeno                            |
| 4 | ¿La logica quedo en la capa correcta?                           | ✅         | Sin cambios respecto a la primera pasada — el rename no movió lógica de capa, solo nombres        |

**Detalle criterio 1 (aceptación) — criterio del plan → evidencia:**

| # criterio                  | Evidencia                                                                                                                                                                                                                                                                            |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 (asignar)                 | `AssignRoleUseCaseImplTests.assigns_the_role_with_a_generated_id_and_the_current_time_and_no_end` + `AssignmentHttpTests.assigns_a_tenant_scoped_role_to_an_application_of_the_tenant` (201)                                                                                         |
| 2 (coherencia de alcance)   | `RoleScopeMustCoverApplicationRuleImplTests` (5 casos) + `AssignRoleRulesValidatorImplTests.rejects_when_the_role_does_not_cover_the_application` + `AssignmentHttpTests.refuses_assigning_an_application_scoped_role_to_another_application` (400 `APPLICATION_OUTSIDE_ROLE_SCOPE`) |
| 3 (no duplicar)             | `AssignmentMustNotDuplicateActiveRuleImplTests` + `AssignRoleRulesValidatorImplTests.rejects_when_the_assignment_is_already_active` + `AssignmentHttpTests.refuses_assigning_the_same_triple_twice` (409)                                                                            |
| 4 (revocar)                 | `RevokeAssignmentUseCaseImplTests.saves_the_assignment_with_the_end_fixed_and_completes` + `AssignmentHttpTests.revoking_removes_the_role_from_the_active_context` (200, confirmado vía `ResolveActiveRolesUseCase`). Idempotencia: observación menor, sin prueba directa            |
| 5 (solo vigentes)           | `SurrealRepositoryIntegrationTests.assignment_repository_excludes_a_revoked_assignment_from_active_roles` + `ResolveActiveRolesUseCaseImplTests`                                                                                                                                     |
| 6 (la fuente es el almacén) | `ResolveActiveRolesUseCaseImpl` solo consulta `AssignmentRepository.findActiveRoleIdsFor`, nunca el JWT — sin ninguna referencia a claims en `assignments`                                                                                                                           |
| 7 (sujeto sin asignaciones) | `ResolveActiveRolesUseCaseImplTests.returns_an_empty_set_when_the_subject_has_no_assignments`                                                                                                                                                                                        |
| 8 (aislamiento)             | `AssignmentHttpTests.the_catalog_never_shows_assignments_of_another_tenant` + `SurrealRepositoryIntegrationTests.assignment_repository_finds_no_assignment_for_a_tenant_it_does_not_belong_to` + `.assignment_repository_lists_the_role_catalog_excluding_other_tenants`             |

**Detalle criterio 4 (capa correcta):** sin cambios respecto a la validación anterior — el rename de
`Vigencia`→`Validity` tocó exclusivamente nombres (clase, campo, accesor generado, mensaje, constante),
verificado archivo por archivo durante esta pasada; ninguna línea de lógica, ninguna firma pública
más allá del propio nombre del tipo, ningún archivo fuera de `assignments`/`commons`/`shared`.

## Criterios de la linea base

> Solo los que el plan declaro. Sin cambios respecto a la pasada anterior — el rename no afecta
> ningún punto de control salvo el criterio 21, que ahora sí cierra.

| #  | Criterio                    | Resultado | Punto de control comprobado                                                                                                                                                    |
|----|-----------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture          | ✅ 🤖      | `LayeredArchitectureTests` + `ModulithStructureTests` en verde                                                                                                                 |
| 2  | Contratos de servicios      | ✅         | Interfaces vacías sobre `shared/contract`; `AssignmentRepository` explícito                                                                                                    |
| 3  | Reglas e integridad         | ✅         | Reglas puras y síncronas; validadores hacen la E/S; cero `if/throw` en los use cases                                                                                           |
| 4  | Capacidades transversales   | ✅         | `IdentifierGenerator`/`TimeProvider` inyectados donde se construye desde cero o se resuelve "ahora"                                                                            |
| 5  | Manejo de mensajes          | ✅         | `ApiResponse.success(...)`, incluido el `Void` del `DELETE` con witness de tipo                                                                                                |
| 6  | Manejo de parámetros        | ✅         | Raw `String` + `RequestFieldParser` en los 3 mappers de entrada                                                                                                                |
| 7  | Adaptadores de persistencia | ✅         | `SurrealAssignmentRepository` sin negocio; tabla en `AssignmentSchema.TABLE`                                                                                                   |
| 9  | Excepciones                 | ✅         | Jerarquía correcta; `InvalidValidityException`/`DuplicateAssignmentException`/`AssignmentNotFoundException`/`ApplicationOutsideRoleScopeException` sin tocar `ApiErrorHandler` |
| 11 | Interacción entre capas     | ✅         | Controller → interactor → use case → rules/validators → repositorio                                                                                                            |
| 12 | SOLID                       | ✅         | Constructor injection contra interfaces en los 13 beans de `AssignmentsConfiguration`                                                                                          |
| 13 | DTOs                        | ✅         | Dos niveles, `RequestFieldParser` de por medio                                                                                                                                 |
| 14 | DTOs seguros                | ✅         | `requireNonNull` en todos los `record` nuevos — barrido explícito sin hallazgos                                                                                                |
| 15 | Validación de dominio       | ✅         | `AssignmentId`, `Validity` (L1: fin posterior a inicio), `Assignment` validan en constructor compacto                                                                          |
| 16 | Repositorios dinámicos      | ✅         | `AssignmentRepository.findBy(AssignmentCriteria, PageWindow)`                                                                                                                  |
| 17 | Consultas dinámicas         | ✅         | `AssignmentCriteria(RoleId, TenantId)` con `matches(Assignment)`                                                                                                               |
| 18 | Paginación                  | ✅         | `PageWindow` (máx. 100) reutilizado                                                                                                                                            |
| 19 | Rangos                      | ✅         | Copia literal de `ListRolesRequestMapper`, probado                                                                                                                             |
| 20 | Adaptadores limpios         | ✅         | Sin reglas de negocio en controller/mappers/repositorio                                                                                                                        |
| 21 | Modelo refinado             | ✅         | `record` inmutables con factorías con nombre y comportamiento; nombres en inglés — el hallazgo de la pasada anterior queda cerrado                                             |
| 22 | Arquitectura reactiva       | ✅         | `Mono` en toda la cadena; único `.block()` en el `ApplicationRunner` de arranque                                                                                               |

## Desviaciones respecto al plan

| Archivo                                                                             | Plan decia                                                                                                                                                                   | Codigo hace                                                                                                              | ¿Justificado?                                                                                                                                                               |
|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pdp/assignments/domain/model/Validity.java` (antes `Vigencia.java`)                | El plan (sección 7) fijaba el nombre `Vigencia` para el VO de vigencia                                                                                                       | Se renombró a `Validity`, con sus cinco puntos de uso, tras el rechazo de la primera validación por convención de idioma | Sí — corrección de un bloqueante real del propio proceso de validación, no una decisión de negocio nueva. Documentado en el cierre del implementador de esta segunda pasada |
| `pdp/assignments/application/rule/validator/impl/AssignRoleRulesValidatorImpl.java` | Igual que en la pasada anterior: `TimeProvider` en el constructor (corrección del tester) + disciplina `Mono.defer` en cada `.then(metodo())` (corrección del implementador) | Sin cambios en esta pasada                                                                                               | Sí — ya validado                                                                                                                                                            |

## Datos para la entrega

- **Mensaje de commit:** `feat(assignments): agregar catálogo de asignaciones vigentes (HU-005)`
- **Cuerpo:** Nuevo slice `assignments` (dominio, aplicación, infraestructura completos): agregado
  `Assignment` con `Validity` (intervalo de vigencia), 2 reglas de negocio propias (no duplicar
  asignación activa, existencia para el tenant al revocar) + 1 nueva en `roles`
  (`RoleScopeMustCoverApplicationRule`, INV-ASN-02), 4 casos de uso (asignar, revocar, listar,
  resolver roles activos — este último sin HTTP, para consumo interno de HU-006) y 3 endpoints
  (`POST`/`DELETE`/`GET /api/v1/roles/{roleId}/assignments`). `[M]` en `identity`: primer
  `@NamedInterface` del módulo (`UserMustExistValidator`, publicado como `identity :: rule`). `[M]`
  en `roles`: `RoleRepository.findById` (sin filtrar por tenant, a diferencia de
  `findByIdForTenant`) y las interfaces nombradas `roles :: dto`/`roles :: exception`. `[M]` en
  `pdp/commons`: `UserId` se muda desde `identity` (segundo consumidor). 465 pruebas, `clean verify`
  en verde.
- **Rama:** `feature/HU-005-asignaciones-vigentes`
- **Archivos a incluir:** todo `pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/**`,
  `pdp/src/test/java/co/edu/uco/seguridad/pdp/assignments/**`, los `[M]` en `pdp/identity/**` y
  `pdp/roles/**` (main y test), `pdp/commons/model/UserId.java`,
  `pdp/commons/message/ValueObjectMessages.java`, `shared/message/RequiredArgumentMessages.java`,
  `shared/web/message/WebContractMessages.java`,
  `pdp/src/test/java/co/edu/uco/seguridad/shared/persistence/surrealdb/SurrealRepositoryIntegrationTests.java`,
  y los 6 archivos de prueba de `identity` corregidos por la mudanza de `UserId`. El plan y este
  reporte se versionan aparte, en `docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
