# Enfoque general de los 23 criterios de la línea base

Este documento presenta el alcance global de la línea base. Cada enlace conduce a la evidencia detallada: decisión arquitectónica, justificación, implementación, ubicación de código y forma de demostración.

| # | Criterio | Enfoque aplicado en la línea base | Evidencia detallada |
|---:|---|---|---|
| 1 | Clean Architecture | Módulo hexagonal con dependencias hacia dominio. | [01](architecture/01-clean-architecture.md) |
| 2 | Contratos de servicios | Casos de uso y puertos explícitos. | [02](architecture/02-service-contracts.md) |
| 3 | Reglas e integridad | Value objects, agregado y unicidad por tenant. | [03](domain-and-data/03-business-rules-data-integrity.md) |
| 4 | Capacidades transversales | Correlación, auditoría, métricas, errores y configuración comunes. | [04](cross-cutting/04-cross-cutting-capabilities.md) |
| 5 | Manejo de mensajes | Envelope de éxito y ProblemDetail para errores. | [05](interfaces/05-message-handling.md) |
| 6 | Manejo de parámetros | DTO, query tipada y headers de correlación validados. | [06](interfaces/06-parameter-handling.md) |
| 7 | Adaptadores dummy | Repositorio, auditoría y transacción en memoria sustituibles. | [07](infrastructure/07-dummy-adapters.md) |
| 8 | Logging e instrumentación | MDC/Reactor Context, Actuator y Micrometer/OpenTelemetry. | [08](cross-cutting/08-logging-instrumentation.md) |
| 9 | Excepciones | Excepciones de dominio y traducción HTTP centralizada. | [09](cross-cutting/09-exception-handling.md) |
| 10 | Transacciones | Puerto reactivo y rollback demostrable en dummy. | [10](domain-and-data/10-transactions.md) |
| 11 | Interacción entre capas | Flujo adaptador → caso de uso → dominio/puertos → adaptador. | [11](architecture/11-layer-interaction.md) |
| 12 | SOLID | Responsabilidades y contratos pequeños, inversión de dependencias. | [12](architecture/12-solid.md) |
| 13 | DTOs | Frontera HTTP separada de command y modelo de dominio. | [13](interfaces/13-input-strategy-dtos.md) |
| 14 | DTOs seguros | Records inmutables y defensa en profundidad de validación. | [14](interfaces/14-secure-dtos.md) |
| 15 | Validación de dominio | Value objects/agregado y specification para filtros. | [15](domain-and-data/15-domain-validation.md) |
| 16 | Repositorios dinámicos | Búsqueda por criterio, no por métodos específicos. | [16](domain-and-data/16-repository-strategy.md) |
| 17 | Consultas dinámicas | Filtros opcionales construidos en tiempo de ejecución. | [17](domain-and-data/17-dynamic-queries.md) |
| 18 | Paginación | Página/tamaño con límites de protección. | [18](domain-and-data/18-pagination.md) |
| 19 | Rangos | Offset/límite como alternativa explícita a paginación. | [19](domain-and-data/19-result-ranges.md) |
| 20 | Adaptadores limpios | Controller, mapper y dummies sin reglas de negocio. | [20](architecture/20-clean-adapters.md) |
| 21 | Modelo refinado | Agregado, entidad interna y value objects con responsabilidades claras. | [21](architecture/21-refined-model.md) |
| 22 | Arquitectura reactiva | WebFlux/Netty/Mono, sin bloqueo en el flujo. | [22](architecture/22-reactive-architecture.md) |
| 23 | Arquitectura antes del negocio | Una HU mínima que prueba primero capacidades reutilizables. | [23](cross-cutting/23-architecture-first.md) |

## Cómo usarlo en la sustentación

1. Explicar el enfoque general de esta tabla.
2. Abrir el criterio consultado desde su enlace.
3. Seguir los enlaces de “Ubicación verificable” hasta la clase y prueba.
4. Ejecutar la demostración descrita en la [guía de verificación](evidence/verification-guide.md).
