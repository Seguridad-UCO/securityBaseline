# Observabilidad de operación

Esta instalación recibe las señales reales de PDP y PEP. El compose consume la configuración local
en `.env`, arranca Collector, Prometheus, Loki, Jaeger y Grafana, y conserva sus datos en volúmenes
Docker. Grafana queda disponible en `http://127.0.0.1:3001` con el usuario configurado en `.env`.

PDP y PEP deben arrancar con el perfil `observability`. En este repositorio sus archivos JSON se
escriben directamente en `observability/logs/`, que el collector monta en solo lectura. Sus puertos
de management quedan en `127.0.0.1:9080` y `127.0.0.1:9081`; el collector los alcanza mediante
`host.docker.internal`, ya configurado en `.env`.

La pila se inicia desde este directorio con `docker compose up -d`. La operación está lista cuando
el health check del collector responde en `http://127.0.0.1:13133/` y Grafana carga el dashboard
`Plataforma de seguridad · Observabilidad`. Al arrancar PDP y PEP, el panel `Servicios que reportan
métricas` muestra ambos procesos al llegar sus métricas al collector.

Una solicitud que atraviese PEP y PDP debe producir métricas HTTP y de autorización, el contador
`security_access_events_total`, el evento `security.access.event.recorded` filtrable por
`correlationId` en Loki y su traza W3C correspondiente en Jaeger. El contrato y los límites de
privacidad están en [TELEMETRY-CONVENTION.md](TELEMETRY-CONVENTION.md).

## Observabilidad del WAF (HU-034)

Cada detección o bloqueo del edge llega a Loki con `service_name="security-edge"`, la regla, la
puntuación, la ruta y el `correlationId` (esquema en [TELEMETRY-CONVENTION.md](TELEMETRY-CONVENTION.md)).
Consultas de ejemplo:

```logql
{service_name="security-edge"} | correlationId="<id>"
{service_name="security-edge"} | edge_rule_ids=~".*941100.*" | edge_path="/api/v1/applications"
{service_name=~"security-(edge|pdp)"} | correlationId="<id>"
```

El dashboard `security-platform` incluye el panel 15 (detecciones), el 16 (por regla principal) y el 17
(por ruta). "Por regla" es la regla principal (`edge.rule_main`); el conjunto completo está en
`edge_rule_ids`.

### Prueba de punta a punta

`tests/Test-WafObservability.ps1` levanta una pila efímera (`compose.test.yml`: edge + stub de PDP +
Collector + Loki + Grafana, proyecto `hu034-obs-test`), ataca el edge y comprueba Loki y Grafana. No
descarga imágenes (exige que `security-edge:local`, `alpine:3.20`, el Collector, Loki y Grafana ya estén
locales) y solo derriba su propio proyecto.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File observability	ests\Test-WafObservability.ps1
# opciones: -KeepStack, -SkipBlocking, -SkipRotation, -Fases deteccion,bloqueo,rotacion
```

## Sin root

Ningún servicio de este stack corre como root. El colector y Jaeger usan el uid 10001; un paso efímero (`volume-permissions`, sin red) entrega a ese uid sus volúmenes nombrados antes de arrancar y conserva los datos que ya existan (offsets del colector, trazas de Jaeger).

Al actualizar desde una versión anterior: los archivos de `logs/` que escribió un contenedor root con modo 600 (p. ej. `security-edge.json`) no los puede leer el colector hasta que cambien de dueño: `chown 10001:10001 logs/security-edge.json` (o bórralos si ya no importan). Los que escriben el edge y el PDP desde ahora ya nacen con el uid 10001.
