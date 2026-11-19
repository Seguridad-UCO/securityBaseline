# Reporte de validacion — HU-010

## Metadata

- **Slice:** `resources`
- **Fecha:** 2026-09-13
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-010.md`
- **Rama:** `feature/HU-010-saga-registro-aplicacion-recurso-inicial`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 83,3s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 631, Failures: 0, Errors: 0, Skipped: 0
```

> Segunda corrida, tras corregir el bloqueante de documentación (ver abajo). Mismo resultado que la
> primera — el bloqueante era de documentación, no de código, así que el build no cambió.

| Comprobacion                                 | Resultado                                                                                                               |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Compilacion                                  | ✅                                                                                                                       |
| Pruebas                                      | ✅ 631 pruebas (15 nuevas de esta historia)                                                                              |
| Cobertura (≥ 50 % por paquete, jacoco-check) | ✅ (`jacoco:check` corrió dentro de `verify`, `BUILD SUCCESS`)                                                           |
| `LayeredArchitectureTests`                   | ✅                                                                                                                       |
| `ModulithStructureTests`                     | ✅ (confirma las dos fronteras nuevas: `applications :: model`, `applications :: usecase`, consumidas desde `resources`) |

`consistencia.ps1` → `CONSISTENTE: todos los slices siguen la misma forma` (8 slices verificados).

`drift.ps1` → 1 hallazgo, preexistente y ajeno a esta historia (ver Observaciones menores). El
hallazgo introducido por la primera corrida (ver Bloqueantes — resuelto) ya no aparece.

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

Segunda validación. La primera (ver el bloqueante resuelto abajo) encontró un solo problema, de
documentación — el código, las pruebas, la arquitectura y los cuatro juicios ya estaban limpios
desde la primera corrida. Corregido, no queda nada pendiente.

## Bloqueantes

Ninguno. El siguiente, encontrado en la primera validación, ya se corrigió:

### [RESUELTO — Juicio 3, drift] La línea base afirmaba que el criterio 10 no se cumple

- **Archivo:** `pdp/docs/criteria-compliance-matrix.md:36` y `:51-61`; `CLAUDE.md:142-146`
- **Problema:** Esta historia es, literalmente, "cablear la saga de compensación" — el trabajo que
  la matriz decía que faltaba para cerrar el criterio 10. Verificado contra el código:
  `RegisterApplicationWithInitialResourceUseCaseImpl.compensate(...)` ahora sí invoca
  `RemoveApplicationUseCase.execute(applicationId)` cuando el registro del recurso falla (con log
  si la propia compensación también falla, sin silencio) — el punto de control del criterio 10
  (`sb-criterios`: "Saga con compensación explícita por paso... si un paso falla, el anterior se
  compensa a mano") **ya se cumple**. Pero la matriz sigue diciendo `**No cumple**` en la fila 10 y
  `Estado final: 22 de 23` en el resumen, y `CLAUDE.md` sigue diciendo "22 de los 23 criterios se
  cumplen... Queda abierto solo el criterio 10". Ambos documentos ahora afirman algo que el código
  ya contradice — exactamente el patrón que este mismo archivo lleva corregido dos veces (agosto
  2026 y 2026-08-31, según su propio historial).
- **Referencia:** Regla invariante 5 del validador — "la deriva preexistente es observación; la
  nueva es bloqueante". Esta deriva no existía antes de esta historia: la introdujo el propio
  cambio, al volver verdadero algo que la documentación seguía llamando falso. `drift.ps1` no lo
  detecta porque valida que las clases citadas existan, no que las afirmaciones de estado sigan
  siendo ciertas — el Juicio 3 del validador existe precisamente para lo que `drift.ps1` no cubre.
- **Corrección esperada:**
    1. `pdp/docs/criteria-compliance-matrix.md`, fila 10: cambiar `**No cumple** — ningún caso de uso
     invoca la compensación: no hay saga cableada` por algo como `**Cumple** — HU-010 cablea
     RegisterApplicationWithInitialResourceUseCaseImpl, que invoca RemoveApplicationUseCase como
     compensación explícita cuando el registro del recurso falla`.
    2. La misma tabla, sección "Resumen": `Cumple: 22` → `23`, `No cumple: 1 — el 10` se retira, y el
       párrafo `Estado final: 22 de 23...` se actualiza para reflejar 23/23 y cuándo se cerró (HU-010).
    3. `CLAUDE.md:142-146`: el párrafo "**22 de los 23 criterios se cumplen**... Queda abierto **solo
       el criterio 10**" deja de ser cierto — actualizarlo a 23/23 y quitar la mención de que sigue
       abierto.
    4. Revisar si alguna excepción de `pdp/docs/ai-harness/drift-ignore.txt` referenciaba este estado
       (no se encontró ninguna al buscar `criterio`/`RemoveApplicationUseCase`, así que probablemente
       no hay que tocar ese archivo).

No era un bloqueante de código. **Corregido:** `criteria-compliance-matrix.md` (fila 10, resumen y
la nota de revisión 2026-08-31), `CLAUDE.md`, `baseline-criteria-overview.md` (callout y tabla) y
`domain-and-data/10-transactions.md` (que además describía una implementación que nunca existió en
el código actual — `RegisterApplicationUseCaseImpl` orquestando dos pasos y una prueba,
`rolls_back_the_saved_resource_and_removes_the_application_when_event_publication_fails`, que
jamás existió; se encontró al corregir y se aprovechó el mismo cambio para dejarlo correcto, ver
`pdp/docs/architecture/pdp-modulith-alignment.md` también actualizado). `drift.ps1` vuelto a correr
tras la corrección: solo el hallazgo preexistente de `pep/`.

## Observaciones menores

### [Juicio 3 — drift] `PepRegistrationProperties` citada y no encontrada por `drift.ps1`

- **Archivo:** `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md:179`
- **Problema:** Mismo hallazgo preexistente ya reportado en `REPORTE-HU-014.md` y `REPORTE-HU-009.md`
  — el detector no cubre el módulo `pep/`, donde la clase sí existe. Ningún archivo de HU-010 toca
  `pep/` (confirmado contra `git status`).
- **Referencia:** Regla invariante 5 del validador.
- **Corrección esperada:** Ninguna a cargo de esta historia.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|---|-----------------------------------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | Ver tabla de criterios más abajo — las 5 filas de la sección 2 del plan tienen prueba o evidencia de build que las respalda                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Identificadores nuevos en inglés (`RegisterApplicationWithInitialResourceUseCase`, `compensate`…); mensajes en español: `WebContractMessages.successApplicationRegisteredWithInitialResource()`, las 6 constantes nuevas de `RequiredArgumentMessages`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | Corregido en la segunda corrida — `criteria-compliance-matrix.md`, `CLAUDE.md`, `baseline-criteria-overview.md` y `domain-and-data/10-transactions.md` ya reflejan que el criterio 10 se cumple. `drift.ps1` solo reporta el hallazgo preexistente de `pep/`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | `RegisterApplicationWithInitialResourceUseCaseImpl` no tiene `if/throw` de negocio: orquesta dos casos de uso ya validados por sus propias reglas y compensa explícitamente — no decide nada nuevo. El controller solo importa DTOs/interactor de su propia capa de infraestructura, nunca `application` directamente. El mapper delega el formato a los value objects vía `RequestFieldParser`. Cero anotaciones de Spring en `domain`/`application` del cambio (grep sin resultados salvo `package-info.java`, que son declaraciones de frontera Modulith, no lógica). Sin `.block()`. Ninguna frontera se relajó para que compilara — las dos ampliaciones (`applications :: usecase`, `applications :: model`) son permisos nuevos y deliberados, verificados por `ModulithStructureTests` en verde |

## Criterios de la linea base

> Solo los que el plan declaró (metadata): 1, 2, 9, 10, 11, 12, 13, 14, 21, 22.

| #  | Criterio                | Resultado                                                                                       | Punto de control comprobado                                                                                                                                                                                                                                                                                                                                                                                                                      |
|----|-------------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture      | 🤖 ✅                                                                                            | `LayeredArchitectureTests` + `ModulithStructureTests` en verde; sin Spring en `domain`/`application` del cambio                                                                                                                                                                                                                                                                                                                                  |
| 2  | Contratos de servicios  | ✅                                                                                               | `RegisterApplicationWithInitialResourceUseCase`/`Interactor` son interfaces vacías que extienden contratos de `shared/contract`                                                                                                                                                                                                                                                                                                                  |
| 9  | Excepciones             | ✅                                                                                               | Ninguna excepción nueva — reutiliza las de `RegisterApplicationRulesValidator`/`RegisterProtectedResourceRulesValidator` sin cambiar su mapeo HTTP (verificado: los tests E2E preexistentes de esos dos endpoints siguen en verde, sin tocar)                                                                                                                                                                                                    |
| 10 | Transacciones           | ✅ **(cierra el único criterio abierto de la línea base — 23/23, documentación ya actualizada)** | `RegisterApplicationWithInitialResourceUseCaseImpl.compensate(...)` invoca `RemoveApplicationUseCase.execute(applicationId)` explícitamente cuando el paso siguiente falla; sin `TransactionPort` genérico; compensación que también falla se registra por log sin cambiar el error que llega al cliente (`RegisterApplicationWithInitialResourceUseCaseImplTests.still_reports_the_original_resource_error_when_the_compensation_itself_fails`) |
| 11 | Interacción entre capas | ✅                                                                                               | `ApplicationWithInitialResourceController` → `RegisterApplicationWithInitialResourceInteractorImpl` → `RegisterApplicationWithInitialResourceUseCaseImpl` → `RegisterApplicationUseCase`/`RegisterProtectedResourceUseCase`/`RemoveApplicationUseCase`. El controller no importa `application`                                                                                                                                                   |
| 12 | SOLID                   | ✅                                                                                               | Contratos mínimos, dependencias inyectadas por constructor contra interfaces (3 colaboradores)                                                                                                                                                                                                                                                                                                                                                   |
| 13 | DTOs                    | ✅                                                                                               | `RegisterApplicationWithInitialResourceRawRequest` (Strings) → mapper → `RegisterApplicationWithInitialResourceRequest` (value objects)                                                                                                                                                                                                                                                                                                          |
| 14 | DTOs seguros            | ✅                                                                                               | Sin Jakarta Validation; tres barreras (campo presente, VO válido, `requireNonNull` en el record — verificado que el implementador completó los dos records `[N]` que el planificador dejó con constructor compacto vacío); respuesta web plana (once campos primitivos, sin anidar el DTO web de `applications` — ver Hallazgo del plan)                                                                                                         |
| 21 | Modelo refinado         | ✅                                                                                               | Sin entidades nuevas — reutiliza `Application`/`ProtectedResource` tal cual, decisión ya documentada en el plan                                                                                                                                                                                                                                                                                                                                  |
| 22 | Arquitectura reactiva   | ✅                                                                                               | Cadena `Mono` completa con `onErrorResume` anidado (compensación, y compensación-que-falla) fail-visible en vez de fail-silent; sin `.block()`                                                                                                                                                                                                                                                                                                   |

## Desviaciones respecto al plan

| Archivo                                                     | Plan decía                                                 | Código hace                                                                                                                                              | ¿Justificado?                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------------------------------------------------|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RegisterApplicationWithInitialResourceInteractorImpl.java` | `[N]`, esqueleto que lanza hasta la fase de implementación | Ya tenía su lógica completa desde la fase de planificación (desvío del protocolo del planificador, ya señalado por el propio implementador en su cierre) | Parcialmente — la lógica es correcta y coincide exactamente con el patrón de `RegisterApplicationInteractorImpl`/`RegisterProtectedResourceInteractorImpl` (verificado línea por línea), así que no hay riesgo de comportamiento; pero el proceso no se siguió como debía. No bloquea esta validación porque el resultado es correcto, se deja registrado para que el planificador lo evite en la próxima historia |

## Datos para la entrega

- **Mensaje de commit:** `feat(resources): saga de registro de aplicacion con recurso inicial (HU-010)`
- **Cuerpo:** Nuevo endpoint `POST /api/v1/applications/with-initial-resource` (201): registra una
  aplicación y su recurso inicial en una sola operación, con compensación explícita
  (`RemoveApplicationUseCase`) si el segundo paso falla — cierra el criterio 10 de la línea base
  (23/23). Vive en `resources` (no en `applications`): es el módulo que ya tenía permiso de mirar
  al otro. Dos fronteras Modulith nuevas y deliberadas: `applications :: usecase`,
  `applications :: model`. `POST /api/v1/applications` y `POST /api/v1/applications/{id}/resources`
  quedan intactos. Incluye la corrección de la documentación de la línea base
  (`criteria-compliance-matrix.md`, `CLAUDE.md`, `baseline-criteria-overview.md`,
  `domain-and-data/10-transactions.md`, `architecture/pdp-modulith-alignment.md`) para que deje de
  afirmar que el criterio 10 sigue abierto.
- **Rama:** `feature/HU-010-saga-registro-aplicacion-recurso-inicial`
- **Archivos a incluir:** los de `git status` para esta historia (código, pruebas y la
  documentación corregida) — el plan y este reporte se versionan aparte.

## Proximos pasos

Listo para el gate 2 (entrega).
