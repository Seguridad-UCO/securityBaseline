# 05. Manejo de mensajes

## Decisión arquitectónica

Éxitos usan un envelope común con código estable; fallos usan RFC 9457 `ProblemDetail` con código estable y correlación.

## Justificación

Mensajes textuales en cada controller son inconsistentes y frágiles para clientes. Se descarta devolver excepciones o textos técnicos al consumidor.

## Implementación

`ApiResponse` incluye `code`, `message`, `data`, timestamp y correlación. `ApiErrorHandler` traduce error de validación, duplicado y error técnico a respuestas HTTP sin stack trace.

## Ubicación verificable

- [`ApiResponse.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/ApiResponse.java)
- [`ApiErrorHandler.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- Prueba: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

El POST devuelve `APPLICATION_REGISTERED`; duplicados devuelven `APPLICATION_ALREADY_EXISTS`. La taxonomía de códigos crecerá centralmente, no por controlador.
