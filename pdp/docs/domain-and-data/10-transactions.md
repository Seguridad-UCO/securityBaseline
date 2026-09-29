# 10. Manejo de transacciones

[← Reglas e integridad](03-business-rules-data-integrity.md) · [Siguiente: validación →](15-domain-validation.md)

## Decisión arquitectónica

No hay un puerto genérico de transacción. El registro de una aplicación protegida orquesta dos
módulos (`applications` y `resources`) y publica un evento de dominio entre medio; cada paso que puede
fallar tiene su **compensación explícita** en vez de estar envuelto en una abstracción transaccional
que prometería una atomicidad que ningún motor involucrado puede dar.

## Justificación

Esto no fue la decisión original: hasta el Stage 3, un puerto `ReactiveTransactionPort` con un
adaptador de snapshot en memoria envolvía todo el flujo, dando la ilusión de una transacción única.
Al llegar la persistencia
real ([ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md))
quedó claro que esa ilusión no se sostenía: las transacciones `BEGIN/COMMIT` de SurrealDB solo cubren
un lote de SurrealQL dentro de **una misma petición HTTP**, y el trabajo real del caso de uso cruza
módulos Java y publica eventos, no solo ejecuta sentencias. Mantener el puerto habría significado que
su implementación real no pudiera cumplir el contrato que el nombre prometía. Se prefirió retirarlo y
nombrar el patrón que ya estaba ahí — una saga — en vez de disfrazarlo de transacción.

## Implementación

`RegisterApplicationWithInitialResourceUseCaseImpl` (slice `resources`, HU-010 — endpoint `POST
/api/v1/applications/with-initial-resource`) encadena dos pasos, cada uno con su propia
compensación:

1. **Registrar la aplicación** (módulo `applications`, vía `RegisterApplicationUseCase`).
2. **Registrar el recurso protegido** (módulo `resources`, vía `RegisterProtectedResourceUseCase`).
   Si este paso falla *después* de que la aplicación ya se guardó, se compensa eliminándola
   (`RemoveApplicationUseCase.execute(applicationId)`) antes de propagar el error del recurso —
   nunca el de la compensación. Si la propia compensación también falla, se registra por log
   (`LOG.error`, con el `applicationId` huérfano) sin cambiar el error que llega al cliente.

No hay snapshot ni rollback de framework: la compensación es una llamada explícita al mismo caso de
uso que ya existía como operación compensatoria sin invocar (`RemoveApplicationUseCase`),
encadenada con `onErrorResume`. Los dos endpoints que registran cada paso por separado (`POST
/api/v1/applications` y `POST /api/v1/applications/{id}/resources`) siguen existiendo, sin cambios,
para quien no necesite la operación combinada.

## Ubicación verificable

- Flujo y compensación: [
  `RegisterApplicationWithInitialResourceUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/usecase/impl/RegisterApplicationWithInitialResourceUseCaseImpl.java)
- Pruebas: `compensates_by_removing_the_application_when_the_resource_registration_fails` y
  `still_reports_the_original_resource_error_when_the_compensation_itself_fails` en
  [
  `RegisterApplicationWithInitialResourceUseCaseImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/application/usecase/impl/RegisterApplicationWithInitialResourceUseCaseImplTests.java)
- Plan y reporte de la historia: `PLAN-HU-010.md` y `REPORTE-HU-010.md` en
  `pdp/docs/ai-harness/workspace/`

## Evidencia y límite

Las pruebas unitarias fuerzan el fallo del registro del recurso (con y sin fallo adicional de la
compensación) contra colaboradores falsos, y verifican que `RemoveApplicationUseCase` se invoca con
el `applicationId` correcto y que el cliente siempre recibe el error original del recurso. No hay
un rechazo de negocio real y reproducible tras crear la aplicación (la unicidad del recurso es por
`(applicationId, path, method)`, y el `applicationId` de este flujo siempre es nuevo — ver
`PLAN-HU-010.md` §0), así que la prueba end-to-end (`ApplicationWithInitialResourceHttpTests`) cubre
solo el camino feliz contra SurrealDB real; la compensación se prueba a nivel de caso de uso. No
cubren un fallo a mitad de una escritura HTTP individual contra SurrealDB (por ejemplo, la conexión
cayéndose entre el `CREATE` y la respuesta), que queda fuera del alcance de una prueba unitaria y
sería terreno de una prueba de resiliencia de infraestructura.

La compensación entre módulos seguirá siendo necesaria mientras cada módulo tenga su propio límite de
consistencia — eso no depende de qué motor de persistencia haya detrás.
