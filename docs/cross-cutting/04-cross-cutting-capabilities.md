# 04. Capacidades transversales

## Decisión arquitectónica

Mensajes, errores, correlación, logging, métricas/trazas, auditoría y configuración son componentes compartidos, no reglas del módulo de aplicaciones.

## Justificación

Duplicarlos por historia hace que cada módulo responda y registre distinto. Se descarta incrustar infraestructura transversal en el agregado o controller.

## Implementación

`shared/web` concentra envelope y correlación; `shared/observability` integra MDC/Reactor. Actuator/Micrometer instrumenta. Auditoría se define como puerto para que la aplicación la solicite sin saber dónde se almacena.

## Ubicación verificable

- [`shared`](../../src/main/java/co/edu/uco/seguridad/shared)
- [`AuditPort.java`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/out/AuditPort.java)
- [`application.properties`](../../src/main/resources/application.properties)

## Evidencia y límite

La prueba HTTP demuestra headers; los logs de prueba muestran IDs. La política de retención y el backend OTLP se definen al habilitar auditoría productiva.
