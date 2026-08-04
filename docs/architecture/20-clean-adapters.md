# 20. Adaptadores limpios

[← SOLID](12-solid.md) · [Siguiente: modelo →](21-refined-model.md)

## Decisión arquitectónica

Los adaptadores conectan protocolos o tecnologías; no contienen decisiones de registro, unicidad, cardinalidad ni consistencia de negocio.

## Justificación

Poner reglas en controller produce endpoints inconsistentes; ponerlas en el dummy las pierde al adoptar SurrealDB. Se descarta el patrón de controlador “grueso” que valida, consulta y persiste directamente.

## Implementación

El adapter web aplica Bean Validation de forma y usa `ProtectedApplicationMapper`. El repositorio dummy ejecuta el criterio que recibe. Auditoría dummy solo registra evidencia. La regla “exactamente un recurso” pertenece al constructor del agregado y la unicidad se decide en el caso de uso.

## Ubicación verificable

- Web: [`infrastructure/web`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web).
- Persistencia dummy: [`InMemoryProtectedApplicationRepository.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/persistence/dummy/InMemoryProtectedApplicationRepository.java).
- Regla: [`ProtectedApplication.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ProtectedApplication.java).

## Evidencia y límite

La prueba HTTP crea una aplicación sin conocer el dummy; la prueba de dominio falla ante un recurso inválido incluso sin WebFlux. El mapper sí traduce formatos, pero nunca toma decisiones de negocio.
