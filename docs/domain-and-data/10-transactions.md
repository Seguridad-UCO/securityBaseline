# 10. Manejo de transacciones

[← Reglas e integridad](03-business-rules-data-integrity.md) · [Siguiente: validación →](15-domain-validation.md)

## Decisión arquitectónica

No hay un puerto genérico de transacción. El registro de una aplicación protegida orquesta dos
módulos (`aplicaciones` y `recursos`) y publica un evento de dominio entre medio; cada paso que puede
fallar tiene su **compensación explícita** en vez de estar envuelto en una abstracción transaccional
que prometería una atomicidad que ningún motor involucrado puede dar.

## Justificación

Esto no fue la decisión original: hasta el Stage 3, un puerto `ReactiveTransactionPort` con un
adaptador de snapshot en memoria envolvía todo el flujo, dando la ilusión de una transacción única.
Al llegar la persistencia real ([ADR-0004](../governance/adr/adr-0004-real-persistence-surrealdb.md))
quedó claro que esa ilusión no se sostenía: las transacciones `BEGIN/COMMIT` de SurrealDB solo cubren
un lote de SurrealQL dentro de **una misma petición HTTP**, y el trabajo real del caso de uso cruza
módulos Java y publica eventos, no solo ejecuta sentencias. Mantener el puerto habría significado que
su implementación real no pudiera cumplir el contrato que el nombre prometía. Se prefirió retirarlo y
nombrar el patrón que ya estaba ahí — una saga — en vez de disfrazarlo de transacción.

## Implementación

`RegisterProtectedApplicationUseCaseImpl` encadena dos pasos, cada uno con su propia compensación:

1. **Registrar la aplicación** (módulo `aplicaciones`, vía `RegisterApplicationInteractor`).
2. **Registrar el recurso protegido** (módulo `recursos`, guardado + publicación del evento de
   dominio). Si este paso falla *después* de guardar el recurso, se compensa borrándolo
   (`resources.deleteById(...)`) antes de propagar el error. Si el paso completo falla después de
   registrar la aplicación, se compensa eliminándola (`removeApplicationInteractor.execute(...)`),
   porque vive detrás del límite de otro módulo y no puede unirse a ninguna transacción de este.

No hay snapshot ni rollback de framework: cada compensación es una llamada explícita al mismo puerto
o interactor que hizo el efecto original, encadenada con `onErrorResume`.

## Ubicación verificable

- Flujo y compensación: [`RegisterProtectedApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImpl.java)
- Prueba: `rolls_back_the_saved_resource_and_removes_the_application_when_event_publication_fails` en
  [`RegisterProtectedApplicationUseCaseImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImplTests.java)
- Nota de implementación con el razonamiento completo: [ADR-0004, sección de retiro de `ReactiveTransactionPort`](../governance/adr/adr-0004-real-persistence-surrealdb.md#nota-de-implementación)

## Evidencia y límite

La prueba fuerza un fallo en la publicación del evento y verifica dos cosas: que el recurso guardado
se borró y que se solicitó la eliminación de la aplicación. Cubre la compensación en Java; no cubre
un fallo a mitad de una escritura HTTP individual contra SurrealDB (por ejemplo, la conexión
cayéndose entre el `CREATE` y la respuesta), que queda fuera del alcance de una prueba unitaria y
sería terreno de una prueba de resiliencia de infraestructura.

La compensación entre módulos seguirá siendo necesaria mientras cada módulo tenga su propio límite de
consistencia — eso no depende de qué motor de persistencia haya detrás.
