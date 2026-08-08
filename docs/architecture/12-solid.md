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
- **OCP:** `ProtectedApplicationCriteria` admite nuevos filtros sin crear métodos de repositorio por
  atributo. Añadir una regla es añadir una clase y una línea en el validator, no editar el caso de
  uso.
- **LSP:** `SnapshotReactiveTransactionAdapter` e `InMemoryProtectedResourceRepository` sustituyen a
  sus puertos; SurrealDB podrá hacerlo sin tocar el caso de uso.
- **ISP:** los contratos son mínimos y separados — `BusinessRule` (sin I/O), `ReactiveBusinessRule`
  (sin retorno), `ReactiveBusinessRuleWithResult` (con retorno), `DomainEventPublisher`, `TimeProvider`,
  `IdentifierGenerator`, `ReactiveTransactionPort`. Ningún implementador recibe métodos que no usa.
- **DIP:** los servicios reciben interfaces por constructor; `ConcurrentHashMap` y las clases de
  Spring solo aparecen en `infrastructure`.

## Ubicación verificable

- [`shared/rule`](../../src/main/java/co/edu/uco/seguridad/shared/rule) y [`shared/port`](../../src/main/java/co/edu/uco/seguridad/shared/port)
- [`recursos/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/rule)
- [`ProtectedApplicationCriteria.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/domain/ProtectedApplicationCriteria.java)
- [`ResourcesConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/config/ResourcesConfiguration.java)

## Evidencia y límite

[`ProtectedResourceRuleTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/application/rule/ProtectedResourceRuleTests.java)
prueba cada regla por separado, con un stub distinto por escenario: eso solo es posible porque cada
contrato es pequeño. SOLID no se “certifica” con una prueba; se preserva con límites, tests y
revisión.
