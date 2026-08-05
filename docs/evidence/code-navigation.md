# Navegación código → decisión

Esta tabla permite empezar en el código y llegar a la justificación que lo gobierna.

| Código / configuración | Criterios documentados |
|---|---|
| [`applications/domain`](../../src/main/java/co/edu/uco/seguridad/pdp) | [01](../architecture/01-clean-architecture.md), [03](../domain-and-data/03-business-rules-data-integrity.md), [15](../domain-and-data/15-domain-validation.md), [21](../architecture/21-refined-model.md) |
| [`application/port/in`](../../src/main/java/co/edu/uco/seguridad/pdp) | [02](../architecture/02-service-contracts.md), [11](../architecture/11-layer-interaction.md) |
| [`application/port/out`](../../src/main/java/co/edu/uco/seguridad/pdp) | [10](../domain-and-data/10-transactions.md), [16](../domain-and-data/16-repository-strategy.md), [18](../domain-and-data/18-pagination.md) |
| [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/pdp) | [01](../architecture/01-clean-architecture.md), [08](../cross-cutting/08-logging-instrumentation.md), [10](../domain-and-data/10-transactions.md), [12](../architecture/12-solid.md) |
| [`infrastructure/web`](../../src/main/java/co/edu/uco/seguridad/pdp) | [05](../interfaces/05-message-handling.md), [06](../interfaces/06-parameter-handling.md), [13](../interfaces/13-input-strategy-dtos.md), [14](../interfaces/14-secure-dtos.md), [20](../architecture/20-clean-adapters.md) |
| [`infrastructure/*/dummy`](../../src/main/java/co/edu/uco/seguridad/pdp) | [07](../infrastructure/07-dummy-adapters.md), [10](../domain-and-data/10-transactions.md) |
| [`shared`](../../src/main/java/co/edu/uco/seguridad/shared) | [04](../cross-cutting/04-cross-cutting-capabilities.md), [08](../cross-cutting/08-logging-instrumentation.md), [09](../cross-cutting/09-exception-handling.md) |
| [`pom.xml`](../../pom.xml) y [`application.properties`](../../src/main/resources/application.properties) | [08](../cross-cutting/08-logging-instrumentation.md), [22](../architecture/22-reactive-architecture.md) |
| [`src/test`](../../src/test) | [23](../cross-cutting/23-architecture-first.md) y evidencia de cada criterio enlazado arriba |
