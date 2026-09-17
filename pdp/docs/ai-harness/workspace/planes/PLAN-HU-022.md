# PLAN: Revocación de tokens — invalidar una sesión antes de que expire

## Metadata

- **ID:** HU-022
- **Slice:** `shared` (capacidad técnica transversal — no es un slice de negocio de `pdp`; ver §1)
- **Tipo:** Mixto (infraestructura + efecto secundario en dos casos de uso existentes)
- **Fecha:** 2026-09-16
- **Rama sugerida:** `feature/HU-022-revocacion-tokens-redis`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-022.md`, `security-platform-architecture/docs/01-governance/adr/ADR-026-redis-cache-and-token-revocation.md`, `ADR-018`, `ADR-025`, código real: `shared/config/SecurityConfiguration.java`, `KeycloakSecurityConfiguration.java`, `InternalSecurityConfiguration.java`, `shared/security/SecurityContext.java`, `PdpPrincipal.java`, `LocalUserPrincipal.java`, `pdp/identity/application/rule/validator/SubjectUserIdLookupValidator.java`. No hay event storming propio de BC-07 en `artefactos-referencia` (carpeta inexistente — tratado como no disponible, sb-fuentes). Decisiones de arquitectura confirmadas con Sebastián el 2026-09-16 (ver §11 — resueltas, no pendientes).
- **Criterios de la línea base que toca:** 1, 2, 4, 7, 8, 9, 11, 12, 21, 22, 23

## 1. Resumen funcional

Cierra la brecha declarada desde ADR-018: hoy nada puede invalidar una sesión antes de que expire
por sí sola. Introduce Redis como almacén de "sujetos con revocación activa" — no un conjunto de
`jti`, sino **un instante `revokedSince` por `UserId`** (decisión de Sebastián: cubre "todas las
sesiones de este sujeto", no una sesión puntual). Un token cuyo `issuedAt` sea anterior o igual a
`revokedSince` del sujeto se rechaza con 401, sin importar que no haya expirado.

**No cubre:** caché distribuida de roles activos (HU-023, mismo Redis, puerto separado); MFA
(HU-024); hosting de Redis en Azure (decisión de despliegue). Tampoco cubre revocar una sesión
puntual entre varias del mismo sujeto — la decisión explícita fue "todas desde T", no por `jti`.

### Dónde se verifica — hallazgo de esta planificación, no estaba en la HU original

El comentario "jti aquí no revoca nada todavía" vive en `SecurityConfiguration` (perfil
`!keycloak`, modo HMAC de desarrollo). Pero el perfil `keycloak` real —el que corre en toda prueba
manual del proyecto— **no revalida un JWT por petición en el navegador**: la sesión BFF es cookie
(`WebSessionServerSecurityContextRepository`), sin token que decodificar en cada request. El JWT
real de Keycloak sí viaja, íntegro, en el canal interno PEP→PDP: `BffSessionTokenResolver` (PEP) lo
saca de la sesión BFF y lo presenta como Bearer contra `/internal/v1/**`, validado por
`internalEvidenceJwtDecoder` (`InternalSecurityConfiguration`) — que hoy ni siquiera exige `jti`.

**Decisión (confirmada con Sebastián):** el gate vive en el canal interno PEP→PDP, decorando
`internalEvidenceJwtDecoder`. El modo dev/HMAC (`SecurityConfiguration`) queda fuera de esta
historia — es un canal de prueba directa, no el que protege el flujo real.

### El hook exacto — por qué no es un `Rule` ni un `if` en un use case

ADR-026 exige que la verificación ocurra "después de que `ReactiveJwtDecoder` valida
firma/issuer/claims... antes de resolverse en `PdpPrincipal`" — es decir, **dentro de la cadena de
autenticación de Spring Security**, no después. Un `Mono.error` lanzado ahí, con un tipo que Spring
Security reconoce como fallo de autenticación (`org.springframework.security.oauth2.jwt.JwtException`
o una subclase), produce 401 automáticamente vía `ApiAuthenticationEntryPoint` — **sin tocar
`ApiErrorHandler`**, que no conoce excepciones de autenticación (su jerarquía son excepciones de
*negocio*, ver `sb-estandares`). Verificarlo después, dentro de `SecurityContext.currentPrincipal()`
(la alternativa que se descartó), habría exigido inventar una excepción nueva y su mapeo a 401 —
más piezas para el mismo resultado, y tarde: el request ya habría pasado la autenticación.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Revocar invalida de inmediato | Tras `RevokeAssignmentUseCase`/`RemoveApplicationAdministratorUseCase`, cualquier JWT del sujeto afectado emitido antes de ese instante es rechazado (401) en el canal interno, aunque no haya expirado |
| 2 | No revocado sigue pasando | Un JWT válido, no expirado y sin revocación activa para su sujeto se acepta sin cambios de comportamiento observable |
| 3 | Fail-closed ante caída de Redis | Si Redis no responde (timeout, caído), la petición se rechaza — nunca se asume "no revocado" |
| 4 | La entrada no persiste indefinidamente | La clave `revoked-since:{userId}` lleva TTL — nunca una clave sin expiración |
| 5 | `verificar.ps1` (suite completa) sigue en verde | `mvnw -f pdp/pom.xml verify`, cobertura ≥ 50 % por paquete nuevo |

## 3. Reglas de negocio

No hay un value object nuevo con invariantes de formato — el dato es una pareja `(UserId, Instant)`
ya validada por los tipos existentes. La única regla es de **comportamiento del puerto**, no de
value object, así que no hay una fila de tabla clásica de rechazo: `TokenRevocationPort.isRevoked`
decide un booleano puro a partir de dos instantes (`issuedAt <= revokedSince`) — comparación, no
regla de negocio con excepción propia. Documentado aquí en vez de omitido para que quede explícito
por qué la sección está casi vacía.

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| 1 | Un JWT cuyo `issuedAt` es anterior o igual al `revokedSince` de su sujeto se rechaza | `RevocationAwareJwtDecoder` (comparación síncrona sobre el resultado de `isRevoked`) | `TokenRevocationPort.isRevoked` | `JwtValidationException` (Spring Security) → **401**, vía `ApiAuthenticationEntryPoint` — no pasa por `ApiErrorHandler` |
| 2 | Si Redis no responde, la petición se rechaza igual que si estuviera revocada | `RedisTokenRevocationAdapter.isRevoked` propaga el error; `RevocationAwareJwtDecoder` no distingue "revocado" de "no se pudo verificar" en el efecto (sí en el log) | — | mismo 401 |

## 4. Modelo de dominio afectado

No hay entidad ni agregado nuevo. Ningún value object nuevo: el puerto habla directamente en
`co.edu.uco.seguridad.pdp.commons.model.UserId` (ya existe, usado por 2+ slices) y `java.time.Instant`.

### Value objects

| VO | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|
| `UserId` | Existente | — | `pdp/commons/model/` |

## 5. Persistencia — Redis, no SurrealDB

- **Almacén:** Redis (nuevo, `spring-boot-starter-data-redis-reactive` — Lettuce reactivo).
- **Forma de la clave:** `revoked-since:{userId}` → valor: instante ISO-8601 (el `revokedSince` más
  reciente para ese sujeto; una revocación posterior sobrescribe, nunca acumula).
- **TTL:** `pdp.security.revocation.retention` (nueva propiedad, tipo `Duration`, default `PT1H`) —
  después de ese tiempo cualquier token con `issuedAt` anterior ya habría expirado por sí solo de
  todas formas, así que la clave deja de aportar y puede vencer.
- **Puerto:**
  ```java
  Mono<Void> revokeAllSince(UserId subject, Instant since);
  Mono<Boolean> isRevoked(UserId subject, Instant issuedAt);
  ```
- **Sin inicializador de esquema:** Redis no tiene DDL — la clave se crea al primer `SET`.

## 6. Endpoint

*(Sección eliminada — esta historia no expone HTTP nuevo. La revocación es efecto secundario
interno de `RevokeAssignmentUseCase` y `RemoveApplicationAdministratorUseCase`, decisión confirmada
con Sebastián; no hay endpoint propio "cerrar mi sesión" en este alcance.)*

## 7. SPEC — el contrato

### Contratos nuevos

```java
// shared/security/revocation/TokenRevocationPort.java
public interface TokenRevocationPort {
    Mono<Void> revokeAllSince(UserId subject, Instant since);
    Mono<Boolean> isRevoked(UserId subject, Instant issuedAt);
}
```

> No extiende `ReactiveOperation<I,O>` de `shared/contract` a propósito: tiene dos operaciones, no
> una. Mismo criterio que `TenantRepository`/`AssignmentRepository` — un puerto con más de un método
> no fuerza un contrato de una sola operación.

### Firmas de la pieza de infraestructura

```java
// shared/security/revocation/RedisTokenRevocationAdapter.java
public final class RedisTokenRevocationAdapter implements TokenRevocationPort {
    public RedisTokenRevocationAdapter(ReactiveRedisTemplate<String, String> redis, RevocationRetentionProperties properties) { }
    // implementa revokeAllSince / isRevoked
}
```

```java
// shared/security/revocation/RevocationAwareJwtDecoder.java
public final class RevocationAwareJwtDecoder implements ReactiveJwtDecoder {
    public RevocationAwareJwtDecoder(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
            SubjectUserIdLookupValidator subjectUserIdLookup) { }
    // decode(String token) -> Mono<Jwt>: delega, resuelve UserId por subject, consulta isRevoked,
    // erroa con JwtValidationException si revocado (o si Redis falla).
}
```

```java
// shared/security/revocation/RevocationRetentionProperties.java
@ConfigurationProperties("pdp.security.revocation")
public record RevocationRetentionProperties(Duration retention) { }
```

### Firmas nuevas en puertos existentes

Ninguna — `TokenRevocationPort` es un puerto nuevo, no una firma añadida a uno existente.

### [M] Firmas que cambian (el implementador las aplica, no se generan como esqueleto)

```java
// shared/security/SecurityContext.java — SIN CAMBIOS DE FIRMA. No se toca: la verificación vive en
// el decoder, antes de que exista un PdpPrincipal. Se documenta aquí solo para que quede explícito
// que se evaluó y se descartó (ver §1, "El hook exacto").
```

```java
// shared/config/InternalSecurityConfiguration.java — [M]: el bean internalEvidenceJwtDecoder pasa
// de construir el NimbusReactiveJwtDecoder directo a envolverlo:
@Bean
ReactiveJwtDecoder internalEvidenceJwtDecoder(InternalEvidenceJwtProperties properties,
        TokenRevocationPort revocation, SubjectUserIdLookupValidator subjectUserIdLookup) {
    ReactiveJwtDecoder delegate = /* la construcción Nimbus que ya existe, sin cambios */;
    return new RevocationAwareJwtDecoder(delegate, revocation, subjectUserIdLookup);
}
```

```java
// pdp/assignments/application/usecase/impl/RevokeAssignmentUseCaseImpl.java — [M]: constructor gana
// TokenRevocationPort y TimeProvider (TimeProvider ya está inyectado hoy — se reutiliza, no se
// añade). Tras revocar la asignación, invoca revocation.revokeAllSince(request.userId(), now) antes
// de completar.
```

```java
// pdp/assignments/application/usecase/impl/RemoveApplicationAdministratorUseCaseImpl.java — [M]:
// mismo patrón — tras remover al administrador, invoca
// revocation.revokeAllSince(request.userId(), now).
```

> Ambos [M] son la aplicación literal del criterio de aceptación #1 (§2) y de la decisión "efecto
> secundario automático desde HU-018/HU-020" (§11, resuelta). El implementador no decide *si*
> invocar la revocación — eso ya está fijado aquí; decide únicamente el detalle de cableado del
> `Mono` (probablemente `.then(Mono.defer(() -> revocation.revokeAllSince(...)))` antes del
> `.thenReturn`/resultado final, sin bloquear el camino feliz si la revocación fallara — **no**: al
> contrario del puerto de caché de HU-023, este puerto es fail-closed también en escritura: si
> `revokeAllSince` falla, la operación completa debe fallar, porque un "removido pero no revocado"
> es exactamente el hueco de seguridad que esta historia cierra. Documentarlo aquí porque es
> fácil copiar por error el patrón fail-open de HU-023.)

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
├── security/
│   └── revocation/
│       ├── TokenRevocationPort.java                    [N]
│       ├── RedisTokenRevocationAdapter.java             [N]
│       └── RevocationAwareJwtDecoder.java                [N]
├── security/
│   └── RevocationRetentionProperties.java                [N]
└── config/
    ├── RedisConfiguration.java                            [N] — @Bean ReactiveRedisTemplate<String,String>,
    │                                                          @Bean TokenRevocationPort (RedisTokenRevocationAdapter)
    └── InternalSecurityConfiguration.java                 [M] — decora internalEvidenceJwtDecoder

pdp/docker-compose.yml                                      [M] — servicio `redis`, mismo patrón que
                                                                    surrealdb/keycloak (imagen oficial,
                                                                    puerto 6379, sin volumen — el dato
                                                                    es descartable por diseño)

pdp/src/main/resources/application.properties               [M] — pdp.security.revocation.retention,
                                                                    spring.data.redis.host/port

pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/application/usecase/impl/
├── RevokeAssignmentUseCaseImpl.java                        [M]
└── RemoveApplicationAdministratorUseCaseImpl.java          [M]

pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/infrastructure/config/
└── AssignmentsConfiguration.java                           [M] — inyecta TokenRevocationPort en los
                                                                    dos beans de arriba
```

No se toca `identity`, `authorization`, `roles`, `profiles`, `resources`, `tenants`, `applications`:
esta historia es enteramente `shared` + dos puntos de invocación en `assignments`.

## 9. Casos de prueba esperados

> `pdp/src/test` no lo toco yo (regla del planificador). Lo que sigue es lo que `@2-tester-spec`
> debe escribir — descripción del caso, no el código.

| Capa | Clase de prueba | Casos |
|---|---|---|
| `infrastructure` (unitaria, sin Spring) | `RevocationAwareJwtDecoderTests` | (a) `SubjectUserIdLookupValidator` no resuelve `UserId` para el `subject` → error, fail-closed (resuelto §11 — sin `UserId` no se puede demostrar que el token no esté revocado, mismo criterio que Redis caído); (b) `isRevoked` devuelve `false` → el `Jwt` pasa igual; (c) `isRevoked` devuelve `true` → error de tipo `JwtValidationException`; (d) `isRevoked` propaga un error (Redis caído) → error igual (fail-closed); (e) delegate falla (firma/issuer inválidos) → nunca se llega a consultar Redis ni al lookup de `UserId` |
| `infrastructure` (Testcontainers, imagen `redis:7-alpine`, `GenericContainer` — sin módulo oficial, mismo criterio que SurrealDB) | `RedisTokenRevocationAdapterTests` | (a) `isRevoked` antes de cualquier `revokeAllSince` → `false`; (b) tras `revokeAllSince(subject, T)`, `isRevoked(subject, issuedAt < T)` → `true`; (c) `isRevoked(subject, issuedAt > T)` → `false` (token emitido después de la revocación es válido); (d) `revokeAllSince` dos veces con instantes distintos → prevalece el más reciente (`SET`, no acumula); (e) la clave tiene TTL (`ttl > 0` tras `revokeAllSince`) |
| `application` (extiende pruebas existentes) | `RevokeAssignmentUseCaseImplTests` | + un caso: tras revocar, el fake de `TokenRevocationPort` capturó `revokeAllSince(userId, now)` con el `userId` correcto |
| `application` (extiende pruebas existentes) | `RemoveApplicationAdministratorUseCaseImplTests` | + un caso equivalente |
| `application` (extiende pruebas existentes, camino de fallo) | ambas de arriba | + un caso: si el fake de `TokenRevocationPort.revokeAllSince` falla, la operación completa falla (fail-closed en escritura, ver §7) |

Necesita, además, un `AbstractRedisIntegrationTest` (mismo patrón que `AbstractSurrealDbIntegrationTest`,
`GenericContainer` + `@DynamicPropertySource`) — no existe todavía; lo crea `@2-tester-spec`, no yo.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-16 |
| Contrato aprobado (gate 1) | ✅ Cerrado | 2026-09-16 |
| Pruebas en rojo | ✅ Confirmado | 2026-09-16 |
| Implementación en verde | ✅ 736/736 | 2026-09-16 |
| Validación | ⛔ RECHAZADO — ver REPORTE-HU-022.md (bloqueante ajeno: cobertura preexistente en `applications`) | 2026-09-16 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades

**Resueltas con Sebastián el 2026-09-16** (no reabrir):
1. Punto de verificación → canal interno PEP→PDP, decorando `internalEvidenceJwtDecoder`.
2. Quién dispara la revocación → efecto secundario automático de `RevokeAssignmentUseCase` (HU-018)
   y `RemoveApplicationAdministratorUseCase` (HU-020). Sin endpoint propio "cerrar sesión" en este
   alcance.
3. Alcance de la revocación → `revokedSince` por `UserId` ("todo lo anterior a T"), no un conjunto
   de `jti`.

**Resuelta con Sebastián el 2026-09-16** (no reabrir):

4. **`RevocationAwareJwtDecoder` sin `UserId` resuelto para el `subject` del JWT → fail-closed.**
   El JWT puede ser criptográficamente válido, pero sin `UserId` no hay con qué consultar su estado
   de revocación — "no se puede demostrar que no esté revocado" se trata igual que Redis caído: se
   rechaza. La alternativa (dejarlo pasar) sería fail-open exactamente en el punto donde el sistema
   no puede verificar la condición de seguridad — descartada.

   ```text
   JWT → validación criptográfica → subject → UserId
     ¿UserId existe?
       Sí → consultar revocación → revocado: rechaza · no revocado: acepta
       No → rechaza (fail-closed)
   ```

   No se inventa un puerto ni un adaptador nuevo para este caso: `RevocationAwareJwtDecoder` usa el
   mismo `SubjectUserIdLookupValidator` que ya tiene inyectado (§7) — lo único que cambia es qué
   hace con un resultado vacío. Contrato fijado para `RevocationAwareJwtDecoderTests` caso (a), §9:

   ```text
   Given: JWT válido
   And: SubjectUserIdLookupValidator no encuentra UserId
   When: RevocationAwareJwtDecoder decodifica el JWT
   Then: la autenticación es rechazada
   And: no se permite continuar sin verificar revocación (nunca se llega a consultar isRevoked)
   ```
