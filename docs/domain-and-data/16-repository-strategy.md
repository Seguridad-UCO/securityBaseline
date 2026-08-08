# 16. Estrategia de repositorios

[← Validación](15-domain-validation.md) · [Siguiente: consultas dinámicas →](17-dynamic-queries.md)

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

Un único método de lectura. La semántica del criterio la resuelve el dominio; el adaptador elige
cómo ejecutarla: hoy un recorrido en memoria, mañana un índice o SurrealQL.

`existsGrant` es la excepción deliberada: la unicidad es una pregunta de sí o no, y responderla con
un índice es más barato que cargar la fila para descartarla.

Las escrituras (`save`, `deleteById`) hablan en tipos de dominio. La traducción a la fila la hace el
mapper de persistencia, de modo que el esquema puede evolucionar sin tocar el modelo.

El orden de las páginas se fija por instante de registro y luego por identificador. Sin un orden
estable, dos páginas consecutivas podrían repetir u omitir filas según el orden de hash.

## Ubicación verificable

- [`ProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/out/ProtectedResourceRepository.java)
- [`InMemoryProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/repository/InMemoryProtectedResourceRepository.java)
- Mapper: [`ProtectedResourcePersistenceMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/mapper/ProtectedResourcePersistenceMapper.java)

## Evidencia y límite

[`SearchProtectedApplicationsUseCaseImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/SearchProtectedApplicationsUseCaseImplTests.java)
comprueba que las páginas son estables y no se solapan. El contrato cubre hoy igualdad y `contains`;
operadores relacionales e `IN/NOT IN` se agregarán como campos nuevos del criterio cuando el modelo
los necesite, sin cambiar esta firma.
