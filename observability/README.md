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
