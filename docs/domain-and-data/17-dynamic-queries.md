# 17. Construcción dinámica de consultas

[← Repositorios](16-repository-strategy.md) · [Siguiente: paginación →](18-pagination.md)


## Decisión arquitectónica

La consulta se construye en tiempo de ejecución desde parámetros opcionales convertidos en
`ApplicationCriteria`.

## Justificación

Un endpoint por combinación de filtros no escala: tres filtros opcionales darían ocho endpoints, y
cada filtro nuevo duplicaría ese número. La construcción dinámica conserva una sola operación de
catálogo.

## Implementación

```text
Authorization: Bearer <jwt>          →  interactor: SecurityContext.currentPrincipal() → tenant
?nameContains=&resourceContains=
        ↓  controller: los recibe como String, sin interpretarlos
        ↓  mapper: normaliza a Optional, construye los value objects y añade el tenant del principal
ApplicationCriteria(tenantId obligatorio, nameContains, resourceContains)
        ↓  use case: la entrega sin inspeccionarla
repositorio: decide cómo ejecutarla
```

El controlador **no** construye la consulta: solo entrega parámetros. El caso de uso tampoco la
inspecciona — si lo hiciera, estaría duplicando la lógica de filtrado que ya vive en el criterio.

`tenantId` no es un parámetro de la query desde ADR-0003: es obligatorio, pero sale del token, no de
algo que el llamador pueda omitir o cambiar. `nameContains`/`resourceContains` siguen siendo
opcionales; cada uno ausente equivale a "no restringir" dentro del tenant. Un fragmento en blanco
cuenta como ausente.

## Ubicación verificable

- `ListApplicationsRequestMapper.java`
- `ApplicationCriteria.java`
- `ListApplicationsUseCaseImpl.java`

## Evidencia y límite

Las pruebas del mapper verifican que solo los filtros presentes llegan al criterio, y las del caso
de uso que la combinación es conjuntiva. La semántica `contains` está documentada aquí y deberá ser
equivalente en el adaptador SurrealDB.
