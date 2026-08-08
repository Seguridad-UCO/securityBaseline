# 19. Rangos de resultados

[← Paginación](18-pagination.md) · [↑ Dominio y datos](README.md)

## Decisión arquitectónica

Además de página y tamaño, se admite el rango explícito `offset` + `limit`. Ambas formas convergen
en el mismo `PageWindow`.

## Justificación

Integraciones y pantallas no siempre usan páginas numeradas; a veces el requisito es literalmente
“desde X, tráeme Y”. El rango lo resuelve sin crear otro endpoint ni otro tipo de ventana.

Que ambas formas produzcan el mismo objeto es lo que evita que existan dos caminos de paginación con
dos conjuntos de límites que puedan divergir.

## Implementación

El mapper exige los dos parámetros del rango juntos y rechaza mezclar rango con paginación:

| Query | Resultado |
|---|---|
| `offset=10&limit=5` | `PageWindow.ofRange(10, 5)` |
| `offset=10` | `CONFLICTING_REQUEST_PARAMETERS` |
| `limit=5` | `CONFLICTING_REQUEST_PARAMETERS` |
| `page=1&size=10&offset=0&limit=5` | `CONFLICTING_REQUEST_PARAMETERS` |

Se rechaza en vez de elegir una interpretación: adivinar qué quiso decir el cliente es cómo se
producen resultados silenciosamente equivocados.

La respuesta devuelve las dos representaciones —`page`, `size`, `offset`, `limit`— para que un
cliente pueda continuar en el estilo que prefiera.

## Ubicación verificable

- [`SearchProtectedApplicationsRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/SearchProtectedApplicationsRequest.java)
- [`PageWindow.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/PageWindow.java)
- Pruebas: [`SearchProtectedApplicationsRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/SearchProtectedApplicationsRequestMapperTests.java)
  y [`ProtectedApplicationHttpTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/ProtectedApplicationHttpTests.java)

## Evidencia y límite

Cada fila de la tabla tiene una prueba. La prueba HTTP consulta con `offset=0&limit=1` y comprueba
la metadata devuelta. El límite máximo de 100 aplica igual a rangos y a páginas, porque lo impone el
mismo constructor.
