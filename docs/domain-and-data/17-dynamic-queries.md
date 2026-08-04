# 17. Construcción dinámica de consultas

## Decisión arquitectónica

La consulta se construye desde parámetros opcionales convertidos en `ProtectedApplicationCriteria` en tiempo de ejecución.

## Justificación

Un endpoint por combinación de filtros no escala y rompe el contrato. La construcción dinámica conserva una sola operación de catálogo.

## Implementación

`tenantId`, `nameContains` y `resourceContains` se normalizan a `Optional`. `matches` aplica solo los que existan. El controller no construye una consulta de memoria/BD: crea el objeto de dominio y lo entrega al caso de uso.

## Ubicación verificable

- [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web/ProtectedApplicationController.java)
- [`ProtectedApplicationCriteria.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ProtectedApplicationCriteria.java)

## Evidencia y límite

La prueba HTTP usa `tenantId` junto con rango. La semántica `contains` está documentada y será equivalente en el adaptador SurrealDB.
