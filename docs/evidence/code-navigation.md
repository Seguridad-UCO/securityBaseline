# Navegación código → decisión

[← Evidencia](README.md)

Esta tabla permite empezar en el código y llegar a la justificación que lo gobierna. Todas las rutas
apuntan a archivos o paquetes que existen.

| Código / configuración | Criterios documentados |
|---|---|
| [`pdp/commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons) | [03](../domain-and-data/03-business-rules-data-integrity.md), [09](../cross-cutting/09-exception-handling.md), [15](../domain-and-data/15-domain-validation.md), [18](../domain-and-data/18-pagination.md), [21](../architecture/21-refined-model.md) |
| [`crosscutting/messages`](../../src/main/java/co/edu/uco/seguridad/pdp/commons/message) | [09](../cross-cutting/09-exception-handling.md) |
| [`resources/domain`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/domain) | [01](../architecture/01-clean-architecture.md), [15](../domain-and-data/15-domain-validation.md), [17](../domain-and-data/17-dynamic-queries.md), [21](../architecture/21-refined-model.md) |
| [`applications/domain`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/domain) · [`tenants/domain`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/domain) | [01](../architecture/01-clean-architecture.md), [21](../architecture/21-refined-model.md) |
| Casos de uso publicados: [`resources`](../../src/main/java/co/edu/uco/seguridad/pdp/resources) | [02](../architecture/02-service-contracts.md), [11](../architecture/11-layer-interaction.md) |
| [`resources/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/rule) · [`applications/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/rule) | [03](../domain-and-data/03-business-rules-data-integrity.md), [09](../cross-cutting/09-exception-handling.md), [12](../architecture/12-solid.md) |
| [`resources/application/port/secondary`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/port/secondary) | [10](../domain-and-data/10-transactions.md), [16](../domain-and-data/16-repository-strategy.md), [18](../domain-and-data/18-pagination.md) |
| [`RegisterApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java) | [01](../architecture/01-clean-architecture.md), [08](../cross-cutting/08-logging-instrumentation.md), [10](../domain-and-data/10-transactions.md), [12](../architecture/12-solid.md) |
| `SearchProtectedApplicationsUseCaseImpl.java` *(no implementado)* | [16](../domain-and-data/16-repository-strategy.md), [17](../domain-and-data/17-dynamic-queries.md), [18](../domain-and-data/18-pagination.md), [19](../domain-and-data/19-result-ranges.md) |
| [`adapter/primary/web/dto`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/primary/web/dto) · [`web/mapper`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/primary/web/mapper) | [06](../interfaces/06-parameter-handling.md), [13](../interfaces/13-input-strategy-dtos.md), [14](../interfaces/14-secure-dtos.md) |
| [`port/primary/interactor`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/primary/web/interactor) · [`ApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/primary/web/controller/ApplicationController.java) | [11](../architecture/11-layer-interaction.md), [20](../architecture/20-clean-adapters.md) |
| [`ApiErrorHandler.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/exceptionhandler/ApiErrorHandler.java) | [05](../interfaces/05-message-handling.md), [09](../cross-cutting/09-exception-handling.md) |
| [`adapter/secondary/persistence`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence) · [`adapter/secondary/audit`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/audit) | [07](../infrastructure/07-dummy-adapters.md), [10](../domain-and-data/10-transactions.md), [16](../domain-and-data/16-repository-strategy.md) |
| [`shared`](../../src/main/java/co/edu/uco/seguridad/shared) | [04](../cross-cutting/04-cross-cutting-capabilities.md), [08](../cross-cutting/08-logging-instrumentation.md), [09](../cross-cutting/09-exception-handling.md), [12](../architecture/12-solid.md) |
| [`pom.xml`](../../pom.xml) · [`application.properties`](../../src/main/resources/application.properties) | [08](../cross-cutting/08-logging-instrumentation.md), [14](../interfaces/14-secure-dtos.md), [22](../architecture/22-reactive-architecture.md) |
| [`azure-pipelines.yml`](../../azure-pipelines.yml) · [`ci/`](../../ci) | [23](../cross-cutting/23-architecture-first.md), [pipelines](../delivery/pipelines.md) |
| [`infra/keyvault`](../../infra/keyvault) | [04](../cross-cutting/04-cross-cutting-capabilities.md), [secretos](../../infra/README.md) |
| [`src/test`](../../src/test) | [23](../cross-cutting/23-architecture-first.md) y evidencia de cada criterio enlazado arriba |
