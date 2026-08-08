# 10. Manejo de transacciones

[← Reglas e integridad](03-business-rules-data-integrity.md) · [Siguiente: validación →](15-domain-validation.md)

## Decisión arquitectónica

La frontera transaccional es el puerto `ReactiveTransactionPort`. El registro y su evidencia de
auditoría ocurren dentro de una sola unidad lógica de trabajo.

## Justificación

Guardar el recurso y fallar al generar la evidencia deja un estado no auditable. Anotar el servicio
con una transacción de framework lo acoplaría a una tecnología que todavía no existe en el proyecto.

## Implementación

El caso de uso delimita la operación con `transaction.execute(...)`.

Hay **dos mecanismos de recuperación** porque hay dos almacenes, y conviene que eso sea visible en
vez de estar escondido:

1. `SnapshotReactiveTransactionAdapter` copia el almacén de `recursos` antes del trabajo y lo
   restaura si el `Mono` termina en error.
2. La eliminación de la aplicación es una **compensación explícita**, porque vive detrás del límite
   de otro módulo y no puede unirse a esta transacción. Es una saga, y nombrarla como tal evita que
   alguien suponga una atomicidad que no existe.

Detalle que hace correcto el rollback: el puerto recibe `Supplier<Mono<T>>` y no un `Mono<T>` ya
ensamblado, y toma la copia dentro de `Mono.defer`. Así el snapshot corresponde al momento de la
suscripción; con un `Mono` ya armado se restauraría un estado equivocado.

## Ubicación verificable

- Puerto: [`ReactiveTransactionPort.java`](../../src/main/java/co/edu/uco/seguridad/shared/port/ReactiveTransactionPort.java)
- Adaptador: [`SnapshotReactiveTransactionAdapter.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/transaction/SnapshotReactiveTransactionAdapter.java)
- Uso: [`RegisterProtectedApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImpl.java)
- Prueba: `rolls_back_the_saved_resource_and_removes_the_application_when_audit_fails` en
  [`RegisterProtectedApplicationUseCaseImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImplTests.java)

## Evidencia y límite

La prueba fuerza un fallo de auditoría y verifica dos cosas: que el almacén de recursos quedó vacío
y que se solicitó la eliminación de la aplicación.

El rollback es demostrativo y exclusivo del dummy. La implementación SurrealDB deberá implementar el
mismo puerto con su transacción real y sus propias pruebas de aislamiento; la compensación entre
módulos seguirá siendo necesaria mientras cada módulo tenga su propio almacén.
