# 18. Paginación

[← Consultas dinámicas](17-dynamic-queries.md) · [Siguiente: rangos →](19-result-ranges.md)

## Decisión arquitectónica

Toda búsqueda está acotada por `PageWindow`. La API ofrece `page` y `size`, con un máximo de 100.

## Justificación

Un catálogo sin límite puede agotar memoria y degradar una API de seguridad. Se descarta devolver
colecciones sin metadatos: el cliente no sabría si hay más.

## Implementación

`SearchProtectedApplicationsQuery` **exige** una `PageWindow`. No existe forma de expresar una
consulta sin límite: el criterio se cumple por construcción del tipo y no por una comprobación que
alguien pueda olvidar. Por eso no hay ninguna regla de negocio que valide el tamaño — sería
redundante con el constructor.

`PageWindow.ofPage(page, size)` calcula el offset con `Math.multiplyExact`, de modo que un `page`
enorme falla en vez de desbordar silenciosamente a un offset negativo.

`ResultPage` devuelve contenido, total y la ventana que lo produjo, y copia el contenido a una lista
inmutable. El total es independiente del tamaño de la página: es el número de filas que cumplen el
criterio, no el número devuelto.

## Ubicación verificable

- [`PageWindow.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/PageWindow.java)
- [`ResultPage.java`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/ResultPage.java)
- [`PageResponse.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/PageResponse.java)
- Pruebas: [`PageWindowTests`](../../src/test/java/co/edu/uco/seguridad/pdp/commons/PageWindowTests.java)

## Evidencia y límite

Las pruebas cubren los límites 1 y 100, el rechazo de 0, 101 y valores negativos, y que el total se
mantiene aunque la ventana devuelva una sola fila. El total del dummy es exacto; la estrategia de
conteo en SurrealDB se decidirá según el volumen real.
