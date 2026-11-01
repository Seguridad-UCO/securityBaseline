# PLAN: Renovación del token de la sesión BFF para el canal PEP

## Metadata

- **ID:** HU-030
- **Slice:** `shared` (técnico, perfil `keycloak`): `shared/auth`, `shared/web/session`, `shared/config`, `shared/security`. No toca ningún módulo de negocio.
- **Tipo:** Mixto (consulta HTTP interna + retiro de persistencia Redis)
- **Fecha:** 2026-10-28
- **Rama sugerida:** `feature/HU-030-renovacion-token-sesion-bff`
- **Fuentes:** `workspace/HU-030.md`; ADR-033 §2 y ADR-025 (`security-platform-architecture/docs/01-governance/adr/`); código verificado (ver §0). Ruta pedida por el invocador: `workspace/PLAN-HU-030.md` (no `planes/`).
- **Criterios de la línea base que toca:** 1, 2, 9, 11, 12, 21, 22 (mínimos). Añade fail-closed (ADR-025) y sesión sin tokens al navegador (ADR-022).

## 0. Evidencia del código (lo que se verificó, no se leyó en docs)

| Hecho | Evidencia |
|---|---|
| El `OAuth2AuthorizedClient` de la sesión **ya guarda el refresh token** | `OidcAuthenticationSuccessHandler.storeRefreshToken` lo lee de `authorizedClients.loadAuthorizedClient("keycloak", …).getRefreshToken()`. El cliente lo guarda el repositorio de clientes autorizados en WebSession de Spring durante el callback (`oauth2Login(...authorizedClientRepository(...))` en `KeycloakSecurityConfiguration`). |
| **Cómo se persiste** | el repositorio de clientes autorizados en WebSession de Spring (spring-security-oauth2-client 7.1.0, fuente leída) guarda en el atributo de `WebSession` `…el repositorio de clientes autorizados en WebSession de Spring.AUTHORIZED_CLIENTS` un `HashMap<registrationId, OAuth2AuthorizedClient>` (objeto Java vivo, **sin serializar**). `saveAuthorizedClient` sustituye el cliente en ese mapa. Ignora el `principal` al cargar (clave solo `registrationId`). |
| El almacén de sesión es **en memoria** | No hay `spring.session`, ni `WebSessionStore` propio en `pdp/src/main` (grep). Spring usa `InMemoryWebSessionStore` (spring-web 7.0.8, fuente leída): `retrieveSession` devuelve **la misma instancia** `InMemoryWebSession` con `attributes = ConcurrentHashMap`; `maxIdleTime` por defecto **30 min** (no configurado en el repo). Dos peticiones de la misma cookie comparten el mismo objeto de sesión. |
| El controlador **no mira la expiración** | `InternalBffSessionTokenController.token` hace `loadAuthorizedClient` y `.map(client -> client.getAccessToken().getTokenValue())`. No existe ningún `ReactiveOAuth2AuthorizedClientManager` en `pdp/src` (grep). |
| La copia Redis solo se escribe en el login y solo la lee el proxy de HU-029 | `refreshTokenStore.find` solo lo llama `KeycloakTokenRefreshAdapter` (HU-029). `store` solo en `OidcAuthenticationSuccessHandler`. `delete` en `KeycloakLogoutController` y `SessionRevocationController`. |
| El PEP trata cualquier error como 401 | `BffSessionTokenResolver.resolve` hace `onErrorMap(error -> EnforcementFailure(UNAUTHENTICATED, "TOKEN_INVALID"))`: un 503 del PDP llegaría al cliente final como 401 (ver riesgos). |
| `accessTokenLifespan` del realm local | 300 s (`pdp/keycloak/import/security-baseline-realm.json`). |
| `ApiErrorHandler` ya traduce `ResponseStatusException` | 401 y 503 (`SERVICE_UNAVAILABLE`) tienen código/mensaje en `shared/web/exceptionhandler/ApiErrorHandler`. No hay que tocar el advice. |

## 1. Resumen funcional

`GET /internal/v1/bff-session-token` entrega siempre un access token vigente: si el del `OAuth2AuthorizedClient` de la sesión vence en menos de `token-renewal-skew`, se canjea el refresh token **de esa misma sesión** contra Keycloak, se guarda la rotación en la sesión y se devuelve el nuevo. Renovaciones simultáneas de una sesión se colapsan en un solo canje. Fail-closed: `401` si la sesión murió, `503` si Keycloak no responde. Se retira el almacén Redis de refresh, el tracker y el wrapper de `refresh_expires_in`. **No cubre:** sesiones distribuidas, tokens de apps externas, cambios en Keycloak, ni el tratamiento de 503 en el PEP.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Access token vencido (o dentro del margen), sesión viva | 200 con token distinto y vigente; el `OAuth2AuthorizedClient` de la sesión queda con el refresh token rotado |
| 2 | Access token vigente | 200 con el mismo token; **cero** llamadas a Keycloak |
| 3 | Dos peticiones concurrentes con token vencido | Un solo canje contra Keycloak; ambas reciben el mismo token nuevo |
| 4 | Keycloak responde `invalid_grant` | 401 `Sesión BFF no activa`; sin token en el cuerpo |
| 5 | Keycloak no responde / 5xx / timeout | 503; sin token vencido en el cuerpo |
| 6 | Sin `RefreshTokenStorePort`, `RedisRefreshTokenStoreAdapter`, tracker ni wrapper | El login funciona aunque Keycloak no envíe `refresh_expires_in` |
| 7 | Navegador | `/api/v1/session` y el callback no contienen tokens (sin cambios; se prueba que siguen así) |
| 8 | Suite completa | `verificar.ps1` verde; `drift.ps1` y `consistencia.ps1` limpios |

## 3. Reglas de negocio

No hay `Rule` de dominio: es seguridad de infraestructura (`shared`).

| # | Regla | Dónde vive | Dato | Excepción → HTTP |
|---|---|---|---|---|
| R1 | Un token que vence en menos de `skew` se renueva antes de entregarse | `BffSessionTokenRenewer` (delegando en el proveedor `refresh_token` de Spring) | `OAuth2AuthorizedClient` de la sesión | — |
| R2 | Tras renovar, el token entregado debe seguir vigente (sin skew) o se rechaza | `BffSessionTokenRenewer` (comparación con `TimeProvider`) | Cliente devuelto por el manager | `BffSessionExpiredException` → 401 |
| R3 | Sin cliente autorizado, o sin refresh token con token vencido, o `invalid_grant`/`invalid_token` | `BffSessionTokenRenewer` (traducción) | `ClientAuthorizationRequiredException`, `ClientAuthorizationException` con código `invalid_grant`/`invalid_token` | `BffSessionExpiredException` → 401 |
| R4 | Cualquier otro fallo de renovación (red, 5xx, timeout, `server_error`, `invalid_client`) | `BffSessionTokenRenewer` | excepción del manager o `TimeoutException` | `TokenRenewalUnavailableException` → 503 |
| R5 | Una renovación en vuelo por sesión | `BffSessionTokenRenewer` (single-flight) | `WebSession.getId()` | — |
| R6 | El login exige refresh token en el cliente autorizado (mensaje explícito) | `OidcAuthenticationSuccessHandler` | `authorizedClients.loadAuthorizedClient` | `IllegalStateException` (como hoy) |

## 4. Modelo de dominio afectado

Ninguno. Sin entidades, VOs ni enums de negocio.

## 5. Persistencia

Se **elimina** la clave Redis `oidc-refresh-token:{sid}`. Ya no se escribe; las claves existentes expiran solas por su TTL (el máximo es la vida del refresh token). No hay migración. Redis sigue usándose para revocación (`TokenRevocationPort`) y caché de roles (`RedisConfiguration`).

## 6. Endpoint

| Verbo | Ruta | Éxito | Entrada | Salida |
|---|---|---|---|---|
| GET | `/internal/v1/bff-session-token` | 200 | cookie `SECURITY_BASELINE_SESSION` + bearer técnico del PEP (sin cambios) | `{"accessToken": "<vigente>"}` (sin cambios) |

- **Autorización:** canal técnico PEP→PDP, sin cambios.
- **Errores:** `BffSessionExpiredException` → 401; `TokenRenewalUnavailableException` → 503 (ambos vía `ResponseStatusException` que ya traduce `ApiErrorHandler`).

## 7. SPEC — el contrato

### Decisiones de los 4 puntos abiertos

**D1. Margen (`token-renewal-skew`).** Propiedad `pdp.security.session.token-renewal-skew`, tipo `Duration`, **por defecto `60s`** (igual al `clockSkew` de Spring), enlazada a `SessionTokenRenewalProperties.tokenRenewalSkew` y aplicada con `refreshToken(c -> c.clockSkew(skew))` en el proveedor. Justificación: el access token del realm vive 300 s; 60 s garantiza que el PEP reciba un token con al menos ~60 s de vida (cubre latencia PEP→app) sin renovar en cada petición (renovaría cada ~4 min). Debe ser ≥ 0 (lo valida el implementador; el skeleton no valida). Segunda propiedad **añadida por el plan**: `pdp.security.session.token-renewal-timeout`, `Duration`, por defecto `5s`, tope de espera del canje (vencido → R4/503). Ambas se documentan en `application-keycloak.properties` con su variable (`PDP_SECURITY_SESSION_TOKEN_RENEWAL_SKEW`, `PDP_SECURITY_SESSION_TOKEN_RENEWAL_TIMEOUT`) — [M].

**D2. Single-flight sin bloquear.** `ConcurrentMap<String, Mono<OAuth2AuthorizedClient>> inFlight` dentro de `BffSessionTokenRenewer`, clave `WebSession.getId()`:

```
inFlight.computeIfAbsent(sessionId, id -> authorize(exchange, principal)   // ensamblado, aún sin suscribir
        .timeout(renewalTimeout)
        .doFinally(s -> inFlight.remove(id, thisMono))                     // remove(key, value): no borra el de otra ronda
        .cache())                                                          // el primero suscribe; los demás comparten resultado o error
```

- El guardado de la rotación en la sesión ocurre **dentro** de `authorize` (el `DefaultReactiveOAuth2AuthorizedClientManager` invoca su `authorizationSuccessHandler` que llama a `saveAuthorizedClient`), antes de `doFinally`; por eso una petición posterior ya ve el token nuevo en la sesión y no renueva.
- Rechazado el atributo en `WebSession`: ensucia la sesión, haría falta un `Mono` en un atributo que cualquier serialización futura (Spring Session) rompería, y exige sincronizar el `getAttribute/put`. El mapa es local, se autolimpia al terminar (no hay expulsión por TTL que mantener) y es correcto con **una instancia** (alcance declarado).
- `cache()` es deliberado: si el primer suscriptor cancela (el PEP corta la conexión), la renovación **continúa** y guarda la rotación; si se cancelara, Keycloak habría rotado el refresh token y la sesión quedaría con uno ya consumido (sesión perdida). Debe haber prueba de ello.
- La ruta feliz (token vigente) no entra en el mapa: el renovador consulta la expiración y solo si hay que renovar crea la entrada. Para decidirlo sin I/O extra usa `loadAuthorizedClient` (en memoria); lo hace el manager. Se acepta que el manager cargue el cliente dos veces: es lectura de un `HashMap` en memoria.
- Tamaño máximo del mapa = nº de sesiones renovando a la vez; nunca crece sin tope por tiempo.
- Correcto porque `InMemoryWebSessionStore` devuelve la misma instancia de sesión a todas las peticiones de esa cookie; si se cambia a un almacén distribuido, el mapa deja de bastar (fuera de alcance, ver §11).

**D3. Logout y refresh tokens vivos.** Tras el retiro, el refresh token del cliente BFF existe **solo** dentro del `OAuth2AuthorizedClient` de la `WebSession` en memoria; `session.invalidate()` lo destruye en el PDP. En Keycloak:
  - `KeycloakLogoutController` (`GET /api/v1/session/logout`) hace RP-initiated logout (`oidcSessionService.logoutUri` → `end_session_endpoint` con `id_token_hint`): la sesión SSO de Keycloak termina y con ella sus refresh tokens. **No queda refresh vivo.**
  - `SessionRevocationController` (`DELETE /api/v1/session`) **no** llama a `end_session_endpoint` hoy (solo `revokeAllSince`, borra la copia Redis e invalida la `WebSession`). Antes, la copia Redis borrada tampoco anulaba el refresh en Keycloak, así que **no hay regresión**: el refresh que cae es el único que el PDP conservaba. Lo que queda tras `DELETE /api/v1/session` es la sesión SSO de Keycloak (preexistente) sin ningún cliente que la use; el PDP tampoco guarda ya el refresh. Cerrar esa sesión SSO (back-channel o `end_session`) queda **fuera de HU-030** y se registra en §11.
  - El refresh token ya no puede sobrevivir al reinicio del PDP: la sesión en memoria se pierde y el usuario vuelve a iniciar sesión (comportamiento ya existente para la `WebSession`).

**D4. Login sin refresh token.** `OidcAuthenticationSuccessHandler` conserva el fallo explícito `IllegalStateException("Keycloak no entregó un refresh token.")` (y el del `sid`, que se mantiene para `KEYCLOAK_SESSION_ID_ATTRIBUTE`), pero ya **no** calcula ni exige expiración: desaparecen `Instant expiresAt`, el tracker y `refresh_expires_in`. El método `storeRefreshToken` se reemplaza por `requireRefreshToken` (carga el cliente, exige refresh token y `sid`, y no escribe nada). El bean `keycloakAuthorizationCodeTokenResponseClient` desaparece de `KeycloakSecurityConfiguration`; Spring usa su cliente de token por defecto.

### Contrato nuevo [N] (ya materializado como esqueleto)

```java
// shared/auth/service/BffSessionTokenRenewer.java
public final class BffSessionTokenRenewer {
    public BffSessionTokenRenewer(ReactiveOAuth2AuthorizedClientManager clientManager,
                                  TimeProvider timeProvider, Duration renewalTimeout)
    public Mono<String> validAccessToken(ServerWebExchange exchange, Authentication principal)
}
// shared/auth/model/BffSessionExpiredException.java        extends RuntimeException  (message, cause) → 401
// shared/auth/model/TokenRenewalUnavailableException.java  extends RuntimeException  (message, cause) → 503
// shared/security/SessionTokenRenewalProperties.java
@ConfigurationProperties(prefix = "pdp.security.session")
public record SessionTokenRenewalProperties(@DefaultValue("60s") Duration tokenRenewalSkew,
                                            @DefaultValue("5s") Duration tokenRenewalTimeout)
// shared/config/SessionTokenRenewalConfiguration.java  (@Profile("keycloak"); registra BffSessionTokenRenewer;
//   construye el DefaultReactiveOAuth2AuthorizedClientManager internamente, NO como bean)
```

Semántica de `validAccessToken(exchange, principal)`: `principal` es el que ya calcula el controlador (`sub` de Keycloak). Construye `OAuth2AuthorizeRequest.withClientRegistrationId("keycloak").principal(principal).attribute(ServerWebExchange.class.getName(), exchange)`. Devuelve `accessToken.getTokenValue()` ya validado contra `timeProvider.now()` (R2). Traducción de errores R3/R4 dentro del Mono. Nunca devuelve un valor vencido y nunca bloquea.

### Contratos modificados [M] (los materializa el implementador, junto con sus consumidores)

```java
// shared/web/session/InternalBffSessionTokenController.java
//   antes: InternalBffSessionTokenController(ServerOAuth2AuthorizedClientRepository clients)
//   ahora: InternalBffSessionTokenController(BffSessionTokenRenewer renewer)
//   token(): bffSecurityContext.load → principal BFF → renewer.validAccessToken(exchange, authorizedClientPrincipal(bff))
//            .map(TokenResponse::new → ok); BffSessionExpiredException → ResponseStatusException(401, "Sesión BFF no activa.");
//            TokenRenewalUnavailableException → ResponseStatusException(503); sin sesión BFF → 401 (como hoy)

// shared/auth/service/OidcAuthenticationSuccessHandler.java
//   antes: (…, ServerOAuth2AuthorizedClientRepository authorizedClients, RefreshTokenStorePort refreshTokenStore,
//           KeycloakRefreshTokenExpirationTracker refreshTokenExpirationTracker)
//   ahora: (ProvisionIdentityUseCase, OidcFlowStateService, OidcRedirectPolicy, KeycloakOidcSessionService,
//           ServerOAuth2AuthorizedClientRepository authorizedClients)
//   storeRefreshToken(...) → requireRefreshToken(...) (Mono<Void>, sin escritura)

// shared/config/KeycloakSecurityConfiguration.java
//   - quitar el @Bean keycloakAuthorizationCodeTokenResponseClient y sus 2 imports (tracker, KeycloakAuthorizationCodeTokenResponseClient)
//   - quitar imports ReactiveOAuth2AccessTokenResponseClient y OAuth2AuthorizationCodeGrantRequest si quedan sin uso
//   - el bean keycloakAuthorizedClientRepository no cambia (sigue siendo el repositorio de clientes autorizados en WebSession de Spring)

// shared/config/RedisConfiguration.java
//   - quitar @Bean refreshTokenStorePort y los imports RedisRefreshTokenStoreAdapter, RefreshTokenStorePort
//   (RedisConfiguration sigue sirviendo a revocación y caché)

// shared/web/session/KeycloakLogoutController.java
//   - quitar parámetro/campo refreshTokenStore, su Objects.requireNonNull y la rama removeRefreshToken
//   - constructor: (KeycloakOidcSessionService, TokenRevocationPort, TimeProvider, String frontendOrigin)
//   - el atributo KEYCLOAK_SESSION_ID_ATTRIBUTE se mantiene (lo consume HU-028/revocación)

// shared/web/session/SessionRevocationController.java
//   - quitar parámetro/campo refreshTokenStore y removeRefreshToken en revoke(); el record
//     AuthenticatedSession.sessionId se queda mientras HU-028 lo use (si HU-028 lo deja sin uso, retirarlo aquí)

// pdp/src/main/resources/application-keycloak.properties
//   + pdp.security.session.token-renewal-skew=${PDP_SECURITY_SESSION_TOKEN_RENEWAL_SKEW:60s}
//   + pdp.security.session.token-renewal-timeout=${PDP_SECURITY_SESSION_TOKEN_RENEWAL_TIMEOUT:5s}
```

### Archivos a retirar (todos en `pdp/src/main/java/co/edu/uco/seguridad/shared/`)

| Archivo | Quién más lo referencia hoy |
|---|---|
| `auth/port/RefreshTokenStorePort.java` | Success handler, LogoutController, RevocationController, RedisConfiguration, `KeycloakTokenRefreshAdapter` (HU-029) |
| `auth/infrastructure/RedisRefreshTokenStoreAdapter.java` | `RedisConfiguration` (queda vacío el paquete `auth/infrastructure`) |
| `auth/service/KeycloakRefreshTokenExpirationTracker.java` | Success handler, `KeycloakSecurityConfiguration`, `KeycloakAuthorizationCodeTokenResponseClient` |
| `auth/service/KeycloakAuthorizationCodeTokenResponseClient.java` | `KeycloakSecurityConfiguration` |

### Pruebas existentes que referencian lo retirado (`pdp/src/test/java/co/edu/uco/seguridad/shared/…`)

| Prueba | Acción en HU-030 |
|---|---|
| `auth/infrastructure/RedisRefreshTokenStoreAdapterTests.java` | **Borrar** |
| `auth/service/KeycloakAuthorizationCodeTokenResponseClientTests.java` | **Borrar** |
| `auth/service/OidcAuthenticationSuccessHandlerTests.java` | **Modificar**: constructor sin store ni tracker (usa ambos en las líneas ~58, 77, 95, 112, 128–141, 245–260); eliminar los casos de expiración (`refresh_expires_in`, `consumeExpiration`); conservar "sin refresh token" y "sin sid" |
| `web/session/KeycloakLogoutControllerTests.java` | **Modificar**: quitar `unusedRefreshTokenStore()`/`storeDeleting()` y el caso "borra el refresh del sid" |
| `web/session/SessionRevocationControllerTests.java` | **Modificar**: quitar `refreshTokenStore` del helper `controller(...)`, `unusedRefreshTokenStore()`, `storeDeleting()` y el caso de borrado |
| `web/session/InternalBffSessionTokenControllerTests.java` | **Reescribir** para el constructor `(BffSessionTokenRenewer)`; conserva los dos casos de principal (`bff-user` y `keycloak-subject`) sustituyendo el `mock(ServerOAuth2AuthorizedClientRepository)` por un renovador con manager falso |
| `config/KeycloakSecurityConfigurationTests.java` | Sin cambio (solo prueba el redirect de fallo; no referencia lo retirado) |
| `auth/service/KeycloakTokenRefreshAdapterTests.java` | **No tocar: HU-029** (referencia `RefreshTokenStorePort`; HU-029 lo borra antes) |

> Nota: estas pruebas usan Mockito (`mock(...)`, `ArgumentCaptor`) pese a que `sb-testing` dice "cero Mockito". El plan **no** lo corrige; las pruebas nuevas de HU-030 siguen `sb-testing` (fakes y lambdas). Se registra como deriva de las skills.

### Orden de integración y quién retira qué referencia a `refreshTokenStore`

| Orden | HU | Retira / cambia |
|---|---|---|
| 1 | **HU-029** | `TokenRefreshController`, `KeycloakTokenRefreshAdapter`, `TokenRefreshPort`, `TokenRefreshResult`, `TokenRefreshResponse` y su Vue. Tras esto nadie llama a `RefreshTokenStorePort.find` |
| 2 | **HU-028** | Puerto de revocación y `SessionRevocationController`. Mientras no termine, **deja intacta** la referencia a `refreshTokenStore` (la conserva en su nueva firma) |
| 3 | **HU-030** | Retira `RefreshTokenStorePort` y **todas** sus referencias restantes: `SessionRevocationController` (la versión que dejó HU-028), `KeycloakLogoutController`, `OidcAuthenticationSuccessHandler`, `RedisConfiguration` y los 4 archivos de la tabla. Las modificaciones de los controladores de HU-030 se calculan sobre el resultado de HU-028 |

El esqueleto [N] de HU-030 es aditivo (no toca ninguno de esos archivos), por eso puede coexistir con HU-029/HU-028 en paralelo sin conflicto. Rebase de HU-030 sobre HU-028 es obligatorio antes de la implementación.

## 8. Árbol de archivos

```
shared/
├── auth/
│   ├── model/
│   │   ├── BffSessionExpiredException.java             [N] esqueleto creado
│   │   └── TokenRenewalUnavailableException.java       [N] esqueleto creado
│   ├── service/
│   │   ├── BffSessionTokenRenewer.java                 [N] esqueleto creado (lanza UnsupportedOperationException)
│   │   ├── OidcAuthenticationSuccessHandler.java       [M]
│   │   ├── KeycloakRefreshTokenExpirationTracker.java  [retirar]
│   │   └── KeycloakAuthorizationCodeTokenResponseClient.java [retirar]
│   ├── port/RefreshTokenStorePort.java                 [retirar]
│   └── infrastructure/RedisRefreshTokenStoreAdapter.java [retirar]
├── security/SessionTokenRenewalProperties.java         [N] esqueleto creado
├── config/
│   ├── SessionTokenRenewalConfiguration.java           [N] creado (registro del renovador; es el equivalente a {Slice}Configuration)
│   ├── KeycloakSecurityConfiguration.java              [M] quitar bean del token client
│   └── RedisConfiguration.java                         [M] quitar bean del almacén
└── web/session/
    ├── InternalBffSessionTokenController.java          [M]
    ├── KeycloakLogoutController.java                   [M]
    └── SessionRevocationController.java                [M]
resources/application-keycloak.properties               [M]
edge/tests/flows/oidc-token-renewal.hurl                [N] (lo escribe el tester/implementador)
```

`BffSessionTokenRenewer` queda registrado en `SessionTokenRenewalConfiguration` (no hay `{Slice}Configuration` en `shared`; es el patrón de `RedisConfiguration`). El manager de Spring **no** es bean (evita ambigüedad con cualquier otro `ReactiveOAuth2AuthorizedClientManager`). No hay cambio de fronteras Modulith: `shared` es `Type.OPEN` y no se cruzan módulos.

## 9. Casos de prueba esperados

Sin Mockito; fakes y lambdas; `StepVerifier`. Nombres en `snake_case`.

| Capa | Clase | Casos |
|---|---|---|
| `shared/auth` (sin Spring) | `BffSessionTokenRenewerTests` | token vigente: devuelve el valor y el manager falso no recibe llamada de canje; vencido: devuelve el token nuevo y el fake guardó la rotación; dentro del skew: renueva; `invalid_grant` → `BffSessionExpiredException`; sin cliente autorizado (`ClientAuthorizationRequiredException`) → `BffSessionExpiredException`; excepción de red/`server_error` → `TokenRenewalUnavailableException`; timeout (usar `StepVerifier.withVirtualTime`) → `TokenRenewalUnavailableException`; cliente devuelto aún vencido (sin refresh token) → `BffSessionExpiredException`; **concurrencia**: N suscripciones simultáneas con un fake que cuenta canjes → contador == 1 y todas reciben el mismo token; tras terminar, nueva renovación crea un canje nuevo (el mapa se vació); error del primer canje se propaga a todos y no deja la entrada atascada; cancelar el primer suscriptor no cancela el canje y la rotación queda guardada |
| `shared/web/session` | `InternalBffSessionTokenControllerTests` (reescrita) | los dos casos de principal existentes; `BffSessionExpiredException` → 401; `TokenRenewalUnavailableException` → 503; sin sesión BFF → 401; el cuerpo nunca contiene token en error |
| `shared/auth/service` | `OidcAuthenticationSuccessHandlerTests` (ajustada) | login con refresh token sin `refresh_expires_in` funciona; sin refresh token → fallo explícito; sin `sid` → fallo explícito; no se escribe nada fuera de la sesión |
| `shared/web/session` | `KeycloakLogoutControllerTests`, `SessionRevocationControllerTests` (ajustadas) | invalidan la sesión y revocan sin el almacén |
| `shared/security` | `SessionTokenRenewalPropertiesTests` | valores por defecto (60 s / 5 s); enlace de propiedades; rechazo de skew negativo (si el implementador lo valida en el constructor compacto) |
| `shared` integración (≤ 1 clase nueva, justificada: es el único modo de probar el manager real contra HTTP) | `BffSessionTokenRenewalIntegrationTests` (`@SpringBootTest`, perfil `keycloak`, Keycloak simulado con `MockWebServer`/`WebTestClient` + servidor de token falso) | vencido renueva y persiste rotación; vigente no llama; `invalid_grant` 401; Keycloak caído 503; concurrencia = 1 canje. **Su ejecución exige sockets de loopback** |
| Edge (Hurl) | `edge/tests/flows/oidc-token-renewal.hurl` | tras la vida del access token, el PEP sigue recibiendo uno vigente y el token del PEP cambia |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-10-28 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades y riesgos pendientes

1. **El PEP convierte el 503 en 401.** `BffSessionTokenResolver` mapea cualquier error del PDP a `UNAUTHENTICATED/TOKEN_INVALID`. El PDP cumplirá 503, pero el cliente final del PEP seguirá viendo 401 si Keycloak cae. El HU no incluye cambios en `pep/`. {PENDIENTE: ¿se amplía el alcance con una HU/HT de PEP que propague 503 (`EnforcementFailure` de indisponibilidad)?}
2. **Hurl de Edge necesita un access token corto.** El realm local usa 300 s; el Hurl no puede esperar eso. HU-030 prohíbe cambios en Keycloak; {PENDIENTE: ¿se permite un override de `accessTokenLifespan` solo en el compose/realm de pruebas de Edge (p. ej. 5–10 s) o se acepta que el Hurl fuerce el vencimiento de otro modo?}.
3. **Cierre de la sesión SSO tras `DELETE /api/v1/session`.** Preexistente: no se llama a `end_session`. No es regresión; queda fuera de HU-030.
4. **Rotación de refresh tokens y réplica única.** Correcto solo con una instancia del PDP y `InMemoryWebSessionStore` (declarado). Con varias, el mapa no coordina y habría que mover a Spring Session + bloqueo distribuido.
5. **Timeout de inactividad de la `WebSession` (30 min por defecto, no configurado).** La sesión puede morir por inactividad antes que el refresh token; el resultado es 401 y nuevo login. No es un fallo, pero conviene que Edge lo contemple.
6. **El `RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler` por defecto del manager** elimina el cliente de la sesión en `invalid_grant`/`invalid_token`: la siguiente petición da 401 sin llamar de nuevo a Keycloak. Es el comportamiento deseado; el implementador debe verificarlo con prueba, no asumirlo.
7. **No se pudo compilar con Maven** (restricción del invocador). Los esqueletos se comprobaron con `javac` suelto contra los jars de `~/.m2` (5 archivos nuevos + `TimeProvider`): sin errores. `ModulithStructureTests`/`LayeredArchitectureTests` no se ejecutaron.
8. Al aplicar el HU-028, si `AuthenticatedSession.sessionId` queda sin consumidores, retirarlo en HU-030.
