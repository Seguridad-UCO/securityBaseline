# Reporte de validacion — HU-023

## Metadata

- **Slice:** `shared` (capacidad técnica transversal, con dos puntos de invocación en `assignments`)
- **Fecha:** 2026-09-16
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-023.md`
- **Rama:** `feature/HU-022-revocacion-tokens-redis` (el trabajo de HU-023 está encima, sin commit propio todavía — ver Datos para la entrega)

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 131,6s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 750, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | ✅ |
| Pruebas | ✅ 750 pruebas, 0 fallos |
| Cobertura (≥ 50 % por paquete) | ✅ ningún paquete bajo el umbral |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |

`consistencia.ps1` → limpio (8 slices verificados). `drift.ps1` → 1 hallazgo
(`SurrealAdministrationEventSchemaInitializer` citado en `CHECKPOINT.md`), preexistente de
HU-020/HU-021 — confirmado ajeno (ver Observaciones; `CHECKPOINT.md` no aparece entre los archivos
que HU-023 toca). `mapa.ps1` → regenerado, 716 clases de producción / 236 de prueba / 9 slices, sin
cambio de conteo respecto a la corrida anterior a esta validación (nada quedó sin registrar).

## Estado final

> ✅ APROBADO — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Deriva] — `SurrealAdministrationEventSchemaInitializer` citado en `CHECKPOINT.md` no existe

- **Archivo:** `pdp/docs/ai-harness/CHECKPOINT.md`
- **Preexistente, no introducido por HU-023:** `git status`/`git diff` sobre el árbol de cambios de
  esta historia no incluye `CHECKPOINT.md` en absoluto (se actualiza recién ahora, como parte del
  cierre de este mismo reporte). El hallazgo viene de HU-020/HU-021. Regla invariante 5 de este
  agente: "La deriva preexistente es observación; la nueva es bloqueante."

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | #1 segunda lectura sirve desde caché: `ResolveActiveRolesUseCaseImplTests.serves_from_the_cache_without_querying_the_repository_on_a_hit` (repositorio *unreachable*, nunca se consulta). #2 invalidación inmediata: `AssignRoleUseCaseImplTests.evicts_the_cache_for_the_user_and_application_after_assigning` + `RevokeAssignmentUseCaseImplTests.evicts_the_cache_for_the_user_and_application_after_revoking`. #3 fail-open ante caída de Redis: `RedisDistributedCachePortTests.get_put_and_evict_never_propagate_an_error_when_redis_is_unreachable` (contra un `ReactiveRedisTemplate` apuntando a un puerto cerrado). #4 TTL: `RedisDistributedCachePortTests.the_cache_key_has_a_ttl_after_put`. #5 métrica: los 3 casos de `ObservedDistributedCachePortTests` (`outcome=hit/miss/evict`). #6 `verify` en verde: ✅, 750/750, sin infractores de cobertura |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Identificadores (`DistributedCachePort`, `RedisDistributedCachePort`, `ObservedDistributedCachePort`, `get`/`put`/`evict`) en inglés; Javadoc de las 4 clases nuevas y de los `[M]` en español; mensajes nuevos de `RequiredArgumentMessages` (`DISTRIBUTED_CACHE_PORT`, `ACTIVE_ROLES_CACHE_RETENTION`, etc.) en español; el log de degradación de `RedisDistributedCachePort` también en español ("Redis no respondió para la caché de roles activos...") |
| 3 | ¿Introdujo deriva doc↔código? | ✅ (sin deriva nueva) | `drift.ps1` reporta 1 hallazgo, confirmado preexistente y ajeno (ver Observaciones) |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | El caché-aside (`cache.get` → miss → `repository` → `cache.put`) vive en `ResolveActiveRolesUseCaseImpl` (application), como orquestación de puertos, no como regla — no hay decisión de negocio, solo una estrategia de lectura. La serialización (`RoleId` unidos por coma) y el fail-open (`onErrorResume`) viven en `RedisDistributedCachePort` (infraestructura), no se filtran a `application`. Cero anotaciones de Spring en `shared/cache/*` (clases Java puras, cableadas a mano en `RedisConfiguration`). Ningún `allowedDependencies` de Modulith se relajó — `shared` es `Type.OPEN`, y `ModulithStructureTests` pasa sin tocar ninguna frontera de `pdp`. No hay `if/throw` de negocio nuevo en ningún use case: `AssignRoleUseCaseImpl`/`RevokeAssignmentUseCaseImpl` solo encadenan la eviction tras guardar, sin decidir nada |

## Criterios de la línea base

> Solo los que el plan declaró (§0, metadata): 1, 2, 4, 7, 8, 9, 11, 12, 21, 22, 23.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ 🤖 | `LayeredArchitectureTests`/`ModulithStructureTests` verdes; cero Spring en `shared/cache/*` |
| 2 | Contratos de servicios | ✅ | `DistributedCachePort` no extiende `ReactiveOperation<I,O>` a propósito (3 operaciones, no 1) — mismo criterio que `TokenRevocationPort`/`AssignmentRepository`, documentado en la SPEC |
| 4 | Capacidades transversales | ✅ | `TimeProvider` reutilizado en `ResolveActiveRolesUseCaseImpl` (ya lo tenía); ningún `Instant.now()`/`UUID.randomUUID()` nuevo en línea |
| 7 | Adaptadores de persistencia | ✅ | `RedisDistributedCachePort` implementa el puerto sin decidir negocio; clave como constantes (`KEY_PREFIX`, `SEPARATOR`), nunca literal disperso |
| 8 | Logging e instrumentación | ✅ | `ObservedDistributedCachePort` (contador Micrometer) + log estructurado de degradación en `RedisDistributedCachePort` (`LOG.atWarn()...addKeyValue(...)`, mismo estilo que `ObservedAccessAuditRepository`); no se registra ningún dato sensible (solo `UserId`/`ApplicationId`, ya expuestos en otros logs del proyecto) |
| 9 | Excepciones | ✅ | No hay excepción de negocio nueva — el fail-open captura y degrada (`onErrorResume`) en vez de traducir a una jerarquía `DomainException`, correcto: esto no es un fallo de negocio, es una degradación de rendimiento documentada por diseño (ADR-026) |
| 11 | Interacción entre capas | ✅ | Sin HTTP nuevo (§6 del plan, sección eliminada); `ActiveRoleNamesLookupValidatorImpl` no cambió — la caché es transparente para su único consumidor de lectura |
| 12 | SOLID | ✅ | `DistributedCachePort` con 3 operaciones mínimas; `ObservedDistributedCachePort` decora sin heredar; dependencias inyectadas por constructor contra interfaces |
| 21 | Modelo refinado | ✅ | `ActiveRolesCacheRetentionProperties` es un `record` inmutable con `Objects.requireNonNull` en el constructor compacto (ya no vacío) |
| 22 | Arquitectura reactiva | ✅ | `Mono<Set<RoleId>>`/`Mono<Void>` en todo el puerto; sin `block()` en `pdp/src/main` (el único `.block()` nuevo queda en `RedisDistributedCachePortTests`, fuera de esta comprobación) |
| 23 | Arquitectura antes del negocio | ✅ 🤖 | `./mvnw verify` en verde, 750/750, sin paquete bajo el 50 % de cobertura |

## Desviaciones respecto al plan

Ninguna. La firma, el árbol de archivos y el mecanismo de invalidación (los dos puntos raíz) se
aplicaron tal como quedaron fijados en el gate 1 — a diferencia de HU-022, aquí no hizo falta ningún
ajuste de cableado imprevisto (`@Primary`, indicador de salud, provisión de Redis en pruebas HTTP
existentes): `RedisConfiguration` ya tenía resuelta la ambigüedad de bean desde HU-022, y
`DistributedCachePort` es un tipo distinto sin colisión posible.

## Datos para la entrega

- **Mensaje de commit:** `feat(shared,assignments): caché distribuida de roles activos (HU-023)`
- **Cuerpo:** Introduce `DistributedCachePort`/`RedisDistributedCachePort` (Redis, clave
  `active-roles:{userId}:{applicationId}`, TTL de respaldo) y `ObservedDistributedCachePort`
  (métrica Micrometer `pdp.cache.active_roles{outcome}`). `ResolveActiveRolesUseCaseImpl` lee en
  caché-aside con degradación transparente a SurrealDB si Redis falla (fail-open, a diferencia del
  fail-closed de `TokenRevocationPort` de HU-022). `AssignRoleUseCaseImpl`/`RevokeAssignmentUseCaseImpl`
  invalidan la caché del sujeto afectado como efecto secundario automático — hallazgo de la
  planificación: estos dos son los únicos puntos raíz de escritura del catálogo (HU-015 a HU-020
  delegan en ellos), así que cubren las 7 rutas de mutación sin tocar `roles`, `profiles` ni
  duplicar nada por slice. No expone HTTP nuevo.
- **Rama:** `feature/HU-022-revocacion-tokens-redis` (el usuario decidió continuar HU-023 sobre la
  misma rama en curso, todavía no fusionada) — o una rama propia
  `feature/HU-023-cache-distribuida-roles-activos` si prefiere separarla; ambas opciones son
  correctas, es una decisión de flujo de git, no de esta validación.
- **Archivos a incluir:** todo lo listado en `git status --short` bajo `pdp/src/main`, `pdp/src/test`
  y `pdp/docs/ai-harness/PROJECT-MAP.md` (regenerado, refleja el estado real del código) — **no** el
  plan ni este reporte, que se versionan aparte en `pdp/docs/ai-harness/workspace/` (mismo criterio
  que HU-022, aunque en la práctica el equipo ya los ha comiteado junto con el código en historias
  anteriores — decisión del usuario al comitear).

## Próximos pasos

Listo para el gate 2 (entrega).
