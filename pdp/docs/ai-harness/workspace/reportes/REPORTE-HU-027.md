# Estado de HU-027 — validación pendiente

> **NO VALIDADO.** Este estado sustituye el reporte anterior, que marcaba la historia aprobada sin
> evidencia reproducible suficiente. No usar las conclusiones anteriores como resultado vigente.

## Trabajo presente en el árbol

- Configuración de Caddy + Coraza/CRS, overlays PDP/PEP y suites Hurl.
- Pipeline dedicado en `ci/edge-pipeline.yml`; todavía no está registrado en Azure DevOps ni se ha
  ejecutado allí.
- Receptor de logs y scrape Prometheus del edge; dashboard actualizado. Los audit logs se
  sanitizan antes de exportarse y no se incluyen en el artefacto de CI.
- La guía conserva `EDGE_MODE=detection` por defecto hasta que la suite complete los dos modos.

## Comprobaciones locales ejecutadas

| Comprobación | Resultado |
|---|---|
| `docker compose config` del overlay PDP con el secreto del realm fixture | ✅ |
| `docker compose config` del overlay PEP | ✅ |
| `docker compose config` de observabilidad con la variable de CI | ✅ |
| Sintaxis YAML del pipeline, Collector y Compose de observabilidad | ✅ |
| Sintaxis Bash del pipeline y de sus tres bloques Python | ✅ |
| `edge/tools/check-exclusions.sh` en Git Bash | ✅ |
| JSON del dashboard Grafana | ✅ |
| `docker version` / conexión con Docker Engine | ⚠️ agotó el límite de espera; no se ejecutaron contenedores |
| `.claude/tools/drift.ps1` | ⚠️ 1 hallazgo preexistente: `SurrealAdministrationEventSchemaInitializer` citado en `pdp/docs/ai-harness/CHECKPOINT.md`, sin clase correspondiente |
| `.claude/tools/consistencia.ps1` | ⚠️ 7 inconsistencias preexistentes en use cases de `applications`, `profiles`, `resources` y `roles` |

## Bloqueantes y trabajo pendiente

1. **Docker no está disponible para la ejecución de pruebas.** No se ejecutaron `docker compose up`,
   Caddy, Hurl, la prueba fail-closed ni la suite completa del PDP.
2. El Collector exporta del audit log únicamente `correlationId` y una marca de detección. El id de
   regla, la puntuación y la ruta requeridos por el criterio de aceptación 5 no se exportan aún;
   falta una extracción segura y su comprobación E2E.
3. Registrar y ejecutar `ci/edge-pipeline.yml` en Azure DevOps.
4. Medir p50/p95 con y sin edge. No se inventan cifras.
5. El spike opcional de sidecar en Azure no se ejecutó; no se modificó el despliegue de Azure.
6. Investigar por separado la violación de Modulith de `authorization`, que bloquea la suite Java y
   no forma parte de HU-027.

**Estado final: pendiente.** No se declara ningún criterio de aceptación validado y no se cambia
ningún overlay a `blocking` por defecto hasta que la evidencia de CI esté verde.
