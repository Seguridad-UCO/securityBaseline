# 09. Manejo de excepciones

## Decisión arquitectónica

Las excepciones de dominio se expresan en el núcleo y se traducen una única vez en el adaptador HTTP.

## Justificación

`try/catch` dispersos ocultan causas y generan respuestas distintas. Se descarta que el dominio devuelva códigos HTTP o `ResponseEntity`.

## Implementación

`DomainException` representa regla violada; `DuplicateProtectedApplicationException` representa conflicto funcional. `ApiErrorHandler` aplica mapeo: validación 400, duplicado 409 y fallo inesperado 500, todos sin datos técnicos sensibles.

## Ubicación verificable

- [`DomainException.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`DuplicateProtectedApplicationException.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ApiErrorHandler.java`](../../src/main/java/co/edu/uco/seguridad/pdp)

## Evidencia y límite

El error de duplicado es verificable al repetir el POST. Nuevos tipos técnicos se mapearán centralmente, preservando el contrato de códigos.
