# REPORTE — HU-031 (el PEP propaga la indisponibilidad del canal BFF) — 2026-10-28

**Veredicto: APROBADO**, con una falla **heredada** de la suite del PEP (`PepArchitectureTests`) que no es de esta historia
y queda abierta como [HU-032](../HU-032.md).

## Cómo se validó

Flujo del harness sobre el módulo `pep/`: `@1-planificador` → compuerta 1 (recorte del responsable: todo 401 del PDP sigue
siendo 401) → `@2-tester-spec` (21 pruebas nuevas, en rojo por aserción de comportamiento: hoy todo error era 401) →
`@3-implementador` (2 archivos de producción) → `mvnw -f pep/pom.xml test` ejecutado desde el Terminal del usuario y lectura
del diff por el responsable.

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | PDP `401` → PEP `401 TOKEN_INVALID`; PDP `503` → PEP `503 BFF_SESSION_UNAVAILABLE` con `Retry-After: 5` | ✅ | `BffSessionTokenResolverTests`, `ProblemWriterTests`, `BffSessionChannelIntegrationTests` |
| 2 | Fallo de red, timeout, otros 5xx, 4xx distintos de 401, 200 vacío/sin `accessToken`/malformado → `503` | ✅ | `BffSessionTokenResolverTests` (20 casos) |
| 3 | Los 4xx distintos de 401 dejan un WARN con el status del PDP, sin token ni cookie | ✅ | prueba de log del resolver |
| 4 | Ninguna petición llega a la aplicación protegida sin token válido | ✅ | integración: 0 peticiones a la app con PDP 503 y con PDP 401 |
| 5 | `Retry-After` solo en `BFF_SESSION_UNAVAILABLE`; `PDP_UNAVAILABLE` no lo lleva; el 429 conserva el suyo | ✅ | `ProblemWriterTests` |
| 6 | Suite de `pep/` | 🟡 | **71 pruebas, 0 fallos, 1 error: `PepArchitectureTests`** (heredado, ver abajo) |

## Falla heredada (no de esta historia)

`PepArchitectureTests` falla **igual sin los cambios de HU-031** (comprobado con `git stash push -- pep/src/main`). Violaciones de
Modulith: `enforcement`, `normalization` e `ingress` dependen de tipos no expuestos de `pep.application.contract`
(`ReactiveOperation`, `OperationWithoutResult`, `ReactiveOperationWithoutResult`); `ingress` depende de tipos no expuestos de
`enforcement` (`EnforceAccessUseCase`, `EnforceAccessRequest`); `RegisterIntegrationInteractor` extiende `ReactiveOperation`.
Arreglarlo es declarar fronteras (CLAUDE.md exige ADR), por eso no se relajó nada: ver HU-032.

## Hallazgo

La cadena del proxy HTTP del PEP exige un `JwtAuthenticationToken`: la rama de cookie BFF de `HttpCaptureInteractor` no es alcanzable
por las rutas proxy; solo lo es `POST /internal/v1/embedded-access-decisions` (starter). Esta historia cubre ese camino. Aceptar
cookie BFF también como proxy es una decisión de producto/seguridad del PEP, anotada en `HU-031.md` y sin HU hasta que haya caso de uso.

## Cambio de contrato público (acotado)

Una petición con cookie BFF que antes recibía `401` ante cualquier fallo del canal al PDP ahora puede recibir `503` con `Retry-After`.
El sobre y el `401/403` existentes no cambian; el camino con `Authorization: Bearer` tampoco.
