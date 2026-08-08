# 17. Construcción dinámica de consultas

[← Repositorios](16-repository-strategy.md) · [Siguiente: paginación →](18-pagination.md)

## Decisión arquitectónica

La consulta se construye en tiempo de ejecución desde parámetros opcionales convertidos en
`ProtectedApplicationCriteria`.

## Justificación

Un endpoint por combinación de filtros no escala: tres filtros opcionales darían ocho endpoints, y
cada filtro nuevo duplicaría ese número. La construcción dinámica conserva una sola operación de
catálogo.

## Implementación

```text
?tenantId=&nameContains=&resourceContains=
        ↓  controller: los recibe como String, sin interpretarlos
        ↓  mapper: normaliza a Optional y construye los value objects
ProtectedApplicationCriteria
        ↓  use case: la entrega sin inspeccionarla
repositorio: decide cómo ejecutarla
```

El controlador **no** construye la consulta: solo entrega parámetros. El caso de uso tampoco la
inspecciona — si lo hiciera, estaría duplicando la lógica de filtrado que ya vive en el criterio.

Cada filtro ausente equivale a “no restringir”. Un fragmento en blanco cuenta como ausente.

## Ubicación verificable

- [`SearchProtectedApplicationsRequestMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/SearchProtectedApplicationsRequestMapper.java)
- [`ProtectedApplicationCriteria.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/domain/ProtectedApplicationCriteria.java)
- [`SearchProtectedApplicationsUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/SearchProtectedApplicationsUseCaseImpl.java)

## Evidencia y límite

Las pruebas del mapper verifican que solo los filtros presentes llegan al criterio, y las del caso
de uso que la combinación es conjuntiva. La semántica `contains` está documentada aquí y deberá ser
equivalente en el adaptador SurrealDB.
