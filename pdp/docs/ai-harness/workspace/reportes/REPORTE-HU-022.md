# Reporte de validacion — HU-022

## Metadata

- **Slice:** `shared` (capacidad técnica transversal, con dos puntos de invocación en `assignments`)
- **Fecha:** 2026-09-16
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-022.md`
- **Rama:** `feature/HU-022-revocacion-tokens-redis` (aún no creada — el trabajo está en `develop`)

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 136,8s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 737, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | ✅ |
| Pruebas | ✅ 737 pruebas, 0 fallos |
| Cobertura (≥ 50 % por paquete) | ✅ ningún paquete bajo el umbral |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |

`consistencia.ps1` → limpio (8 slices verificados). `drift.ps1` → 1 hallazgo (`SurrealAdministrationEventSchemaInitializer` citado en `CHECKPOINT.md`), preexistente de HU-020/HU-021 — ver Observaciones. `mapa.ps1` → regenerado, 712 clases de producción / 234 de prueba / 9 slices.

## Estado final

> ✅ APROBADO — sin bloqueantes.

## Bloqueantes

Ninguno. La pasada anterior de esta validación (misma fecha) había RECHAZADO por un paquete de
`applications` (`ApplicationRepository.findUniqueByName`, `default` sin cobertura) — confirmado
entonces como preexistente e independiente de HU-022. A pedido explícito del usuario, se resolvió
en el mismo cambio agregando `ApplicationRepositoryTests` (mismo patrón ya usado en
`ProfileRepositoryTests` para el mismo problema en otro slice), en vez de abrirlo como historia
técnica aparte. Con eso, `mvnw verify` está en verde.

## Observaciones menores

### [Alcance] — Dos correcciones ajenas a la SPEC literal de HU-022, incluidas a pedido del usuario

- **Archivos:** `pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/domain/package-info.java` (nuevo), `pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/package-info.java` (modificado), `pdp/src/test/java/co/edu/uco/seguridad/pdp/applications/application/secondaryport/repository/ApplicationRepositoryTests.java` (nuevo)
- **Qué son:** (1) el fix de Modulith preexistente e independiente de HU-022 (`EvaluateInternalAccessUseCaseImpl`, de la PR #55 de un compañero, consumía `applications.domain.Application` sin `@NamedInterface` — verificado con `git stash` que la violación existe también sin ningún cambio de HU-022); (2) la prueba que cierra la cobertura de `ApplicationRepository.findUniqueByName`.
- **Por qué es observación y no bloqueante:** ninguno de los dos está en el árbol de PLAN-HU-022.md §8, que además dice explícitamente "No se toca ... `applications`". En la validación anterior recomendé separarlos en cambios propios; el usuario decidió explícitamente incluirlos en el mismo commit que HU-022 ("mete todo junto"). Es una decisión suya de alcance del cambio, no un defecto de la historia — ambos fixes son correctos y necesarios para que el build esté verde por razones ajenas entre sí, y ninguno introduce lógica de negocio nueva en `applications`.

### [Alcance] — `management.health.redis.enabled=false` y `@Primary` en `RedisConfiguration`

- **Archivos:** `pdp/src/main/resources/application.properties`, `pdp/src/main/java/co/edu/uco/seguridad/shared/config/RedisConfiguration.java`
- **Qué es:** ninguno de los dos está en la SPEC literal del plan (§7/§8), pero ambos son correcciones necesarias que `@3-implementador` descubrió y documentó: sin `@Primary`, el contexto de Spring no arrancaba para *ninguna* prueba `@SpringBootTest` (bean `ReactiveRedisTemplate<String,String>` ambiguo con el `reactiveStringRedisTemplate` que autoconfigura Boot); sin deshabilitar el indicador de salud de Redis, `/actuator/health` devolvía 503 en cualquier prueba sin Redis vía Testcontainers, rompiendo `CorsConfigurationTests` y `SecurityWebFilterChainTests`. Ver "Desviaciones respecto al plan".

### [Deriva] — `SurrealAdministrationEventSchemaInitializer` citado en `CHECKPOINT.md` no existe

- **Archivo:** `pdp/docs/ai-harness/CHECKPOINT.md`
- **Preexistente, no introducido por HU-022:** `git diff --stat` sobre ese archivo no muestra cambios en esta sesión; su último commit (`b8c1ae6`, HU-020/HU-021) es anterior a este trabajo. Regla invariante 5 de este agente: "La deriva preexistente es observación; la nueva es bloqueante."

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | #1 inmediatez: `RedisTokenRevocationAdapterTests.is_revoked_when_issued_before_the_revoked_since_instant` + `RevocationAwareJwtDecoderTests.rejects_with_a_jwt_validation_exception_when_it_is_revoked`. #2 no revocado pasa: `RevocationAwareJwtDecoderTests.passes_the_jwt_through_when_it_is_not_revoked`. #3 fail-closed en caída de Redis: `RevocationAwareJwtDecoderTests.rejects_when_the_revocation_check_itself_fails` + `RevokeAssignmentUseCaseImplTests.fails_the_whole_operation_when_revocation_fails` (fail-closed también en escritura, §7). #4 TTL: `RedisTokenRevocationAdapterTests.the_revocation_key_has_a_ttl`. #5 `verify` en verde: ✅, 737/737, sin infractores de cobertura |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Identificadores (`TokenRevocationPort`, `RevocationAwareJwtDecoder`, `revokeAllSince`, `isRevoked`) en inglés; Javadoc de las clases nuevas y de los `[M]` en español; mensajes nuevos de `RequiredArgumentMessages` (`REACTIVE_REDIS_TEMPLATE`, `TOKEN_REVOCATION_PORT`, etc.) en español |
| 3 | ¿Introdujo deriva doc↔código? | ✅ (sin deriva nueva) | `drift.ps1` reporta 1 hallazgo, confirmado preexistente (ver Observaciones) |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | La comparación `issuedAt <= revokedSince` vive en `RedisTokenRevocationAdapter.isRevoked` (infraestructura, sobre datos ya leídos) y su interpretación en `RevocationAwareJwtDecoder` (también infraestructura — es el hook de la cadena de Spring Security, no un use case, tal como razona PLAN-HU-022.md §1). No hay `if/throw` de negocio en `RevokeAssignmentUseCaseImpl`/`RemoveApplicationAdministratorUseCaseImpl`: solo delegan al puerto. Cero anotaciones de Spring en `shared/security/revocation/*` (clases Java puras que reciben sus colaboradores por constructor). `RedisConfiguration`/`InternalSecurityConfiguration` concentran el único cableado consciente de Spring, como exige `sb-estandares`. Ningún `allowedDependencies` de Modulith se relajó para compilar — `ModulithStructureTests` pasa; el único ajuste de frontera (`applications :: aggregate` en `authorization`) declara una frontera nueva y real, no la relaja |

## Criterios de la línea base

> Solo los que el plan declaró (§0, metadata): 1, 2, 4, 7, 8, 9, 11, 12, 21, 22, 23.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ 🤖 | `LayeredArchitectureTests`/`ModulithStructureTests` verdes; cero Spring en `shared/security/revocation/*` |
| 2 | Contratos de servicios | ✅ | `TokenRevocationPort` es un puerto con dos operaciones (no fuerza `ReactiveOperation<I,O>`, documentado en la SPEC por qué), igual criterio que `AssignmentRepository` |
| 4 | Capacidades transversales | ✅ | `TimeProvider` reutilizado (no `Instant.now()` en línea) en `RevokeAssignmentUseCaseImpl`/`RemoveApplicationAdministratorUseCaseImpl` |
| 7 | Adaptadores de persistencia | ✅ | `RedisTokenRevocationAdapter` implementa el puerto sin decidir negocio; nombre de clave como constante (`KEY_PREFIX`), nunca literal disperso |
| 8 | Logging e instrumentación | N/A | Esta historia no agrega logging propio; no se registran tokens ni el valor del JWT en ningún punto nuevo |
| 9 | Excepciones | ✅ | `JwtValidationException` es de Spring Security, no de la jerarquía `DomainException` — correcto: PLAN-HU-022.md §1 explica por qué (es fallo de autenticación, no de negocio, así que no pasa por `ApiErrorHandler`) |
| 11 | Interacción entre capas | ✅ | El decoder se resuelve en `InternalSecurityConfiguration`, dentro de la cadena de Spring Security — no hay controller ni interactor nuevos (esta historia no expone HTTP, §6 del plan) |
| 12 | SOLID | ✅ | `TokenRevocationPort` con dos operaciones mínimas; `RevocationAwareJwtDecoder` decora sin heredar; dependencias inyectadas por constructor contra interfaces |
| 21 | Modelo refinado | ✅ | `RevocationRetentionProperties` es un `record` inmutable con `Objects.requireNonNull` en el constructor compacto (ya no vacío) |
| 22 | Arquitectura reactiva | ✅ | `Mono<Void>`/`Mono<Boolean>` en todo el puerto; sin `block()` en el camino de una petición |
| 23 | Arquitectura antes del negocio | ✅ 🤖 | `./mvnw verify` en verde, 737/737, sin paquete bajo el 50 % de cobertura |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `RedisConfiguration.java` | `@Bean ReactiveRedisTemplate<String,String>` sin más detalle (§8) | Agrega `@Primary` | Sí — sin él, Boot no puede elegir entre este bean y `reactiveStringRedisTemplate` (autoconfigurado por el starter), y el contexto de Spring no arranca para ninguna prueba. Documentado en el Javadoc de la clase |
| `application.properties` | Solo `pdp.security.revocation.retention` y `spring.data.redis.host/port` (§8) | Agrega `management.health.redis.enabled=false` | Sí — el indicador de salud de Redis (autoconfigurado por el starter, no pedido por ningún criterio de aceptación) tumbaba `/actuator/health` a 503 en pruebas sin Redis vía Testcontainers, rompiendo 2 pruebas preexistentes ajenas a esta historia |
| `AssignmentHttpTests.java`, `InternalSecurityChainIntegrationTests.java` | No declarados en el árbol §8 (el plan no anticipó tocar pruebas HTTP existentes) | Ambas ahora provisionan Redis (`AbstractRedisIntegrationTest.REDIS`, hecho `public` para eso) y la segunda vincula una identidad para `"evidence-subject"` | Sí — consecuencia directa de cablear `[M]` `InternalSecurityConfiguration` con el decoder real; sin esto, dos pruebas preexistentes se rompían |
| `pdp/applications/domain/package-info.java`, `pdp/authorization/package-info.java` | No declarados; plan dice explícitamente "No se toca ... applications" | Corrige una violación de Modulith preexistente e independiente | Sí, por decisión explícita del usuario de incluirlo en este mismo cambio en vez de separarlo (ver Observaciones) |
| `ApplicationRepositoryTests.java` | No declarado; el paquete que cubre pertenece a `applications`, fuera del alcance §8 | Cierra la cobertura de `ApplicationRepository.findUniqueByName` (gap preexistente, no relacionado con la revocación de tokens) | Sí, por decisión explícita del usuario de incluirlo en este mismo cambio en vez de abrirlo como historia técnica aparte |

## Datos para la entrega

- **Mensaje de commit:** `feat(shared,assignments): revocación de tokens con Redis (HU-022)`
- **Cuerpo:** Introduce `TokenRevocationPort`/`RedisTokenRevocationAdapter` (Redis, clave `revoked-since:{userId}` con TTL) y `RevocationAwareJwtDecoder`, que decora `internalEvidenceJwtDecoder` en el canal interno PEP→PDP para rechazar (401, fail-closed) un JWT emitido antes de la última revocación del sujeto — o sin `UserId` resoluble, o si Redis no responde. `RevokeAssignmentUseCaseImpl`/`RemoveApplicationAdministratorUseCaseImpl` invocan la revocación como efecto secundario automático tras revocar (fail-closed también en escritura). No expone HTTP nuevo. Incluye, por decisión explícita del usuario, dos correcciones ajenas a la SPEC de esta historia: el fix de un `@NamedInterface` de Modulith faltante en `applications.domain` (violación preexistente de la PR #55) y una prueba que cierra la cobertura de `ApplicationRepository.findUniqueByName` (gap preexistente en `applications`, ajeno a la revocación de tokens).
- **Rama:** `feature/HU-022-revocacion-tokens-redis`
- **Archivos a incluir:** todo lo listado en `git status --short` bajo `pdp/src/main`, `pdp/src/test`, `pdp/pom.xml`, `pdp/docker-compose.yml`, `pdp/src/main/resources/application.properties` — **no** el plan ni este reporte (`pdp/docs/ai-harness/workspace/planes/PLAN-HU-022.md`, `pdp/docs/ai-harness/workspace/reportes/REPORTE-HU-022.md`), que se versionan aparte. `pdp/docs/ai-harness/PROJECT-MAP.md` sí se incluye (regenerado por `mapa.ps1`, refleja el estado real del código).

## Próximos pasos

Listo para el gate 2 (entrega).
