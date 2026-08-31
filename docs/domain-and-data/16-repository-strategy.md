# 16. Estrategia de repositorios

[← Validación](15-domain-validation.md) · [Siguiente: consultas dinámicas →](17-dynamic-queries.md)

> **Estado — 2026-08-31.** Lo que este documento describe sobre la **búsqueda con criterios y
> paginación** (`ProtectedApplicationCriteria`, `SearchProtectedApplicationsUseCase` y sus mappers y
> pruebas) es el **diseño acordado, no código existente**. `PageWindow`, `ResultPage` y `PageResponse`
> existen y están probados, pero ningún caso de uso los usa todavía. Pendiente de la historia
> **HU-001**; ver [el harness](../ai-harness/README.md).

## Decisión arquitectónica

El repositorio expone búsqueda por objeto criterio y ventana, no métodos específicos como
`findByName` o `findByTenantAndName`.

## Justificación

El catálogo crecerá en atributos y combinaciones. Multiplicar métodos convierte cada filtro nuevo en
un cambio de la API del puerto y de todos sus adaptadores.

## Implementación

```java
Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window);
```

Un único método de lectura. La semántica del criterio la resuelve el dominio; el adaptador la traduce
a su motor: `SurrealProtectedResourceRepository` la traduce a un `WHERE` dinámico de SurrealQL —
ver [ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md), que fija SurrealDB como
motor y documenta que este puerto no cambió de forma al sustituir el adaptador dummy por el real.

`existsGrant` es la excepción deliberada: la unicidad es una pregunta de sí o no, y responderla con
un índice es más barato que cargar la fila para descartarla.

Las escrituras (`save`, `deleteById`) hablan en tipos de dominio. La traducción a la fila la hace el
mapper de persistencia, de modo que el esquema puede evolucionar sin tocar el modelo.

El orden de las páginas se fija por instante de registro y luego por identificador. Sin un orden
estable, dos páginas consecutivas podrían repetir u omitir filas según el orden de hash.

## Ubicación verificable

- [`ProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/port/secondary/repository/ProtectedResourceRepository.java)
- [`SurrealProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence/repository/SurrealProtectedResourceRepository.java)
  — construye el `WHERE` a partir del criterio y ejecuta en el mismo lote HTTP la página y el conteo
  total (`SELECT ... GROUP ALL`), para que ambos vean el mismo estado.
- Mapper: [`ProtectedResourcePersistenceMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence/mapper/ProtectedResourcePersistenceMapper.java)

## Evidencia y límite

`SearchProtectedApplicationsUseCaseImplTests` *(no implementado)*
comprueba, con un doble en memoria, que las páginas son estables y no se solapan.
[`SurrealRepositoryIntegrationTests`](../../src/test/java/co/edu/uco/seguridad/shared/persistence/surrealdb/SurrealRepositoryIntegrationTests.java)
ejercita el mismo contrato contra una SurrealDB real (Testcontainers), incluyendo el conteo total
tras guardar y borrar. El contrato cubre hoy igualdad y `contains`; operadores relacionales e
`IN/NOT IN` se agregarán como campos nuevos del criterio cuando el modelo los necesite, sin cambiar
esta firma.
