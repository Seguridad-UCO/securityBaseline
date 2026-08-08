# ADR-0002: Adoptar eventos de dominio, publicados vía ApplicationEventPublisher

[← Gobierno](../README.md)

## Estado

**Implementada.** El título original contemplaba el Event Publication Registry de Spring Modulith;
la implementación actual no lo usa. Ver [Nota de implementación](#nota-de-implementación).

## Contexto

El flujo E-1 (`RegisterProtectedApplicationUseCaseImpl`) es una saga entre dos módulos: registra la
aplicación vía `RegisterApplicationInteractor` y luego el recurso protegido dentro de una
transacción local, con **compensación explícita** (`RemoveApplicationInteractor`) si el segundo
paso falla, porque el almacén del otro módulo no puede unirse a la misma transacción.

Hacía falta un rastro reutilizable del hecho de negocio "se registró un recurso protegido" para que
interesados (auditoría, futura notificación al PEP/PDP) lo consuman sin acoplarse al caso de uso.

## Decisión

Se incorpora `AggregateRoot` + `DomainEvent` en `pdp/commons`, con un puerto `DomainEventPublisher`
en `shared/event`, implementado sobre `ApplicationEventPublisher` de Spring. La auditoría deja de
ser un puerto invocado por el caso de uso y pasa a ser un **listener** de esos eventos.

## Justificación

1. **Desacopla auditoría del caso de uso.** El caso de uso registra el hecho
   (`ProtectedResourceRegistered`) en el agregado; quién reacciona es decisión de infraestructura.
2. **Migra la saga hacia consistencia eventual sin perder la garantía actual.** La compensación
   explícita se conserva; los eventos cubren primero auditoría.
3. **Puerto con consumidor real.** `DomainEventPublisher` se introduce junto con su primer
   consumidor (`InMemoryAuditAdapter` como listener), no como contrato especulativo.

## Alternativas consideradas

- **No adoptar eventos; mantener solo la compensación manual.** Funciona, pero impide que futuros
  adaptadores reaccionen a "recurso registrado" sin tocar el caso de uso.
- **Bus de mensajes externo (Kafka/RabbitMQ).** Prematuro para el tamaño actual del sistema.
- **Event Publication Registry de Modulith desde el día uno.** Requiere almacén persistente que
  aún no existe (ver nota de implementación).

## Consecuencias

- Clases: `AggregateRoot` (`pdp/commons`), `DomainEvent` y `DomainEventPublisher` (`shared/event`),
  `EventPublisherConfiguration` (`shared/config`).
- `Application` y `ProtectedResource` registran eventos en sus factorías
  (`ApplicationRegistered`, `ProtectedResourceRegistered`).
- `InMemoryAuditAdapter` escucha eventos; `AuditPort` se eliminó.
- Prueba de publicación/consumo: `ProtectedResourceAuditListenerTests`.

## Nota de implementación

`@ApplicationModuleListener` combina `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` y
depende del Event Publication Registry de Modulith, que necesita un almacén persistente. Este
proyecto aún no tiene base de datos real (repositorios dummy en memoria; ver
[07. Adaptadores dummy](../../infrastructure/07-dummy-adapters.md)).

Por eso `DomainEventPublisher` se implementa con `ApplicationEventPublisher`
(`SpringDomainEventPublisher`) y el listener usa `@EventListener` síncrono. Cuando exista
persistencia real (ADR-0004), se podrá respaldar el publisher con el registry de Modulith sin
cambiar casos de uso ni eventos de dominio.

Lo que aún no se tiene: entrega asíncrona tras commit, reintento ante caída y rastreo de listeners
pendientes. Se reconsidera junto con ADR-0004.

**Actualización tras ADR-0004 (persistencia real ya existe):** la condición de "aún no hay base de
datos real" ya no aplica — SurrealDB está detrás de los tres repositorios desde el Stage 4. Sin
embargo el Event Publication Registry de Spring Modulith 2.1 solo trae módulos de respaldo para JPA,
JDBC, MongoDB y Neo4j (`spring-modulith-events-{jpa,jdbc,mongodb,neo4j}`); no existe un módulo de
respaldo para SurrealDB, y este proyecto no habla SQL/JDBC con SurrealDB (usa su API HTTP vía
`WebClient`, ver ADR-0004). Construir un `EventPublicationRepository` propio sobre esa misma API HTTP
es viable pero es trabajo adicional no trivial, y no se ha hecho. La decisión de este ADR
(`ApplicationEventPublisher` + `@EventListener` síncrono) sigue vigente; el motivo cambió de "no hay
almacén real" a "el almacén real que hay no tiene un backend de registry soportado de fábrica".
