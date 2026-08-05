# 07. Adaptadores dummy

## Decisión arquitectónica

Mientras SurrealDB y auditoría real no existen, se usan implementaciones en memoria detrás de puertos, con comportamiento suficiente para probar registro, búsqueda, auditoría y rollback.

## Justificación

Esperar infraestructura real bloquearía la validación de arquitectura. Simularla dentro del servicio invalidaría la sustitución. Se descarta presentar el dummy como fuente de verdad productiva.

## Implementación

`InMemoryProtectedApplicationRepository` implementa el repositorio con `ConcurrentHashMap`; `InMemoryAuditAdapter` guarda eventos; `SnapshotReactiveTransactionAdapter` hace rollback de la memoria. La configuración conecta estos adapters como beans.

## Ubicación verificable

- [`persistence/dummy`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`audit/dummy`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ApplicationConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- Prueba de rollback: [`ProtectedApplicationServiceTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

El dummy hace funcionar el endpoint y las pruebas sin DB. Se reemplaza por adaptadores SurrealDB/auditoría reales sin cambiar el dominio, casos de uso ni controller.
