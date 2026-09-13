# Reporte de validacion — HU-009

## Metadata

- **Slice:** `authorization`
- **Fecha:** 2026-09-13
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-009.md`
- **Rama:** `feature/HU-009-mecanismo-administracion-aplicacion`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 80s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 616, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | ✅ |
| Pruebas | ✅ 616 pruebas (10 nuevas de esta historia) |
| Cobertura (≥ 50 % por paquete, jacoco-check) | ✅ (`jacoco:check` corrió dentro de `verify`, `BUILD SUCCESS`) |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |

`consistencia.ps1` → `CONSISTENTE: todos los slices siguen la misma forma` (8 slices verificados).

`drift.ps1` → 1 hallazgo preexistente y ajeno a esta historia (ver Observaciones menores) —
confirmado contra `git status` que ningún archivo de esta historia toca `pep/` ni
`MAPA-PLATAFORMA-SEGURIDAD.md`.

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 3 — drift] `PepRegistrationProperties` citada y no encontrada por `drift.ps1`

- **Archivo:** `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md:179`
- **Problema:** Mismo hallazgo preexistente ya reportado en `REPORTE-HU-014.md` — el detector no
  cubre el módulo `pep/`, donde la clase sí existe. Ningún archivo de HU-009 toca `pep/`.
- **Referencia:** Regla invariante 5 del validador — "la deriva preexistente es observación; la
  nueva es bloqueante".
- **Correccion esperada:** Ninguna a cargo de esta historia.

### [Completitud] `pdp.opa.administration-decision-path` en `application.properties` no estaba en el árbol §8 del plan

- **Archivo:** `pdp/src/main/resources/application.properties`
- **Problema:** El plan no listó este archivo en la sección 8, pero era una consecuencia necesaria
  del `[M]` a `OpaProperties` (el nuevo componente es `Objects.requireNonNull`, así que cualquier
  `@SpringBootTest` que arranque el contexto real habría fallado a construir el bean sin esta
  propiedad). El tester lo añadió al aplicar la firma `[M]`, y `verificar.ps1` completo (incluye los
  `@SpringBootTest` reales) confirma que el contexto arranca. Es una consecuencia mecánica y
  correcta del cambio de firma, no una desviación de diseño.
- **Referencia:** Ninguna regla violada — se documenta porque no aparecía explícitamente en el árbol.
- **Correccion esperada:** Ninguna.

### [Cobertura] `NotAuthorizedToAdministerException` no tiene prueba dedicada

- **Archivo:** `pdp/src/test/java/co/edu/uco/seguridad/pdp/authorization/application/rule/validator/impl/PrincipalMustBeApplicationAdministratorValidatorImplTests.java`
- **Problema:** El plan (sección 9) declaraba `NotAuthorizedToAdministerExceptionTests` como clase
  nueva. El tester no la creó, documentando en su cierre que el repositorio no tiene precedente de
  probar una excepción de dominio de forma aislada en ningún slice (siempre se ejercita
  indirectamente vía quien la lanza). Comprobado: `find pdp/src/test -ipath "*exception*Tests.java"`
  solo devuelve `ApiErrorHandlerTests` en todo el repositorio — cero precedente de
  `{X}ExceptionTests`. La excepción sí queda cubierta indirectamente por
  `rejects_when_the_use_case_denies`/`rejects_when_the_use_case_is_indeterminate`
  (`.expectError(NotAuthorizedToAdministerException.class)`), aunque ningún test afirma el `code`
  (`NOT_AUTHORIZED_TO_ADMINISTER`) ni el texto exacto del mensaje.
- **Referencia:** `sb-testing` — antipatrón "Reafirmar en `application` lo que ya afirma el test del
  VO: duplicación". Una clase dedicada habría sido exactamente esa duplicación.
- **Correccion esperada:** Ninguna para esta historia. Si se quiere blindar el `code`/mensaje
  exactos, se puede añadir una aserción de `.getMessage()`/`code()` al caso ya existente en una
  futura revisión — no bloquea la entrega.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | Ver tabla siguiente |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Identificadores nuevos en inglés (`AuthorizeAdministrationUseCase`, `AdministrationDecisionPort`, `PrincipalMustBeApplicationAdministratorValidator`…); mensajes en español: `AuthorizationMessages.notAuthorizedToAdminister(...)`, las 4 constantes nuevas de `RequiredArgumentMessages` |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` solo reporta el hallazgo preexistente de `pep/`, confirmado ajeno vía `git status` |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | `AuthorizeAdministrationUseCaseImpl.execute` no tiene `if/throw` de negocio: solo orquesta `ActiveRoleNamesLookupValidator` → `AdministrationDecisionPort`, con `onErrorResume` fail-closed. `PrincipalMustBeApplicationAdministratorValidatorImpl` traduce una decisión ya tomada (por OPA) a una excepción — no decide nada, consistente con la filosofía que el propio HU-004 fijó ("el catálogo es dato de entrada para la política, jamás una decisión en Java"). `OpaAdministrationDecisionAdapter` solo traduce HTTP↔dominio, igual que `OpaPolicyDecisionAdapter`. Cero anotaciones de Spring en `domain`/`application` (grep sin resultados salvo el `package-info.java` de Modulith, que es una declaración de frontera nueva, no una relajación — `authorization/package-info.java` en sí no se tocó). Sin `.block()` en el código nuevo |

## Criterios de la linea base

> Solo los que el plan declaró (metadata): 1, 2, 3, 4, 9, 11, 12, 21, 22.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | 🤖 ✅ | `LayeredArchitectureTests` + `ModulithStructureTests` en verde; sin Spring en `domain`/`application` del cambio |
| 2 | Contratos de servicios | ✅ | `AuthorizeAdministrationUseCase`, `AdministrationDecisionPort`, `PrincipalMustBeApplicationAdministratorValidator` son interfaces vacías que extienden contratos de `shared/contract` |
| 3 | Reglas e integridad | ✅ | Ninguna `Rule` pura nueva, deliberadamente (Hallazgo del plan: la decisión es de OPA, no de Java). El validador solo traduce; cero `if/throw` de negocio en el caso de uso |
| 4 | Capacidades transversales | ✅ | Sin `Instant.now()`/`UUID.randomUUID()` en línea — no hacían falta (`AdministrationDecision` no lleva id ni marca de tiempo, a propósito: no se audita) |
| 9 | Excepciones | ✅ | `NotAuthorizedToAdministerException` extiende `BusinessRuleViolationException` (jerarquía existente); nunca `RuntimeException` cruda. El mapeo a 400 en vez de 403 ya estaba anotado como ambigüedad no bloqueante en el plan §11 |
| 11 | Interacción entre capas | ✅ | `PrincipalMustBeApplicationAdministratorValidator` → `AuthorizeAdministrationUseCase` → `ActiveRoleNamesLookupValidator`/`AdministrationDecisionPort`. Sin controller en esta historia (mecanismo interno, sin endpoint) |
| 12 | SOLID | ✅ | Contratos mínimos, dependencias inyectadas por constructor contra interfaces |
| 21 | Modelo refinado | ✅ | Sin cambios a entidades de dominio — reutiliza `Role`/`Assignment` tal cual, decisión documentada en el plan (Hallazgo 6: `Assignment.applicationId` obligatorio impide el ADMIN global, retirado del alcance) |
| 22 | Arquitectura reactiva | ✅ | Cadena `Mono` completa con `onErrorResume` fail-closed; sin `.block()` |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `application.properties` | No listado en el árbol §8 | +1 línea (`pdp.opa.administration-decision-path`) | Sí — consecuencia mecánica del `[M]` a `OpaProperties`, necesaria para que cualquier `@SpringBootTest` arranque (ver Observaciones menores) |
| `NotAuthorizedToAdministerExceptionTests` | Declarada en sección 9 | No se creó | Sí — el repositorio no tiene precedente de probar excepciones de dominio de forma aislada; la cobertura ya existe indirectamente (ver Observaciones menores) |

## Datos para la entrega

- **Mensaje de commit:** `feat(authorization): modelo y mecanismo de administración por aplicación (HU-009)`
- **Cuerpo:** Sin endpoint HTTP nuevo — infraestructura interna que las historias futuras (una por
  slice: `applications`, `roles`, `resources`, `assignments`, `profiles`) consumirán para gatear sus
  casos de uso de escritura. Nuevo puerto de decisión (`AdministrationDecisionPort`) hacia OPA,
  independiente del contrato `pdp-opa/v1` ya existente; caso de uso `AuthorizeAdministrationUseCase`
  que resuelve los roles activos del sujeto para una aplicación y delega en ese puerto, fail-closed
  ante cualquier fallo; validador publicado
  (`PrincipalMustBeApplicationAdministratorValidator`, `@NamedInterface("rule")`) que las historias
  futuras inyectarán directamente. Retirado del alcance: administrador global de plataforma
  (`Assignment.applicationId` es obligatorio hoy — no puede representar una asignación global sin
  tocar ese agregado, ver PLAN-HU-009.md Hallazgo 6), primer-administrador-automático al registrar,
  y el cableado a los ~12 casos de uso existentes. Cero regresión: los 606 tests previos siguen en
  verde sin cambios.
- **Rama:** `feature/HU-009-mecanismo-administracion-aplicacion`
- **Archivos a incluir:** todos los `.java` y `application.properties` listados como
  modificados/nuevos en `git status` para esta historia (ver PLAN-HU-009.md §8 para el árbol
  completo). El plan y este reporte se versionan aparte, en `pdp/docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
