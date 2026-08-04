# 18. Paginación

## Decisión arquitectónica

Todas las búsquedas se limitan con `PageWindow`; la API ofrece `page` y `size` con máximo 100.

## Justificación

Un catálogo sin límite puede consumir memoria y degradar una API de seguridad. Se descarta devolver colecciones sin metadatos.

## Implementación

`PageWindow.page(page,size)` calcula el offset y valida valores. `ApplicationPage` devuelve contenido, total, offset y límite; el controller lo traduce a `PageResponse`.

## Ubicación verificable

- [`PageWindow.java`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/out/PageWindow.java)
- [`ApplicationPage.java`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/out/ApplicationPage.java)
- [Contrato de consulta](../interfaces/06-parameter-handling.md).

## Evidencia y límite

El límite 1..100 se prueba por constructor y evita cargas no acotadas. El total dummy es exacto; la estrategia de conteo en SurrealDB se decidirá según volumen.
