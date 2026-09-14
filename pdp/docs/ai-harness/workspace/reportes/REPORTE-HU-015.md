# Reporte de validación — HU-015 (segunda vuelta)

> **Addendum — 2026-09-13, posterior a este reporte.** El bloqueante único de esta vuelta (deriva de
> documentación) se corrigió: los tres documentos citados abajo se actualizaron para apuntar a las
> clases reales de `assignments`, y `drift.ps1` volvió a quedar en el único hallazgo preexistente y
> ajeno (`PepRegistrationProperties`). La rama se fusionó a `develop` en PR #48. Durante la
> verificación posterior de `develop` completo (`clean verify`, no `-Rapido`) apareció además un
> **bug de producción no atribuible a esta historia**: el índice único
> `role_scope_name`/`profile_scope_name` no indexaba ni aplicaba unicidad cuando `applicationId`
> está ausente (roles/perfiles de alcance `TENANT`/`GLOBAL`) — corregido en un commit aparte
> (`56891ca`) sobre `develop`, junto con un fallo de cobertura no relacionado (dos métodos `default`
> de puerto secundario sin ejercitar, commit `ff4774f`). `develop` queda **VERDE**: 653 pruebas, 0
> fallos, cobertura, consistencia y deriva en verde. Este addendum lo escribe la sesión que aplicó
> los fixes, no una nueva pasada de `@4-validador` — el veredicto formal de los cuatro juicios de
> esta vuelta (todos ✅ salvo el Juicio 3, ya resuelto) sigue siendo válido tal como se escribió
> abajo. Ver `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md` §3 para el detalle del bug.

## Metadata

- **Slice:** `applications` (sin cambios propios) + `assignments` + `authorization` + `roles` + `identity`
- **Fecha:** 2026-09-13
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-015.md` (con las enmiendas §13, §14 y §15)
- **Rama:** `feature/HU-015-administracion-aplicaciones`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `clean verify`).

```
ESTADO: ROJO  (mvnw clean verify, 214,3s, exit 1)
JDK: Java 25 en C:\Users\sebas\.jdks\corretto-25.0.4.1

PRUEBAS: Tests run: 649, Failures: 2, Errors: 1, Skipped: 0
```

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ⚠️ 3 fallos de 649 — los tres preexistentes en `develop` desde la primera vuelta (ver Observación B1), cero nuevos atribuibles a esta historia |
| Cobertura (≥ 50 % por paquete) | N/A — Surefire falla antes de `jacoco:check` (`BUILD FAILURE` en la línea 911 del log, fase `test`, nunca llega a `verify`) |
| `LayeredArchitectureTests` | ✅ (3/3) |
| `ModulithStructureTests` | ✅ (1/1) |

`consistencia.ps1`: **CONSISTENTE**, los 8 slices.

`drift.ps1`: **7 hallazgos, 6 nuevos** — ver Bloqueante abajo.

## Estado final

> ⛔ **RECHAZADO** — hay 1 bloqueante.

**Un solo bloqueante = RECHAZADO**, aunque el cierre del bloqueante de la primera vuelta (endpoints
gateados activos, fixture de identidad, 0 fallos nuevos de prueba) sea correcto y completo.

## Bloqueantes

### [Juicio 3] — El traslado/eliminación de clases de `applications` dejó 3 documentos con enlaces y citas rotas

- **Archivos:**
  - `pdp/docs/interfaces/13-input-strategy-dtos.md` — cita `RegisterApplicationRawRequest.java`,
    `RegisterApplicationRequestMapper.java` y `RegisterApplicationRequestMapperTests.java`, los tres
    con enlace relativo roto (las tres clases se eliminaron al mover el registro a `assignments`,
    PLAN-HU-015.md §0/§13).
  - `pdp/docs/architecture/pdp-modulith-alignment.md` — cita `RegisterApplicationInteractor` (dos
    veces) como parte del módulo `applications`; esa clase ya no existe ahí.
  - `pdp/docs/evidence/verification-guide.md` — cita `RegisterApplicationRequestMapperTests` en su
    tabla de pruebas de mapper; ese archivo se eliminó (su reemplazo,
    `RegisterApplicationWithFirstAdministratorRequestMapperTests`, vive en `assignments`).
- **Problema:** `RegisterApplicationInteractor(+Impl)`, `RegisterApplicationRawRequest`,
  `RegisterApplicationRequestMapper` y su prueba se eliminaron correctamente como código huérfano
  (el registro ahora lo sirve `ApplicationRegistrationController` de `assignments`), pero **ningún
  agente actualizó los tres documentos que los citaban**. `drift.ps1` pasó de 1 hallazgo (preexistente,
  ajeno) a 7 — los 6 nuevos son consecuencia directa de esta historia.
- **Referencia:** `4-validador.md` Juicio 3 y regla invariante 5: *"la deriva preexistente es
  observación; la nueva es bloqueante"*. `drift.ps1` estaba en verde (salvo la excepción ya conocida
  de `PepRegistrationProperties`) antes de esta rama.
- **Corrección esperada:** actualizar los tres documentos para que reflejen el estado real —
  `13-input-strategy-dtos.md` y `verification-guide.md` deben apuntar a
  `RegisterApplicationWithFirstAdministratorRawRequest`/`RequestMapper`/`RequestMapperTests` en
  `assignments` (o retirar el ejemplo si ya no es representativo), y
  `pdp-modulith-alignment.md` debe quitar `RegisterApplicationInteractor` de `applications` y, si
  corresponde, documentar dónde vive ahora. No es una historia nueva: es la actualización de
  documentación que el propio cambio de código dejó pendiente.

## Observaciones menores

### [FASE 1] — B1: 3 fallos de prueba preexistentes, ajenos a esta historia

- **Archivos:** `InternalSecurityChainIntegrationTests` (falta `openssl` en esta máquina);
  `SurrealRepositoryIntegrationTests.role_repository_saves_and_finds_a_role_with_its_granted_resources`
  y `.profile_repository_saves_and_finds_a_profile_with_its_granted_roles` (nombres fijos que chocan
  con datos de corridas anteriores en la misma base de Testcontainers no limpiada).
- **Verificado (repetido en esta segunda vuelta):** mismos tres, sin cambios desde la primera
  validación; ninguno lo toca ni lo causa HU-015.
- **`PepRegistrationProperties`** (`MAPA-PLATAFORMA-SEGURIDAD.md`): preexistente en `origin/develop`,
  ajeno al PEP — confirmado de nuevo, no forma parte de los 6 hallazgos nuevos de `drift.ps1`.

Ninguna otra observación.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | Criterio 1 (alta de admin al registrar): `ApplicationRegistrationController` activo, `POST /api/v1/applications` → `RegisterApplicationWithFirstAdministratorUseCaseImplTests` (2/2) + `ApplicationHttpTests` end-to-end en verde. Criterio 2/3 (gate en Remove/Rotate): `ApplicationAdministrationController` con `DELETE` y `POST .../credential-rotations` activos, validados contra un `OpaFixtureServer` real en `ApplicationHttpTests.rotating_the_credential_of_an_existing_application_replaces_it_immediately`. Criterio 4 (backfill): `InternalApplicationAdministratorController` activo, `AssignApplicationAdministratorUseCaseImplTests` (2/2) + `AssignApplicationAdministratorInteractorImplTests` (4/4). Criterio 5 (cero regresión): `RegisterApplicationUseCaseImplTests`/`RemoveApplicationUseCaseImplTests`/`RotateApplicationCredentialUseCaseImplTests` de `applications` en verde sin tocarlos |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Identificadores en inglés (`SubjectUserIdLookupValidator`, `RoleLookupByNameInScopeValidator`, `linkTestIdentity`...); Javadoc y mensajes nuevos en español (`successApplicationRemoved`, `successApplicationAdministratorAssigned`, mensajes de `RequiredArgumentMessages`) |
| 3 | ¿Introdujo deriva doc↔código? | ⛔ | 6 hallazgos nuevos en `drift.ps1` — ver Bloqueante |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | Sin `if` de negocio en ningún use case; controllers delegan al interactor; mappers delegan formato a value objects; `SurrealRoleRepository.findByNameInScope`/`SurrealSecurityUserRepository.findIdentityBySubject` solo consultan; cero Spring en `application`; los `allowedDependencies` ampliados (`applications :: model`, `roles :: model`, `roles :: usecase`, `identity :: rule` en `authorization`, `roles :: dto` sobre `primaryport/response`) están documentados y justificados en PLAN-HU-015.md §0/§13/§14, no relajados a ciegas |

## Criterios de la línea base

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | 🤖 ✅ | `LayeredArchitectureTests` (3/3) + `ModulithStructureTests` (1/1) en verde |
| 2 | Contratos de servicios | ✅ | Las 7 interfaces nuevas (`RegisterApplicationWithFirstAdministratorUseCase`, `AssignApplicationAdministratorUseCase`, `AdministerApplicationRemovalUseCase`, `AdministerApplicationCredentialRotationUseCase`, `RoleLookupByNameInScopeValidator`, `SubjectUserIdLookupValidator`) son vacías y extienden un contrato de `shared/contract` |
| 3 | Reglas e integridad | ✅ | Ninguna `Rule` nueva — decisión de administración ya la toma OPA (HU-009); "encuentra o crea" es consulta, no regla |
| 9 | Excepciones | ✅ | Reutiliza `NotAuthorizedToAdministerException`/`ApplicationNotFoundException`; el `IllegalStateException` nuevo (UserId irresoluble) documentado como falla explícita de un caso extremo, no una regla de negocio |
| 11 | Interacción entre capas | ✅ | Los tres controllers nuevos/modificados solo importan su interactor y sus DTOs |
| 12 | SOLID | ✅ | Constructores con `Objects.requireNonNull` contra interfaces |
| 20 | Adaptadores limpios | ✅ | Mappers delegan formato a value objects vía `RequestFieldParser` |
| 21 | Modelo refinado | ✅ | Ningún agregado nuevo — reutiliza `Application`/`Role`/`Assignment`/`SecurityUser` tal cual |
| 22 | Arquitectura reactiva | ✅ | `Mono` en toda la cadena; cero `block()` en `pdp/src/main`; el bug de evaluación temprana de `.then(mono)` sigue corregido |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `roles/application/primaryport/response/package-info.java` | No estaba en el árbol §8 | `@NamedInterface("dto")` nuevo | Sí — exigido por `ModulithStructureTests` (§13) |
| `assignments/package-info.java`, `authorization/package-info.java` | `allowedDependencies` de la sección 7 | Ampliados con `roles :: model`, `applications :: model`, `identity :: rule` | Sí — documentado en §13/§14 |
| `identity/application/secondaryport/repository/SecurityUserRepository.findIdentityBySubject` + `SubjectUserIdLookupValidator` | No estaban en el plan original | Nuevos, para resolver el `UserId` del llamador sin depender de que el principal ya lo traiga | Sí — enmienda §14, aprobada en gate 1 tras encontrar que el diseño original rompía las 7 clases `*HttpTests` |
| `AbstractSurrealDbIntegrationTest.linkTestIdentity` (`pdp/src/test`) | No estaba en el plan original | Fixture compartido, aditivo | Sí — enmienda §15, aprobada en gate 1 |
| Los tres documentos citados en el Bloqueante | Sin mención | Quedaron citando clases eliminadas | **No** — es el bloqueante de esta vuelta |

## Datos para la entrega

> No aplica todavía — el estado es RECHAZADO. Se completa cuando la revalidación apruebe.

## Próximos pasos

Actualizar los tres documentos (`13-input-strategy-dtos.md`, `pdp-modulith-alignment.md`,
`verification-guide.md`) para que dejen de citar las clases eliminadas de `applications` y, donde
corresponda, apunten a sus reemplazos en `assignments`. No requiere tocar `pdp/src/main` ni
`pdp/src/test` — es puramente documentación. Luego se repite la validación.
