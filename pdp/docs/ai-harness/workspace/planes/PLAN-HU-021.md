# PLAN: Auditoría de operaciones administrativas — un evento propio, en `shared`

## Metadata

- **ID:** HU-021
- **Slice:** `shared` (el puerto, el evento y su persistencia — capacidad técnica transversal, no vocabulario del PDP) +
  retrofit `[M]` de 15 casos de uso ya existentes en `authorization` y `assignments`
- **Tipo:** Escritura (nueva capacidad) + modificación de casos de uso existentes
- **Fecha:** 2026-09-15
- **Rama sugerida:** `feature/HU-021-auditoria-operaciones-administrativas`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-021.md` (dictada), código real: `AccessEvent`/`AccessAuditRepository`/
  `SurrealAccessAuditRepository`/`ObservedAccessAuditRepository` (HU-007, precedente exacto de forma), `DomainEvent`/
  `DomainEventPublisher` (`shared/event`, precedente de "capacidad técnica en `shared`, no en un slice")
- **Criterios de la línea base que toca:** 1, 4, 8 (ADR-0002), 9, 11, 12, 21, 22
- **Depende de:** HU-015 a HU-020 (todas ya cerradas o planificadas en esta misma sesión) — este plan asume que HU-020
  se implementa antes de que este se ejecute; si no, los ítems de HU-020 en la tabla de retrofit (§8) se difieren a una
  historia de seguimiento sin bloquear el resto.

## 0. Decisiones tomadas antes de planificar (resuelven lo que `HU-021.md` dejaba abierto)

| Pregunta de `HU-021.md`              | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Por qué                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ¿Dónde vive el puerto de auditoría?  | **`shared`** (`shared/audit/AdministrationAuditRepository`, `shared/event/DomainEvent` como marcador)                                                                                                                                                                                                                                                                                                                                                                                                 | Lo necesitan 5 slices (`applications` vía `authorization`, `roles`, `resources`, `assignments`, `profiles`); `shared` es `Type.OPEN` — cualquier módulo ya lo consume sin declarar un `allowedDependencies` nuevo, a diferencia de publicar esto desde `authorization` (que habría exigido `:: audit` en `roles`/`resources`/`profiles`/`assignments`, cuatro fronteras nuevas para una capacidad que no es vocabulario de ninguno de ellos) |
| Forma del evento                     | `AdministrationEvent(eventId, correlationId, tenantId, applicationId, subject, operation, outcome, occurredOn)`                                                                                                                                                                                                                                                                                                                                                                                       | Mínimo necesario, mismo espíritu que `AccessEvent`: identificadores y resultado, nunca el cuerpo de la petición                                                                                                                                                                                                                                                                                                                              |
| Catálogo de `operation`              | Enum `AdministrationOperation`: `APPLICATION_REGISTERED, APPLICATION_REMOVED, APPLICATION_CREDENTIAL_ROTATED, ADMINISTRATOR_ASSIGNED, ADMINISTRATOR_REMOVED, ROLE_DEFINED, RESOURCE_REGISTERED, RESOURCE_GRANTED, ROLE_ASSIGNED, ROLE_REVOKED, PROFILE_DEFINED, PROFILE_ROLE_ADDED, PROFILE_ASSIGNED, PROFILE_REVOKED` — una por cada caso de uso `Administer*UseCaseImpl` existente que escribe (ver §8). Listar (HU-020) **no** audita — mismo criterio que `AccessEvent` nunca auditó una consulta |
| `outcome`                            | Enum `AdministrationOutcome`: `ALLOWED`, `DENIED` — sin `ReasonCode` de `AccessEvent` (es vocabulario de decisión de acceso de negocio, no de administración); si se rechaza, `DENIED` alcanza, la razón siempre es "no administra la aplicación" hasta que exista una segunda razón de rechazo                                                                                                                                                                                                       |
| ¿Endpoint de consulta?               | No — mismo criterio que HU-007: evidencia en SurrealDB, consultable directamente, sin endpoint HTTP propio                                                                                                                                                                                                                                                                                                                                                                                            |
| ¿Retroactivo reabre HU-016 a HU-020? | No — este plan **modifica** (`[M]`) los `Administer*UseCaseImpl` ya construidos, con su propio contrato de prueba (nuevas pruebas que confirman la llamada a auditoría, no una reescritura de las pruebas existentes de gate)                                                                                                                                                                                                                                                                         |
| ¿Transaccional o best-effort?        | **Best-effort respecto a la operación de negocio** — mismo criterio que HU-007 fijó para `AccessEvent`: si guardar la auditoría falla, la operación administrativa ya ocurrida no se deshace (verificado leyendo `AuthorizeUseCaseImpl`: la publicación de `AccessEvent` no está en el camino crítico de la respuesta)                                                                                                                                                                                |

## 1. Resumen funcional

Nuevo puerto `AdministrationAuditRepository` en `shared/audit`, con su evento `AdministrationEvent`,
su adaptador real sobre SurrealDB (tabla `administration_event`) y su decorador de observabilidad
(`ObservedAdministrationAuditRepository`, mismo patrón que `ObservedAccessAuditRepository`: contador
Prometheus `security.administration.events` con `operation`/`outcome` como labels, más el log
estructurado que el collector ya envía a Loki). Cada uno de los 15 `Administer*UseCaseImpl` que hoy
gatea una escritura (HU-015 a HU-020) gana una llamada a este puerto tras resolver el resultado —
sea éxito o rechazo — sin bloquear la respuesta si el guardado falla.

## 2. Criterios de aceptación

| # | Criterio                                                                                                            | Resultado esperado                                                                            |
|---|---------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 1 | Una operación administrativa exitosa queda persistida con `outcome=ALLOWED`                                         | `AdministrationAuditRepositoryImplTests`/pruebas de cada `Administer*UseCaseImpl` retrofitted |
| 2 | Una operación administrativa rechazada (`NotAuthorizedToAdministerException`) queda persistida con `outcome=DENIED` | Ídem                                                                                          |
| 3 | Un fallo al guardar la auditoría no impide que la operación de negocio ya resuelta se devuelva                      | Prueba dedicada por caso de uso retrofitted (best-effort)                                     |
| 4 | El evento queda expuesto a Prometheus y a los logs estructurados                                                    | `ObservedAdministrationAuditRepositoryImplTests` (mismo patrón que HU-007)                    |
| 5 | Ningún endpoint HTTP nuevo                                                                                          | Sin controller nuevo en este plan                                                             |
| 6 | Suite completa                                                                                                      | `verificar.ps1` en verde                                                                      |

## 3. Reglas de negocio

Ninguna. Auditar no es una decisión de negocio — es registrar una que ya se tomó (mismo criterio que
HU-007).

## 4. Modelo de dominio afectado

### Nuevo, en `shared`

| Tipo                      | Forma                                                                                                                                                                                                                        | Vive en         |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| `AdministrationEvent`     | `record(UUID eventId, String correlationId, TenantId tenantId, ApplicationId applicationId, String subject, AdministrationOperation operation, AdministrationOutcome outcome, Instant occurredOn)`, implementa `DomainEvent` | `shared/audit/` |
| `AdministrationOperation` | enum, ver §0                                                                                                                                                                                                                 | `shared/audit/` |
| `AdministrationOutcome`   | enum `ALLOWED`, `DENIED`                                                                                                                                                                                                     | `shared/audit/` |

`shared/audit` importa `TenantId`/`ApplicationId` de `pdp/commons` — mismo precedente que otras
capacidades de `shared` (`shared/security/PdpPrincipal` ya importa `TenantId`), no es un ciclo porque
`commons` es vocabulario puro sin dependencias hacia `shared`.

## 5. Persistencia

Nueva tabla `administration_event` (SurrealDB), estructuralmente idéntica a `access_event` salvo los
campos propios: `eventId`, `correlationId`, `tenantId`, `applicationId`, `subject`, `operation`,
`outcome`, `occurredOn`. Un índice sobre `correlationId`, mismo criterio que `access_event`.

## 6. Endpoint

Ninguno — ver §0.

## 7. SPEC — el contrato

### `shared/audit` — nuevos `[N]`

```java
// shared/audit/AdministrationAuditRepository.java
public interface AdministrationAuditRepository {
    Mono<Void> save(AdministrationEvent event);
    Flux<AdministrationEvent> findByCorrelationId(String correlationId);
}
```

```java
// shared/audit/AdministrationEvent.java — record, componentes en §4, implements DomainEvent
```

```java
// shared/audit/AdministrationOperation.java, shared/audit/AdministrationOutcome.java — enums, §0
```

### Infraestructura — nuevos `[N]`

**Hallazgo antes de fijar la ruta:** `shared` hoy solo aloja capacidades técnicas puras (el cliente
genérico `SurrealDbClient`, `ReactiveTelemetry`) — **ningún adaptador de persistencia concreto vive
ahí**; cada `Surreal{X}Repository` real vive en el slice dueño de ese repositorio. Solo la
**interfaz** de un puerto necesita estar en `shared` para que 5 slices la consuman sin declarar
`allowedDependencies` nuevos (§0) — el **adaptador concreto** no tiene esa restricción, porque nada
fuera de la `Configuration` que lo instancia necesita ver la clase de implementación. Por eso el
adaptador real vive en `authorization`, que ya es dueño de `AccessAuditRepository` — es una
continuación natural, no un precedente nuevo:

```java
// pdp/authorization/infrastructure/adapter/secondary/persistence/repository/SurrealAdministrationAuditRepository.java
// implements shared.audit.AdministrationAuditRepository, misma forma que SurrealAccessAuditRepository
// (SurrealQL literal, sin ORM)
// pdp/authorization/infrastructure/adapter/secondary/persistence/schema/AdministrationEventSchema.java
// TABLE = "administration_event", CORRELATION_INDEX
// pdp/authorization/infrastructure/adapter/secondary/persistence/entity/AdministrationEventEntity.java
// pdp/authorization/infrastructure/adapter/secondary/persistence/mapper/AdministrationEventPersistenceMapper.java
// — misma forma que su par de AccessEvent
// pdp/authorization/infrastructure/adapter/secondary/persistence/schema/SurrealAdministrationEventSchemaInitializer.java
// — ApplicationRunner, crea la tabla + índice
// pdp/authorization/infrastructure/adapter/secondary/observability/ObservedAdministrationAuditRepository.java
// — decora el repositorio real, incrementa "security.administration.events" con operation/outcome
// como labels, emite el log estructurado — mismo paquete donde ya vive ObservedAccessAuditRepository
```

### Retrofit `[M]` — 13 casos de uso existentes ganan la llamada a auditoría

> **Corregido en una segunda pasada de `2-tester-spec`** (tras el primer intento de
> `3-implementador`): la tabla original tenía 15 filas, incluyendo
`RegisterApplicationWithFirstAdministratorUseCaseImpl`
> y `AssignApplicationAdministratorUseCaseImpl` (slice `assignments`). Se retiran las dos: ninguna
> de sus requests (`RegisterApplicationWithFirstAdministratorRequest`, `AssignApplicationAdministratorRequest`)
> trae `subject` — no hay con qué construir un `AdministrationEvent` completo ahí — y auditar en
> ambas capas duplicaría el evento: `AssignApplicationAdministratorUseCaseImpl` ya se invoca
> exclusivamente detrás de `AdministerApplicationAdministratorAssignmentUseCaseImpl` (HU-020), que
> sí tiene el `AdministrationRequest` completo y ya audita `ADMINISTRATOR_ASSIGNED`.
> `RegisterApplicationWithFirstAdministratorUseCaseImpl` no tiene gate propio (HU-015: el primer
> administrador se crea sin necesitar ya administrar la aplicación) y por eso tampoco tiene
> `AdministrationRequest` disponible — auditar `APPLICATION_REGISTERED` ahí exigiría inventar un
> `subject` sintético, que no es preferible a simplemente no auditar ese paso interno de registro.
> Sus constructores vuelven a la forma sin `AdministrationAuditRepository` (ver §7).

Cada uno de los 13 gana `AdministrationAuditRepository audit` en su constructor, y su `execute` pasa de
`mustBeAdministrator.execute(...).then(Mono.defer(() -> delegate.execute(...)))` a una forma que
audita ambas ramas sin cambiar el resultado que ya devolvía — el patrón exacto (un solo método
privado `audited(Mono<T> operation, AdministrationOperation kind, AdministrationRequest context)`
reutilizable) lo fija `2-tester-spec`/`3-implementador` contra la firma real de cada clase, no este
plan; lo que este plan fija es **la lista cerrada de quién cambia y con qué `operation`**:

| Caso de uso `[M]`                                                  | Slice           | `AdministrationOperation`        |
|--------------------------------------------------------------------|-----------------|----------------------------------|
| `AdministerApplicationRemovalUseCaseImpl`                          | `authorization` | `APPLICATION_REMOVED`            |
| `AdministerApplicationCredentialRotationUseCaseImpl`               | `authorization` | `APPLICATION_CREDENTIAL_ROTATED` |
| `AdministerRoleDefinitionUseCaseImpl`                              | `authorization` | `ROLE_DEFINED`                   |
| `AdministerResourceGrantUseCaseImpl`                               | `authorization` | `RESOURCE_GRANTED`               |
| `AdministerResourceRegistrationUseCaseImpl`                        | `authorization` | `RESOURCE_REGISTERED`            |
| `AdministerAssignmentCreationUseCaseImpl`                          | `authorization` | `ROLE_ASSIGNED`                  |
| `AdministerAssignmentRevocationUseCaseImpl`                        | `authorization` | `ROLE_REVOKED`                   |
| `AdministerProfileDefinitionUseCaseImpl`                           | `authorization` | `PROFILE_DEFINED`                |
| `AdministerProfileRoleAdditionUseCaseImpl`                         | `authorization` | `PROFILE_ROLE_ADDED`             |
| `AdministerProfileAssignmentCreationUseCaseImpl`                   | `authorization` | `PROFILE_ASSIGNED`               |
| `AdministerProfileAssignmentRevocationUseCaseImpl`                 | `authorization` | `PROFILE_REVOKED`                |
| `AdministerApplicationAdministratorAssignmentUseCaseImpl` (HU-020) | `authorization` | `ADMINISTRATOR_ASSIGNED`         |
| `AdministerApplicationAdministratorRemovalUseCaseImpl` (HU-020)    | `authorization` | `ADMINISTRATOR_REMOVED`          |

`AdministerApplicationAdministratorListUseCaseImpl` (HU-020) **no** se toca — es consulta, no
escritura (ver §0). `RegisterApplicationWithFirstAdministratorUseCaseImpl` y
`AssignApplicationAdministratorUseCaseImpl` tampoco — ver nota arriba.

Los 13 casos de uso también ganan `IdentifierGenerator identifiers` y `TimeProvider time` en el
constructor (necesarios para `eventId`/`occurredOn` de `AdministrationEvent` — la convención
invariante del proyecto prohíbe `UUID.randomUUID()`/`Instant.now()` en `application`). Sin fuente
de `correlationId` real disponible en `AdministrationRequest`, el evento se autocorrelaciona:
`correlationId = eventId.toString()`.

> **Por qué el retrofit no lo materializa este plan:** cada clase de la tabla ya tiene sus propias
> pruebas de gate (`allows()`/`denies()`) que se romperían al agregar un parámetro al constructor —
> exactamente el caso que separa `[N]` de `[M]` en este harness (`1-planificador.md`): el planificador
> fija el contrato (la tabla de arriba, la firma de `AdministrationAuditRepository`), `2-tester-spec`
> aplica la firma nueva a cada constructor con cuerpo `throw` y arregla los 15×2 fakes que dejan de
> compilar, `3-implementador` rellena la llamada real.

### Firmas nuevas en `RequiredArgumentMessages`

```java
public static final String ADMINISTRATION_AUDIT_REPOSITORY = "se requiere el repositorio de auditoría de operaciones administrativas";
```

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado (15 archivos, tabla de arriba, más lo listado aquí).

```
shared/
├── audit/
│   ├── AdministrationAuditRepository.java                                  [N]
│   ├── AdministrationEvent.java                                            [N]
│   ├── AdministrationOperation.java                                        [N]
│   └── AdministrationOutcome.java                                          [N]
└── message/RequiredArgumentMessages.java                                   [M] agrega 1 constante

pdp/authorization/infrastructure/
├── adapter/secondary/persistence/
│   ├── repository/SurrealAdministrationAuditRepository.java                [N]
│   ├── entity/AdministrationEventEntity.java                               [N]
│   ├── mapper/AdministrationEventPersistenceMapper.java                    [N]
│   └── schema/AdministrationEventSchema.java                               [N]
│   └── schema/SurrealAdministrationEventSchemaInitializer.java             [N]
├── adapter/secondary/observability/ObservedAdministrationAuditRepository.java [N]
└── config/AuthorizationConfiguration.java                                  [M] — agrega el bean del repositorio (decorado) + su schema initializer (mismo patrón que `accessAuditRepository`/`accessEventSchemaInitializer`, líneas 131-138 actuales) + 12 use cases ganan `AdministrationAuditRepository` en su constructor (tabla de §7)

pdp/assignments/infrastructure/config/AssignmentsConfiguration.java         [M] — 2 use cases ganan `AdministrationAuditRepository` (tabla de §7); sin cambio a `assignments/package-info.java` — el tipo que importan es `shared.audit.AdministrationAuditRepository` (interfaz), nunca la clase concreta de `authorization`, y `shared` ya es `Type.OPEN` para todo el proyecto
```

## 9. Casos de prueba esperados

| Capa                                                          | Clase de prueba                                                                                                                                                                                                                                                                                      | Casos                                                                                                                                                     |
|---------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `shared` (dominio del evento)                                 | `AdministrationEventTests` (si el record gana invariantes no triviales; si no, se cubre indirectamente)                                                                                                                                                                                              | construcción válida, `requireNonNull` por componente                                                                                                      |
| `infrastructure` (`shared`)                                   | `SurrealAdministrationAuditRepositoryTests`                                                                                                                                                                                                                                                          | guarda y encuentra por `correlationId`, contra SurrealDB real (`AbstractSurrealDbIntegrationTest`) — mismo patrón que `SurrealRepositoryIntegrationTests` |
| `infrastructure` (`shared`)                                   | `ObservedAdministrationAuditRepositoryTests`                                                                                                                                                                                                                                                         | incrementa el contador correcto con las labels correctas; delega el guardado real                                                                         |
| `application`/`infrastructure` (por cada uno de los 15 `[M]`) | Se **extiende** la clase de prueba existente de cada caso de uso (no se reemplaza): un caso nuevo por clase — "audita `ALLOWED` en éxito", "audita `DENIED` en rechazo", "no bloquea el resultado si la auditoría falla" — usando un fake de `AdministrationAuditRepository` que captura lo recibido | ~15 × 2-3 casos nuevos = 30-45 casos                                                                                                                      |

Presupuesto total estimado: **40-55 pruebas** — la historia más grande del backlog en superficie de
prueba, precisamente porque toca 15 clases ya existentes en vez de construir una sola vez.

## 10. Trazabilidad

| Fase                       | Estado                                         | Fecha      |
|----------------------------|------------------------------------------------|------------|
| Plan                       | ✅ Generado                                     | 2026-09-15 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                     | 2026-09-15 |
| Pruebas en rojo            | ✅ Confirmado (dos pasadas de `2-tester-spec`)  | 2026-09-15 |
| Implementación en verde    | ⚠️ Verde en pruebas, incompleta contra el plan | 2026-09-15 |
| Validación                 | ⛔ RECHAZADO — ver REPORTE-HU-021.md            | 2026-09-15 |
| Entrega (gate 2)           | ⏳ Bloqueada hasta corregir los 3 bloqueantes   |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. Un solo condicionamiento real:

1. **Si HU-020 todavía no está implementada cuando esta historia se ejecute**, las dos filas de HU-020
   en la tabla de retrofit (§7) se excluyen de esta historia y quedan como una extensión de una línea
   para cuando HU-020 exista — no bloquea el resto del retrofit.
