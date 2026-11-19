# Reporte de validación — HU-002

> Segunda pasada. La primera (2026-09-06, más temprano el mismo día) fue **RECHAZADA** por un
> bloqueante; se corrigió y esta pasada lo revalida desde cero, de forma independiente.

## Metadata

- **Slice:** `nuevo: authorization` (más una extensión publicada en `resources`)
- **Fecha:** 2026-09-06
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-002.md`
- **Rama:** `feature/hu-002-endpoint-decision`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `clean verify`), de forma independiente.

```
ESTADO: VERDE  (mvnw clean verify, 43,9s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4
PRUEBAS: Tests run: 277, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobación                                   | Resultado                                                  |
|------------------------------------------------|------------------------------------------------------------|
| Compilación                                    | ✅                                                          |
| Pruebas                                        | ✅ 277 pruebas, 0 fallos                                    |
| Cobertura (≥ 50 % por paquete, `jacoco-check`) | ✅ — corrida con `clean`                                    |
| `LayeredArchitectureTests`                     | ✅                                                          |
| `ModulithStructureTests`                       | ✅                                                          |
| `consistencia.ps1`                             | ✅ CONSISTENTE — 6 slices                                   |
| `drift.ps1`                                    | ✅ SIN DERIVA — 12 excepciones preexistentes, ninguna nueva |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno. El único de la pasada anterior —`AccessRequest`/`AccessDecision` sin validar sus
componentes— se corrigió. Verificado con evidencia, no de memoria: abrí los cinco records que
podían tener el mismo hueco (`AccessRequest`, `AccessDecision`, `PolicyReference`,
`ProtectedResourceExistence`, `ProtectedResourceLookup`) y los cinco validan cada componente no
primitivo con `Objects.requireNonNull` y su constante en `RequiredArgumentMessages`.
`AccessDecision.policyReferences` además copia con `List.copyOf(...)`, como pedía la corrección.

Hice un barrido propio (no solo confié en el reporte del implementador) sobre todo `record` nuevo
de `authorization` y de la extensión en `resources`: no quedó ninguno sin cubrir.

## Observaciones menores

- **`WebContractMessages.conflictingPagingModes()` sigue en inglés** (`"Use either page/size or
  offset/limit, not both"`), con su texto fijado por `WebContractMessagesTests`. Es deriva
  **preexistente de HU-001**, no introducida ni tocada por esta historia — no bloquea HU-002, pero
  queda como pendiente de una limpieza futura si alguien retoma ese archivo.
- Los tres mensajes que sí eran de esta historia (`successApplicationRegistered`,
  `successCatalogQueried`, `successAccessEvaluated`) ya están en español, verificados contra la
  aserción que los fija en `WebContractMessagesTests` — coinciden carácter por carácter.

## Los cuatro juicios

| # | Juicio                                        | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                  |
|---|-----------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan? | ✅         | Sin cambios respecto a la primera pasada — la corrección no tocó ningún camino funcional, solo añadió validación. Los 9 criterios de la sección 2 siguen con su prueba concreta (ver detalle en el historial de este reporte / commit de la primera pasada)                                                                                |
| 2 | ¿Convención de idioma?                        | ✅         | Los tres mensajes de esta historia en español, verificados byte a byte contra la prueba que los fija. Identificadores en inglés, Javadoc en español, sin cambios respecto a la primera pasada                                                                                                                                              |
| 3 | ¿Introdujo deriva doc↔código?                 | ✅         | `drift.ps1` en verde, 0 hallazgos nuevos                                                                                                                                                                                                                                                                                                   |
| 4 | ¿La lógica quedó en la capa correcta?         | ✅         | La corrección es puramente validación en constructores compactos — no movió lógica de capa, no añadió `if` en ningún use case, no tocó el flujo reactivo. Los DTOs crudos/web mantienen correctamente **cero** validación (es responsabilidad del mapper), confirmado contra el precedente (`CreateTenantRawRequest`, `TenantWebResponse`) |

## Criterios de la línea base

| #  | Criterio                    | Resultado | Punto de control comprobado                                                                                                                                                                                                                                                                   |
|----|-----------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture          | 🤖 ✅      | `LayeredArchitectureTests` + `ModulithStructureTests` verdes                                                                                                                                                                                                                                  |
| 2  | Contratos de servicios      | ✅         | Sin cambios — ver primera pasada                                                                                                                                                                                                                                                              |
| 3  | Reglas e integridad         | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 4  | Capacidades transversales   | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 5  | Manejo de mensajes          | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 6  | Manejo de parámetros        | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 7  | Adaptadores de persistencia | N/A       | Sin adaptador propio en esta historia                                                                                                                                                                                                                                                         |
| 9  | Excepciones                 | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 11 | Interacción entre capas     | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 12 | SOLID                       | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 13 | DTOs                        | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 14 | DTOs seguros                | ✅         | **Corregido.** Las tres barreras están completas ahora: campo presente (`RequestFieldParser` en el mapper), VO válido (`ApplicationId::of`/`ResourcePath::new`/`HttpVerb::parse`), y `requireNonNull` en el record — verificado abriendo los cinco archivos, no releyendo el reporte anterior |
| 20 | Adaptadores limpios         | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 21 | Modelo refinado             | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |
| 22 | Arquitectura reactiva       | ✅         | Sin cambios                                                                                                                                                                                                                                                                                   |

## Desviaciones respecto al plan

Las cinco ya registradas en la primera pasada (correlationId vía Contexto de Reactor, constructor
de `AuthorizeUseCaseImpl` y de `DenyByDefaultPolicyDecisionAdapter` ampliados con
`IdentifierGenerator`/`TimeProvider`, y las dos interfaces nombradas de Modulith descubiertas al
compilar) — todas ya aprobadas y sin cambios en esta pasada.

| Archivo                                                 | Plan decía                                                                                               | Código hace                                                               | ¿Justificado?                                                                                                |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `AccessRequest`, `AccessDecision`, `PolicyReference`    | Constructor compacto vacío (correcto para un esqueleto del planificador)                                 | El implementador añadió `Objects.requireNonNull` en la fase de corrección | ✅ Sí — es exactamente lo que el esqueleto esperaba que pasara; el bloqueante era que no había pasado todavía |
| `ProtectedResourceExistence`, `ProtectedResourceLookup` | No declarados como pendientes de corrección (el reporte anterior solo señaló los dos de `authorization`) | También corregidos, por consistencia con el mismo patrón                  | ✅ Sí — mismo criterio 14, mismo hueco, no señalarlo habría sido inconsistente con el propio hallazgo         |

## Datos para la entrega

- **Mensaje de commit:** `feat(authorization): endpoint de decisión con contrato completo y denegación por defecto`
- **Cuerpo:** nuevo slice `authorization` con `POST /api/v1/authorize`. `AccessRequest`/`AccessDecision`
  como *published language* (tri-estado, INV-POL-04), con las tres barreras de DTO seguro completas.
  `DenyByDefaultPolicyDecisionAdapter` deniega por defecto (ADR-012) a través de `PolicyDecisionPort`,
  listo para que HU-004 lo sustituya por OPA sin tocar el contrato. Extiende `resources` con
  `ProtectedResourceMustExistRule`/`Validator` publicados. 277 pruebas totales, 27 nuevas de esta
  historia.
- **Rama:** `feature/hu-002-endpoint-decision`
- **Archivos a incluir:** todo `src/main` y `src/test` tocado por esta historia. El plan y los dos
  reportes se versionan aparte, en `docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega).
