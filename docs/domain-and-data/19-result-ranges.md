# 19. Rangos de resultados

## Decisión arquitectónica

Además de página/tamaño, se admite el rango explícito `offset` + `limit`.

## Justificación

Integraciones y pantallas no siempre usan páginas numeradas. El rango permite “desde X, traer Y” sin crear otro endpoint.

## Implementación

El controller exige ambos parámetros juntos y los transforma a `PageWindow`. No permite semántica ambigua (`offset` sin `limit`).

## Ubicación verificable

- [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`PageWindow.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- Prueba HTTP: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

La prueba consulta `offset=0&limit=1`. La API limita el valor a 100 para conservar el control de recursos.
