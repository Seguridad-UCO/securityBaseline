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

El Collector recibe ambos archivos como `file_log/edge` y `file_log/edge_audit`. Antes de exportar
el audit log, conserva solo el `correlationId` y una marca `edge.detection`; elimina el URI,
cabeceras, cuerpos, direcciones IP y detalles de reglas que podrían contener valores de la petición.
El archivo local de auditoría sigue siendo evidencia sensible: no se publica como artefacto de CI.
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
