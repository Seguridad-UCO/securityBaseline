# 12. Cumplimiento de SOLID

[← Interacción](11-layer-interaction.md) · [Siguiente: adaptadores →](20-clean-adapters.md)


## Decisión arquitectónica

SOLID se aplica mediante tipos y dependencias concretas, no como una declaración general.

## Justificación

La seguridad requiere cambios localizados y pruebas confiables. Un servicio que valide, persista,
audite y traduzca HTTP incumple responsabilidad única y vuelve inseparable el negocio de la
infraestructura.

## Implementación

- **SRP:** cada regla decide una sola cosa y lanza una sola excepción. El rules validator compone;
  el caso de uso orquesta; el interactor mapea; el controller transporta; el dummy almacena.
- **OCP:** `ApplicationCriteria` admite nuevos filtros sin crear métodos de repositorio por
  atributo. Añadir una regla es añadir una clase y una línea en el validator, no editar el caso de
  uso.
- **LSP:** `SurrealProtectedResourceRepository`, `SurrealApplicationRepository` y
  `SurrealTenantRepository` sustituyen a sus dummies en memoria implementando exactamente el mismo
  puerto (ADR-0004); ningún caso de uso cambió al hacer el reemplazo.
- **ISP:** los contratos son mínimos y separados — `Operation` (sin I/O), `ReactiveOperation`
  (sin retorno), `ReactiveOperationWithResult` (con retorno), `DomainEventPublisher`, `TimeProvider`,
  `IdentifierGenerator`. Ningún implementador recibe métodos que no usa.
- **DIP:** los servicios reciben interfaces por constructor; `ConcurrentHashMap` y las clases de
  Spring solo aparecen en `infrastructure`.

## Ubicación verificable

- [`shared/rule`](../../src/main/java/co/edu/uco/seguridad/shared/contract) y [`shared/port`](../../src/main/java/co/edu/uco/seguridad/shared/port)
- [`resources/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/rule)
- `ApplicationCriteria.java`
- [`ResourcesConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/config/ResourcesConfiguration.java)

## Evidencia y límite

[`ProtectedResourceMustBeUniqueRuleImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/application/rule/impl/ProtectedResourceMustBeUniqueRuleImplTests.java)
prueba cada regla por separado, con un stub distinto por escenario: eso solo es posible porque cada
contrato es pequeño. SOLID no se “certifica” con una prueba; se preserva con límites, tests y
revisión.
