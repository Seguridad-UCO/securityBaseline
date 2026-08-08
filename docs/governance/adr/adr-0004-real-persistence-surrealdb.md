# ADR-0004: Persistencia real con SurrealDB detrás de los puertos existentes

[← Gobierno](../README.md)

## Estado

Aceptada — pendiente de implementación.

## Contexto

Los repositorios actuales (`InMemoryTenantRepository`, `InMemoryApplicationRepository`,
`InMemoryProtectedResourceRepository`) son dummies en memoria (ver
[07. Adaptadores dummy](../infrastructure/07-dummy-adapters.md)). El compromiso del proyecto es
SurrealDB como motor de persistencia detrás de los mismos puertos secundarios.

El adaptador de transacción actual (`SnapshotReactiveTransactionAdapter`) copia el mapa en memoria
antes del trabajo y lo restaura si falla. En una base real la transacción la da el motor.

## Decisión

Se reemplazarán los repositorios dummy por adaptadores reales sobre SurrealDB, implementando
**los mismos puertos** (`ProtectedResourceRepository`, `ApplicationRepository`, `TenantRepository`):
puerto en términos de dominio → adaptador → driver → *entity mapper*. La transacción real vía
`ReactiveTransactionPort` reemplazará a `SnapshotReactiveTransactionAdapter` y al puerto
`SnapshotCapable` (solo útil para el dummy).

## Justificación

1. **Cumple el compromiso documentado** del proyecto con SurrealDB.
2. **`domain/` y `application/` no cambian.** Criterio de aceptación: sustituir el adaptador
   secundario no debe tocar las capas internas.
3. **Secretos ya reservados.** El scaffolding de Azure Key Vault reserva
   `pdp-datasource-password` por ambiente (ver [`infra/README.md`](../../infra/README.md)).
4. **Puertos en dominio.** Los casos de uso siguen hablando de agregados, no de entidades de
   persistencia.

## Alternativas consideradas

- **PostgreSQL + R2DBC.** Más familiar en stacks Spring reactivos, pero contradice el compromiso
  con SurrealDB; solo se reconsideraría ante una limitación técnica real del motor.
- **JPA/Hibernate.** No es reactivo; incompatible con WebFlux/Reactor.
- **Mantener los dummies indefinidamente.** Descartado: el alcance incluye persistencia real.

## Consecuencias

- Driver reactivo de SurrealDB y migraciones de esquema.
- Paquete `infrastructure/adapter/secondary/persistence/surrealdb/` por módulo con datos.
- Retiro de `SnapshotReactiveTransactionAdapter` y `SnapshotCapable`.
- Las configuraciones de módulo cambian la implementación cableada, no la forma de los puertos.
- Pruebas con Testcontainers + SurrealDB.
- Consumo real de secretos de Key Vault por ambiente.
