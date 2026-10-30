# Edge — Caddy + Coraza + OWASP CRS

El overlay de HU-027 coloca el WAF delante del PDP o el PEP en Compose. **No implica que el tráfico
de Azure ni Keycloak pase por este edge.** La implementación local dispone de modos `detection` y
`blocking`; el predeterminado se mantiene en `detection` hasta que CI demuestre que los flujos
legítimos pasan y los ataques se bloquean.

El contrato detallado y sus decisiones están en
[`PLAN-HU-027.md`](../pdp/docs/ai-harness/workspace/planes/PLAN-HU-027.md) y ADR-032.

## Topologías locales

Los overlays eliminan los puertos publicados de los backends; el edge es el único acceso HTTP:

```powershell
$realm = Get-Content edge\tests\realm\security-baseline-ci-realm.json -Raw | ConvertFrom-Json
$env:EDGE_CI_KEYCLOAK_CLIENT_SECRET = $realm.clients[0].secret
docker compose -p edge -f pdp\docker-compose.yml -f edge\compose.pdp.yml up -d --build --wait
```

Para el PEP:

```powershell
docker compose -p edge-pep -f pep\compose.integration.yml -f edge\compose.pep.yml up -d --build --wait
```

El scrape de métricas requiere la red interna dedicada `security-edge-metrics`, creada al arrancar
`observability/compose.yml`. El pipeline arranca el Collector antes de las topologías edge para
crear esta red y montar el directorio temporal de logs. Solo Caddy y el Collector se conectan a
ella. El admin API queda en `localhost:2020`; `edge:2019/metrics` sirve métricas solamente, sin
exponer el API administrativo.

Para probar los overlays, usa los perfiles `test` del Compose con los archivos de
[`tests/`](tests/). Las variables para la suite viven en `tests/hurl.env.compose`; la clave del
cliente del realm es un fixture local y nunca debe reutilizarse fuera de CI/Compose.

## Modos y promoción

| `EDGE_MODE` | Comportamiento | Estado |
|---|---|---|
| `detection` | Registra coincidencias sin bloquear | Predeterminado de ambos overlays |
| `blocking` | Bloquea con `403 REQUEST_REJECTED`; el rate limit responde `429 RATE_LIMITED` | Debe probarse antes de promoverlo |

1. Ejecutar `edge/tools/check-exclusions.sh`; cada exclusión debe tener una prueba Hurl gemela.
2. En `detection`, ejecutar todos los flujos y confirmar que no hay detecciones aparte del canario
   interno del healthcheck.
3. Si un flujo legítimo dispara CRS, justificar una exclusión específica y añadir su prueba gemela.
4. En `blocking`, ejecutar todos los flujos y ataques; los ataques deben recibir `403`.
5. Probar el rate limit desde dos servicios con IP de origen distintas.
6. Solo con pipeline verde en ambos modos, revisar y registrar p50/p95 con y sin edge antes de
   cambiar el default a `blocking`.

## Observabilidad y estado

Caddy elimina el query string, IPs y cabeceras del access log. Coraza mantiene el audit JSON local;
el Collector solo exporta su `correlationId` y una marca de detección, no datos de petición ni
detalles de reglas. El Collector y el dashboard usan `service.name = security-edge`; el scrape de
métricas llega por la red privada.

Latencia medida en Compose local (2026-10-30, `blocking`, 3x400 peticiones por caso, ver
[`EVIDENCIA-HU-027.md`](../pdp/docs/ai-harness/workspace/reportes/EVIDENCIA-HU-027.md)): el edge añade
~1,4 ms p50 / ~2,5 ms p95 a `/actuator/health` y ~2,3 ms p50 / ~3,9 ms p95 a `POST /api/v1/authorize`.
La memoria en B1 y el spike de sidecar en Azure siguen **pendientes** (no hay cambio de despliegue de
Azure en esta HU). Memoria local de referencia: edge ~76 MiB, PDP ~436 MiB. El YAML de
[`ci/edge-pipeline.yml`](../ci/edge-pipeline.yml) debe registrarse como pipeline separado en Azure
DevOps antes de que pueda aportar resultados de CI.

## Rutas que el edge no expone

Todo `/internal/*` es **default-deny** hacia el PDP: el edge responde `404` sin cuerpo en `detection` y
`blocking` (regla de enrutamiento, no del WAF; `tests/flows/internal-channel-blocked.hurl`). El canal
PEP→PDP (`bff-session-token`, `access-decisions`, `credential-validations`, `.../administrators`, ...) lo usa el
PEP directo contra el PDP, por la red interna, nunca a través de este edge. Con upstream PEP
(`EDGE_INTERNAL_ALLOW_PEP=true`, fijado solo en `compose.pep.yml`) se permiten exactamente dos rutas propias del
PEP, las que necesita el starter embebido: `PUT /internal/v1/integrations/*/*` y
`POST /internal/v1/embedded-access-decisions`.
