# Reporte de validación — HU-019

## Metadata

- **Slice:** `profiles` (pierde dos escrituras) + `assignments` (pierde dos escrituras) + `authorization` (las recibe,
  orquestadas)
- **Fecha:** 2026-09-15
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-019.md`
- **Rama:** `feature/HU-019-gatear-perfiles-administracion` (sugerida; no creada todavía — el ciclo completo corrió
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
| Pruebas                        | ✅ 675 pruebas, 0 fallos (incluye las de HU-018, validada en el mismo corte) |
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

- **Archivo:** los 4 interactores `Administer Profile*InteractorImpl` en `authorization`
- **Problema:** mismo caso que HU-018 (ver `REPORTE-HU-018.md`) — el parseo raw→request y, en el caso de
  `AdministerProfileDefinitionInteractorImpl`, la validación de alcance (`GLOBAL` rechazado, `applicationId`
  obligatorio/prohibido según el nivel) quedaron inline en el interactor en vez de en un `DefineProfileRequestMapper`
  dedicado como hizo HU-016 para `DefineRoleRequestMapper`.
- **Referencia:** `sb-estandares`; `consistencia.ps1` no lo detecta.
- **Corrección esperada:** no bloquea — la lógica de validación de alcance está probada end-to-end (
  `ProfileHttpTests.refuses_a_global_scope_on_this_channel` sigue en verde, ejercitando la ruta real a través del nuevo
  controller). Si se retoma este módulo, extraer el mapper dejaría el módulo con una sola forma.

### [Observación] — Comentario de clase desactualizado en `ProfileHttpTests.java`

- **Archivo:**
  `pdp/src/test/java/co/edu/uco/seguridad/pdp/profiles/infrastructure/adapter/primary/web/ProfileHttpTests.java`
- **Problema:** el javadoc de la clase todavía dice "toda la cadena... es esqueleto todavía: se espera rojo por 500" —
  deriva preexistente de la era HU-011, ajena a esta historia (el archivo no se tocó).
- **Referencia:** regla invariante 5 del validador: "la deriva preexistente es observación, la nueva es bloqueante".
- **Corrección esperada:** ninguna en esta historia; queda anotado para quien retome ese archivo.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|---|-----------------------------------------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | Criterios 1-3 (definir perfil con/sin admin, TENANT sin gate): `AdministerProfileDefinitionUseCaseImplTests` (3/3) + `AdministerProfileDefinitionInteractorImplTests` (2/2) + e2e `ProfileHttpTests` (2/2, sigue en verde). Criterios 4-6 (agregar rol con/sin admin, TENANT sin gate): `AdministerProfileRoleAdditionUseCaseImplTests` (3/3) + `AdministerProfileRoleAdditionInteractorImplTests` (3/3). Criterios 7-8 (asignar perfil con/sin admin): `AdministerProfileAssignmentCreationUseCaseImplTests` (2/2) + `AdministerProfileAssignmentCreationInteractorImplTests` (1/1) + e2e `ProfileAssignmentHttpTests` (2/2, corregido con `OpaFixtureServer`, ver desviaciones). Criterios 9-10 (revocar con/sin admin): `AdministerProfileAssignmentRevocationUseCaseImplTests` (2/2) + `AdministerProfileAssignmentRevocationInteractorImplTests` (1/1). Criterio 11 (listar sin cambios): `ProfileControllerTests.list_delegates...`. Criterio 12 (perfil/asignación inexistente): `ProfileApplicationLookupValidatorImplTests`/`ProfileAssignmentApplicationLookupValidatorImplTests` cubren ambos `NotFoundException` |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Identificadores en inglés; mensajes nuevos (`AuthorizationMessages.globalProfileScopeNotAdministrableYet`, etc.) en español                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | `drift.ps1` SIN DERIVA                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | Sin `if` de negocio en ningún use case; el gate condicional (`Optional.map(...).orElseGet(Mono::empty)`) es control de flujo, no una regla, mismo criterio ya aceptado en HU-016; controllers delegan sin decidir; `ProfileApplicationLookupValidatorImpl`/`ProfileAssignmentApplicationLookupValidatorImpl` solo consultan y traducen; `allowedDependencies` ampliados (`profiles :: rule/usecase/dto/model`, `assignments :: model` en `authorization`) documentados con su motivo en el `package-info.java`. Ver observación menor sobre el mapper inline                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

## Criterios de la línea base

| #  | Criterio                | Resultado | Punto de control comprobado                                                                                                                                                                                                                     |
|----|-------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture      | 🤖 ✅      | `LayeredArchitectureTests` (3/3) + `ModulithStructureTests` (1/1) en verde                                                                                                                                                                      |
| 2  | Contratos de servicios  | ✅         | Las 4 interfaces nuevas (`AdministerProfileDefinitionUseCase`, `AdministerProfileRoleAdditionUseCase`, `AdministerProfileAssignmentCreationUseCase`, `AdministerProfileAssignmentRevocationUseCase`) son vacías y extienden `ReactiveOperation` |
| 3  | Reglas e integridad     | ✅         | Ninguna `Rule` nueva — la decisión de administración ya la toma OPA (HU-009)                                                                                                                                                                    |
| 9  | Excepciones             | ✅         | Reutiliza `NotAuthorizedToAdministerException`/`ProfileNotFoundException`/`ProfileAssignmentNotFoundException`, y `MalformedRequestFieldException` para el rechazo de alcance `GLOBAL` (mismo patrón que HU-016)                                |
| 11 | Interacción entre capas | ✅         | Los tres controllers nuevos/modificados solo importan su interactor y sus DTOs                                                                                                                                                                  |
| 12 | SOLID                   | ✅         | Constructores con `Objects.requireNonNull` contra interfaces                                                                                                                                                                                    |
| 21 | Modelo refinado         | ✅         | Ningún agregado nuevo — reutiliza `Profile`/`ProfileAssignment` tal cual                                                                                                                                                                        |
| 22 | Arquitectura reactiva   | ✅         | `Mono`/`Mono.zip` en toda la cadena; cero `block()` en `pdp/src/main`                                                                                                                                                                           |

## Desviaciones respecto al plan

| Archivo                                                                                                                                                                | Plan decía                                                         | Código hace                                                                                                                                                                                                                                                                       | ¿Justificado?                                                                                                                                                                     |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DefineProfileRequestMapper.java`/`AddRoleToProfileRequestMapper.java`/etc. (en `authorization`)                                                                       | §8 los listaba como `[N] ← profiles`/`← assignments`               | No se crearon; el parseo quedó inline en cada interactor                                                                                                                                                                                                                          | Parcialmente — ver observación menor. No bloquea                                                                                                                                  |
| `pdp/profiles/application/usecase/package-info.java`, `pdp/profiles/application/primaryport/response/package-info.java`, `pdp/profiles/domain/model/package-info.java` | No estaban en el árbol §8                                          | Nuevos, `@NamedInterface(...)`                                                                                                                                                                                                                                                    | Sí — trampa de Modulith: primer consumidor externo de esos tres subpaquetes de `profiles`, documentado en `authorization/package-info.java`                                       |
| `AuthorizationMessages.java`                                                                                                                                           | El plan no anticipaba mensajes de rechazo de alcance para perfiles | Se agregaron `globalProfileScopeNotAdministrableYet()`/`applicationIdNotApplicableForProfileTenantScope()`, espejo de los que HU-016 ya tiene para roles                                                                                                                          | Sí — mismo patrón exacto que `DefineRoleRequestMapper`, sin el cual `DefineProfileRawRequest` no podía rechazar `GLOBAL`                                                          |
| `pdp/src/test/.../ProfileAssignmentHttpTests.java`                                                                                                                     | No estaba en el alcance de HU-019 tocar un test de HU-011          | Se le agregó un `OpaFixtureServer` embebido (mismo patrón que `AssignmentHttpTests`)                                                                                                                                                                                              | Sí — regresión real: el gate incondicional de `AssignProfileUseCase` necesita OPA alcanzable; sin la fixture, el test preexistente fallaba con 400 `NOT_AUTHORIZED_TO_ADMINISTER` |
| `ListProfilesInteractorImplTests.java` (nuevo)                                                                                                                         | No estaba en la sección 9 del plan                                 | Se agregó para restituir la cobertura de `profiles...interactor.impl`, que cayó al eliminar `DefineProfileInteractorImpl`/`AddRoleToProfileInteractorImpl` (sin ella, ese paquete quedaba con `ListProfilesInteractorImpl` como único miembro y sin ninguna prueba unitaria real) | Sí — mismo criterio que la "trampa comprobada" ya documentada en `4-validador.md`: un refactor que retira código puede dejar una clase huérfana de cobertura                      |

## Datos para la entrega

- **Mensaje de commit:**
  `feat(authorization): gatea definicion/composicion/asignacion de perfiles por administracion de aplicacion (HU-019)`
- **Cuerpo:** Nuevos `ProfileApplicationLookupValidator` (`profiles :: rule`) y
  `ProfileAssignmentApplicationLookupValidator` (`assignments :: rule`). Cuatro casos de uso nuevos en `authorization`
  gatean `DefineProfileUseCase`/`AddRoleToProfileUseCase` (condicional, vacío para alcance `TENANT`) y
  `AssignProfileUseCase`/`RevokeProfileAssignmentUseCase` (incondicional) vía el mecanismo de HU-009. Las rutas de
  escritura se movieron de `ProfileController`/`ProfileAssignmentController` a `ProfileAdministrationController`/
  `ProfileAssignmentAdministrationController`; los controllers viejos quedan solo con lectura (`ProfileController`) o se
  eliminan (`ProfileAssignmentController`, sin lectura que conservar).
- **Rama:** `feature/HU-019-gatear-perfiles-administracion`
- **Archivos a incluir:** todo lo nuevo/modificado en `pdp/src/main` y `pdp/src/test` bajo `profiles`/`assignments`/
  `authorization` para esta historia — el plan y este reporte se versionan aparte

## Próximos pasos

Listo para el gate 2 (entrega). Confirma para proceder con commit y push.
