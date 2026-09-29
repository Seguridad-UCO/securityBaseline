# 08. Logging e instrumentación

[← Capacidades transversales](04-cross-cutting-capabilities.md) · [Siguiente: excepciones →](09-exception-handling.md)

## Decisión arquitectónica

Logging estructurado con MDC alimentado desde el Reactor Context, más Actuator y el bridge
Micrometer/OpenTelemetry. Cada caso de uso emite un evento con nombre estable.

## Justificación

Una plataforma de seguridad debe poder reconstruir el recorrido de una petición. Se descarta loguear
tokens o payloads completos, y también depender de mensajes sin correlación.

## Implementación

`CorrelationWebFilter` lee o genera `X-Request-Id` y `X-Correlation-Id`, los escribe en el Reactor
Context y los devuelve en la respuesta.

`ReactiveLogContext` los traslada al MDC **solo mientras se ejecuta la sentencia de log**, con
`MDC.putCloseable`. Poblar el MDC durante toda la cadena reactiva sería incorrecto: en un modelo no
bloqueante el hilo cambia, y los identificadores acabarían adheridos al hilo equivocado.

Los casos de uso se instrumentan con `.transform(ReactiveLogContext.withContext(LOG, evento))`, que
registra éxito o error sin ensuciar la lógica con `doOnSuccess`/`doOnError` repetidos. Eventos
actuales: `application.register`, `application.remove`, `protected_application.register`,
`protected_application.search`.

En caso de error se registra el **tipo** de la excepción, no su mensaje: el tipo basta para
diagnosticar y no arrastra datos del negocio al log.

> Corrección respecto a versiones anteriores de este documento: no se crea una `Observation` de
> Micrometer a mano. La instrumentación de WebFlux ya produce el tramo por petición, y añadir una
> observación manual duplicaría el tramo. El bridge OTEL permite exportar a un colector configurado
> externamente.

## Ubicación verificable

- [`CorrelationWebFilter.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/CorrelationWebFilter.java)
- [`ReactiveLogContext.java`](../../src/main/java/co/edu/uco/seguridad/shared/observability/ReactiveLogContext.java)
- Uso: [
  `RegisterApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java)
- [`pom.xml`](../../pom.xml) y [`application.properties`](../../src/main/resources/application.properties)

## Evidencia y límite

La prueba HTTP envía `X-Request-Id: req-42` y comprueba que vuelve en la respuesta. La salida de
`mvn verify` muestra los eventos con sus identificadores en el patrón de log configurado.

No se configura un endpoint OTLP local para no enviar telemetría fuera de la línea base. El token
del colector ya tiene nombre reservado en Key Vault (`pdp-otlp-token`).
