# 07. Adaptadores de persistencia y auditoría

[← Infraestructura](README.md)


## Estado actual

Desde el Stage 4 ([ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md)) los tres
repositorios secundarios (`TenantRepository`, `ApplicationRepository`,
`ProtectedResourceRepository`) tienen implementaciones **reales** sobre SurrealDB. Este archivo
conserva el nombre `07-dummy-adapters.md` porque otras páginas ya enlazan a él por ruta, pero ya no
describe dummies para esos tres puertos — ver [Historia](#historia-los-dummies-de-los-stages-0-3)
más abajo para lo que había antes.

## Implementación

| Adaptador | Puerto | Qué hace de verdad |
|---|---|---|
| [`SurrealTenantRepository`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/adapter/secondary/persistence/repository/SurrealTenantRepository.java) | `TenantRepository` | Busca un tenant por id vía `SELECT` parametrizado contra SurrealDB |
| [`SurrealApplicationRepository`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/secondary/persistence/repository/SurrealApplicationRepository.java) | `ApplicationRepository` | Unicidad por tenant, alta y baja vía `CREATE`/`DELETE` |
| [`SurrealProtectedResourceRepository`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence/repository/SurrealProtectedResourceRepository.java) | `ProtectedResourceRepository` | Ejecuta la specification como `WHERE` dinámico, ordena y pagina con `ORDER BY ... LIMIT ... START` |

Cada módulo con datos también registra un inicializador de esquema (`ApplicationRunner`) que define
su tabla e índices de forma idempotente en el arranque:
[`SurrealTenantSchemaInitializer`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/adapter/secondary/persistence/schema/SurrealTenantSchemaInitializer.java),
[`SurrealApplicationSchemaInitializer`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/secondary/persistence/schema/SurrealApplicationSchemaInitializer.java),
[`SurrealProtectedResourceSchemaInitializer`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence/schema/SurrealProtectedResourceSchemaInitializer.java).

No hay driver Java de SurrealDB en el classpath: los tres adaptadores hablan HTTP crudo a través del
cliente compartido
[`SurrealDbClient`](../../src/main/java/co/edu/uco/seguridad/shared/persistence/surrealdb/SurrealDbClient.java)
(sobre `WebClient`). El porqué de esta decisión —no había un driver Java viable— está documentado en
la [Nota de implementación de ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md#nota-de-implementación).

`InMemoryAuditAdapter` sigue existiendo y sigue siendo un dummy — solo identificadores, nunca el
payload — pero no implementa ningún puerto: desde el Stage 2 escucha `ProtectedResourceRegistered`
con `@EventListener` en vez de que el caso de uso la invoque por un puerto de auditoría (ver
[ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md)). Sustituirla por un
sumidero real (log estructurado, sistema externo) es trabajo futuro fuera del alcance de las cuatro
etapas actuales.

Los repositorios reales siguen sin devolver **entidades de persistencia** directamente al dominio:
`SurrealTenantRepository` y `SurrealProtectedResourceRepository` mapean la fila SurrealDB a la
misma `Entity`+`Mapper` que usaban sus predecesores dummy, así que el mapper existe y se ejerce
igual que antes — sustituir el almacén no cambió esa disciplina.

## Ubicación verificable

- [`resources/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence)
- [`resources/infrastructure/adapter/secondary/audit`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/audit)
- [`applications/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/secondary/persistence)
- [`tenants/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/adapter/secondary/persistence)
- [`shared/persistence/surrealdb`](../../src/main/java/co/edu/uco/seguridad/shared/persistence/surrealdb) (cliente HTTP compartido)
- Configuración: [`ResourcesConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/config/ResourcesConfiguration.java), [`ApplicationsConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/config/ApplicationsConfiguration.java), [`TenantsConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/config/TenantsConfiguration.java)

## Evidencia y límite

Las pruebas de integración con contexto de Spring completo (`ApplicationHttpTests`,
`SecurityWebFilterChainTests`, `PdpApplicationTests`, `InMemoryAuditAdapterTests`)
corren contra una SurrealDB real provista por Testcontainers — ver
[`AbstractSurrealDbIntegrationTest`](../../src/test/java/co/edu/uco/seguridad/AbstractSurrealDbIntegrationTest.java).
`SurrealRepositoryIntegrationTests` ejercita los tres repositorios directamente (sin levantar el
contexto de Spring) contra el mismo contenedor compartido, cubriendo casos que el flujo HTTP no
ejercita explícitamente: existencia antes/después de guardar, borrado idempotente y paginación.

## Historia: los dummies de los Stages 0-3

Hasta el Stage 4, los tres repositorios eran implementaciones en memoria **detrás de los mismos
puertos**, con comportamiento suficiente para probar registro, búsqueda y rollback sin bloquear la
validación de la arquitectura por la ausencia de infraestructura real:

| Adaptador (retirado) | Puerto | Qué hacía |
|---|---|---|
| `InMemoryTenantRepository` | `TenantRepository` | Servía el catálogo de tenants desde configuración |
| `InMemoryApplicationRepository` | `ApplicationRepository` | Unicidad por tenant sin distinguir mayúsculas |
| `InMemoryProtectedResourceRepository` | `ProtectedResourceRepository` | Ejecutaba la specification, ordenaba y paginaba |
| `SnapshotReactiveTransactionAdapter` | `ReactiveTransactionPort` | Copiaba y restauraba el mapa en memoria ante error |

El acoplamiento entre la configuración y el tipo concreto del dummy —que existía porque el adaptador
de transacción necesitaba la capacidad de snapshot que el puerto de repositorio deliberadamente no
declaraba— se resolvió en el Stage 1 con un puerto dedicado, `SnapshotCapable`. Ambos,
`SnapshotCapable` y `ReactiveTransactionPort`, se **retiraron por completo** en el Stage 4 en vez de
implementarse sobre SurrealDB: el modelo de transacción HTTP de SurrealDB no puede envolver trabajo
que cruza módulos Java, así que `RegisterApplicationUseCaseImpl` pasó a una saga con
compensación explícita por paso — ver la
[Nota de implementación de ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md#nota-de-implementación).
