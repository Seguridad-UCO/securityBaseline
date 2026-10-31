# PLAN: Edge con WAF (Coraza + OWASP CRS) delante del PEP y del PDP — de detección a bloqueo con evidencia

## Metadata

- **ID:** HU-027
- **Slice:** `nuevo: edge` — **no es un slice del PDP**: es un componente de infraestructura en `edge/` (raíz del monorepo), sin código Java
- **Tipo:** Infraestructura (topología, configuración, pruebas de caja negra, CI)
- **Fecha:** 2026-09-20
- **Rama sugerida:** `feature/HU-027-edge-waf-coraza`
- **Fuentes:** `security-platform-architecture/docs/01-governance/adr/ADR-032-web-application-firewall.md` (decisión), ADR-025, ADR-014, ADR-007, ADR-010, ADR-020, ADR-022; `observability/TELEMETRY-CONVENTION.md`, `observability/collector.yml`, `observability/compose.yml`; `pdp/docker-compose.yml`, `pep/compose.integration.yml`, `pdp/Dockerfile`; `shared/web/handler/ApiAccessDeniedHandler`, `shared/web/handler/ApiErrorHandler`, `shared/web/filter/CorrelationWebFilter`; `KeycloakSecurityConfiguration` (rutas públicas del BFF); `ci/templates/stage-deploy.yml`, `ci/pep-pipeline.yml`; `pdp/infra/README.md`; `az network nsg rule list` (NSG de Keycloak) y `curl -I` al endpoint público de Keycloak
- **Criterios de la línea base que toca:** 4 (correlación `X-Request-Id`/`X-Correlation-Id` de punta a punta), 5 y 9 (el bloqueo responde con el mismo `ProblemDetail` y un `code` estable, sin `RuntimeException` ni formato ad hoc), 8 (log JSON sin datos sensibles, bajo la convención de telemetría), 23 (`verify` en verde, ningún secreto en el repositorio). Los estructurales 1, 2, 11, 12, 21 y 22 **no se tocan**: la historia no modifica Java; el validador comprueba que siguen en verde, no que cambiaron.

## 0. Hallazgos de la fase 2 que cambian la historia

La HU se escribió con tres supuestos que el estado real no sostiene. El plan parte del estado real:

| Supuesto de la HU | Estado real | Consecuencia en el plan |
|---|---|---|
| "Compose es la única topología donde hoy corren PEP y PDP juntos" | **No existe tal topología.** `pdp/docker-compose.yml` levanta solo SurrealDB, Keycloak y Redis (el PDP corre en el host con `mvnw`); `pep/compose.integration.yml` es una demo con fixtures en red interna | Dos overlays (`edge/compose.pdp.yml`, `edge/compose.pep.yml`), misma imagen y reglas — decisión de Sebastián en la fase 3 |
| "La suite E2E del PDP pasa por el edge" | **No hay suite HTTP de caja negra.** Las pruebas de integración son `WebTestClient` in-process; `verificar.ps1` es solo Maven | Suite nueva en `hurl`, ejecutada como servicio del mismo Compose (resuelve `edge`/`keycloak` por nombre) — decisión de Sebastián |
| "Keycloak: ADR-020 dice NSG restringido; el login necesita al navegador" | **Ambas cosas son ciertas a la vez.** El 8080 está restringido a las IPs de los App Service; el **443 es público**, servido por un **Caddy** en la VM (`Via: 1.1 Caddy`, HSTS, certificado válido). ADR-020 ("sin TLS") está desactualizada y **la config de ese Caddy no está versionada en ningún repositorio** | Keycloak queda **fuera** de esta historia (decisión de Sebastián). Hallazgo registrado en §11 para el reporte y para una historia siguiente |
| Realm local importable | `pdp/keycloak/import/` está **vacío**: el realm local se configura a mano | La suite necesita un realm de CI versionado (§7.6). Es un activo que el proyecto no tenía y el README de Keycloak pedía |

Y un hallazgo a favor: que Keycloak ya esté detrás de Caddy en producción es el desempate del motor
(§1).

## 1. Resumen funcional

Se agrega un contenedor `edge` —Caddy con el módulo Coraza y OWASP CRS v4— que pasa a ser el único
puerto publicado delante del PDP (overlay sobre `pdp/docker-compose.yml`) y del PEP (overlay sobre
`pep/compose.integration.yml`). Inspecciona cada petición con CRS en paranoia 1, limita por IP en
rutas sensibles, añade cabeceras de seguridad, propaga o genera `X-Request-Id`/`X-Correlation-Id`,
y responde a un bloqueo con el mismo `ProblemDetail` del PDP bajo un `code` propio. Entra en modo
detección; una suite `hurl` de ataques y de flujos legítimos (incluido el OIDC del BFF contra un
Keycloak real) corre en un pipeline propio y es la que autoriza el paso a bloqueo. Un *spike* al
inicio decide si el edge llega también a App Service como sidecar del PDP.

**Motor: Coraza sobre Caddy.** Contra los cinco criterios de ADR-032:

| Criterio | `owasp/modsecurity-crs` (nginx) | Coraza + Caddy |
|---|---|---|
| Procesa cuerpos JSON | Sí | Sí (`SecRequestBodyAccess On` + procesador JSON) |
| Log JSON a stdout | Sí (`SecAuditLogFormat JSON`) | Sí: Caddy loguea JSON nativo; Coraza audit log JSON |
| Healthcheck real del motor | Requiere petición canario | Igual: petición canario (§7.4). Además Caddy **no arranca** con una directiva inválida |
| Imagen mantenida | Sí, por el proyecto CRS (libmodsecurity v3 en mantenimiento) | Sí, releases de `coraza-caddy`; se construye con `xcaddy` y versiones fijadas |
| Huella en B1 | nginx + libmodsecurity (C) | Un binario Go; sin dependencias nativas |
| **Desempate** | — | **Ya hay un Caddy en producción delante de Keycloak.** Un solo proxy/motor para Compose, sidecar y VM, con TLS automático. Una historia siguiente puede activar Coraza en ese Caddy sin introducir otra pieza |

**No cubre:** Keycloak (fuera, con hallazgo); WAF administrado; limitación por identidad; PL2;
cambios en aplicaciones protegidas o en casos de uso del PDP; CSP del frontend; `threat-model.md`
(vive en el repositorio de arquitectura, que para el harness es solo lectura — se entrega como PR
aparte, §11).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Bloqueo | Con `EDGE_MODE=blocking`, cada petición de `edge/tests/attacks/*.hurl` recibe `403`, `Content-Type: application/problem+json`, `code = REQUEST_REJECTED` y un `correlationId`; el log del PDP/PEP **no** registra esa petición |
| 2 | Sin falsos positivos | `edge/tests/flows/*.hurl` (registro/consulta de aplicaciones, administradores, autorización, **login OIDC → sesión → CSRF → API → logout** contra el Keycloak del Compose) pasa completo con los códigos esperados y el audit log de Coraza queda **vacío** para esas peticiones |
| 3 | Exclusión sin prueba falla | `edge/tools/check-exclusions.sh` falla si existe `edge/rules/exclusions/<id>.conf` sin `edge/tests/exclusions/<id>.hurl` (o al revés); corre en el pipeline antes que la suite |
| 4 | Fail-closed | Con un archivo de reglas corrupto, `edge` no pasa su healthcheck y `docker compose up --wait` termina en error; `pdp`/`pep` no publican puertos en los overlays (verificado con `docker compose config`) |
| 5 | Evidencia | Cada bloqueo aparece en Loki con `rule_id`, `anomaly_score`, ruta y `correlationId` (`service.name = security-edge`); para una petición que sí pasó, el mismo `correlationId` aparece en el log del PDP. El panel de Grafana tiene la fila "Edge" (bloqueos por regla, por ruta; 403/429 y latencia) |
| 6 | Limitación por IP | N+1 peticiones a `/api/v1/authorize` desde una IP en la ventana → la última recibe `429`; el mismo volumen desde dos IPs (dos servicios `hurl` en el Compose) no dispara |
| 7 | Cabeceras | Toda respuesta a través del edge lleva `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` |
| 8 | Medición | `edge/README.md` registra p50/p95 de `flows/` con y sin edge (misma suite, misma máquina) y, si el spike del sidecar pasa, la memoria de `edge` y `pdp` en B1 |
| 9 | Nada se relajó | `git diff` de `pep/src/main/resources/application.properties`, `CorsProperties`/`pdp.security.cors.*` y `SecurityConfiguration`/`KeycloakSecurityConfiguration` es vacío |
| 10 | Paso a bloqueo documentado | `edge/README.md` contiene la checklist detección → bloqueo (§6) y el estado en que queda cada overlay (`EDGE_MODE` por defecto) |
| 11 | Verde | `verificar.ps1` (suite completa) en verde; `ci/edge-pipeline.yml` en verde |

## 3. Reglas del edge (invariantes de configuración)

No hay reglas de negocio: hay invariantes de configuración que el validador comprueba en archivos.

| # | Invariante | Dónde vive | Cómo se verifica |
|---|---|---|---|
| E1 | Solo `edge` publica puertos en los overlays | `edge/compose.*.yml` | `docker compose -f … config` no muestra `ports:` en `pdp`/`pep` |
| E2 | Modo por variable, nunca hardcodeado: `EDGE_MODE ∈ {detection, blocking}` | `edge/Caddyfile` + `edge/rules/00-engine.conf` | El mismo `Caddyfile` sirve a ambos modos |
| E3 | CRS v4 fijado por versión y checksum, paranoia 1, `ANOMALY_INBOUND=5` | `edge/Dockerfile`, `edge/rules/01-crs-setup.conf` | Versión y SHA en el Dockerfile; `PARANOIA`/umbral en `crs-setup` |
| E4 | Exclusiones solo por regla **y** por ruta (`ctl:ruleRemoveTargetById` / `ruleRemoveById` bajo un `SecRule REQUEST_URI`), nunca `SecRuleRemoveById` global ni cambio de umbral | `edge/rules/exclusions/*.conf` | `check-exclusions.sh` rechaza un archivo con `SecRuleRemoveById` fuera de un `SecRule REQUEST_URI` |
| E5 | Una exclusión ↔ una prueba, mismo id | `edge/rules/exclusions/<id>.conf` ↔ `edge/tests/exclusions/<id>.hurl` | `check-exclusions.sh` |
| E6 | El bloqueo nunca revela regla ni puntuación en la respuesta | `edge/Caddyfile` (handler de error) | `attacks/*.hurl` asserta ausencia de `rule` en el cuerpo |
| E7 | `X-Request-Id`/`X-Correlation-Id` se generan si faltan y se propagan al upstream **con esos mismos nombres** (los que lee `CorrelationWebFilter`) | `edge/Caddyfile` | `flows/correlation.hurl`: manda un `X-Correlation-Id` y lo recibe de vuelta; sin mandarlo, recibe uno |
| E8 | Ningún secreto en `edge/` | `edge/` | `grep` del pipeline; el client secret del BFF de CI vive en el realm de CI, que es un secreto de **prueba** documentado como tal |
| E9 | El edge no reescribe rutas ni cuerpos: solo inspecciona, limita, añade cabeceras y proxya | `edge/Caddyfile` | `flows/` cubre rutas con query, cuerpo JSON y redirecciones OIDC intactas |

## 4. Modelo de dominio afectado

Ninguno. La historia no toca `pdp/src/main/java`.

## 5. Persistencia

No aplica.

## 6. Rutas del edge y proceso operativo

### 6.1 Rutas

| Ruta en el edge | Qué hace | Upstream |
|---|---|---|
| `/_edge/health` | `200` estático — vivo | ninguno |
| `/_edge/canary` | `200` estático, **pero pasa por el WAF**: `GET /_edge/canary?p=<script>` debe dar `403` en `blocking`. Es el healthcheck del motor | ninguno |
| `/*` (overlay PDP) | WAF → rate limit por ruta → cabeceras → proxy | `http://pdp:8080` |
| `/*` (overlay PEP) | Igual | `http://pep:8081` |

Limitación por IP (valores iniciales, **se ajustan con la fase de detección**, §6.2):

| Zona | Rutas | Ventana | Clave |
|---|---|---|---|
| `authorize` | `/api/v1/authorize` | 60 req / 1 min | IP cliente |
| `session` | `/api/v1/session*`, `/oauth2/*`, `/login/*` | 30 req / 1 min | IP cliente |
| `default` | resto | 300 req / 1 min | IP cliente |

IP cliente: en Compose, la remota; en App Service, `X-Forwarded-For` **solo** si viene del
front-end de Azure (`trusted_proxies` en el Caddyfile). Sin esa lista, un cliente podría falsificar
su IP y esquivar la limitación.

### 6.2 Detección → bloqueo (checklist que vive en `edge/README.md`)

1. Overlays arrancan en `EDGE_MODE=detection`. El pipeline corre la suite en **ambos** modos: en
   `detection`, `flows/` debe pasar y el audit log debe quedar vacío; en `blocking`, `attacks/`
   debe recibir `403` y `flows/` seguir pasando.
2. Cada detección de `flows/` se clasifica: falso positivo → exclusión + prueba (§7.5); ataque real
   en la suite → error de la suite.
3. Umbrales de limitación se fijan con el tráfico observado (peticiones por IP y minuto en `flows/`
   × margen), nunca a ojo.
4. Cuando el pipeline está en verde en ambos modos, el valor por defecto de `EDGE_MODE` en los
   overlays pasa a `blocking`. Ese cambio es un commit propio, con las cifras del criterio 8.
5. En Azure (si el sidecar existe): `EDGE_MODE=detection` una ventana acordada con tráfico real,
   revisión de Loki, luego `blocking`. Es operación, fuera del pipeline, pero la checklist lo dice.

## 7. SPEC — el contrato

> Aquí no hay firmas Java. El contrato son **archivos, nombres, variables y formatos**. El tester
> escribe `edge/tests/**/*.hurl` contra esto; el implementador escribe `Caddyfile`, reglas, overlays
> y pipeline contra esto. Nada de lo que no esté aquí se construye.

### 7.1 Imagen y variables de entorno

```
edge/Dockerfile
  FROM caddy:<2.x>-builder  →  xcaddy build
      --with github.com/corazawaf/coraza-caddy/v2@<versión fijada>
      --with github.com/mholt/caddy-ratelimit@<versión fijada>
  + OWASP CRS v4.<x>.<y> descargado en build, verificado por SHA-256, en /etc/coraza/crs/
  FROM caddy:<2.x>-alpine  →  binario + /etc/caddy/Caddyfile + /etc/coraza/
  HEALTHCHECK → /usr/local/bin/edge-healthcheck.sh
```

| Variable | Valores | Default | Uso |
|---|---|---|---|
| `EDGE_MODE` | `detection` \| `blocking` | `detection` | `SecRuleEngine DetectionOnly` / `On` |
| `EDGE_UPSTREAM` | URL | — (obligatoria) | `reverse_proxy` |
| `EDGE_LISTEN` | `:puerto` | `:8088` | Puerto de escucha (en sidecar no puede ser 8080: lo usa el PDP en el mismo namespace de red) |
| `EDGE_PARANOIA` | `1` | `1` | `tx.blocking_paranoia_level` — PL2 fuera de alcance |
| `EDGE_ANOMALY_INBOUND` | entero | `5` | Umbral entrante CRS |
| `EDGE_TRUSTED_PROXIES` | CIDRs separados por coma | vacío | `trusted_proxies` para `X-Forwarded-For` (App Service) |
| `EDGE_RATE_AUTHORIZE`, `EDGE_RATE_SESSION`, `EDGE_RATE_DEFAULT` | `N/ventana` (p. ej. `60/1m`) | los de §6.1 | Zonas de rate limit |
| `EDGE_LOG_DIR` | ruta | `/var/log/security` | Destino de `security-edge.json` y `security-edge-audit.json` |
| `OTEL_SERVICE_NAME` | string | `security-edge` | Solo documental — el collector lo añade como recurso (§7.7) |
| `DEPLOYMENT_ENVIRONMENT` | string | `local` | Idem |

### 7.2 Estructura del `Caddyfile` (orden de handlers, sin cuerpo)

```
{ admin off (Compose) / admin localhost:2019 con metrics (para Prometheus) ; order coraza_waf first ; order rate_limit before reverse_proxy }

{$EDGE_LISTEN} {
  log access   → json (time_key "@timestamp", time_format rfc3339_nano, level_key "level") → stdout + {$EDGE_LOG_DIR}/security-edge.json
  handle /_edge/health  → respond 200
  coraza_waf { directives: include /etc/coraza/rules/*.conf ; load_owasp_crs (o include del CRS fijado) }
  handle /_edge/canary  → respond 200               # después del WAF, a propósito
  header { HSTS ; nosniff ; Referrer-Policy no-referrer ; Cache-Control no-store ; -Server }
  request_header  X-Request-Id {http.request.uuid} si falta ; X-Correlation-Id {http.request.uuid} si falta
  rate_limit { zone authorize … ; zone session … ; zone default … }   → 429 con el ProblemDetail de §7.3 (code RATE_LIMITED)
  handle_errors → si status 403 originado por coraza → ProblemDetail §7.3 ; 429 → idem
  reverse_proxy {$EDGE_UPSTREAM} { header_up X-Forwarded-* ; trusted_proxies {$EDGE_TRUSTED_PROXIES} }
}
```

> El implementador verifica los nombres exactos de las directivas contra la versión fijada de
> `coraza-caddy` (`load_owasp_crs` vs. `include @coraza/crs`) y de `caddy-ratelimit`. El contrato
> es el **orden y el comportamiento**, no la sintaxis literal.

### 7.3 Respuesta a un bloqueo y a una limitación

Mismo formato que `ApiAccessDeniedHandler` (`application/problem+json`), con `code` distinto:

```json
{
  "type": "urn:security-baseline:error:request-rejected",
  "title": "REQUEST_REJECTED",
  "status": 403,
  "detail": "La solicitud fue rechazada por la política de seguridad de la plataforma",
  "code": "REQUEST_REJECTED",
  "requestId": "<X-Request-Id>",
  "correlationId": "<X-Correlation-Id>"
}
```

Limitación: `status: 429`, `type: urn:security-baseline:error:rate-limited`, `title`/`code`
`RATE_LIMITED`, cabecera `Retry-After`. **Nunca** `rule`, `ruleId`, `score`, `msg` de CRS en el
cuerpo ni en cabeceras. Las cabeceras `X-Request-Id`/`X-Correlation-Id` van también en la respuesta.

### 7.4 Healthcheck del motor

`edge/healthcheck.sh` (dentro de la imagen):

- `GET /_edge/health` → `200`, si no: `exit 1`.
- Si `EDGE_MODE=blocking`: `GET /_edge/canary?p=<script>alert(1)</script>` → debe ser `403`; si
  es `200`, el motor no está bloqueando: `exit 1`.
- Si `EDGE_MODE=detection`: el canario debe ser `200` **y** el audit log debe haber crecido en la
  última invocación (el motor detecta aunque no bloquee); si no crece: `exit 1`.

Un archivo de reglas inválido hace que Caddy no arranque (fail-closed de arranque); un motor que
arranca sin reglas cargadas lo atrapa el canario.

### 7.5 Reglas y exclusiones

```
edge/rules/
├── 00-engine.conf          SecRuleEngine {$EDGE_MODE→On|DetectionOnly}, SecRequestBodyAccess On, procesador JSON, límites de cuerpo ≥ los del PEP (nunca menores: E9)
├── 01-crs-setup.conf       paranoia, umbrales, tx.allowed_methods, tx.allowed_request_content_type (JSON + form-urlencoded para el login OIDC)
├── 10-platform-before.conf reglas propias previas a CRS (ids 100000–199999): p. ej. bloquear rutas nunca legítimas (`/.env`, `/.git`)
├── exclusions/
│   └── <id>-<slug>.conf    id en 900000–999999, UNA exclusión por archivo, con comentario "FP: <ruta> <regla> <por qué>"
└── 90-platform-after.conf  reglas propias posteriores a CRS
```

Contrato de una exclusión (E4/E5):

```
# FP: POST /api/v1/roles — 942100 (SQLi libinjection) dispara con nombres de rol con comillas
# Prueba: edge/tests/exclusions/900001-roles-name-quotes.hurl
SecRule REQUEST_URI "@beginsWith /api/v1/roles" \
  "id:900001,phase:1,pass,nolog,ctl:ruleRemoveTargetById=942100;ARGS:name"
```

`edge/tools/check-exclusions.sh`: falla si (a) hay `.conf` sin `.hurl` con el mismo id o viceversa,
(b) un archivo de `exclusions/` contiene `SecRuleRemoveById` fuera de un `SecRule REQUEST_URI`, o
(c) un archivo de `exclusions/` toca `tx.inbound_anomaly_score_threshold` o `tx.blocking_paranoia_level`.

### 7.6 Suite `hurl` y realm de CI

```
edge/tests/
├── hurl.env.compose            variables: EDGE_URL=http://edge:8088, KC_URL=http://keycloak:8080, usuario/clave de prueba
├── attacks/                    deben dar 403 en blocking
│   ├── sqli-query.hurl · sqli-json-body.hurl · xss-query.hurl · path-traversal.hurl
│   ├── scanner-user-agent.hurl · method-not-allowed.hurl · content-type-mismatch.hurl
│   └── canary.hurl
├── flows/                      deben pasar en ambos modos, audit log vacío
│   ├── health.hurl · correlation.hurl · headers.hurl
│   ├── oidc-login-session-logout.hurl   (GET /oauth2/authorization/keycloak → login en Keycloak por formulario → callback → cookie → GET /api/v1/session → XSRF → logout)
│   ├── applications-register-list.hurl · administrators-list-add-remove.hurl · authorize.hurl
│   └── rate-limit-under-threshold.hurl
├── ratelimit/                  over-threshold.hurl (429) — se ejecuta desde un segundo servicio hurl para el caso "dos IPs"
└── exclusions/                 <id>-<slug>.hurl — reproduce el FP y asserta que pasa
```

La suite corre **dentro del Compose** como servicio `hurl` (imagen oficial de hurl, misma red), para
que las redirecciones OIDC hacia `http://keycloak:8080/...` resuelvan. Dos servicios `hurl` (`hurl`
y `hurl-second-ip`) cubren el criterio 6.

Realm de CI: `edge/tests/realm/security-baseline-ci-realm.json`, montado en el overlay en
`/opt/keycloak/data/import/`. Contiene el realm `security-baseline`, el client `security-baseline-bff`
(confidencial, secreto de **prueba** documentado como tal en el archivo), el audience mapper y un
usuario `edge-tester`. **No se usa fuera del Compose.**

### 7.7 Overlays

```
edge/compose.pdp.yml     (se usa con: docker compose -f pdp/docker-compose.yml -f edge/compose.pdp.yml up --wait)
  services:
    pdp:      build pdp/Dockerfile · SPRING_PROFILES_ACTIVE=dev,keycloak,observability · sin ports · env: PDP_DATASOURCE_*, PDP_KEYCLOAK_ISSUER=http://keycloak:8080/realms/security-baseline, PDP_REDIS_HOST=redis, PDP_CORS_ALLOWED_ORIGINS, TELEMETRY_LOG_FILE=/var/log/security/security-pdp.json · volumen ./observability/logs:/var/log/security
    keycloak: (override) volumen ./edge/tests/realm:/opt/keycloak/data/import:ro · sin ports
    surrealdb, redis: (override) sin ports
    edge:     build edge/ · ports "8088:8088" (único publicado) · EDGE_UPSTREAM=http://pdp:8080 · EDGE_MODE=${EDGE_MODE:-detection} · volumen ./observability/logs:/var/log/security · depends_on pdp healthy
    hurl, hurl-second-ip: profile "test" · image hurl · command hurl --test --variables-file … tests/**/*.hurl

edge/compose.pep.yml     (con: docker compose -f pep/compose.integration.yml -f edge/compose.pep.yml up --wait)
    pep: sin ports · edge: EDGE_UPSTREAM=http://pep:8081 · ports "8089:8088" · red protected + una red con salida para hurl
```

### 7.8 Observabilidad

`observability/collector.yml` **[M]**:

- Receptor `filelog/edge`: `include: [/var/log/security/security-edge.json, /var/log/security/security-edge-audit.json]`; operadores: `json_parser` (timestamp desde `@timestamp`), `add` `resource["service.name"]=security-edge`, `add` `resource["deployment.environment.name"]=${env:DEPLOYMENT_ENVIRONMENT}`, y para el audit log: `move` de `transaction.id` → `attributes.correlationId` **solo si** Coraza lo toma de la cabecera (si no, el implementador incluye `X-Correlation-Id` en el audit log vía `SecAuditLogParts` y lo extrae con `regex_parser`), `move` de la primera regla disparada → `attributes.rule_id`, `attributes.anomaly_score`.
- Job Prometheus `security-edge` → `edge:2019/metrics` (métricas de Caddy: peticiones por status, latencia). Las cifras **por regla** salen de Loki (LogQL sobre `rule_id`), no de Prometheus — coherente con la convención: sin rutas ni ids como etiquetas de Prometheus.
- Pipeline `logs`: añadir `filelog/edge` a `receivers`.

`observability/grafana/dashboards/security-platform.json` **[M]**: fila "Edge": 403/429 por minuto
(Prometheus), bloqueos por `rule_id` y por ruta (Loki), p95 del edge (Prometheus).

`observability/TELEMETRY-CONVENTION.md` **[M]**: párrafo con `service.name = security-edge`, los dos
archivos, y los eventos `edge.request.blocked` / `edge.request.rate_limited`.

### 7.9 CI

`ci/edge-pipeline.yml` **[N]** (trigger: PR y `main`, paths `edge/**`, `observability/**`,
`pdp/Dockerfile`, `pep/**`):

1. `edge/tools/check-exclusions.sh`
2. `docker build edge/` + `docker run … caddy validate`
3. Overlay PDP en `EDGE_MODE=detection`: `up --wait` → `hurl` sobre `flows/` → assert audit log vacío
4. Overlay PDP en `EDGE_MODE=blocking`: `attacks/` (403) + `flows/` + `ratelimit/` + `exclusions/`
5. Overlay PEP en `blocking`: `attacks/` + `flows/health.hurl` + `correlation.hurl`
6. Fail-closed: arranca con `edge/tests/fixtures/broken-rule.conf` montado → `up --wait` debe fallar
7. Publica `hurl --report-junit` y los logs de `observability/logs/`

Debe **registrarse en Azure DevOps** (mismo pendiente que arrastra `ci/pep-pipeline.yml`).

### 7.10 Spike: sidecar en App Service (primer paso de la implementación, con criterio de caída)

Objetivo: en `app-pdp-dev`, `edge` como contenedor principal (`WEBSITES_PORT=8088`, `isMain`) y
`pdp` como sidecar sin puerto expuesto, `EDGE_UPSTREAM=http://localhost:8080`,
`EDGE_TRUSTED_PROXIES` con el rango del front-end de App Service.

| Criterio de caída | Consecuencia |
|---|---|
| `az` del agente de CI no soporta `sitecontainers` y `az rest` contra la API de sitecontainers tampoco funciona en B1 | Sidecar fuera; `ci/templates/stage-deploy.yml` no cambia; se documenta en §11 y en ADR-032 §Implementación |
| Memoria: `edge` + `pdp` superan el límite de B1 bajo `flows/` | Idem |
| Latencia p95 del edge > 20 ms añadidos en DEV | Se registra; no es caída por sí sola |

Si pasa: `ci/templates/stage-deploy.yml` **[M]** — reemplaza `az webapp config container set` por la
configuración de sitecontainers (edge principal + pdp sidecar), `EDGE_MODE=detection` en DEV.

## 8. Árbol de archivos

```
edge/                                           [N]
├── README.md                                   [N] contrato operativo: variables, modos, checklist detección→bloqueo, cifras (criterio 8)
├── Dockerfile                                  [N]
├── Caddyfile                                   [N]
├── healthcheck.sh                              [N]
├── rules/
│   ├── 00-engine.conf                          [N]
│   ├── 01-crs-setup.conf                       [N]
│   ├── 10-platform-before.conf                 [N]
│   ├── 90-platform-after.conf                  [N]
│   └── exclusions/README.md                    [N] (las exclusiones aparecen durante la fase de detección)
├── tools/check-exclusions.sh                   [N]
├── compose.pdp.yml                             [N]
├── compose.pep.yml                             [N]
└── tests/
    ├── hurl.env.compose                        [N]
    ├── realm/security-baseline-ci-realm.json   [N]
    ├── fixtures/broken-rule.conf               [N]
    ├── attacks/*.hurl                          [N]  ← tester
    ├── flows/*.hurl                            [N]  ← tester
    ├── ratelimit/*.hurl                        [N]  ← tester
    └── exclusions/*.hurl                       [N]  ← tester, una por exclusión que aparezca
ci/edge-pipeline.yml                            [N]
ci/templates/stage-deploy.yml                   [M] solo si el spike del sidecar pasa
observability/collector.yml                     [M] receptor filelog/edge + job Prometheus security-edge
observability/grafana/dashboards/security-platform.json   [M] fila Edge
observability/TELEMETRY-CONVENTION.md           [M] service.name security-edge, eventos del edge
pdp/docs/ai-harness/workspace/HU-027.md         [M] al cerrar (estado)
docs/PLATAFORMA.md                              [M] el edge en el diagrama de la plataforma
```

Nada en `pdp/src/**` ni en `pep/src/**`.

## 9. Casos de prueba esperados

> Aquí las pruebas son `hurl`, no JUnit. El presupuesto de `sb-testing` se traduce en: una prueba
> por comportamiento del contrato, sin duplicar entre `attacks/` y `flows/`.

| Carpeta | Archivo | Casos |
|---|---|---|
| `attacks/` | `sqli-query.hurl` | `GET /api/v1/applications?name=' OR 1=1--` → 403, problem+json, `code=REQUEST_REJECTED`, sin `rule` en cuerpo |
| | `sqli-json-body.hurl` | `POST /api/v1/applications` con `{"name":"x' UNION SELECT"}` → 403 (prueba el procesador JSON) |
| | `xss-query.hurl` | `?q=<script>alert(1)</script>` → 403 |
| | `path-traversal.hurl` | `GET /api/v1/../../etc/passwd` y `/.env`, `/.git/config` → 403 |
| | `scanner-user-agent.hurl` | `User-Agent: sqlmap/1.0` → 403 |
| | `method-not-allowed.hurl` | `TRACE /api/v1/session` → 403 (o 405 del edge, no del PDP) |
| | `content-type-mismatch.hurl` | `POST /api/v1/applications` con `Content-Type: text/xml` → 403 |
| | `canary.hurl` | `/_edge/canary?p=<script>` → 403 en blocking |
| `flows/` | `health.hurl` | `/_edge/health` 200; `/actuator/health` vía edge 200 |
| | `correlation.hurl` | con `X-Correlation-Id: abc` → respuesta trae `abc`; sin él → trae un UUID; el log del PDP contiene el mismo id (assert sobre el archivo de log, vía un `hurl` que lo lee del volumen o un paso del pipeline) |
| | `headers.hurl` | las cuatro cabeceras de seguridad presentes; `Server` ausente |
| | `oidc-login-session-logout.hurl` | redirección a Keycloak, formulario, callback, cookie `SECURITY_BASELINE_SESSION`, `GET /api/v1/session` 200, `XSRF-TOKEN` presente, `POST` con `X-XSRF-TOKEN` aceptado, logout 200/302 |
| | `applications-register-list.hurl` | registro con nombre con acentos y comillas simples legítimas (`"O'Higgins"`) → 201; listado paginado → 200 (candidato a FP: 942100) |
| | `administrators-list-add-remove.hurl` | HU-020 vía edge → 200/201/204 (o 403 por MFA fail-closed de HU-024 — documentar el esperado real) |
| | `authorize.hurl` | `POST /api/v1/authorize` con cuerpo real → 200 |
| | `rate-limit-under-threshold.hurl` | N-1 peticiones → todas 200 |
| `ratelimit/` | `over-threshold.hurl` | N+1 → la última 429 con `Retry-After` y problem+json `RATE_LIMITED`; ejecutado en paralelo desde `hurl-second-ip` para el caso "dos IPs no disparan" |
| `exclusions/` | `<id>-<slug>.hurl` | reproduce el FP exacto y asserta 2xx |
| Pipeline | fail-closed | overlay con `broken-rule.conf` → `up --wait` falla en ≤ 60 s |
| Pipeline | detección limpia | tras `flows/` en `detection`, `security-edge-audit.json` no contiene transacciones con `rule_id` |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-20 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo (`hurl`) | ⏳ Pendiente | |
| Spike sidecar | ⏳ Pendiente | |
| Implementación en verde (detection + blocking) | ⏳ Pendiente | |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes y hallazgos para el reporte

1. **Keycloak público sin WAF y con Caddy no versionado.** Fuera de esta historia por decisión.
   Historia siguiente propuesta: traer el `Caddyfile` de `vm-pdp-keycloak-shared` al repositorio
   (`pdp/infra/keycloak-vm/`), activar Coraza allí con las mismas reglas de `edge/rules/`, y
   corregir ADR-020 (ya no es "sin TLS": hay HTTPS público con certificado válido).
2. **`threat-model.md`** vive en `security-platform-architecture` (solo lectura para el harness).
   Las seis filas STRIDE de ADR-032 se entregan como PR aparte a ese repositorio, junto con la
   sección *Implementación* de ADR-032, al cerrar la historia — no las escribe el implementador.
3. **Sintaxis exacta de `coraza-caddy` y `caddy-ratelimit`** en las versiones fijadas: el contrato
   es comportamiento y orden (§7.2); el implementador confirma directivas al fijar versiones.
4. **Criterio 2, administradores (HU-020) vía edge:** con HU-024 en fail-closed hasta que el realm
   tenga MFA, `POST`/`DELETE` de administradores devuelven 403 **del PDP** (no del edge). La prueba
   debe distinguir ambos 403 por `code` (`FORBIDDEN` vs `REQUEST_REJECTED`) — es exactamente para lo
   que existe el `code` distinto.
5. **`sitecontainers` en B1:** el `az` local (versión antigua) no conoce el comando; el spike
   (§7.10) lo resuelve en el agente de CI o con `az rest`. Criterio de caída escrito.
