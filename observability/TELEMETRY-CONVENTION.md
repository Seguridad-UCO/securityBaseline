# Convención de telemetría

Esta convención es el contrato entre PDP/PEP, el OpenTelemetry Collector y Grafana. Aplica a
tráfico real; no requiere ni contiene generadores de tráfico.

## Activación y transporte

Los dos servicios mantienen la telemetría como perfil explícito: arránquelos con
`SPRING_PROFILES_ACTIVE=observability` (o añádalo al perfil funcional que corresponda). El perfil
expone el management local en `9080` (PDP) y `9081` (PEP), exporta trazas W3C por OTLP/HTTP y
escribe JSON Lines en `TELEMETRY_LOG_FILE`. Nunca exponga esos puertos fuera de la red de
operación: el collector es quien los consulta.

Las variables de cada proceso son `OTEL_SERVICE_NAME`, `DEPLOYMENT_ENVIRONMENT`,
`OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, `TELEMETRY_LOG_FILE` y `MANAGEMENT_SERVER_ADDRESS`. Los
targets `PDP_METRICS_TARGET` y `PEP_METRICS_TARGET` del collector están definidos en el `.env` de
esta instalación y apuntan a los puertos de management locales de ambos servicios.

## Contrato de logs JSON

Cada línea es un objeto JSON con `@timestamp` RFC 3339/UTC y `level`. El collector requiere y
promueve a atributos de recurso `service.name` y `deployment.environment.name`; por eso ambos son
obligatorios. Para correlacionar logs con trazas usa `traceId` y `spanId` hexadecimales W3C cuando
existen; para seguir la petición de negocio usa `requestId` y `correlationId`.

Los nombres de eventos reservados son:

- `http.request.completed` y `http.request.failed`: resultado de una solicitud HTTP.
- `security.authorization.completed`: resultado de la evaluación PDP.
- `pep.enforcement.completed`: resultado de la aplicación de la decisión en PEP.
- `security.access.event.recorded`: evidencia `access_event` confirmada en SurrealDB.
- `edge.request.blocked` y `edge.request.rate_limited`: rechazos del WAF y del limitador por IP.

`security.access.event.recorded` lleva solamente `decisionId`, `requestId`, `correlationId`,
`decision`, `reason` y `action`. No lleva token, JWT, cuerpo, sujeto, ruta del recurso ni URLs.
Las dimensiones de métricas son de vocabulario cerrado: `decision`, `reason`, `action`, `service`
y `environment`; no se permiten IDs, tenant, aplicación, usuario ni ruta como etiquetas de
Prometheus.

El edge usa `service.name = security-edge` y escribe `security-edge.json` (acceso) y
`security-edge-audit.json` (reglas que detectaron una petición). El query string se redacta en el
log de acceso. El audit log conserva únicamente las partes configuradas por Coraza; el campo
`logdata` añade puntuación, ruta sin query y `correlationId` para correlacionar detecciones sin
copiar parámetros de la URL.

El Collector recibe ambos archivos como `file_log/edge` y `file_log/edge_audit`. El audit log tiene
su **propio pipeline** (`logs/edge_audit`, fail-closed): una línea que no parsea se descarta y nunca viaja
cruda. `transform/edge_audit_projection` **sustituye el cuerpo** del registro por una proyección saneada
(lista blanca: lo que no se nombra no sale) y conserva `timestamp` desde `transaction.unix_timestamp`.

La regla `85-audit-on-detection.conf` (id 100900, `msg` constante `edge-detection`) escribe en su
`logdata` `anomaly_score=<n> path=<ruta> correlationId=<id>`; el Collector lo extrae junto con los
ids y mensajes de `messages[]`.

| Dónde | Clave | Contenido |
|---|---|---|
| Etiquetas Loki (índice) | `service_name`, `deployment_environment_name` | solo estas dos |
| Structured metadata y cuerpo JSON | `correlationId` | saneado a `[A-Za-z0-9._:-]`, máx. 128 |
| | `edge.rule_ids` | ids de regla separados por coma (incluye 949110; sin la regla de auditoría 100900) |
| | `edge.rule_msgs` | mensajes de esas reglas separados por ` \| ` |
| | `edge.rule_main` | primera regla que no sea de evaluación (excluye 949110, 959100, 980xxx y 100900) |
| | `edge.score` | puntuación de anomalía de entrada |
| | `edge.path` | ruta **sin query string** |
| | `edge.method`, `edge.interrupted`, `edge.transaction_id` | método, si Coraza interrumpió y id de transacción de Coraza |

En LogQL las claves se consultan con `_` en lugar de `.` (`| correlationId="…" | edge_rule_ids=~".*941100.*"`).
No viajan nunca: query string, `client_ip`/puerto, `Matched Data`, cabeceras, cuerpos, `raw` ni `tags` de
las reglas. No existe `edge.status`: el audit (partes AKZ) no trae el estado HTTP; está en el log de acceso
(`security-edge.json`) y `edge.interrupted=true` equivale a 403 en este despliegue.

El audit log del edge se rota por tamaño (`EDGE_AUDIT_MAX_BYTES`, `EDGE_AUDIT_KEEP`; ver
[`edge/README.md`](../edge/README.md)) y el canario de detección se espacia con
`EDGE_DETECT_PROBE_INTERVAL`. El archivo local de auditoría y sus rotados `.gz` siguen siendo evidencia
sensible: no se publican como artefacto de CI.
Prometheus consulta las métricas de Caddy por la red interna dedicada `security-edge-metrics` en
`edge:2019/metrics`. Solo el edge y el Collector se conectan a esa red; el puerto 2019 no se
publica en el host y sirve métricas, no la API administrativa de Caddy.

## Semántica operativa

La métrica `security_access_events_total{decision,reason}` incrementa únicamente después de que
la escritura de `access_event` fue confirmada. Así, su diferencia con
`security_authorization_seconds_count` revela una falla de auditoría. Una falla de persistencia se
registra como error y no cambia la decisión de acceso, por diseño fail-safe de auditoría.

El collector elimina URLs completas y mensajes/trazas de excepciones de las trazas antes de
exportarlas. No añada información sensible a atributos, nombres de spans, logs ni etiquetas.
