# 10. Manejo de transacciones

## Decisión arquitectónica

La frontera transaccional es `ReactiveTransactionPort`; registrar y auditar ocurre dentro de una sola unidad lógica de trabajo.

## Justificación

Guardar la aplicación y fallar al generar evidencia deja un estado no auditable. Anotar directamente el servicio con una transacción de framework lo acoplaría a una tecnología que aún no existe.

## Implementación

`SnapshotReactiveTransactionAdapter` toma copia del almacén dummy antes del trabajo y la restaura si el `Mono` termina en error. El servicio delimita la operación mediante `transaction.execute(...)`.

## Ubicación verificable

- Puerto: [`ReactiveTransactionPort.java`](../../src/main/java/co/edu/uco/seguridad/pdp).
- Dummy: [`SnapshotReactiveTransactionAdapter.java`](../../src/main/java/co/edu/uco/seguridad/pdp).
- Evidencia: prueba `rolls_back_save_when_audit_fails` en [`ProtectedApplicationServiceTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

El rollback es demostrativo y exclusivo del dummy. La implementación SurrealDB deberá implementar el mismo puerto usando su transacción real y pruebas de aislamiento.
