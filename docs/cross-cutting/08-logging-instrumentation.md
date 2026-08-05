# 08. Logging e instrumentación

## Decisión arquitectónica

Se usa logging estructurado con MDC, Reactor Context, Actuator y Micrometer/OpenTelemetry. Cada registro de aplicación crea la observación `protected_application.register`.

## Justificación

Una plataforma de seguridad debe reconstruir el recorrido de una petición. Se descarta loguear tokens/datos completos o depender de mensajes no correlacionados.

## Implementación

`CorrelationWebFilter` propaga IDs; `ReactiveLogContext` los inserta en MDC solamente al registrar. El servicio crea `Observation`; el bridge OTEL permite exportar a un collector configurado externamente. Actuator expone health y metrics.

## Ubicación verificable

- [`CorrelationWebFilter.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/CorrelationWebFilter.java)
- [`ReactiveLogContext.java`](../../src/main/java/co/edu/uco/seguridad/shared/observability/ReactiveLogContext.java)
- [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`pom.xml`](../../pom.xml) y [`application.properties`](../../src/main/resources/application.properties).

## Evidencia y límite

La salida de `mvn test` contiene `request-test-1` y `correlation-test-1` en el evento de registro. No se configura endpoint OTLP local para no enviar telemetría fuera de la línea base.
