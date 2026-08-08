# 07. Adaptadores dummy

[← Infraestructura](README.md)

## Decisión arquitectónica

Mientras SurrealDB y la auditoría real no existan, se usan implementaciones en memoria **detrás de
puertos**, con comportamiento suficiente para probar registro, búsqueda, auditoría y rollback.

## Justificación

Esperar la infraestructura real bloquearía la validación de la arquitectura. Simularla dentro del
servicio invalidaría la sustitución que se quiere demostrar. Se descarta presentar el dummy como
fuente de verdad productiva.

## Implementación

| Adaptador | Puerto | Qué hace de verdad |
|---|---|---|
| `InMemoryTenantRepository` | `TenantRepository` | Sirve el catálogo de tenants desde configuración |
| `InMemoryApplicationRepository` | `ApplicationRepository` | Unicidad por tenant sin distinguir mayúsculas |
| `InMemoryProtectedResourceRepository` | `ProtectedResourceRepository` | Ejecuta la specification, ordena y pagina |
| `SnapshotReactiveTransactionAdapter` | `ReactiveTransactionPort` | Copia y restaura ante error |

`InMemoryAuditAdapter` ya no está en esta tabla porque no implementa ningún puerto: desde el Stage 2
escucha `ProtectedResourceRegistered` con `@EventListener` en vez de que el caso de uso la invoque
por un puerto de auditoría (ver [ADR-0002](../governance/adr/adr-0002-domain-events-modulith-registry.md)).
Sigue siendo un dummy — solo identificadores, nunca el payload — pero ya no es un "adaptador detrás de
un puerto" en el mismo sentido que los demás; es un consumidor de eventos.

Los dummies restantes almacenan **entidades de persistencia**, no objetos de dominio. Eso no es
ceremonia: es lo que obliga a que el mapper exista y se ejerza desde el primer día, de modo que
sustituir el almacén no descubra después que el modelo estaba acoplado a la fila.

El acoplamiento entre la configuración y el tipo concreto del dummy —que existía porque el adaptador
de transacción necesitaba la capacidad de snapshot que el puerto de repositorio deliberadamente no
declara— se resolvió en el Stage 1 con un puerto dedicado,
[`SnapshotCapable`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/transaction/SnapshotCapable.java):
`InMemoryProtectedResourceRepository` lo implementa junto con `ProtectedResourceRepository`, y
`ResourcesConfiguration` cablea cada punto de inyección por el contrato que le corresponde, nunca por
el tipo concreto. `SnapshotCapable` desaparece junto con el dummy cuando la persistencia real
implemente `ReactiveTransactionPort` con la transacción propia del motor
([ADR-0004](../governance/adr/adr-0004-real-persistence-surrealdb.md)).

## Ubicación verificable

- [`recursos/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence)
- [`recursos/infrastructure/adapter/secondary/audit`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/audit)
- [`aplicaciones/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/aplicaciones/infrastructure/adapter/secondary/persistence)
- [`tenants/infrastructure/adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/adapter/secondary/persistence)
- Configuración: [`ResourcesConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/config/ResourcesConfiguration.java)

## Evidencia y límite

La prueba de rollback usa el adaptador de transacción y el repositorio reales del dummy, no un mock:
verifica el comportamiento, no la interacción. Los dummies se reemplazan por adaptadores SurrealDB y
de auditoría reales sin cambiar dominio, casos de uso, reglas ni controlador — ver
[ADR-0004](../governance/adr/adr-0004-real-persistence-surrealdb.md) y
[ADR-0002](../governance/adr/adr-0002-domain-events-modulith-registry.md) (auditoría por eventos).
