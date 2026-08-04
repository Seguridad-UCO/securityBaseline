# 12. Cumplimiento de SOLID

[← Interacción](11-layer-interaction.md) · [Siguiente: adaptadores →](20-clean-adapters.md)

## Decisión arquitectónica

SOLID se aplica mediante tipos y dependencias concretas, no como una declaración general.

## Justificación

La seguridad requiere cambios localizados y pruebas confiables. Un servicio que valide, persista, audite y traduzca HTTP incumple responsabilidad única y vuelve inseparable el negocio de la infraestructura.

## Implementación

- **SRP:** agregado preserva invariantes; servicio orquesta; controller transporta; dummy almacena.
- **OCP:** `ProtectedApplicationCriteria` admite nuevos filtros sin crear métodos de repositorio por atributo.
- **LSP:** el dummy sustituye al puerto de repositorio; SurrealDB podrá hacerlo sin tocar el servicio.
- **ISP:** `AuditPort`, `TimeProvider`, `ApplicationIdGenerator` y transacción son contratos pequeños.
- **DIP:** `ProtectedApplicationService` recibe interfaces, nunca `ConcurrentHashMap` ni clases Spring.

## Ubicación verificable

- [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/applications/application/service/ProtectedApplicationService.java)
- [`application/port/out`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/out)
- [`InMemoryProtectedApplicationRepository.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/persistence/dummy/InMemoryProtectedApplicationRepository.java)

## Evidencia y límite

[`ProtectedApplicationServiceTests.java`](../../src/test/java/co/edu/uco/seguridad/applications/application/service/ProtectedApplicationServiceTests.java) sustituye colaboraciones por dummies y prueba el comportamiento. SOLID no se “certifica” con una sola prueba: se preserva mediante límites, tests y revisión.
