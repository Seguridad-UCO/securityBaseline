# 06. Manejo de parámetros

## Decisión arquitectónica

Body, query y headers tienen una estrategia explícita: DTO validado, criterio/ventana tipados y filtro de correlación.

## Justificación

Parámetros sin normalizar generan validaciones repetidas e interpretaciones distintas. Se descarta pasar `String` libre al dominio.

## Implementación

`@Valid` protege body; `@RequestParam` alimenta `ProtectedApplicationCriteria` y `PageWindow`; `CorrelationWebFilter` lee/genera encabezados. La ausencia o combinación inválida se convierte en `VALIDATION_ERROR`.

## Ubicación verificable

- [`RegisterProtectedApplicationRequest.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web/RegisterProtectedApplicationRequest.java)
- [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web/ProtectedApplicationController.java)
- [`CorrelationWebFilter.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/CorrelationWebFilter.java).

## Evidencia y límite

La prueba HTTP demuestra body, headers y query/rango. La autenticación de headers llegará con Keycloak, sin reutilizar estos IDs como identidad.
