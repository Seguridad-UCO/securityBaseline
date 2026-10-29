# PLAN: HU-029 — Retirar el proxy de refresh (cada cliente renueva con el suyo)

> Reemplaza al plan del 2026-10-21 (`planes/PLAN-HU-029.md`, endpoint de refresco), que queda obsoleto: al
> cerrar la HU, este archivo se mueve a `planes/PLAN-HU-029.md` sobrescribiendo el anterior.

## Metadata

- **ID:** HU-029 (replanteada 2026-10-28)
- **Slice:** `shared` (`shared/web/session`, `shared/auth`) + panel `securityBaseline-vue`
- **Tipo:** RETIRO. No hay piezas [N] ni [M] nuevas: no se materializa ningún esqueleto en `src/main`.
- **Fuentes consultadas:** `workspace/HU-029.md`, `HANDOFF-VALIDACION-HU-027-029.md`, ADR-033 (security-platform-architecture),
  `TokenRefreshController`, `KeycloakTokenRefreshAdapter`, `TokenRefreshPort`, `TokenRefreshResult`, `TokenRefreshResponse`,
  `SecurityConfiguration`, `KeycloakSecurityConfiguration`, `ApiErrorHandler`, `SessionController`,
  `securityBaseline-vue/src/api/index.ts` y `src/composables/useInactivityTracker.ts`.
- **Criterios de la línea base que toca:** 1, 2, 9, 11, 12, 21, 22 y 23 (ninguno cambia de estado: el retiro solo
  debe dejarlos como están). El 9 interesa porque el `404` sale por `ApiErrorHandler` (`RESOURCE_NOT_FOUND`).
- **Restricciones de paralelismo:** HU-028 y HU-030 corren aparte. Esta HU **no toca** `RefreshTokenStorePort`,
  `RedisRefreshTokenStoreAdapter`, `RedisConfiguration`, `KeycloakAuthorizationCodeTokenResponseClient`,
  `KeycloakRefreshTokenExpirationTracker`, `SessionRevocationController`, `KeycloakLogoutController`,
  `InternalBffSessionTokenController`, **`KeycloakSecurityConfiguration`** ni **`OidcAuthenticationSuccessHandler`**.
  Declaración explícita: ninguna de estas dos últimas se modifica en esta HU.

## 1. Resumen funcional

`POST /api/v1/session/token/refresh` deja de existir. No se sustituye por nada en el PDP: cada cliente OIDC renueva
con Keycloak y su propio cliente (ADR-033). El panel Vue deja de llamar a la ruta y su keepalive de inactividad
usa `GET /api/v1/session`.

## 2. Decisiones resueltas con evidencia del código

### D1. Cómo se verifica el `404` (punto 1 de la HU)

Evidencia:
- Las dos cadenas (`SecurityConfiguration` línea 68 y `KeycloakSecurityConfiguration` línea 94) cierran con
  `.anyExchange().authenticated()`. **No existe `pathMatchers` para la ruta**; el mapeo vive solo en
  `TokenRefreshController` (`@PostMapping`, `@Profile("keycloak")`). Por tanto **no hay que tocar la configuración de
  seguridad**: al borrar el controlador la ruta cae en `anyExchange`.
- Anónimo: el filtro de autorización responde antes que el enrutado → `ApiAuthenticationEntryPoint` → **401**
  (`code=UNAUTHORIZED`). Confirmado por `SecurityWebFilterChainTests.refuses_a_request_without_a_token`, que ya
  afirma ese comportamiento para otra ruta.
- Autenticado: pasa el filtro, ningún handler lo atiende → `ResponseStatusException` 404 → `ApiErrorHandler.statusException`
  → **404 con `code=RESOURCE_NOT_FOUND`**.

Contrato exacto que se afirma:

| Petición | Estado | `code` |
|---|---|---|
| `POST /api/v1/session/token/refresh` sin credenciales | 401 | `UNAUTHORIZED` |
| Igual con `Authorization: Bearer <válido>` | 404 | `RESOURCE_NOT_FOUND` |
| Igual con cookie BFF **sin** cabecera CSRF | 403 | (CSRF; `bffSessionRequest()` exige `SECURITY_BASELINE_SESSION`) |
| Igual con cookie BFF **con** CSRF | 404 | `RESOURCE_NOT_FOUND` |

Matiz que la HU no menciona: "credenciales válidas → 404" solo es cierto con Bearer o con cookie + CSRF. Una cookie
sin CSRF da 403 antes de llegar al 404. La prueba de aceptación usa Bearer.

Limitación honesta: no existe ninguna prueba `@SpringBootTest` con el perfil `keycloak` (requiere Keycloak y Redis; el
`grep` de `ActiveProfiles`/`"keycloak"` en `src/test` no devuelve ninguna). Las pruebas de integración corren con el
perfil por defecto, donde el controlador **nunca existió** (`@Profile("keycloak")`), así que esas pruebas pasarían también
antes del retiro: son guardas de regresión del contrato, no una prueba rojo→verde. La prueba que sí distingue "antes/después"
en perfil `keycloak` es el `hurl` del Edge (criterio 4) y la ausencia de código (criterio 2).

### D2. Keepalive del panel Vue (punto 2 de la HU)

Evidencia:
- `src/api/index.ts` ya define `refreshSession = () => request(... "/api/v1/session")` (línea 43), idéntico a `getSession`.
- `refreshSessionToken` (líneas 45-48) hace `POST /api/v1/session/token/refresh` y, si falla, `.catch(() => refreshSession())`.
  Con cookie BFF la ruta siempre respondía 401, así que **en la práctica el keepalive ya era `GET /api/v1/session`**; el POST
  era una llamada inútil que fallaba siempre.
- `useInactivityTracker.ts` importa `refreshSessionToken` (línea 2) y lo usa **solo** en `stayConnected()` (línea 90).
- `GET /api/v1/session` (`SessionController.current`) además re-siembra la cookie CSRF.

Decisión: `stayConnected()` pasa a `await refreshSession()`; se borra `refreshSessionToken`; el `try/catch` y el `finally`
(`resetIdleTimer(true)`) se conservan tal cual. Confirmado: el inventario previo era correcto.

## 3. Contradicciones entre la HU y el código (a notar, ninguna reabre una decisión)

1. **Matiz del criterio 1** (ver D1): "credenciales válidas → 404" omite el 403 de CSRF con cookie sin token.
2. **"Prueba HTTP de que la ruta ya no existe"** no puede ser rojo→verde en perfil `keycloak` desde el harness (D1).
3. **Retiro incompleto de la cadena de refresh, por diseño:** tras el borrado, `RefreshTokenStorePort.find(...)` se queda sin
   ningún lector (`KeycloakTokenRefreshAdapter` era el único; `store`/`delete` siguen usándolos `OidcAuthenticationSuccessHandler`,
   `KeycloakLogoutController` y `SessionRevocationController`). Es código muerto transitorio hasta HU-030; no se toca aquí.
4. **`TokenRevocationPort.revokeToken` NO queda huérfano** (lo sigue usando `SessionRevocationController`): no se retira.
   `SubjectUserIdLookupValidator` tampoco (lo usan `BulkResourceController`, `CatalogMaintenanceController` y asignaciones).
5. El criterio 2 ("ningún `grant_type=refresh_token` en un controlador HTTP") ya queda cierto con el borrado del adaptador, pero
   el adaptador no es "controlador"; la verificación real es por `grep` de `refresh_token` en `src/main` (sección 8).
6. **Documentación obsoleta que la HU no lista:** `securityBaseline-vue/docs/HU-FE-001.md`, `PLAN-PRUEBAS-FUNCIONAL-END-TO-END.md`,
   `docs/planes/PLAN-HU-FE-001.md` y `docs/reportes/REPORTE-HU-FE-001.md` (ver sección 6; el alcance habla genéricamente de
   "documentos del Vue que citan el endpoint").
7. Existen `workspace/reportes/REPORTE-HU-029.md` y `workspace/planes/PLAN-HU-029.md` de la versión antigua; ambos describen un
   endpoint que ya no existe.

## 4. Reglas / SPEC

No hay reglas de negocio ni firmas nuevas. Sección 7 de la plantilla: **vacía** (retiro). Ningún `package-info.java`
cambia; ninguna `{Slice}Configuration` cambia (las clases retiradas se registraban por `@RestController`/`@Service`, no por
`@Bean`: confirmado por `grep` de `TokenRefresh` en `src/main`, solo aparece en los 5 archivos a borrar).
Modulith: se borran clases de `shared`, que es `Type.OPEN`; no hay riesgo de frontera.

## 5. Árbol de archivos

### 5.1 `pdp/src/main/java/co/edu/uco/seguridad/` — BORRAR (5)

| Archivo | Nota |
|---|---|
| `shared/web/session/TokenRefreshController.java` | Único mapeo de la ruta |
| `shared/web/session/dto/TokenRefreshResponse.java` | Solo lo usa el controlador |
| `shared/auth/port/TokenRefreshPort.java` | Único implementador: el adaptador |
| `shared/auth/model/TokenRefreshResult.java` | Solo lo usan puerto, adaptador y controlador |
| `shared/auth/service/KeycloakTokenRefreshAdapter.java` | `@Service @Profile("keycloak")`; único lector de `RefreshTokenStorePort.find` |

Si `shared/web/session/dto/` quedara vacío, se borra la carpeta (comprobar con `ls`; hoy solo contiene este DTO según
`PROJECT-MAP.md` línea 294). Si `shared/auth/model` o `shared/auth/port` quedaran sin clases, idem; verificarlo antes de borrar la carpeta.

### 5.2 `pdp/src/main` — EDITAR

Ninguno. Aclaraciones: `KeycloakSecurityConfiguration` y `OidcAuthenticationSuccessHandler` **no se modifican**;
`SecurityConfiguration` no se modifica; no hay `application*.properties` que referencien la ruta (grep limpio).

### 5.3 `pdp/src/test/java/co/edu/uco/seguridad/` — BORRAR (2)

| Archivo | Cubría |
|---|---|
| `shared/web/session/TokenRefreshControllerTests.java` | Controlador (refresh + revocación del jti previo, 401) |
| `shared/auth/service/KeycloakTokenRefreshAdapterTests.java` | Canje, `invalid_grant` → 401, Keycloak caído → 503 |

`grep -rln "TokenRefresh" pdp/src/test` devuelve exactamente estos dos; ninguna otra prueba referencia las clases retiradas.
Las pruebas de `KeycloakLogoutController`, `SessionRevocationController` y `RedisTokenRevocationAdapter` implementan
`TokenRevocationPort` con clases anónimas propias: no dependen de lo retirado.

### 5.4 `pdp/src/test` — ESCRIBIR (la escribe el tester; el planificador solo la describe)

Archivo a editar: `shared/security/SecurityWebFilterChainTests.java` (ya trae `client()`, `TestJwtSupport.signedToken(...)` y extiende
`AbstractSurrealDbIntegrationTest`). Añadir 2 métodos:

1. `retired_token_refresh_route_answers_404_for_an_authenticated_caller` — `POST /api/v1/session/token/refresh` con
   `Authorization: Bearer <TestJwtSupport.signedToken("universidad-uco","test-subject")>` → `expectStatus().isNotFound()`
   y `$.code = RESOURCE_NOT_FOUND`.
2. `retired_token_refresh_route_answers_401_without_credentials` — mismo POST sin cabecera → `isUnauthorized()` y `$.code = UNAUTHORIZED`.

Limitación (D1): corren en perfil por defecto y pasan también antes del retiro. Se aceptan como guarda del contrato.
Presupuesto de pruebas: net negativo (se retiran 2 clases de prueba, se añaden 2 métodos); no afecta al umbral de cobertura
porque no hay código nuevo.

### 5.5 `securityBaseline-vue` — EDITAR

| Archivo | Cambio |
|---|---|
| `src/api/index.ts` | Borrar `refreshSessionToken` (líneas 45-48). Conservar `refreshSession` (línea 43) |
| `src/composables/useInactivityTracker.ts` | Línea 2: importar `getApiBaseUrl, refreshSession`. Línea 90: `await refreshSession();` |

Buscar pruebas del Vue que mockeen `refreshSessionToken` (`grep -rn refreshSessionToken src tests` hoy solo devuelve los dos
archivos de arriba; si existe un `*.spec.ts` que lo mockee se actualiza al mismo nombre `refreshSession`).

### 5.6 Documentos a actualizar

| Documento | Cambio |
|---|---|
| `pdp/docs/ai-harness/CHECKPOINT.md` (línea 39) | Fila HU-029: "Retiro del proxy `POST /api/v1/session/token/refresh` (ADR-033)"; añadir/ajustar HU-030 |
| `pdp/docs/ai-harness/workspace/ROADMAP-PDP.md` (líneas 5 y 90) | HU-029 pasa de "endpoint de refresco" a "retiro del proxy" |
| `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md` (líneas 38-39) | Idem |
| `pdp/docs/ai-harness/PROJECT-MAP.md` (líneas 275, 293, 294, 368, 402) | **No se edita a mano**: se regenera con `mapa.ps1` |
| `pdp/docs/ai-harness/workspace/planes/PLAN-HU-029.md` y `reportes/REPORTE-HU-029.md` | Marcar como reemplazados por ADR-033 (o sustituir por este plan) |
| `securityBaseline-vue/docs/HU-FE-001.md` (línea 21) | Quitar `POST .../token/refresh`; keepalive = `GET /api/v1/session` |
| `securityBaseline-vue/docs/PLAN-PRUEBAS-FUNCIONAL-END-TO-END.md` (línea 91) | Idem |
| `securityBaseline-vue/docs/planes/PLAN-HU-FE-001.md` (línea 10), `docs/reportes/REPORTE-HU-FE-001.md` (línea 15) | Idem |
| ADR-033 | Ya publicada (Accepted, 2026-10-28); no se edita |

## 6. Orden de ejecución sugerido

1. Tester: añadir los 2 métodos de 5.4 (verdes desde el inicio, ver D1).
2. Implementador: borrar 5.1 y 5.3 **en el mismo paso** (compilan juntos o rompen juntos).
3. Vue: 5.5, luego typecheck.
4. Documentos 5.6; regenerar `mapa.ps1`.
5. Validación completa (sección 8).

## 7. Qué NO se hace

- No se toca Keycloak ni se habilita `password` grant.
- No se retira el almacén Redis de refresh, su adaptador, el tracker ni `KeycloakAuthorizationCodeTokenResponseClient` (HU-030).
- No se crea `410 Gone`, ni sustituto, ni librería cliente de renovación.
- No se altera el alcance del logout (HU-028) ni `InternalBffSessionTokenController` (HU-030).
- Riesgo aceptado (ADR-033): el PEP sigue pudiendo recibir un access token vencido de la sesión BFF hasta HU-030.

## 8. Criterios de aceptación verificables

| # | Criterio | Comando |
|---|---|---|
| A1 | Las 5 clases de main y las 2 de test ya no existen | `grep -rlE "TokenRefresh(Controller\|Port\|Result\|Response)\|KeycloakTokenRefreshAdapter" pdp/src` → sin salida |
| A2 | La ruta no aparece en `src/main` ni en config | `grep -rn "session/token/refresh" pdp/src` → solo las 2 pruebas de 5.4 |
| A3 | Ningún canje `refresh_token` expuesto por HTTP | `grep -rn "refresh_token" pdp/src/main/java` → sin coincidencias en `shared/web` ni en clases `@RestController`; las que queden son de HU-030 (`KeycloakAuthorizationCodeTokenResponseClient`, tracker) |
| A4 | Contrato 404 (Bearer) / 401 (anónimo) | `powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1 -Rapido` incluye `SecurityWebFilterChainTests` en verde |
| A5 | El Vue no usa la ruta ni el símbolo | `grep -rn "refreshSessionToken\|token/refresh" securityBaseline-vue/src` → sin salida |
| A6 | Typecheck del Vue | `npm run typecheck` (o el script equivalente de `package.json`) en `securityBaseline-vue`, verde |
| A7 | Flujo Edge sin regresión | `hurl` de `edge/tests/flows/oidc-login-session-logout.hurl` (ya consulta `/api/v1/session`, no la ruta retirada) |
| A8 | Opcional: 404 real en perfil `keycloak` | Añadir al `.hurl` anterior, tras el login, `POST /api/v1/session/token/refresh` con la cookie y `X-XSRF-TOKEN` → `HTTP 404` |
| A9 | Documentación sin deriva | `powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1` → SIN DERIVA; `consistencia.ps1` → CONSISTENTE |
| A10 | Suite completa con cobertura | `powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1` → verde (`LayeredArchitectureTests`, `ModulithStructureTests` incluidos) |
| A11 | Mapa regenerado | `mapa.ps1`; `grep -n TokenRefresh pdp/docs/ai-harness/PROJECT-MAP.md` → sin salida |

## 9. Ambigüedades pendientes

Ninguna que bloquee. Decisión de proceso para quien coordine: A8 (añadir el paso 404 al `.hurl` del Edge) es opcional y es la única
verificación automatizable de "404 en perfil `keycloak`"; se recomienda hacerla. Coordinar con HU-028/HU-030 solo el orden de
edición de `CHECKPOINT.md`, `ROADMAP-PDP.md` y `MAPA-PLATAFORMA-SEGURIDAD.md` (los tres planes tocan esos documentos).
