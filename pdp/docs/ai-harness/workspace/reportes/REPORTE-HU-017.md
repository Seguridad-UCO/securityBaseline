# Reporte de validación — HU-017

## Metadata

- **Slice:** `resources` (pierde una escritura) y `authorization` (la recibe, orquestada)
- **Fecha:** 2026-09-14
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-017.md`
- **Rama:** `feature/HU-017-gatear-registro-recursos`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `mvnw clean verify`).

```
ESTADO: VERDE  (mvnw clean verify, 112,1s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 671, Failures: 0, Errors: 0, Skipped: 0
```

`jacoco-check` corrió (línea 865 del log) y no reportó ninguna violación de umbral.

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 671 pruebas, 0 fallos |
| Cobertura (≥ 80 % en código nuevo) | ✅ `jacoco-check` corrió y no reportó violaciones |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |
| `consistencia.ps1` | ✅ CONSISTENTE — 8 slices |
| `drift.ps1` | ✅ SIN DERIVA |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

- **`resources/application/primaryport/response/package-info.java` (nuevo)** — no estaba en el árbol
  §8 original del plan como archivo separado (sí estaba anticipado en el texto). `ModulithStructureTests`
  marcó que `RegisteredProtectedResourceResponse` nunca había cruzado a otro módulo: solo
  `primaryport/request` tenía `@NamedInterface("dto")`. Corregido con el mismo patrón que `request`
  — omisión real de una historia anterior a HU-017 (nadie había necesitado consumir la respuesta
  desde fuera de `resources` hasta ahora), no una relajación para esta historia.
- **`OpaFixtureServer.java` y `AuthorizationHttpTests.java` (modificados, fuera de esta historia)** —
  al gatear `RegisterProtectedResourceUseCase`, la *fixture* de `AuthorizationHttpTests`
  (`@BeforeEach`, registra una aplicación y un recurso) empezó a fallar: esa clase deja el
  `OpaFixtureServer` en `DENY` por defecto a propósito, para sus propias pruebas de autorización de
  negocio (ADR-012) — y ese mismo `DENY` empezó a aplicar también a la decisión de administración
  que la *fixture* ahora necesita. Corregido agregando `respondWithForPath(path, status, body)` a
  `OpaFixtureServer` (respuesta por ruta exacta, sin tocar el default) y fijando la ruta de
  administración en `ALLOW` desde `AuthorizationHttpTests`, sin tocar el default `DENY` de la ruta de
  autorización — verificado que las 6 pruebas de esa clase, incluidas las que afirman `DENY`, siguen
  en verde. Aplicado con aprobación explícita de Sebastián en el mismo tramo.
- **`ResourceAdministrationController.java`** — igual que `RoleAdministrationController` en HU-016:
  el plan lo marcaba `[N]` con nota de creación diferida por riesgo de colisión de rutas; se creó
  real en la fase de `@2-tester-spec`, en el mismo paso que se retiró `register()` de
  `ProtectedResourceController` — exactamente como el plan anticipaba.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | Ver tabla detallada abajo |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Clases nuevas (`AdministerResourceRegistrationUseCase`, `ResourceAdministrationController`…) en inglés; Javadoc y los tres mensajes nuevos de `RequiredArgumentMessages` en español; métodos de prueba en inglés descriptivo |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` → SIN DERIVA, 16 excepciones preexistentes sin cambio. `PROJECT-MAP.md` regenerado (631 clases) |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | Ver detalle abajo |

**Detalle del juicio 1 — criterios de aceptación:**

| # Plan §2 | Criterio | Evidencia |
|---|---|---|
| 1 | Registrar recurso como admin → 201 | `AdministerResourceRegistrationUseCaseImplTests.registers_the_resource_when_the_principal_administers_the_application`; `ResourceAdministrationControllerTests.register_combines_the_path_variable_with_the_body_and_replies_with_201`; `AuthorizationHttpTests` (e2e — su propia fixture registra un recurso con éxito en las 6 pruebas de la clase) |
| 2 | Registrar recurso sin ser admin → 400 `NOT_AUTHORIZED_TO_ADMINISTER` | `AdministerResourceRegistrationUseCaseImplTests.never_registers_the_resource_when_the_principal_does_not_administer_the_application` |
| 3 | `RegisterApplicationWithInitialResourceUseCase` (HU-010) sin gate, cero regresión | Clase sin tocar (no aparece en el árbol de cambios); sus pruebas existentes (`RegisterApplicationWithInitialResourceUseCaseImplTests` y equivalentes HTTP) siguen en verde dentro de las 671 |
| 4 | `GET .../resources` sin cambios | `ProtectedResourceControllerTests.list_passes_the_path_variable_through_and_replies_with_200` |
| 5 | `verificar.ps1` en verde | ✅ 671/671, incluido `jacoco-check` |

**Detalle del juicio 4 — capa correcta:**

- `AdministerResourceRegistrationUseCaseImpl`: `mustBeAdministrator.execute(input.administration()).then(Mono.defer(...))` — sin `if/throw` de negocio. A diferencia de HU-016, sin `Optional`: el gate siempre se evalúa porque un recurso protegido siempre tiene `applicationId`. Ninguna `Rule` nueva hacía falta (mismo argumento que el plan §3).
- `ResourceAdministrationController`: solo delega al interactor y envuelve con `ApiResponse` — mismo patrón que todos los controllers del proyecto.
- `RegisterProtectedResourceRequestMapper`: la validación de formato vive en `RequestFieldParser` + los constructores de `ResourcePath`/`HttpVerb`/`ApplicationId`, no en el mapper. Sin catálogo de mensajes propio (a diferencia de HU-016, este mapper nunca lanzó uno).
- Sin anotaciones de Spring en `domain` ni `application` de ninguna pieza nueva.
- `allowedDependencies` de `authorization` se amplió una vez (`resources :: usecase`) — ya prevista en el plan §8 como consecuencia directa del diseño aprobado en el gate 1, no una relajación ad-hoc.

## Criterios de la línea base

> Los que el plan declaró: 1, 2, 9, 11, 12, 21, 22.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ 🤖 | `LayeredArchitectureTests` en verde; sin Spring en `domain`/`application` de las piezas nuevas |
| 2 | Contratos de servicios | ✅ | `AdministerResourceRegistrationUseCase`/`AdministerResourceRegistrationInteractor`: interfaces vacías extendiendo `ReactiveOperation` |
| 9 | Excepciones | ✅ | Reutiliza `NotAuthorizedToAdministerException` existente, sin excepción nueva |
| 11 | Interacción entre capas | ✅ | Controller → interactor → use case → validador — verificado en Juicio 4 |
| 12 | SOLID | ✅ | `AdministerResourceRegistrationUseCaseImpl` depende de interfaces (`PrincipalMustBeApplicationAdministratorValidator`, `RegisterProtectedResourceUseCase`) inyectadas por constructor |
| 21 | Modelo refinado | ✅ | `AdministerResourceRegistrationRequest`: record inmutable con `Objects.requireNonNull` en ambos componentes (verificado con el grep de records vacíos — ninguno) |
| 22 | Arquitectura reactiva | ✅ | `Mono` en toda la cadena nueva; sin `block()`; `Mono.defer(...)` para diferir el caso de uso real tras el gate |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `resources/application/primaryport/response/package-info.java` | Anticipado en el texto, no en el árbol como archivo propio | Creado | Sí — omisión real encontrada al compilar, no una relajación |
| `OpaFixtureServer.java`, `AuthorizationHttpTests.java` | No declarados | `OpaFixtureServer` gana `respondWithForPath`; `AuthorizationHttpTests` fija ALLOW solo en la ruta de administración | Sí — consecuencia directa de gatear un endpoint que la fixture de esa clase usaba sin depender de OPA hasta ahora; aprobado explícitamente por Sebastián |
| `ResourceAdministrationController.java` | `[N]`, con nota de creación diferida | Creado en la fase de tester, no de planificación | Sí — el propio plan anticipaba esta secuencia por el riesgo de colisión de rutas |

## Datos para la entrega

- **Mensaje de commit:** `feat(resources): gatea RegisterProtectedResourceUseCase por administración de aplicación (HU-017)`
- **Cuerpo:** Mueve el registro de recursos protegidos a `authorization`, gateado por
  `PrincipalMustBeApplicationAdministratorValidator` (HU-009/ADR-023) — sin `Optional`, a diferencia
  de HU-016: un recurso siempre pertenece a una aplicación, así que el gate siempre se evalúa.
  `RegisterApplicationWithInitialResourceUseCase` (HU-010) sigue sin gatearse. `ProtectedResourceController`
  conserva solo la consulta; `ResourceAdministrationController` (nuevo, en `authorization`) expone la
  escritura en la misma ruta de siempre. Incluye una corrección ajena (Modulith
  `resources :: dto` sobre `primaryport/response`, nunca publicado) y una mejora a
  `OpaFixtureServer` (respuesta por ruta) para que `AuthorizationHttpTests` pudiera seguir probando
  DENY de negocio mientras su fixture necesita ALLOW de administración.
- **Rama:** `feature/HU-017-gatear-registro-recursos`
- **Archivos a incluir:** todo `pdp/src/main` y `pdp/src/test` tocado en este tramo (ver `git status`
  — 14 archivos de producción/config + 6 de prueba). El plan y este reporte se versionan aparte, en
  `pdp/docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega).

> **Gate 2 — antes de que esto salga del repositorio.** El reporte está aprobado. Confirma para
> proceder con commit y push.
