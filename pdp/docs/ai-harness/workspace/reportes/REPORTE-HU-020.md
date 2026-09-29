# Reporte de validacion — HU-020

## Metadata

- **Slice:** `assignments`, `authorization`
- **Fecha:** 2026-09-15
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-020.md`
- **Rama:** `feature/HU-020-autoservicio-administradores`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Pegar el resumen tal cual, no el log.

```
ESTADO: VERDE  (mvnw clean verify, 231,3s, exit 0)
JDK: Java 25 en C:\Users\sebas\.jdks\corretto-25.0.4.1

PRUEBAS: Tests run: 706, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion                   | Resultado     |
|--------------------------------|---------------|
| Compilacion                    | ✅             |
| Pruebas                        | ✅ 706 pruebas |
| Cobertura (≥ 50 % por paquete) | ✅             |
| `LayeredArchitectureTests`     | ✅             |
| `ModulithStructureTests`       | ✅             |

## Estado final

> ✅ APROBADO — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [FASE 2] — `assignments/package-info.java` sin cambio, tal como el plan anticipaba

- **Archivo:** `pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/package-info.java`
- **Observación:** el plan (§8) dejaba como condicional un ajuste a `allowedDependencies` "si `assignments :: usecase`
  no cubre ya los dos nuevos" — no hizo falta: `ModulithStructureTests` pasa sin tocar el archivo, confirmando que
  HU-015 ya dejó la declaración cubierta.
- **Justificado:** sí, es exactamente el resultado que el plan preveía como más probable.

## Los cuatro juicios

> Lo que ninguna prueba puede verificar. Cada uno se responde con evidencia, no con una impresion.

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                 |
|---|-----------------------------------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅         | Ver tabla de trazabilidad de criterios abajo — los 9 criterios de PLAN-HU-020.md §2 tienen prueba dedicada                                                                                                                                                                                                                |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol)  | ✅         | Identificadores (`RemoveApplicationAdministratorUseCaseImpl`, `LastAdministratorMustNotBeRevokedRule`, …) en inglés; Javadoc y `AssignmentsMessages.cannotRemoveLastAdministrator`/`WebContractMessages.successApplicationAdministrator*` en español                                                                      |
| 3 | ¿Introdujo deriva doc↔codigo?                                   | ✅         | `drift.ps1` → `SIN DERIVA: todos los enlaces resuelven y toda clase citada existe`                                                                                                                                                                                                                                        |
| 4 | ¿La logica quedo en la capa correcta?                           | ✅         | El único `if` de negocio (¿es el único admin?) vive en `LastAdministratorMustNotBeRevokedRuleImpl` (una `Rule`, no un `if` inline en el use case); `ApplicationAdministratorController` solo delega a los 3 interactores; ningún adaptador de persistencia decide nada; cero anotaciones Spring en `domain`/`application` |

### Trazabilidad de criterios (PLAN-HU-020.md §2)

| # | Criterio                                                                                   | Prueba/archivo que lo demuestra                                                                                                                                       |
|---|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Agregar administrador, siendo administrador → 201                                          | `ApplicationAdministratorControllerTests.assign_delegates_to_the_interactor_and_replies_with_201`, `AdministerApplicationAdministratorAssignmentUseCaseImplTests`     |
| 2 | Agregar, sin ser administrador → 400 `NOT_AUTHORIZED_TO_ADMINISTER`                        | `AdministerApplicationAdministratorAssignmentUseCaseImplTests.never_assigns_...`                                                                                      |
| 3 | Listar, siendo administrador → 200                                                         | `ApplicationAdministratorControllerTests.list_delegates_...`, `ListApplicationAdministratorsUseCaseImplTests`                                                         |
| 4 | Listar, sin ser administrador → 400                                                        | `AdministerApplicationAdministratorListUseCaseImplTests.never_lists_...`                                                                                              |
| 5 | Quitar administrador que no es el único, siendo administrador → 200                        | `RemoveApplicationAdministratorUseCaseImplTests.revokes_when_there_is_more_than_one_active_administrator`                                                             |
| 6 | Quitar, sin ser administrador → 400                                                        | `AdministerApplicationAdministratorRemovalUseCaseImplTests.never_removes_...`                                                                                         |
| 7 | Quitar al único administrador activo → 400 `CANNOT_REMOVE_LAST_ADMINISTRATOR`, sin revocar | `RemoveApplicationAdministratorUseCaseImplTests.rejects_when_it_is_the_only_active_administrator_and_never_revokes`, `LastAdministratorMustNotBeRevokedRuleImplTests` |
| 8 | Endpoint interno mTLS de backfill (HU-015) sigue funcionando                               | `InternalApplicationAdministratorControllerTests` (sin cambios, sigue verde)                                                                                          |
| 9 | Suite completa                                                                             | `verificar.ps1` en verde (706/706)                                                                                                                                    |

## Criterios de la linea base

Ninguno declarado por el plan (HU-020 no toca la matriz `pdp/docs/criteria-compliance-matrix.md`).

## Desviaciones respecto al plan

| Archivo                                                                                              | Plan decia                                                       | Codigo hace                                                                                                                       | ¿Justificado?                                                                                                                                                                                 |
|------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AdministratorRevocationEligibility.java`                                                            | `int activeAdministratorCount` (§4)                              | Gana `ApplicationId applicationId` como primer componente                                                                         | Sí — `CannotRemoveLastAdministratorException` necesita el `applicationId` para su mensaje y la SPEC original lo omitía; corregido en FASE 1 de `2-tester-spec`, documentado en el propio plan |
| `RemoveApplicationAdministratorUseCaseImpl`/`ListApplicationAdministratorsUseCaseImpl` (constructor) | Sin firma fijada en §7                                           | `RoleLookupByNameInScopeValidator`, `AssignmentRepository`, `TimeProvider` (+ la regla y `RevokeAssignmentUseCase` en el primero) | Sí — el plan (§3, R2) ya describía el algoritmo; la firma concreta la fijó `2-tester-spec` contra ese algoritmo, documentado en el plan                                                       |
| Interactores/controller/raw requests/web response de `authorization`                                 | El árbol §8 los marca `[N]`, materializables por el planificador | Los materializó `2-tester-spec`, no `1-planificador`                                                                              | Sí — mismo criterio ya establecido en HU-018: crear/recortar controllers exige poder tocar sus pruebas, algo vedado al planificador                                                           |

## Datos para la entrega

- **Mensaje de commit:** `feat(assignments,authorization): autoservicio de administradores de aplicación (HU-020)`
- **Cuerpo:** nueva regla `LastAdministratorMustNotBeRevokedRule` (dominio, `assignments`);
  `RemoveApplicationAdministratorUseCase`/`ListApplicationAdministratorsUseCase` (`assignments`, reutilizan
  `AssignApplicationAdministratorUseCase` de HU-015 para el alta); gate incondicional en `authorization` vía los tres
  `AdministerApplicationAdministrator*UseCaseImpl`; endpoints nuevos
  `POST/DELETE/GET /api/v1/applications/{applicationId}/administrators` en `ApplicationAdministratorController`.
- **Rama:** `feature/HU-020-autoservicio-administradores`
- **Archivos a incluir:** todo lo nuevo/modificado en `pdp/src/main` y `pdp/src/test` bajo `assignments`/
  `authorization`/`shared/message` relacionado con HU-020 (el plan y este reporte se versionan aparte, en
  `docs/ai-harness/workspace/`).

## Proximos pasos

Listo para el gate 2 (entrega).
