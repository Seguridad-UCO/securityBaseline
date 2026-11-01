# REPORTE — HU-034 (observabilidad del WAF en Loki y Grafana) — 2026-10-30

**Veredicto: APROBADO, con una verificación pendiente (imagen del edge).**

## Cómo se validó

Flujo del harness: `@1-planificador` (verificación empírica de los hechos, ver [PLAN-HU-034.md](../PLAN-HU-034.md)) → compuerta 1 (el responsable
aceptó las siete propuestas) → `@2-tester-spec` (prueba de punta a punta) → `@3-implementador` → validación: la prueba
`observability/tests/Test-WafObservability.ps1` sobre una pila efímera con el edge real, el colector de producción, Loki 3.7.7 y
Grafana 13.2.1: **44 aserciones, 44 PASS, 0 FAIL (165 s)**. No se descargó nada; la pila se derribó con `down -v` solo en su proyecto.

## Qué estaba roto y cómo quedó

| Defecto | Resultado |
|---|---|
| La regla 85 del edge no entregaba nada en `logdata` (la cabeza de la cadena lleva el mensaje; la puntuación usada no existía) | Reescrita: cada transacción con puntuación > 0 deja `anomaly_score`, ruta y `correlationId` |
| El Collector borraba reglas y ruta, buscaba el `correlationId` en cabeceras que el audit no guarda, y el timestamp no casaba | Pipeline propio y fail-closed: lista blanca de 9 campos, timestamp epoch, línea que no parsea se descarta |
| El panel de Grafana buscaba un texto que ninguna regla escribe; "All" daba HTTP 400 en el panel 12 | Panel 15 reescrito, paneles 16 (por regla principal) y 17 (por ruta), `allValue` corregido |
| El audit log crecía sin tope (~95 MB/día) | Rotación copytruncate por tamaño (`EDGE_AUDIT_*`) y sonda del motor espaciada (`EDGE_DETECT_PROBE_INTERVAL`, 600 s por defecto) |

## Criterios

| # | Criterio | Resultado |
|---|---|---|
| 1 | Tras un ataque, LogQL por `correlationId` devuelve regla, puntuación y ruta (detection y blocking) | ✅ D-2…D-7, B-2, B-4 |
| 2 | El mismo `correlationId` está en los logs del PDP si la petición pasó; un bloqueo no llega al PDP | ✅ D-12, D-12b, B-3 |
| 3 | El panel muestra bloqueos por regla y por ruta con datos reales | ✅ G-3…G-7 |
| 4 | El audit log tiene tope o rotación documentada | ✅ R-2…R-8 (sin pérdida ni duplicados: R-6) |
| — | Privacidad: sin query, IP, `Matched Data` ni secretos en Loki; solo dos etiquetas indexadas | ✅ D-8…D-11, D-14, D-15 |

## Desviaciones y decisiones a conocer

- **Sin `edge.status`:** el audit no tiene estado HTTP (reintroducirlo exigiría la parte F, prohibida por `00-engine.conf`). `edge.interrupted=true` equivale a 403.
- **`edge.rule_main`** (campo añadido): la primera regla que no sea de evaluación; permite el panel "por regla" sin repartir listas en LogQL.
- Dos reglas con el mismo `msg` colapsan en una (no se observó ninguna colisión en CRS 4.15).
- La latencia de detección de un motor roto en medio de la ventana es ≤ `EDGE_DETECT_PROBE_INTERVAL`.

## Pendiente / no verificado

- **La imagen del edge no se reconstruyó** (requiere red; no autorizado). La prueba monta reglas y scripts reales sobre `security-edge:local`; el `COPY` del `Dockerfile` para `audit-rotate.sh` solo se verificó por lectura. Debe comprobarse al construir la imagen (CI o un `docker build` autorizado).
- Las suites Hurl del edge (`edge/tests`) no se repitieron con la regla 85 nueva: la imagen en marcha tiene la anterior.
- Fuera de alcance: alertas, retención en Azure, sidecar en App Service, y el log de stderr de Caddy (`docker logs`), que sigue mostrando `Matched Data`.
