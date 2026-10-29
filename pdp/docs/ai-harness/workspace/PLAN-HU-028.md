# PLAN: HU-028 (replanteada) — Logout con revocación acotada por canal (delta sobre la implementación previa)

## Metadata

- **ID:** HU-028
- **Slice:** `shared` (`shared/security/revocation`, `shared/web/session`); no hay slice de negocio.
- **Tipo:** Escritura (efecto de seguridad) — es un **DELTA**: la implementación previa ya existe.
- **Fecha:** 2026-10-28
- **Rama sugerida:** `feature/HU-028-logout-revocacion-por-cliente`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-028.md` (replanteada), ADR-034, ADR-026, ADR-025,
  `HANDOFF-VALIDACION-HU-027-029.md`, código de `shared/security/revocation`, `shared/web/session`,
  `shared/config/{Keycloak,Internal,}SecurityConfiguration`, `ApiErrorHandler`,
  `ApiAuthenticationEntryPoint`, `InternalSecurityChainIntegrationTests`.
- **Criterios de la línea base que toca:** 1, 2, 4, 5, 7, 9, 11, 12, 20, 21, 22, 23.
- **Orden de integración:** HU-029 → **HU-028** → HU-030. En esta HU **no** se retira
  `RefreshTokenStorePort` de `SessionRevocationController` ni de `KeycloakLogoutController`.

## 1. Resumen funcional

El logout del panel (cookie BFF) sigue siendo **global**: `revokeAllSince(usuario, T)`, borra el refresh
del BFF, invalida la sesión web. `DELETE /api/v1/session` con **bearer** deja de ser global: revoca el
`jti` presentado y los tokens del mismo usuario emitidos al mismo cliente (`azp`) hasta T; no toca otros
clientes, ni el refresh del BFF, ni la sesión BFF. Sin `azp` solo revoca el `jti`. Se añade la prueba E2E
con un bearer real firmado con un JWKS local. **No cubre:** config Keycloak, renovación (HU-029/030),
revocación por evento de otros casos de uso, `jwtDecoder` de perfiles no-keycloak.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | `GET /api/v1/session/logout` (cookie) | Sin cambios: global; token anterior → 401 después. `KeycloakLogoutController` **no se modifica** |
| 2 | `DELETE /api/v1/session` bearer con `azp` | 204; el mismo JWT y otros del mismo `sub`+`azp` con `iat <= T` → 401 |
| 2b | Otro cliente del mismo sub / sesión BFF | Otro cliente sigue 200; sesión BFF y refresh BFF intactos. Sin `azp`: solo su `jti` muere |
| 3 | Revocación falla (Redis) | 503 envelope `DEPENDENCY_UNAVAILABLE`; nada más se ejecuta (sesión BFF no invalidada) |
| 4 | Otro sujeto | Intacto |
| 5 | Panel | Logout global como hoy |
| 6 | `verificar.ps1` | Verde (los 2 errores/violaciones preexistentes del handoff no son de esta HU) |

## 3. Reglas de negocio

No hay `Rule` ni caso de uso: es política técnica de `shared` (adaptador primario → puerto de `shared`),
no hay slice de negocio. Decisiones, todas síncronas salvo el I/O del puerto:

| # | Regla | Dónde vive | Puerto | Excepción → HTTP |
|---|---|---|---|---|
| 1 | Canal = tipo de principal: `Jwt` = bearer; `LocalUserPrincipal`/`PdpPrincipal`/`Principal` = cookie BFF | `SessionRevocationController` (adaptador primario) | — | — |
| 2 | Cookie: `revokeAllSince` global (+ jti si hubiera) + borrar refresh + invalidar sesión, **en ese orden** | ídem | `TokenRevocationPort`, `RefreshTokenStorePort` | fallo Redis → 503 |
| 3 | Bearer con `azp`: `revokeToken(jti,exp)` + `revokeClientSince(user, azp, T)` | ídem | `TokenRevocationPort` | fallo Redis → 503 |
| 4 | Bearer sin `azp` (ausente o en blanco): solo `revokeToken` | ídem | idem | idem |
| 5 | Bearer: **nunca** `refreshTokenStore.delete` ni `webSession.invalidate` | ídem | — | — |
| 6 | Decoder: además de `isRevoked` e `isTokenRevoked`, consulta `isClientRevoked(user, azp, iat)` si hay `azp`; fail-closed | `RevocationAwareJwtDecoder` | `TokenRevocationPort` | `JwtValidationException` → 401 (entry point) |
| 7 | Bearer cuyo `sub` no resuelve `UserId` | controller | `SubjectUserIdLookupValidator` | `ResponseStatusException(401)` (hoy `IllegalStateException` → 500) |

**Sobre de error (leído de `ApiErrorHandler` y `ApiAuthenticationEntryPoint`), sin tocar `ApiErrorHandler`:**
- Token inválido/expirado/revocado/ausente: lo corta la cadena de filtros antes del controller →
  `ApiAuthenticationEntryPoint`, **401 `ProblemDetail`** `code=UNAUTHORIZED`, mismo cuerpo que
  `ApiErrorHandler`. Es la "carrera aceptable" de la HU.
- Redis falla **dentro del controller**: el puerto propaga `RedisSystemException`/
  `RedisConnectionFailureException` (`DataAccessException`) → handler existente `dependencyUnavailable`:
  **503 `DEPENDENCY_UNAVAILABLE`**. El controller **no** captura ni traduce.
- Regla 7: `ResponseStatusException` 401 → `statusException` ya lo mapea a `UNAUTHORIZED`.
- Ojo (ver §11-1): con Redis totalmente caído, un bearer **no llega** al controller (el decoder falla
  cerrado con 401). El 503 de bearer solo ocurre si Redis falla entre decodificar y escribir.

## 4. Modelo de dominio afectado

Ninguno: sin entidades ni value objects nuevos. Se usa `UserId` (commons) y `String clientId` (el `azp`
crudo; no se crea VO: es una clave opaca del IdP, sin invariante propia en el PDP).

## 5. Persistencia (Redis, no SurrealDB)

- **Clave nueva:** `revoked-client-since:{userId}:{clientId}` → valor `Instant.toString()`, TTL
  `RevocationRetentionProperties.retention()` (igual que `revoked-since:`). `SET` sobrescribe (heredado:
  dos logouts concurrentes, gana el último en escribir, no el T mayor).
- **Semántica de `isClientRevoked`:** `!issuedAt.isAfter(since)`; sin clave → `false`.
- Sin esquema ni inicializador.

## 6. Endpoint

| Verbo | Ruta | Éxito | Entrada | Salida |
|---|---|---|---|---|
| DELETE | `/api/v1/session` | 204 | ninguna (sujeto y cliente salen del principal/JWT) | vacío |

- **Autorización:** requiere autenticación (`anyExchange().authenticated()` en ambas cadenas). El propio
  sujeto; cliente = `azp` del JWT. CSRF solo exigido con cookie `SECURITY_BASELINE_SESSION`; con bearer no.
- **Errores:** 401 (entry point), 503 `DEPENDENCY_UNAVAILABLE`.
- **Cadena de seguridad:** no se toca (ADR-026). Canal interno `/internal/v1/**` no tiene logout propio; el
  `DELETE` por bearer es suficiente (decisión 5 de la HU: no hay nada que añadir ahí).

## 7. SPEC — el contrato

### Firmas nuevas en puertos existentes — [N] (aditivas: no rompen a nadie)

```java
// shared/security/revocation/TokenRevocationPort.java   (MATERIALIZADO como esqueleto)
default Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) { throw new UnsupportedOperationException("pendiente: HU-028"); }
default Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) { throw new UnsupportedOperationException("pendiente: HU-028"); }
```

```java
// shared/security/revocation/RedisTokenRevocationAdapter.java   (MATERIALIZADO como esqueleto)
@Override public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since)
@Override public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt)
// Contrato: set("revoked-client-since:" + subject.value() + ":" + clientId, since.toString(), retention).then()
//           get(...).map(Instant::parse).map(s -> !issuedAt.isAfter(s)).defaultIfEmpty(false)
```

Decisión sobre los dobles (16 clases anónimas de `TokenRevocationPort` en 5 archivos de test, ver abajo):
el esqueleto usa `default` **para no romper el build hoy**. En la fase de implementación el `default` se
**quita** (el puerto es contrato real; un `default` que lanza es una trampa) y el tester ajusta los dobles
con `throw new UnsupportedOperationException()` (mecánico). Archivos y dobles:
`RemoveApplicationAdministratorUseCaseImplTests` (3), `RevocationAwareJwtDecoderTests` (4),
`KeycloakLogoutControllerTests` (3), `SessionRevocationControllerTests` (3), `TokenRefreshControllerTests` (3).
Los dos primeros además cambian por comportamiento. Si el costo en los otros tres se juzga excesivo, la
alternativa aceptable es conservar los `default` (documentado, no recomendado).

### [M] Modificaciones de comportamiento (las aplica el implementador; firmas públicas SIN cambio)

```java
// shared/security/revocation/RevocationAwareJwtDecoder.java — mismos constructores y decode(String)
// decode: tras resolver userId, Mono.zip de
//   revocation.isRevoked(userId, iat), revocation.isTokenRevoked(userId, jti),
//   (azp no vacío) ? revocation.isClientRevoked(userId, azp, iat) : Mono.just(false)
// OR de los tres. Mismo onErrorMap no-JwtException -> JwtValidationException (401). El bypass del
// token técnico del PEP (isTechnicalPepToken) se conserva primero.
```

```java
// shared/web/session/SessionRevocationController.java — constructor y @DeleteMapping SIN cambio
public SessionRevocationController(TokenRevocationPort, TimeProvider, SubjectUserIdLookupValidator, RefreshTokenStorePort)
public Mono<Void> revokeSession(ServerWebExchange exchange)
// Cambio: AuthenticatedSession gana canal (bearer si jwt != null). revoke():
//   bearer: revokeToken(jti,exp) [si jti y exp] .then( azp ? revokeClientSince(user, azp, now) : empty )
//           — sin removeRefreshToken, sin invalidar sesión web.
//   cookie: igual que hoy (revokeAllSince -> revokeToken -> removeRefreshToken -> invalidate).
// Jwt sin identidad local: Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).
// IMPORTANTE: la rama Jwt hoy extrae sid como sessionId; el bearer deja de usarlo (no borra refresh).
```

`KeycloakLogoutController`, `InternalSecurityConfiguration`, `KeycloakSecurityConfiguration`,
`ApiErrorHandler`, `RedisConfiguration`: **sin cambios**.

### Prueba E2E (la escribe el tester; diseño aquí para fijar el contrato de infraestructura de test)

Ver §9 y §11-2. Requiere, en `src/test`: una `@TestConfiguration` con un `@Primary TokenRevocationPort`
decorador de `@Qualifier("tokenRevocationPort")` (el bean real de `RedisConfiguration`) con un interruptor
para fallar **solo** `revokeToken`/`revokeClientSince`/`revokeAllSince` (las lecturas `is*` siguen reales).

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
├── security/revocation/
│   ├── TokenRevocationPort.java            [N] +revokeClientSince/isClientRevoked (default throw; quitar default al implementar)
│   ├── RedisTokenRevocationAdapter.java    [N] esqueleto de los 2 métodos (throw) — lógica la pone el implementador
│   └── RevocationAwareJwtDecoder.java      [M] consulta por azp (§7)
└── web/session/
    └── SessionRevocationController.java    [M] ramas por canal (§7); conserva RefreshTokenStorePort
pdp/src/test/java/co/edu/uco/seguridad/shared/   (lo toca el tester, no el planificador)
├── security/revocation/{RedisTokenRevocationAdapterTests, RevocationAwareJwtDecoderTests}.java
├── web/session/{SessionRevocationControllerTests, KeycloakLogoutControllerTests, TokenRefreshControllerTests}.java  (dobles)
├── pdp/assignments/.../RemoveApplicationAdministratorUseCaseImplTests.java                                          (dobles)
└── web/session/SessionLogoutBearerIntegrationTests.java   [N] E2E (nombre sugerido) + @TestConfiguration de fallo
```
No hay clase nueva de `application`, así que no hay entrada nueva en ninguna `{Slice}Configuration`.
Modulith: nada cruza módulos (`shared` es OPEN; el controller sigue dependiendo de `identity` por
`SubjectUserIdLookupValidator` como hoy). Sin cambio de frontera.

## 9. Casos de prueba esperados

| Capa | Clase | Casos |
|---|---|---|
| Adaptador Redis (Testcontainers, `AbstractRedisIntegrationTest`) | `RedisTokenRevocationAdapterTests` | `revokeClientSince` + `isClientRevoked` true con `iat<=T`, false con `iat>T`; otro `clientId` mismo usuario → false; otro usuario → false; segunda revocación sobrescribe; sin clave → false; TTL = retención (ver `ttl` de la clave); independiente de `revoked-since:` |
| Decoder (fakes) | `RevocationAwareJwtDecoderTests` | token con `azp` revocado por cliente → `JwtValidationException`; con `azp` no revocado pasa; sin `azp` **no** invoca `isClientRevoked` (doble que lanza `AssertionError`); `azp` en blanco = sin `azp`; `isClientRevoked` falla (Redis) → `JwtValidationException`; token técnico PEP no consulta nada |
| Controller (`MockServerWebExchange`, a mano) | `SessionRevocationControllerTests` | bearer con azp: `revokeToken` + `revokeClientSince(user,azp,FIXED_NOW)`, **no** `revokeAllSince`, **no** `delete(sid)` (store que lanza `AssertionError`), sesión web no invalidada aunque exista; bearer sin azp: solo `revokeToken`, nunca `revokeClientSince`; cookie (`LocalUserPrincipal`): `revokeAllSince` + delete refresh + sesión invalidada (regresión); `PdpPrincipal`: `revokeAllSince`; Redis falla en bearer → propaga `RedisSystemException` y no invalida; Jwt sin identidad → `ResponseStatusException` 401; **reescribir** los tests actuales `revokes_tokens_and_removes_refresh_credential...` y `rejects_the_same_bearer...` (afirman el contrato viejo: delete de sid) |
| Logout panel | `KeycloakLogoutControllerTests` | sin lógica nueva; solo ajustar dobles |
| E2E (`@SpringBootTest` RANDOM_PORT, perfil `keycloak`, bearer real) | `SessionLogoutBearerIntegrationTests` | (a) `DELETE` con bearer cliente A → 204; (b) mismo JWT → `GET` autenticado = 401 envelope `UNAUTHORIZED`; (c) otro JWT mismo sub+azp A con `iat<=T` → 401; (d) JWT mismo sub, azp B → 200; (e) JWT sin azp: `DELETE` → solo ese jti muere, otro JWT sin azp sigue 200; (f) otro `sub` → 200; (g) JWT de A con `iat` posterior a T (firmado con `iat = now+5s`, no `Thread.sleep`) → 200; (h) escritura falla (decorador) → 503 `DEPENDENCY_UNAVAILABLE` y el JWT sigue válido; (i) sin bearer → 401; (j) con cookie de sesión BFF presente y bearer, la sesión no se invalida (opcional) |

Presupuesto: ≈ 7 + 6 + 8 + 9 pruebas; el E2E es la 5.ª clase con contexto Spring y está justificado
por el criterio de aceptación (handoff punto 3).

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado (esqueletos [N] listos; sin compilar, ver §11-4) | 2026-10-28 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades y riesgos

1. **Criterio 3 vs ADR-025 en bearer.** El decoder falla cerrado con **401** si Redis está caído; el 503
   solo se alcanza si Redis falla entre decodificar y escribir (y siempre en el canal cookie, que no pasa
   por el decoder). El E2E lo simula con el decorador, no apagando el contenedor. No se reabre
   (decisión ya tomada); se deja constancia para que el validador no espere 503 con Redis apagado.
2. **El perfil `keycloak` SÍ puede levantarse con el patrón de `InternalSecurityChainIntegrationTests`,
   pero no basta con copiarlo.** El patrón (JWKS en `com.sun.net.httpserver.HttpServer`, firma RS256,
   `linkTestIdentity`) se reutiliza tal cual para firmar y publicar claves. Lo que falta: (a) el decoder
   revocation-aware del **perfil por defecto de test (`test`, `!keycloak`) NO existe** — `jwtDecoder` de
   `SecurityConfiguration` no envuelve `RevocationAwareJwtDecoder`; solo lo hacen `keycloakBearerJwtDecoder`
   e `internalEvidenceJwtDecoder`. Por eso el E2E debe correr con `@ActiveProfiles("keycloak")`.
   (b) Ese perfil exige el repositorio reactivo de registros de cliente de Spring: `application-keycloak.properties` trae
   `issuer-uri` con descubrimiento OIDC **eager al arrancar**, y `client-secret=${PDP_KEYCLOAK_CLIENT_SECRET}`
   sin default. En el `@DynamicPropertySource`: fijar `client-secret` a un valor de prueba; sobrescribir
   `pdp.security.jwt.{jwk-set-uri,issuer,audience}` al JWKS local; y resolver el descubrimiento —
   **opción A (preferida)**: dejar `issuer-uri` en blanco y dar `authorization-uri`, `token-uri`,
   `jwk-set-uri` explícitos (verificar que Boot acepta issuer vacío); **opción B**: el mismo `HttpServer`
   sirve `/.well-known/openid-configuration` completo (issuer idéntico, `authorization_endpoint`,
   `token_endpoint`, `jwks_uri`, `response_types_supported`, `subject_types_supported`,
   `id_token_signing_alg_values_supported`). **Opción C (último recurso)**: un perfil de test propio
   con una `@TestConfiguration` que construya la cadena `/api/v1/**` con `RevocationAwareJwtDecoder`;
   prueba menos fiel al cableado real. No pude ejecutarlo (sin sockets en este sandbox): la viabilidad de
   A/B debe confirmarla el tester en su primer intento y, si ambas fallan, escalar a C.
   (c) Tokens: `azp`, `jti`, `sub` (vinculado con `linkTestIdentity`), `aud=security-baseline-pdp`, `iss`
   igual a la propiedad; **no** llevan `tenant` en este perfil. (d) `iat` con precisión de segundos y
   `isClientRevoked` usa `<=`: un token emitido al mismo cliente en el mismo segundo de T también cae
   revocado; el E2E fija `iat` explícitos (pasado/futuro) en vez de dormir.
3. **Perfiles no-keycloak (`dev`/`test`):** `jwtDecoder` no consulta revocación, así que
   `DELETE /api/v1/session` ahí responde 204 pero el bearer reutilizado no se rechaza. Preexistente,
   fuera de alcance; el validador debe correr la prueba bajo `keycloak`.
4. **Compilación a mano (no se ejecutó Maven):** las dos firmas usan `Mono`, `Instant`, `UserId` ya
   importados en ambos archivos; los `default` del puerto lanzan sin devolver (válido en Java); los
   `@Override` del adaptador coinciden con las firmas del puerto. Todos los dobles existentes siguen
   compilando porque los métodos nuevos son `default`. Falta confirmar con `verificar.ps1 -Compilar`.
5. **Dependencias cruzadas:** HU-029 retira `TokenRefreshController` (que usa `TokenRevocationPort`);
   HU-030 retira `RefreshTokenStorePort`. Al integrar tras HU-029, rebasar `TokenRefreshControllerTests`
   (su doble del puerto desaparece o cambia). Esta HU no toca esos archivos salvo el doble.
6. **Fuera de contrato:** `DELETE` con cookie no distingue `sid`; sigue borrando el refresh por
   `KEYCLOAK_SESSION_ID`. Se retira en HU-030, no aquí.
