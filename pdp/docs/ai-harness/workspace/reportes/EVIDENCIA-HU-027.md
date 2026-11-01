# Evidencia de validación — HU-027 (Edge con WAF Coraza + OWASP CRS v4)

> Pasada FINAL (2026-10-30): `edge-pdp` **reconstruido y recreado** con el árbol actual (incluye HU-033: `MFA_REQUIRED` responde 403; HU-028/029/030/031 commiteadas) y `edge` reconstruido con `/internal/*` en default-deny. Todas las cifras de abajo salen de esta pasada.
> Ejecutada 2026-10-29/30 contra el stack Compose `-p edge` reconstruido (`edge-pdp` con el código de HEAD `a22e007`, que incluye HU-028/029/030/031; `edge` con la regla 85 corregida). Rol: implementador+validador de la parte de infraestructura (`edge/`, compose, pipeline, docs). No toqué `pdp/src` ni `pep/src` (los cambios actuales en `pdp/src` son de HU-033, de otro agente). Sin commits. No sustituye a `REPORTE-HU-027.md` (anterior).
> Comando base: `docker compose -p edge -f pdp/docker-compose.yml -f edge/compose.pdp.yml --profile test run --rm --no-deps hurl --test --jobs 1 --path-as-is --variables-file hurl.env.compose --variable run_id=<id> <archivo>` (Hurl 7.1.0 en la red del Compose, secuencial).

## Resumen por criterio

| # | Criterio | Evidencia | Estado |
|---|---|---|---|
| 1 | Bloqueo con sobre y `correlationId`; nada llega al PDP | `blocking`: 8/8 archivos de ataque (12 peticiones) `403 REQUEST_REJECTED`. Los ataques ahora llevan `X-Correlation-Id: edge-attack-<id>`: en `blocking` 0 líneas con ese id en `security-pdp.json`; en `detection` (control de sensibilidad) 6 líneas. Delante del PEP: no ejecutado (ver tarea 8) | ✅ topología PDP / 🟡 PEP |
| 2 | Flujos completos sin detecciones | `detection`: 10/10 flujos, 119 peticiones, audit log con 0 entradas aparte del canario del healthcheck. `blocking`: 10/10 flujos | ✅ |
| 3 | Exclusión sin prueba rompe el pipeline | `check-exclusions.sh`: OK hoy; prueba negativa (exclusión 900100 sin `.hurl` gemelo) sale con código 1. El pipeline lo invoca con `set -e`. Hoy hay **0 exclusiones**; el script solo exige que exista el gemelo, no que reproduzca un falso positivo. Pipeline no ejecutado (tarea 6) | 🟡 |
| 4 | Fail-closed | Regla corrupta montada en `rules/exclusions/`: `edge: Caddyfile inválido: no se arranca`, `exit=1`. Motor apagado (`SecRuleEngine Off`) en contenedor aparte: `unhealthy`, "canario en blocking -> 200 (esperado 403)". `docker ps`: solo `edge` publica puerto (8088); `edge-pdp` expone 8080/tcp sin publicar | ✅ |
| 5 | Loki/Grafana: regla, puntuación, ruta, correlationId | **Destino: [HU-034](../HU-034.md)** (no se arregla aquí). Pila definida (`observability/compose.yml`: Collector, Loki, Grafana, panel "Edge — detecciones por regla y ruta") pero **no levantada**: sus imágenes (otel-collector, loki, grafana, prometheus, jaeger) no están en local y descargarlas requiere autorización. Brecha real por análisis estático: `collector.yml` (`transform/edge_audit_privacy`) borra `messages` (ids de regla) y `transaction.request` (ruta); el correlationId se extrae de `transaction.request.headers`, que es `null` porque `SecAuditLogParts AKZ` no registra cabeceras; el panel consulta `anomaly_score=` pero la regla 100900 es `nolog`. El log de acceso de Caddy sí trae ruta, estado y `X-Correlation-Id` de respuesta. Correlación en el PDP: `hurl-corr-<id>` aparece en `security-pdp.json` | ⛔ |
| 6a | `/internal/*` default-deny (decisión del responsable) | Todo `/internal/*` → 404 sin cuerpo en la topología PDP, en `detection` y `blocking` (`flows/internal-channel-blocked.hurl`, 14 peticiones, 0 detecciones). Lista blanca solo con upstream PEP (`EDGE_INTERNAL_ALLOW_PEP=true`, fijada por `compose.pep.yml`): `PUT /internal/v1/integrations/*/*` y `POST /internal/v1/embedded-access-decisions`; verificada con un edge de prueba (upstream PDP): `allowlist/internal-allowlist-pep.hurl` 7 peticiones ✔ (las dos permitidas no dan 404; el resto, incluido otro método sobre las mismas rutas e inventadas, 404). `docker compose config` del overlay PEP válido | ✅ (PDP) / 🟡 (lista blanca sin PEP real) |
| 6 | 429 por IP; varias IPs no | `over-threshold.hurl` (61 peticiones, 1 IP): última `429`. En paralelo `hurl-second-ip` + `rate-limit-under-threshold` (50): sin 429 | ✅ |
| 7 | Cabeceras | `curl -D- /api/v1/session`: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`; `flows/headers.hurl` ✔ | ✅ |
| 8 | Latencia p50/p95 y memoria B1 | Ver tabla de latencia. Memoria local: edge ~77 MiB, edge-pdp ~407 MiB (pasada final) (`docker stats`, segunda pasada: edge ~80 MiB, PDP ~406 MiB). Memoria en B1 y sidecar Azure: no medidos | 🟡 |
| 9 | Ningún control relajado | Ningún archivo de `pdp/src`/`pep/src` fue editado por esta validación. Cambios de configuración solo en el overlay de pruebas del edge (ver decisiones) | ✅ |
| 10 | `threat-model.md` | Dejó de ser stub (6 filas de ADR-032 con estado real) en `security-platform-architecture/docs/06-security/threat-model.md` (+ fila del README), rama `docs/adr-033-034-renovacion-y-logout`, commit en esa rama | ✅ |
| 11 | `verificar.ps1` en verde | Ejecutado por el responsable desde el Terminal del usuario sobre el árbol final (HEAD `174c40d`): `mvnw clean verify` **902 pruebas, 0 fallos, 0 errores**, JaCoCo ≥ 50 % por paquete; `consistencia.ps1` CONSISTENTE; `drift.ps1` SIN DERIVA | ✅ |

## Tarea 1 — modo `detection` (causa y corrección)

- Reproducido: `EDGE_MODE=detection` → `edge` `unhealthy`; log de salud: `el audit log no creció tras el canario: el motor no detecta`; `security-edge-audit.json` quedaba en 0 bytes.
- Causa: `rules/85-audit-on-detection.conf` excluía del audit las peticiones con `X-Correlation-Id` que casen `^edge-healthcheck` (prefijo), y el canario de `healthcheck.sh` usa `edge-healthcheck-detect`. El audit nunca crecía. Con otro id (`probe-1`) el canario sí se auditaba (920350, 941100…, 949110).
- Corrección: `!@streq edge-healthcheck` (solo el latido exacto). Tras reconstruir el edge queda `healthy` en `detection` y en `blocking`.
- Resultado en detección: 9 flujos, 104 peticiones, 0 detecciones fuera del canario. Ataques en `detection`: los 8 archivos "fallan" como se espera (no se bloquea: el PDP responde 401/400/200) y las clases quedan auditadas con `is_interrupted=false` (911100, 913100, 920420, 930100/930110, 941xxx, 942xxx, 949110).
- Modo por defecto sin cambios (`EDGE_MODE:-detection` en ambos overlays); la pila quedó corriendo en `blocking`, como estaba.

## Tarea 2 — HU-029 y HU-030

- `oidc-login-session-logout.hurl` (11 peticiones ✔): tras login por cookie, `POST /api/v1/session/token/refresh` con cookie + CSRF → **404**; sin cookie → **401**. Con cookie y sin CSRF → 403 (CSRF del PDP, observado en sondeo).
- Canal `/internal` (decisión del responsable, **default-deny**): `Caddyfile` `@internal_denegado` (expresión CEL): cualquier ruta que empiece por `/internal` (regex `(?i)^/+internal`, cubre mayúsculas, `//`, `%69nternal`, `;x=1`, `.json`) → `respond 404` sin cuerpo, antes del WAF/proxy, igual en `detection` y `blocking`; excepción solo si `EDGE_INTERNAL_ALLOW_PEP=true` (valida el entrypoint, default `false` en el Dockerfile) y exactamente `PUT .../integrations/{a}/{b}` o `POST .../embedded-access-decisions`. `flows/internal-channel-blocked.hurl` (14 peticiones ✔ en ambos modos): bff-session-token, access-decisions, `applications/names/*/credential-validations`, `applications/{id}/administrators`, `/internal/v1/inventada`, las dos rutas del starter (404 en PDP), y las variantes. `allowlist/internal-allowlist-pep.hurl` (solo topología PEP; no está en el pipeline porque no se pudo probar contra un PEP real).
- `flows/bff-session-token-internal.hurl` (13 peticiones ✔ en ambos modos): login por el edge (cookie capturada en el callback) y canal llamado **directo al PDP** (`pdp_url=http://pdp:8080`, como el PEP) con la identidad técnica `security-pep-internal` + cookie: sin identidad → 401; identidad sin cookie → 401; con ambas dos llamadas devuelven tokens con distinto `jti`, `exp2 >= exp1` y ambos con `exp` posterior al `iat` de la identidad técnica; tras logout → 401. Los tokens no se imprimen.
- Limitación: "vigentes" se verifica por `exp`; usar el token como bearer en `/api/v1/session` devolvió 401 (camino bearer pendiente de HU-028). No se contrastó contra el skew por defecto (60 s).

## Tarea 4 — latencia (blocking; límites de tasa elevados solo durante la medición)

Método: Hurl `[Options] repeat: 400` dentro de la red del Compose, un calentamiento + 3 rondas de 400 por caso, sobre `pdp:8080` (sin edge) y `edge:8088` (con edge), una conexión keep-alive. Casos: `GET /actuator/health` y `POST /api/v1/authorize` con cuerpo JSON (sin sesión → 401: ejercita el WAF con inspección de cuerpo sin depender de SurrealDB/OPA). Se elevó `EDGE_RATE_AUTHORIZE`/`EDGE_RATE_DEFAULT` con un override temporal (revertido) porque el límite de 60/min daría 429.

| Caso | Sin edge p50 / p95 / p99 (ms) | Con edge p50 / p95 / p99 (ms) | Añadido p50 / p95 |
|---|---|---|---|
| `/actuator/health` (n=1200) | 32,13 / 37,38 / 43,16 | 33,68 / 38,71 / 43,69 | +1,55 / +1,33 ms |
| `/api/v1/authorize` (n=1200) | 1,00 / 2,33 / 3,60 | 3,76 / 5,49 / 6,77 | +2,77 / +3,16 ms |

Pasadas anteriores (mismo método): +1,4 / +2,5 ms y +2,3 / +3,9 ms; +2,8 / +6,3 ms y +2,6 / +3,9 ms. La variación entre pasadas es ruido de la máquina.

Cargas cortas en máquina de desarrollo con Docker Desktop: sirven para el orden de magnitud, no como cifra de B1.

## Tarea 6 — pipeline

`ci/edge-pipeline.yml` existe (no está referenciado desde `azure-pipelines.yml`; debe registrarse como pipeline separado; no se pudo ejecutar). Ejecuta `check-exclusions.sh` (criterio 3), las suites en `detection` y `blocking`, fail-closed, rate limit, PEP y publica JUnit. Defectos encontrados y corregidos (validado solo como YAML):
1. `assert_detection_logs_clean` filtraba el canario por cabecera, pero el audit no guarda cabeceras: ahora también lo ignora por la ruta `/_edge/canary`.
2. `assert_pdp_correlation` buscaba `hurl-corr-<id>` en el log del PDP, pero `/actuator/health` no deja línea: `flows/correlation.hurl` envía ahora el id también a `GET /api/v1/session` (401, sí registrado).
3. `assert_attack_not_in_pdp_logs` era vacuo: solo `canary.hurl` llevaba `edge-attack-<id>`. Ahora todos los ataques lo llevan (verificado: 6 líneas en `detection`, 0 en `blocking`).
4. Hurl se ejecutaba sin `--path-as-is`: `path-traversal.hurl` fallaba (libcurl normaliza `%2e%2e`). Añadidos `--jobs 1 --path-as-is`.
5. Se añadieron `flows/bff-session-token-internal.hurl` (contra el PDP directo) y `flows/internal-channel-blocked.hurl` a las listas de `detection` y `blocking`.

## Tarea 8 — overlay PEP

`docker compose -f pep/compose.integration.yml -f edge/compose.pep.yml config --quiet` → válido. No se levantó: construir `pep` exige `eclipse-temurin:25-jre` (no está en local) y descargar dependencias Maven dentro de la imagen (`dependency:go-offline`); no se forzó. `docker compose config --quiet` del overlay PEP sigue válido tras añadir `EDGE_INTERNAL_ALLOW_PEP`. Pendiente: flujo legítimo y ataque a través del PEP (🟡: configuración válida, sin ejecución; motivo: imagen base y dependencias Maven no disponibles sin descargar). Ojo al abrirlo: el bloqueo de `/internal` usa el mismo Caddyfile; `embedded-access-decisions` e `integrations` deben seguir pasando hacia el PEP.

## Decisiones técnicas

1. Regla 85: exclusión exacta (`@streq`) en vez de prefijo. El canario de `detection` sigue auditándose a propósito (~11 KB por latido cada 10 s).
2. `edge/compose.pdp.yml`: `PDP_SECURITY_SESSION_TOKEN_RENEWAL_SKEW=3600s` (variable `EDGE_TEST_TOKEN_RENEWAL_SKEW`), solo overlay de pruebas del edge (decisión del responsable). El default de producción (60 s) no cambia.
3. `edge/compose.pdp.yml`: `EDGE_RATE_SESSION=150/1m` solo en la topología de pruebas; con el contrato (30/1m) la suite recibía 429 a partir del tercer inicio de sesión seguido desde una IP. El default del Dockerfile no cambia; el umbral real sigue por fijar con tráfico observado.
4. Realm de CI (solo el fixture `edge/tests/realm/`): cliente `security-pep-internal` (service account, audience `security-baseline-bff`, secreto de prueba, también en `hurl.env.compose`). Importación verificada en un Keycloak desechable (`Realm 'security-baseline' imported`; cliente con service account). En el `edge-keycloak` vivo se creó el mismo cliente con `kcadm`; no se borró ningún volumen.
5. `edge/compose.pdp.yml`: `PDP_SECURITY_MFA_ENABLED=false` solo en la topología de pruebas del edge (como `distribution/compose.yaml`; el default de producción `true` no cambia; razón documentada en el compose). Con ello `administrators-list-add-remove.hurl` recupera la aserción estricta original (listado 200 + `$.data`); el archivo es idéntico a HEAD salvo el comentario de cabecera.
6. `Caddyfile`/`entrypoint.sh`/`Dockerfile`/`compose.pep.yml`: `/internal/*` default-deny con lista blanca solo para upstream PEP (ver tarea 2).

## Hallazgos y destinos

1. PDP (otra HU, no tocado aquí): `MFA_REQUIRED` responde 400 y debe ser 403 (ADR-031); lo corrige HU-033 en `pdp/src`.
2. Canal `/internal`: resuelto en el edge con default-deny (cubre también `credential-validations` y `administrators`). Pendiente de verificar con un PEP real: que las dos rutas permitidas funcionen de punta a punta por el edge.
3. ⛔ Audit log de Coraza sin rotación (~11 KB cada 10 s por el canario en `detection`, ~95 MB/día) y ⛔ criterio 5 (Collector sin regla/ruta/correlationId, panel que busca `anomaly_score=`): destino [HU-034](../HU-034.md), no se corrigen en HU-027.
4. ⛔ Sidecar de App Service en B1 y memoria en B1: no ejecutados (sin cambio de despliegue Azure en esta HU).
5. `pdp/Dockerfile` HEALTHCHECK usa curl, inexistente en la imagen JRE (anotado en `compose.pdp.yml`). Los valores de `hurl.env.compose` y del realm de CI son de prueba.

## Verificación independiente del responsable (2026-10-30)

Repetí las suites yo mismo, con `EDGE_MODE` fijado explícitamente y `run_id` único (un `docker compose run` sin `EDGE_MODE`
**recrea `edge` en `detection`**, el valor por defecto del compose: ojo al ejecutarlas a mano).

- `blocking`: 10/10 flows y 8/8 ataques (`403 REQUEST_REJECTED`).
- `detection`: 10/10 flows; los 8 ataques no bloquean (se auditan), como se espera.
- Corregido: `internal-allowlist-pep.hurl` estaba en `flows/` pero solo vale con `EDGE_INTERNAL_ALLOW_PEP=true`; rompía `flows/*.hurl` en la
  topología PDP. Se movió a `edge/tests/allowlist/`.
- Corregido: finales de línea. Un `entrypoint.sh` en CRLF rompe el contenedor; `.gitattributes` fija `edge/**` y `*.sh` en LF.

## Estado de HU-027

**🟡 Parcial.** Cerrados ✅: criterios 1 (topología PDP), 2, 4, 6, 7, 9, 10, 11. Abiertos: 3 (el pipeline no se ejecutó y hoy hay 0 exclusiones),
5 (HU-034), 8 (memoria en B1 y sidecar), overlay PEP (imágenes sin descargar) y el pipeline sin registrar en Azure DevOps.
