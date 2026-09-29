# Reporte de validación — HU-018

## Metadata

- **Slice:** `assignments` (pierde dos escrituras) + `authorization` (las recibe, orquestadas)
- **Fecha:** 2026-09-15
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-018.md`
- **Rama:** `feature/HU-018-gatear-asignaciones-administracion` (sugerida; no creada todavía — el ciclo completo corrió
  sobre `develop`)

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `clean verify`).

```
ESTADO: VERDE  (mvnw clean verify, 140,4s, exit 0)
JDK: Java 25 en C:\Users\sebas\.jdks\corretto-25.0.4.1

PRUEBAS: Tests run: 675, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobación                   | Resultado                                                                   |
|--------------------------------|-----------------------------------------------------------------------------|
| Compilación                    | ✅                                                                           |
| Pruebas                        | ✅ 675 pruebas, 0 fallos (incluye las de HU-019, validada en el mismo corte) |
| Cobertura (≥ 50 % por paquete) | ✅ (`jacoco:check` corrió dentro de `verify`, `BUILD SUCCESS`)               |
| `LayeredArchitectureTests`     | ✅ (3/3)                                                                     |
| `ModulithStructureTests`       | ✅ (1/1)                                                                     |

`consistencia.ps1`: **CONSISTENTE**, los 8 slices.

`drift.ps1`: **SIN DERIVA**.

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 4] — Sin mapper dedicado para las conversiones raw→request en `authorization`

- **Archivo:**
  `pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/infrastructure/adapter/primary/web/interactor/impl/AdministerAssignmentCreationInteractorImpl.java` (
  y su equivalente de revocación)
- **Problema:** HU-016/HU-017 extrajeron el parseo raw→request a una clase `{Nombre}RequestMapper` dedicada en
  `authorization/.../mapper/`, con su propia prueba movida. HU-018 en cambio hace el `RequestFieldParser.parse(...)`
  directo dentro del interactor — funciona igual (delega el formato al value object, mismo criterio de siempre) pero
  rompe la simetría de forma con el resto del módulo.
- **Referencia:** `sb-estandares` — "antes de escribir un mapper, abre el equivalente en `tenants`"; `consistencia.ps1`
  no lo detecta porque no compara la profundidad interna de un método, solo la forma de paquetes/clases entre slices.
- **Corrección esperada:** no bloquea esta historia (el comportamiento es correcto y está probado), pero si una historia
  futura vuelve a tocar estos interactores, extraer el parseo a un mapper dedicado dejaría `authorization` con una sola
  forma, no dos.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---|-----------------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | Criterios 1-2 (asignar con/sin administrador): `AdministerAssignmentCreationUseCaseImplTests` (2/2) + `AdministerAssignmentCreationInteractorImplTests` (1/1). Criterios 3-4 (revocar con/sin administrador): `AdministerAssignmentRevocationUseCaseImplTests` (2/2) + `AdministerAssignmentRevocationInteractorImplTests` (1/1). Criterio 5 (listar sin cambios): `AssignmentControllerTests.list_delegates...`. Criterio 6 (asignación inexistente): `AssignmentApplicationLookupValidatorImplTests.refuses_an_assignment_that_does_not_exist_for_the_tenant` → `AssignmentNotFoundException`. Criterio 7 (`AssignApplicationAdministratorUseCase` sin cambios): código no tocado, `AssignApplicationAdministratorInteractorImplTests` preexistente sigue en verde |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Identificadores en inglés (`AssignmentApplicationLookupValidator`, `AdministerAssignmentCreationUseCaseImpl`...); mensajes nuevos en `RequiredArgumentMessages` en español                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | `drift.ps1` SIN DERIVA                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | Sin `if` de negocio en ningún use case (`.then(Mono.defer(...))` puro); controllers delegan sin decidir; `AssignmentApplicationLookupValidatorImpl` solo consulta y traduce, no decide; cero Spring en `application`; `allowedDependencies` ampliados (`assignments :: rule`, `assignments :: model` en `authorization`) documentados y justificados en el `package-info.java` de `authorization`, no relajados a ciegas. Ver observación menor sobre el mapper inline                                                                                                                                                                                                                                                                                               |

## Criterios de la línea base

| #  | Criterio                | Resultado | Punto de control comprobado                                                                                                           |
|----|-------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture      | 🤖 ✅      | `LayeredArchitectureTests` (3/3) + `ModulithStructureTests` (1/1) en verde                                                            |
| 2  | Contratos de servicios  | ✅         | `AdministerAssignmentCreationUseCase`/`AdministerAssignmentRevocationUseCase` son interfaces vacías que extienden `ReactiveOperation` |
| 3  | Reglas e integridad     | ✅         | Ninguna `Rule` nueva — el gate lo decide OPA vía el mecanismo ya existente de HU-009                                                  |
| 9  | Excepciones             | ✅         | Reutiliza `NotAuthorizedToAdministerException`/`AssignmentNotFoundException`, sin excepciones nuevas                                  |
| 11 | Interacción entre capas | ✅         | `AssignmentAdministrationController` solo importa sus interactores y sus DTOs                                                         |
| 12 | SOLID                   | ✅         | Constructores con `Objects.requireNonNull` contra interfaces                                                                          |
| 21 | Modelo refinado         | ✅         | Ningún agregado nuevo — reutiliza `Assignment` tal cual                                                                               |
| 22 | Arquitectura reactiva   | ✅         | `Mono` en toda la cadena; cero `block()` en `pdp/src/main`                                                                            |

## Desviaciones respecto al plan

| Archivo                                                                                                          | Plan decía                              | Código hace                                              | ¿Justificado?                                                                                                                                                                                                                                                |
|------------------------------------------------------------------------------------------------------------------|-----------------------------------------|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AssignRoleRequestMapper.java`/`RevokeAssignmentRequestMapper.java` (en `authorization`)                         | §8 los listaba como `[N] ← assignments` | No se crearon; el parseo quedó inline en cada interactor | Parcialmente — ver observación menor. No bloquea, pero es una desviación real de forma                                                                                                                                                                       |
| `pdp/assignments/application/rule/validator/package-info.java`, `pdp/assignments/domain/model/package-info.java` | No estaban en el árbol §8               | Nuevos, `@NamedInterface("rule")`/`("model")`            | Sí — trampa de Modulith detectada compilando (primer consumidor externo de esos subpaquetes), documentada en `authorization/package-info.java`                                                                                                               |
| `pdp/src/test/.../ProfileAssignmentHttpTests.java` (HU-011, no HU-018)                                           | No mencionado en el plan de HU-018      | Se le agregó un `OpaFixtureServer` embebido              | Sí — regresión real encontrada al correr la suite completa: el gate incondicional de asignación de perfil (HU-019, misma sesión) necesita OPA alcanzable; sin la fixture, ese test preexistente fallaba con 400. Documentado también en el reporte de HU-019 |

## Datos para la entrega

- **Mensaje de commit:**
  `feat(authorization): gatea asignacion/revocacion de roles por administracion de aplicacion (HU-018)`
- **Cuerpo:** Nuevo `AssignmentApplicationLookupValidator` (`assignments :: rule`) resuelve la aplicación de una
  asignación existente. `AdministerAssignmentCreationUseCase`/`AdministerAssignmentRevocationUseCase` (`authorization`)
  gatean `AssignRoleUseCase`/`RevokeAssignmentUseCase` de forma incondicional vía el mecanismo de HU-009. Las rutas
  `POST`/`DELETE /api/v1/roles/{roleId}/assignments{/id}` se movieron de `AssignmentController` a
  `AssignmentAdministrationController`; `AssignmentController` queda solo con `GET`.
- **Rama:** `feature/HU-018-gatear-asignaciones-administracion`
- **Archivos a incluir:** todo lo nuevo/modificado en `pdp/src/main` y `pdp/src/test` bajo `assignments`/`authorization`
  para esta historia — el plan y este reporte se versionan aparte

## Próximos pasos

Listo para el gate 2 (entrega). Confirma para proceder con commit y push.
