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
| `InMemoryAuditAdapter` | `AuditPort` | Registra identificadores y momento, nunca el payload |
| `SnapshotReactiveTransactionAdapter` | `ReactiveTransactionPort` | Copia y restaura ante error |

Los dummies almacenan **entidades de persistencia**, no objetos de dominio. Eso no es ceremonia: es
lo que obliga a que el mapper exista y se ejerza desde el primer día, de modo que sustituir el
almacén no descubra después que el modelo estaba acoplado a la fila.

El adaptador de auditoría guarda solo identificadores. Una auditoría que copiara el payload se
convertiría en una segunda copia de los datos que la plataforma debe proteger.

Un solo detalle de acoplamiento, y está confinado: la configuración expone el repositorio de
recursos por su tipo concreto, porque el adaptador de transacción necesita la capacidad de snapshot
que el puerto deliberadamente no declara. Desaparece con el dummy.

## Ubicación verificable

- [`recursos/infrastructure/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/persistence)
- [`recursos/infrastructure/audit`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/audit)
- [`aplicaciones/infrastructure/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/aplicaciones/infrastructure/persistence)
- [`tenants/infrastructure/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/infrastructure/persistence)
- Configuración: [`ResourcesConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/config/ResourcesConfiguration.java)

## Evidencia y límite

La prueba de rollback usa el adaptador de transacción y el repositorio reales del dummy, no un mock:
verifica el comportamiento, no la interacción. Los dummies se reemplazan por adaptadores SurrealDB y
de auditoría reales sin cambiar dominio, casos de uso, reglas ni controlador.
