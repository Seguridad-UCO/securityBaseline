# Enfoque general de los 23 criterios de la línea base

Este documento presenta el alcance global de la línea base. Cada enlace conduce a la evidencia
detallada: decisión arquitectónica, justificación, implementación, ubicación de código y forma de
demostración.

Para el antes y el después criterio por criterio, ver la
[matriz de cumplimiento](criteria-compliance-matrix.md).

| # | Criterio | Enfoque aplicado en la línea base | Evidencia detallada |
|---:|---|---|---|
| 1 | Clean Architecture | Módulos hexagonales con dependencias hacia el dominio. | [01](architecture/01-clean-architecture.md) |
| 2 | Contratos de servicios | Casos de uso y puertos explícitos, con convención de firmas. | [02](architecture/02-service-contracts.md) |
| 3 | Reglas e integridad | Rules individuales, separadas por uso de repositorio. | [03](domain-and-data/03-business-rules-data-integrity.md) |
| 4 | Capacidades transversales | Correlación, auditoría, reloj, identificadores, transacción y errores comunes. | [04](cross-cutting/04-cross-cutting-capabilities.md) |
| 5 | Manejo de mensajes | Envelope de éxito y ProblemDetail con código estable. | [05](interfaces/05-message-handling.md) |
| 6 | Manejo de parámetros | Query tipada por mapper y headers de correlación. | [06](interfaces/06-parameter-handling.md) |
| 7 | Adaptadores dummy | Repositorios, auditoría y transacción en memoria, sustituibles. | [07](infrastructure/07-dummy-adapters.md) |
| 8 | Logging e instrumentación | MDC desde Reactor Context, Actuator y bridge OTEL. | [08](cross-cutting/08-logging-instrumentation.md) |
| 9 | Excepciones | Una excepción por condición, traducción HTTP centralizada. | [09](cross-cutting/09-exception-handling.md) |
| 10 | Transacciones | Puerto reactivo con rollback demostrable y compensación explícita. | [10](domain-and-data/10-transactions.md) |
| 11 | Interacción entre capas | Controller → interactor → caso de uso → rules → dominio/puertos. | [11](architecture/11-layer-interaction.md) |
| 12 | SOLID | Contratos pequeños, reglas sustituibles, inversión de dependencias. | [12](architecture/12-solid.md) |
| 13 | DTOs | Raw DTO en `String` → mapper → DTO validado tipado. | [13](interfaces/13-input-strategy-dtos.md) |
| 14 | DTOs seguros | Tres barreras independientes, sin Jakarta Validation. | [14](interfaces/14-secure-dtos.md) |
| 15 | Validación de dominio | Invariantes en constructores y specification para filtros. | [15](domain-and-data/15-domain-validation.md) |
| 16 | Repositorios dinámicos | Búsqueda por criterio y ventana, no por método específico. | [16](domain-and-data/16-repository-strategy.md) |
| 17 | Consultas dinámicas | Filtros opcionales construidos en tiempo de ejecución. | [17](domain-and-data/17-dynamic-queries.md) |
| 18 | Paginación | Ventana obligatoria con máximo de 100. | [18](domain-and-data/18-pagination.md) |
| 19 | Rangos | Offset/límite convergente con paginación; ambigüedad rechazada. | [19](domain-and-data/19-result-ranges.md) |
| 20 | Adaptadores limpios | Controller, mappers y dummies sin reglas de negocio. | [20](architecture/20-clean-adapters.md) |
| 21 | Modelo refinado | Entidades y value objects inmutables, Java puro, sin Lombok. | [21](architecture/21-refined-model.md) |
| 22 | Arquitectura reactiva | WebFlux/Netty/Mono, sin bloqueo en producción. | [22](architecture/22-reactive-architecture.md) |
| 23 | Arquitectura antes del negocio | Capacidades, pipeline y secretos antes de nuevas historias. | [23](cross-cutting/23-architecture-first.md) |

## Cómo usarlo en la sustentación

1. Explicar el enfoque general de esta tabla.
2. Mostrar la [matriz de cumplimiento](criteria-compliance-matrix.md) para el antes y el después.
3. Abrir el criterio consultado desde su enlace.
4. Seguir los enlaces de “Ubicación verificable” hasta la clase y la prueba.
5. Ejecutar la demostración descrita en la [guía de verificación](evidence/verification-guide.md).
