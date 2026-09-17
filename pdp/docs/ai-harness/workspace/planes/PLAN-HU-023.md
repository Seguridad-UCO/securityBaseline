# PLAN: Caché distribuida de roles activos, invalidada por evento

## Metadata

- **ID:** HU-023
- **Slice:** `shared` (capacidad técnica transversal, no un slice de negocio de `pdp` — mismo
  criterio que HU-022; dos puntos de invocación en `assignments`)
- **Tipo:** Mixto (infraestructura + lectura cacheada + efecto secundario de invalidación en dos
  casos de uso existentes)
- **Fecha:** 2026-09-16
- **Rama sugerida:** `feature/HU-023-cache-distribuida-roles-activos`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-023.md`,
  `security-platform-architecture/docs/01-governance/adr/ADR-026-redis-cache-and-token-revocation.md`,
  código real: `ResolveActiveRolesUseCaseImpl.java`, `AssignRoleUseCaseImpl.java`,
  `RevokeAssignmentUseCaseImpl.java`, `ActiveRoleNamesLookupValidatorImpl.java`,
  `ObservedAccessAuditRepository.java`/`ObservedAccessAuditRepositoryTests.java` (patrón de
  decorador con métrica), `RedisConfiguration.java`/`TokenRevocationPort.java` (HU-022, precedente
  directo de puerto Redis). No hay event storming propio de este contexto en
  `artefactos-referencia`. Decisiones de diseño confirmadas con Sebastián el 2026-09-16 (ver §11 —
  resueltas, no pendientes).
- **Criterios de la línea base que toca:** 1, 2, 4, 7, 8, 9, 11, 12, 21, 22, 23

## 1. Resumen funcional

Introduce `DistributedCachePort` (Redis, puerto separado de `TokenRevocationPort` de HU-022, per
ADR-026) para cachear la resolución de roles activos de un sujeto por aplicación
(`ResolveActiveRolesUseCase`), la lectura de mayor frecuencia de la plataforma. Lectura en
caché-aside: `ResolveActiveRolesUseCaseImpl` consulta primero Redis; si no está (o Redis falla,
fail-open), consulta `AssignmentRepository` como hoy y puebla la caché. Invalidación por evento,
nunca por temporizador como único mecanismo: **hallazgo de esta planificación** — los 7 casos de
uso que mutan una asignación (HU-015 a HU-020) delegan, todos, en solo dos puntos raíz
(`AssignRoleUseCaseImpl`, `RevokeAssignmentUseCaseImpl`); invalidar ahí cubre los 7 sin duplicar
nada por slice (ver §1.1). Un TTL corto de respaldo (`pdp.cache.active-roles.retention`, default
`PT60S`) acota el riesgo de una invalidación que falla silenciosamente. Métrica de acierto/fallo vía
`ObservedDistributedCachePort` (Micrometer), mismo patrón que `ObservedAccessAuditRepository`.

**No cubre:** revocación de tokens (HU-022, puerto distinto); cachear la decisión de autorización en
sí (`AuthorizeUseCase`/`AuthorizeAdministrationUseCase` — excluido explícitamente por ADR-026); MFA
(HU-024).

### 1.1 Dónde se invalida — hallazgo de esta planificación

`AssignRoleUseCase`/`RevokeAssignmentUseCase` no son los únicos casos de uso que mutan una
asignación, pero sí son los **únicos que tocan el repositorio directamente** — los otros cinco
delegan en ellos, confirmado leyendo cada implementación:

| Caso de uso (HU de origen) | Delega en |
|---|---|
| `AssignProfileUseCaseImpl` (HU-011) | `AssignRoleUseCase.execute(...)` por cada rol del perfil |
| `AssignApplicationAdministratorUseCaseImpl` (HU-015) | `AssignRoleUseCase.execute(...)` |
| `RegisterApplicationWithFirstAdministratorUseCaseImpl` (HU-015) | `AssignRoleUseCase.execute(...)` |
| `RevokeProfileAssignmentUseCaseImpl` (HU-011) | `RevokeAssignmentUseCase.execute(...)` por cada asignación generada |
| `RemoveApplicationAdministratorUseCaseImpl` (HU-020) | `RevokeAssignmentUseCase.execute(...)` |

Invalidar dentro de `AssignRoleUseCaseImpl.execute()` y `RevokeAssignmentUseCaseImpl.execute()`
cubre las 7 rutas de escritura del catálogo (HU-015 a HU-020) sin agregar una sola línea a los otros
cinco casos de uso ni a `assignments`/`roles`/`profiles` por separado — exactamente lo que ADR-026
pide ("no se duplica en cada slice"), y el mismo patrón que `TokenRevocationPort` ya usa en HU-022
(con una diferencia deliberada: HU-022 wireó también el punto derivado
`RemoveApplicationAdministratorUseCaseImpl` por fidelidad literal a su SPEC, generando una
redundancia inocua pero innecesaria; aquí **no** se repite ese patrón — solo los dos puntos raíz).

**Decisión (confirmada con Sebastián):** llamada directa desde los dos puntos raíz, no un evento de
dominio vía `DomainEventPublisher`. Menos piezas para el mismo resultado, y el propio
`ActiveRoleNamesLookupValidatorImpl` (el único consumidor de lectura) queda sin cambios: no sabe que
hay una caché debajo.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Segunda lectura sirve desde caché | Dos llamadas seguidas a `ResolveActiveRolesUseCase.execute` con el mismo `(userId, applicationId)`, sin cambios de asignación entre medio: la segunda no consulta `AssignmentRepository` |
| 2 | Invalidación inmediata | Tras `AssignRoleUseCase`/`RevokeAssignmentUseCase` (y, transitivamente, los 5 casos de uso que delegan en ellos), una consulta inmediatamente posterior para el mismo `(userId, applicationId)` refleja el cambio, nunca un dato obsoleto |
| 3 | Fail-open ante caída de Redis | Si Redis no responde, `ResolveActiveRolesUseCase` sigue resolviendo contra `AssignmentRepository` sin fallar — cero regresión funcional. Una escritura (`evict`) que falla no bloquea `AssignRoleUseCase`/`RevokeAssignmentUseCase`: es best-effort, respaldado por el TTL corto |
| 4 | La entrada no persiste indefinidamente | La clave `active-roles:{userId}:{applicationId}` lleva TTL (`pdp.cache.active-roles.retention`, default `PT60S`), incluso siendo invalidada por evento |
| 5 | Métrica de acierto/fallo | `ObservedDistributedCachePort` registra un contador por resultado (`hit`/`miss`/`evict`) en Micrometer, expuesto en `/actuator/metrics` |
| 6 | `verificar.ps1` (suite completa) sigue en verde | `mvnw -f pdp/pom.xml verify`, cobertura ≥ 50 % por paquete nuevo |

## 3. Reglas de negocio

No hay una regla de negocio que rechace nada — esta historia no introduce ningún value object ni
ninguna restricción de negocio nueva. `DistributedCachePort` es una decisión de **infraestructura**
(caché-aside con invalidación explícita), no una regla de dominio: no hay fila de tabla clásica
porque no hay excepción que lanzar. Documentado aquí en vez de omitido, mismo criterio que
PLAN-HU-022.md §3.

## 4. Modelo de dominio afectado

Ninguna entidad ni value object nuevo. El puerto habla en tipos ya existentes:
`co.edu.uco.seguridad.pdp.commons.model.UserId`, `ApplicationId`, `RoleId` (los tres ya usados por
2+ slices, en `pdp/commons/model/`) y `java.util.Set`.

## 5. Persistencia — Redis, no SurrealDB

- **Almacén:** Redis (mismo servicio que HU-022, ya desplegado — sin cambios a
  `docker-compose.yml`).
- **Forma de la clave:** `active-roles:{userId}:{applicationId}` → valor: los `RoleId` (UUID) unidos
  por coma, o cadena vacía si el sujeto no tiene ningún rol activo en esa aplicación (estado válido,
  **distinto** de "no está en caché": una clave ausente en Redis es un *miss* — `Mono.empty()` en el
  puerto —, una clave presente con valor `""` es un *hit* con conjunto vacío).
- **TTL:** `pdp.cache.active-roles.retention` (nueva propiedad, tipo `Duration`, default `PT60S`) —
  red de seguridad de respaldo: acota cuánto puede durar una entrada obsoleta si un `evict()`
  llegara a fallar silenciosamente contra Redis (ver §7, fail-open en escritura).
- **Puerto:**
  ```java
  Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId);
  Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds);
  Mono<Void> evict(UserId subject, ApplicationId applicationId);
  ```
- **Sin inicializador de esquema:** igual que HU-022, Redis no tiene DDL.

## 6. Endpoint

*(Sección eliminada — esta historia no expone HTTP nuevo. Es transparente para
`ActiveRoleNamesLookupValidatorImpl` y para cualquier endpoint que ya consuma
`ResolveActiveRolesUseCase`.)*

## 7. SPEC — el contrato

### Contratos nuevos

```java
// shared/cache/DistributedCachePort.java
public interface DistributedCachePort {
    Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId);
    Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds);
    Mono<Void> evict(UserId subject, ApplicationId applicationId);
}
```

> No extiende `ReactiveOperation<I,O>` de `shared/contract` a propósito — mismo criterio que
> `TokenRevocationPort` (HU-022): tres operaciones, no una.
>
> **Las tres operaciones son fail-open por contrato: ninguna propaga un error de Redis.** `get`
> resuelve `Mono.empty()` (se trata igual que un *miss*) si Redis no responde; `put`/`evict`
> completan igual (`Mono<Void>` que siempre llega a `onComplete`), registrando el fallo solo en el
> log. Es la pieza que hace que los tres puntos de invocación (`ResolveActiveRolesUseCaseImpl`,
> `AssignRoleUseCaseImpl`, `RevokeAssignmentUseCaseImpl`) no necesiten manejar el error ellos
> mismos — lo mismo que decidió el usuario en §11: `evict` es *best-effort*, respaldado por el TTL
> corto de §5. Contraste deliberado con `TokenRevocationPort` (HU-022), que sí es fail-closed en las
> dos direcciones porque protege una decisión de seguridad, no de rendimiento (ADR-026).

### Firmas de la pieza de infraestructura

```java
// shared/cache/RedisDistributedCachePort.java
public final class RedisDistributedCachePort implements DistributedCachePort {
    public RedisDistributedCachePort(ReactiveRedisTemplate<String, String> redis, ActiveRolesCacheRetentionProperties properties) { }
    // implementa get / put / evict — fail-open interno (ver arriba), reutiliza el mismo bean
    // ReactiveRedisTemplate<String,String> que ya publica RedisConfiguration para HU-022.
}
```

```java
// shared/cache/ObservedDistributedCachePort.java
public final class ObservedDistributedCachePort implements DistributedCachePort {
    public ObservedDistributedCachePort(DistributedCachePort delegate, MeterRegistry metrics) { }
    // decora: delega y registra un contador pdp.cache.active_roles{outcome=hit|miss|evict} tras
    // cada operación — mismo patrón que ObservedAccessAuditRepository (Micrometer, sin lógica
    // propia de negocio).
}
```

```java
// shared/cache/ActiveRolesCacheRetentionProperties.java
@ConfigurationProperties("pdp.cache.active-roles")
public record ActiveRolesCacheRetentionProperties(Duration retention) { }
```

### Firmas nuevas en puertos existentes

Ninguna — `DistributedCachePort` es un puerto nuevo.

### [M] Firmas que cambian (el implementador las aplica, no se generan como esqueleto)

```java
// pdp/assignments/application/usecase/impl/AssignRoleUseCaseImpl.java — [M]: constructor gana
// DistributedCachePort. Tras guardar la asignación, invoca
// cache.evict(response.userId(), response.applicationId()) antes de devolver la respuesta —
// best-effort (el puerto nunca propaga error, ver §7), así que no cambia el camino de error
// existente del caso de uso.
```

```java
// pdp/assignments/application/usecase/impl/RevokeAssignmentUseCaseImpl.java — [M]: mismo patrón —
// constructor gana DistributedCachePort (además del TokenRevocationPort que ya tiene desde HU-022).
// Tras revocar, invoca cache.evict(assignment.userId(), assignment.applicationId()).
```

```java
// pdp/assignments/application/usecase/impl/ResolveActiveRolesUseCaseImpl.java — [M]: constructor
// gana DistributedCachePort. execute() intenta cache.get(input.userId(), input.applicationId())
// primero; en el miss (Mono.empty(), que ya cubre tanto "no está en caché" como "Redis no
// respondió", por el fail-open de §7) consulta AssignmentRepository como hoy y puebla la caché con
// cache.put(...) antes de devolver la respuesta.
```

```java
// shared/config/RedisConfiguration.java — [M]: agrega
// @Bean ActiveRolesCacheRetentionProperties (vía @EnableConfigurationProperties) y
// @Bean DistributedCachePort distributedCachePort(ReactiveRedisTemplate<String,String> redis,
//         ActiveRolesCacheRetentionProperties properties, MeterRegistry metrics) {
//     return new ObservedDistributedCachePort(new RedisDistributedCachePort(redis, properties), metrics);
// }
// Reutiliza el mismo bean ReactiveRedisTemplate<String,String> que ya publica para HU-022 — sin
// @Primary nuevo, sin segundo bean de conexión.
```

> Nota de mensaje reutilizado: el constructor de `RedisDistributedCachePort` valida su
> `ReactiveRedisTemplate<String,String>` con la constante ya existente
> `RequiredArgumentMessages.REACTIVE_REDIS_TEMPLATE` (creada en HU-022, texto "se requiere el
> ReactiveRedisTemplate de revocación") — decisión consciente de no tocar ese texto ni la clase de
> HU-022 ya validada solo para precisar la redacción; el argumento sigue siendo, literalmente, el
> mismo bean.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
├── cache/
│   ├── DistributedCachePort.java                          [N]
│   ├── RedisDistributedCachePort.java                      [N]
│   ├── ObservedDistributedCachePort.java                   [N]
│   └── ActiveRolesCacheRetentionProperties.java            [N]
└── config/
    └── RedisConfiguration.java                              [M] — @Bean DistributedCachePort
                                                                    (Redis + decorador Observed)

pdp/src/main/resources/application.properties                [M] — pdp.cache.active-roles.retention

pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/application/usecase/impl/
├── AssignRoleUseCaseImpl.java                                [M]
├── RevokeAssignmentUseCaseImpl.java                          [M]
└── ResolveActiveRolesUseCaseImpl.java                        [M]

pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/infrastructure/config/
└── AssignmentsConfiguration.java                              [M] — inyecta DistributedCachePort en
                                                                      los tres beans de arriba
```

No se toca `identity`, `authorization`, `roles`, `profiles`, `resources`, `tenants`, `applications`
— igual que HU-022. En particular, `ActiveRoleNamesLookupValidatorImpl` (`authorization`) **no
cambia**: sigue llamando a `ResolveActiveRolesUseCase.execute(...)` exactamente igual, sin saber que
ahora hay una caché debajo — la transparencia del cambio es la prueba de que el diseño está en la
capa correcta.

## 9. Casos de prueba esperados

> `pdp/src/test` no lo toco yo (regla del planificador). Lo que sigue es lo que `@2-tester-spec`
> debe escribir — descripción del caso, no el código.

| Capa | Clase de prueba | Casos |
|---|---|---|
| `infrastructure` (unitaria, sin Spring) | `ObservedDistributedCachePortTests` | (a) `get` que resuelve con un conjunto → delega, registra `outcome=hit`; (b) `get` vacío → delega, registra `outcome=miss`; (c) `evict` → delega, registra `outcome=evict`; verificado con `SimpleMeterRegistry` real y un delegado fake (lambdas), mismo patrón que `ObservedAccessAuditRepositoryTests` |
| `infrastructure` (Testcontainers, reutiliza `AbstractRedisIntegrationTest` de HU-022 — no crea uno nuevo) | `RedisDistributedCachePortTests` | (a) `get` antes de cualquier `put` → vacío (miss); (b) `put` con un conjunto no vacío y luego `get` → el mismo conjunto; (c) `put` con conjunto vacío y luego `get` → conjunto vacío (hit, no miss — distingue de (a)); (d) `evict` tras `put` → `get` vuelve a vacío; (e) tras `put`, la clave tiene TTL (`ttl > 0`); (f) contra un `ReactiveRedisTemplate` construido con un host/puerto que no responde, `get`/`put`/`evict` completan sin propagar error (fail-open, §7) |
| `application` (extiende pruebas existentes) | `AssignRoleUseCaseImplTests` | + un caso: tras asignar, el fake de `DistributedCachePort` capturó `evict(userId, applicationId)` con los valores correctos de la respuesta |
| `application` (extiende pruebas existentes) | `RevokeAssignmentUseCaseImplTests` | + un caso equivalente, con el `userId`/`applicationId` de la asignación revocada |
| `application` (extiende pruebas existentes) | `ResolveActiveRolesUseCaseImplTests` | + dos casos: (a) `DistributedCachePort.get` resuelve con un conjunto → el resultado sale de ahí, el fake de `AssignmentRepository` nunca se consulta (verificado con un repositorio *unreachable*, mismo patrón que ya usan las pruebas de `assignments`); (b) `get` vacío → consulta `AssignmentRepository` como hoy, y el fake de `DistributedCachePort` capturó `put(userId, applicationId, roleIds)` con el resultado |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-16 |
| Contrato aprobado (gate 1) | ✅ Cerrado | 2026-09-16 |
| Pruebas en rojo | ✅ Confirmado (13 pruebas nuevas, color correcto) | 2026-09-16 |
| Implementación en verde | ✅ 750/750, `clean verify` | 2026-09-16 |
| Validación | ✅ APROBADO — ver REPORTE-HU-023.md | 2026-09-16 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes

**Resueltas con Sebastián el 2026-09-16** (no reabrir):

1. **Mecanismo de invalidación** → llamada directa desde los dos puntos raíz (`AssignRoleUseCaseImpl`,
   `RevokeAssignmentUseCaseImpl`), no un evento de dominio vía `DomainEventPublisher`. Ver §1.1: los
   otros 5 casos de uso que mutan una asignación ya delegan en estos dos, así que la cobertura es
   completa sin tocarlos.
2. **TTL de respaldo** → sí, corto (`pdp.cache.active-roles.retention`, default `PT60S`). Habilita
   que `evict()` sea *best-effort* (fail-open también en escritura) de forma segura: si una
   invalidación falla contra Redis, el dato obsoleto vence solo, no permanece indefinidamente.
3. **Métrica de acierto/fallo** → sí, en esta misma historia, vía `ObservedDistributedCachePort`
   (Micrometer), mismo patrón que `ObservedAccessAuditRepository`.

Sin ambigüedades pendientes — las 3 preguntas abiertas de HU-023.md quedaron resueltas arriba.
