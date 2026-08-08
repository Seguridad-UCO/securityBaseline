# 06. Manejo de parámetros

[← Mensajes](05-message-handling.md) · [Siguiente: DTOs →](13-input-strategy-dtos.md)

## Decisión arquitectónica

Body, query y headers tienen cada uno una estrategia explícita: DTO en dos niveles para el body,
DTO en dos niveles para la query, y filtro de correlación para los headers.

## Justificación

Parámetros sin normalizar generan validaciones repetidas e interpretaciones distintas. Se descarta
pasar `String` libre al dominio.

## Implementación

Todos los `@RequestParam` se declaran `required = false` y de tipo `String`. Es deliberado: si el
framework convirtiera `?page=primera` a `int`, fallaría antes de llegar a nuestro código y el
cliente recibiría un error que no controlamos. Interpretar esas cadenas es trabajo del mapper.

La ventana de resultados se valida en **un solo setter** que recibe los cuatro parámetros, porque su
validez es una propiedad de la combinación y no de ningún valor por separado — un setter por
parámetro no podría ver que llegó `offset` sin `limit`.

Combinaciones y su resultado:

| Query | Resultado |
|---|---|
| sin parámetros | ventana por defecto (offset 0, limit 20) |
| `page=2&size=25` | `PageWindow.ofPage(2, 25)` |
| `page=3` | tamaño por defecto |
| `offset=10&limit=5` | `PageWindow.ofRange(10, 5)` |
| `offset=10` | `CONFLICTING_REQUEST_PARAMETERS` |
| `page=1&size=10&offset=0&limit=5` | `CONFLICTING_REQUEST_PARAMETERS` |
| `size=500` | `MALFORMED_REQUEST_FIELD` |
| `page=primera` | `MALFORMED_REQUEST_FIELD` |

`CorrelationWebFilter` lee o genera `X-Request-Id` y `X-Correlation-Id`, los propaga por el Reactor
Context y los devuelve en la respuesta, incluidas las de error.

## Ubicación verificable

- [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/controller/ProtectedApplicationController.java)
- [`SearchProtectedApplicationsRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/SearchProtectedApplicationsRequest.java)
- [`CorrelationWebFilter.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/CorrelationWebFilter.java)
- Pruebas: [`SearchProtectedApplicationsRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/SearchProtectedApplicationsRequestMapperTests.java)

## Evidencia y límite

Cada fila de la tabla anterior tiene una prueba. La prueba HTTP comprueba además que `X-Request-Id`
se refleja en la respuesta. La autenticación por headers llegará con Keycloak y no reutilizará estos
identificadores como identidad.
