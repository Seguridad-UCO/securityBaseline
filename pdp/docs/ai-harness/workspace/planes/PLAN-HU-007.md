# PLAN: HU-007 — `AccessEvent`, la evidencia correlacionada

## Metadata

- **ID:** HU-007
- **Slice:** `authorization` (existente) — nuevo evento de dominio, nuevo puerto secundario, nueva persistencia
- **Tipo:** Escritura (registro de evidencia) — sin endpoint HTTP nuevo
- **Fecha:** 2026-09-12
- **Rama sugerida:** `feature/HU-007-evento-acceso`
- **Fuentes:**
    - `pdp/docs/ai-harness/workspace/HU-007.md` (historia dictada, ya redactada; criterios 1-6 y alcance
      tomados literalmente de ahí)
    - `pdp/docs/ai-harness/workspace/ROADMAP-PDP.md` línea 73 (origen: paso 9 de UC-01, INV-AUD-01, BC-11)
    - Código real: `DomainEventPublisher`/`SpringDomainEventPublisher` (ADR-0002), `ProtectedResourceRegistered`
        + `InMemoryAuditAdapter` (el único precedente de evento de dominio del proyecto),
          `SurrealSchemaInitializer`, `AuthorizeUseCaseImpl`, `AccessRequest`, `AccessDecision`
- **Criterios de la línea base que toca:** 1, 2, 4, 7, 9, 11, 12, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — el patrón `DomainEventPublisher` + `@EventListener` no sirve para persistir de verdad

La historia pide seguir "el patrón ya usado en `resources`" — pero ese patrón
(`ProtectedResourceRegistered` + `InMemoryAuditAdapter`) solo funciona porque el único listener hoy
es **síncrono, sin E/S** (una cola en memoria). `SpringDomainEventPublisher.publish(event)` envuelve
`ApplicationEventPublisher.publishEvent(event)` en `Mono.fromRunnable(...)`: Spring entrega el evento
a cada `@EventListener` **de forma síncrona**, sin esperar nada reactivo que ese método devuelva.

Un `@EventListener` que necesite escribir en SurrealDB (`Mono<Void>` real) no tiene forma de
integrarse con eso sin **suscribirse manualmente** (`mono.subscribe()` dentro del listener) — la
regla invariante 1 de `sb-reactivo` lo prohíbe explícitamente, y con razón: nadie controlaría ese
`Mono` huérfano si falla o se cuelga.

**Decisión:** `AccessEvent` sigue siendo un `record implements DomainEvent` (vocabulario de dominio,
igual que `ProtectedResourceRegistered`) — pero su persistencia **no** pasa por
`DomainEventPublisher`/`@EventListener`. `AuthorizeUseCaseImpl` llama directo a un puerto secundario
nuevo (`AccessAuditRepository`), como ya hace con `PolicyDecisionPort`. Esto no es relajar el patrón
por pereza: es la misma razón por la que `resources` nunca puso E/S real en su listener.

### Hallazgo 2 — no hace falta resolver nada nuevo para saber "qué se decidió"

`AccessRequest` (tenant, aplicación, recurso, acción, sujeto, ids de correlación) y `AccessDecision`
(estado, `reasonCode`, ids) entre los dos ya tienen todo lo que el evento necesita. No se toca
ninguno de los dos — `AccessEvent` se arma leyendo ambos, después de que la decisión ya existe.

### Hallazgo 3 — "consultable" no significa "con endpoint HTTP" todavía

La propia historia excluye "el Servicio de auditoría como contenedor aparte — es otro despliegue".
Sin ese consumidor, publicar un endpoint HTTP de consulta hoy expondría datos de auditoría sin que
exista todavía una decisión de quién puede leerlos (una decisión de seguridad real, no técnica).
**Alcance de esta historia:** la evidencia queda persistida y es consultable por `correlationId` a
nivel de repositorio (probado con una prueba de integración) — el endpoint HTTP, si se necesita,
es su propia historia cuando exista quien lo consuma.

## 1. Resumen funcional

Cada decisión que sale de `AuthorizeUseCaseImpl` —`ALLOW`, `DENY` o `INDETERMINATE`, sin excepción—
arma un `AccessEvent` y lo guarda vía `AccessAuditRepository` en una tabla nueva de SurrealDB. Un
fallo al guardar se registra en el log (ERROR, con el mismo `ReactiveLogContext` que ya usa el resto
del proyecto) y **nunca** cambia la decisión ya calculada — eso sigue viajando al PEP/BFF sin
alterarse. El evento nunca lleva el token, la evidencia JWT ni el cuerpo de la petición: solo
identificadores y el resultado.

**No cubre:** el Servicio de auditoría como despliegue aparte, `HashBlockchain` (BC-11 ya lo marca
como no-requisito), ningún endpoint HTTP de consulta (hallazgo 3), y el canal interno del PEP no
necesita ningún cambio propio — comparte `AuthorizeUseCaseImpl` con el canal BFF, así que ya queda
auditado con este mismo cambio.

## 2. Criterios de aceptación

| # | Criterio (de HU-007.md)                     | Resultado esperado                                                                                                                                                                             |
|---|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Toda decisión emite evento                  | `recordAudit(...)` envuelve el resultado final de la cadena de `AuthorizeUseCaseImpl`, después de los tres `onErrorResume` — se alcanza sin importar qué camino produjo la decisión            |
| 2 | Correlación                                 | `AccessEvent.correlationId`/`decisionId` igual a los de la `AccessDecision` devuelta                                                                                                           |
| 3 | Sin secretos                                | El `record` no tiene ningún campo que pueda llevar un token o el cuerpo — solo identificadores y el resultado (ver estructura en la SPEC)                                                      |
| 4 | Estado técnico distinguible                 | `AccessEvent.state`/`reasonCode` son los mismos `DecisionState`/`ReasonCode` de la decisión — `INDETERMINATE`/`CONTEXT_UNAVAILABLE` no se confunde con `DENY`/`POLICY_DENY`                    |
| 5 | El fallo de auditoría no altera la decisión | Si `AccessAuditRepository.save(...)` falla, `AuthorizeUseCaseImpl.execute(...)` sigue devolviendo la decisión original — se registra el fallo por log, no se propaga como error de la petición |
| 6 | Consulta por correlación                    | `AccessAuditRepository.findByCorrelationId(...)` — probado contra SurrealDB real                                                                                                               |

## 3. Reglas de negocio

Ninguna regla nueva. Armar y guardar el evento es un efecto secundario de una decisión ya tomada —
no valida ni rechaza nada.

| # | Regla           | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|-----------------|------------|-------------------------|------------------|
| — | (ninguna nueva) | —          | —                       | —                |

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguno existente cambia. `AccessEvent` es un evento de dominio (no un agregado): se construye una
vez, se persiste, nunca se modifica ni se recupera para mutarlo.

### Value objects

| VO              | Nuevo o existente | Invariantes                                                                                                                  | Vive en |
|-----------------|-------------------|------------------------------------------------------------------------------------------------------------------------------|---------|
| (ninguno nuevo) | —                 | `AccessEvent` reutiliza `TenantId`, `ApplicationId`, `ResourcePath`, `HttpVerb`, `DecisionState`, `ReasonCode` ya existentes | —       |

## 5. Persistencia

- **Tabla:** `access_event` (constante en `AccessEventSchema`)
- **Campos:** `id`, `decisionId`, `requestId`, `correlationId`, `tenantId`, `applicationId`, `subject`,
  `resourcePath`, `action`, `state`, `reasonCode`, `occurredOn` — todos String/ISO-8601 en la fila,
  como en el resto de los adaptadores (`{X}Entity` siempre plano).
- **Consultas nuevas en el puerto:**
    - `Mono<Void> save(AccessEvent event)`
    - `Flux<AccessEvent> findByCorrelationId(String correlationId)`
- **Índice:** `DEFINE INDEX ... ON access_event COLUMNS correlationId` — no único (una correlación
  puede, en teoría, acumular más de un evento si el PEP reintenta la misma petición).
- **Inicializador de esquema:** nuevo, `SurrealAccessEventSchemaInitializer` (mismo patrón que
  `SurrealProtectedResourceSchemaInitializer`).

## 6. Endpoint

No aplica — sin sección 6. Ver hallazgo 3: la consulta por `correlationId` queda a nivel de
repositorio, probada por integración, sin superficie HTTP en esta historia.

## 7. SPEC — el contrato

### El evento de dominio — `authorization/domain/event/` [N]

```java
// pdp/authorization/domain/event/AccessEvent.java
public record AccessEvent(java.util.UUID eventId, java.util.UUID decisionId, String requestId, String correlationId,
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId, String subject,
        co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath resourcePath,
        co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb action,
        co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState state,
        co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode reasonCode, java.time.Instant occurredOn)
        implements co.edu.uco.seguridad.shared.event.DomainEvent {
}
```

> **Corrección sobre el borrador inicial de esta sección:** la primera versión de este plan incluía
> una fábrica `of(AccessRequest, AccessDecision)` en el propio récord — `LayeredArchitectureTests`
> la rechazó de inmediato (`domain` no puede depender de `application`, y esos dos tipos son DTOs de
> `application`). No hizo falta esperar al tester: se corrigió en la misma FASE 5, materializando el
> esqueleto sin esa fábrica. Quien arma el `AccessEvent` es `AuthorizeUseCaseImpl` directamente, con
> el constructor canónico — es el único sitio que ya conoce `AccessRequest` y `AccessDecision` a la
> vez. El constructor compacto sigue validando cada componente no primitivo con
> `Objects.requireNonNull` (mismo patrón que `ProtectedResourceRegistered`) — el implementador lo
> añade, el planificador lo deja vacío a propósito.

### El puerto secundario — `authorization/application/secondaryport/` [N]

```java
// pdp/authorization/application/secondaryport/AccessAuditRepository.java
public interface AccessAuditRepository {
    reactor.core.publisher.Mono<Void> save(co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent event);
    reactor.core.publisher.Flux<co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent> findByCorrelationId(String correlationId);
}
```

### Firmas modificadas [M]

```java
// pdp/authorization/application/usecase/impl/AuthorizeUseCaseImpl.java
// Nuevo componente en el constructor: AccessAuditRepository audit (después de rolesLookup, antes de
// policyDecisionPort en la lista — el orden exacto lo fija el implementador junto con el resto).
```

> Nota para el implementador (no es lógica, es la forma del contrato ya decidida en el hallazgo 1):
> el último paso de `execute(...)` pasa a ser
> `.flatMap(decision -> recordAudit(input, decision))`, después de los tres `onErrorResume`
> existentes — así se alcanza sin importar qué camino produjo la decisión. `recordAudit(...)` arma el
> `AccessEvent` con `identifiers.next()` como `eventId`, llama a `audit.save(...)`, aplica
> `.onErrorResume(...)` para registrar el fallo por log sin propagarlo (criterio 5), y siempre
> devuelve `decision` sin cambios (`.thenReturn(decision)` o equivalente).

## 8. Árbol de archivos

```
pdp/authorization/
├── domain/event/
│   └── AccessEvent.java                                              [N]
├── application/
│   ├── secondaryport/
│   │   └── AccessAuditRepository.java                                 [N]
│   └── usecase/impl/
│       └── AuthorizeUseCaseImpl.java                                  [M] — +dependencia, +paso recordAudit
└── infrastructure/
    ├── adapter/secondary/persistence/
    │   ├── entity/AccessEventEntity.java                              [N]
    │   ├── mapper/AccessEventPersistenceMapper.java                   [N]
    │   ├── repository/SurrealAccessAuditRepository.java               [N]
    │   └── schema/
    │       ├── AccessEventSchema.java                                 [N]
    │       └── SurrealAccessEventSchemaInitializer.java                [N]
    └── config/
        └── AuthorizationConfiguration.java                            [M] — +2 beans, +dependencia en authorizeUseCase
shared/message/
└── RequiredArgumentMessages.java                                      [M] — constantes nuevas
```

No hay cruce de módulo Modulith nuevo: todo lo nuevo vive dentro de `authorization`, que ya podía
usar sus propios tipos (`TenantId`/`ApplicationId`/`ResourcePath`/`HttpVerb` son de `commons`,
`OPEN`). `allowedDependencies` de `authorization` no cambia.

## 9. Casos de prueba esperados

| Capa                                       | Clase de prueba                                                                                                  | Casos                                                                                                                                                                                                                                                                                                                                                                             |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| domain                                     | `AccessEventTests`                                                                                               | el constructor compacto rechaza cada componente no primitivo en `null` (mismo caso por campo que `ProtectedResourceRegisteredTests`, si existe como referencia)                                                                                                                                                                                                                   |
| infrastructure (mapper)                    | `AccessEventPersistenceMapperTests`                                                                              | ida y vuelta entidad↔dominio conserva todos los campos, incluida la ausencia/presencia correcta de cada `Instant`/enum parseado                                                                                                                                                                                                                                                   |
| application (existente, extendida)         | `AuthorizeUseCaseImplTests`                                                                                      | cada uno de los seis casos existentes gana un fake de `AccessAuditRepository` (`save` que captura el evento recibido); nuevo caso: el evento capturado tiene el mismo `correlationId`/`decisionId`/`state`/`reasonCode` que la decisión devuelta; nuevo caso: si `audit.save(...)` falla, la decisión devuelta es idéntica a la que se habría devuelto sin ese fallo (criterio 5) |
| infrastructure (persistencia, integración) | `SurrealRepositoryIntegrationTests` (extendida, mismo archivo que ya cubre `assignments`/`roles`/`tenants`/etc.) | `save(...)` seguido de `findByCorrelationId(...)` devuelve el evento guardado; dos eventos con la misma correlación (reintento simulado) aparecen ambos                                                                                                                                                                                                                           |

Presupuesto estimado: **8-10 pruebas nuevas**, más el ajuste mecánico de firma en los seis tests
existentes de `AuthorizeUseCaseImplTests`.

## 10. Trazabilidad

| Fase                       | Estado                                              | Fecha      |
|----------------------------|-----------------------------------------------------|------------|
| Plan                       | ✅ Generado                                          | 2026-09-12 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                          | 2026-09-12 |
| Pruebas en rojo            | ✅ Rojo confirmado (`UnsupportedOperationException`) | 2026-09-12 |
| Implementación en verde    | ✅ Verde                                             | 2026-09-12 |
| Validación                 | ✅ Aprobado — ver `REPORTE-HU-007.md`                | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                                         |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee. Dos decisiones de alcance **documentadas, no preguntadas** (§0): (a) la
persistencia no pasa por `DomainEventPublisher`/`@EventListener` — motivo técnico irrenunciable, no
una preferencia; (b) sin endpoint HTTP de consulta todavía — no hay quién lo consuma ni una decisión
tomada de quién puede leer auditoría. Si más adelante se decide exponerlo, es una historia propia
(necesitaría su propia decisión de autorización: ¿quién puede leer el rastro de auditoría de qué
tenant?), no una extensión mecánica de esta.
