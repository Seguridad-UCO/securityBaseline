# PLAN-HU-031 — El PEP propaga la indisponibilidad del canal BFF en vez de convertirla en 401

> Módulo: `pep/` (pom `pep/pom.xml`). El harness (`sb-*`, `verificar.ps1`) está afinado para `pdp/`; aquí manda el código del PEP.
> Consultado: HU-031, HU-030, ADR-014 (matriz 401/403/503: «reintentar un 403 no tiene sentido; un 503 sí»; IdP no disponible = 503), ADR-033 §2 (PDP: `401` sesión muerta, `503` Keycloak sin responder), `InternalBffSessionTokenController` del PDP (`ResponseStatusException` 401 `Sesión BFF no activa.` / 503).

## 1. Hechos del código (evidencia)

- `BffSessionTokenResolver.resolve` hace `.retrieve()...onErrorMap(error -> UNAUTHENTICATED/TOKEN_INVALID)`: **todo** error (4xx, 5xx, red, timeout, JSON) es 401. El `onErrorMap` solo envuelve la llamada al PDP; si falla `technical.token()` ya sale `UNAVAILABLE/PDP_SERVICE_IDENTITY_UNAVAILABLE` (503) — no se toca.
- Consumidores: `HttpCaptureInteractor` (proxy) y `EmbeddedAccessDecisionController` (starter embebido). Ambos propagan el error tal cual; `IngressLimitsFilter` lo captura con `onErrorResume(EnforcementFailure)` y `ProblemWriter.write` lo traduce. No hay `@ControllerAdvice`. Por tanto **un cambio en el resolver cubre los dos caminos**.
- `EnforcementFailure.Kind.UNAVAILABLE` ya existe y `ProblemWriter` lo mapea a 503 con `application/problem+json`, `Cache-Control: no-store`, `code`, `requestId`, `correlationId`, y log `pep.enforcement.unavailable`. Es el sobre de plataforma. Patrón idéntico ya usado en `PdpApplicationCredentialValidationAdapter` (4xx → UNAUTHENTICATED, resto → UNAVAILABLE, `onErrorMap(!EnforcementFailure)`).
- `ProblemWriter` solo pone `Retry-After` en 429 (`"1"`).
- No existen pruebas de `BffSessionTokenResolver`. `PepHttpIntegrationTests`/`FixtureServers` no cubren la cookie BFF.
- `pdpWebClient` ya lleva `responseTimeout(properties.timeout())` (`EnforcementConfiguration`): un timeout llega como `WebClientRequestException`, no hace falta añadir parámetro al constructor.

## 2. Decisión 1 — contrato hacia el cliente

**503 con el sobre de la plataforma (problem+json de `ProblemWriter`) y `Retry-After`. No transparencia del cuerpo del PDP.**

- Razón: ADR-014 define 503 como «dependencia caída, el cliente puede reintentar»; el cuerpo del PDP es un detalle interno (p. ej. `Sesión BFF no activa.`) que no debe cruzar el PEP; el cliente ya sabe interpretar el sobre del PEP (`code`, `requestId`, `correlationId`).
- **Kind: se reutiliza `UNAVAILABLE`; NO se crea un Kind nuevo.** Un Kind por causa multiplicaría el `switch` exhaustivo de `ProblemWriter`; la causa va en `code`. Código nuevo: `BFF_SESSION_UNAVAILABLE`.
- **`Retry-After`:** `ProblemWriter` lo añade cuando `kind == UNAVAILABLE && code == BFF_SESSION_UNAVAILABLE` con valor `"5"` (una caída de Keycloak no se resuelve en 1 s; 429 usa 1 s porque es limitación local). Se acota por código para no cambiar las demás respuestas 503 (`CAPACITY_EXCEEDED`, `ACCESS_INDETERMINATE`, `INTEGRATION_REGISTRATION_DISABLED`: ahí un reintento sugerido sería engañoso). Es una cabecera **aditiva** únicamente en la respuesta nueva.
- Sin reintentos automáticos en el PEP (fuera de alcance).

## 3. Decisión 2 — clasificación de la respuesta/fallo del PDP

Se reemplaza `retrieve()+onErrorMap` por `exchangeToMono` (patrón de `PdpApplicationCredentialValidationAdapter`), leyendo status y liberando el cuerpo siempre.

| Resultado de la llamada al PDP | Hacia el cliente | Kind / code |
|---|---|---|
| 200 + `accessToken` no vacío | token | — |
| 200 con cuerpo vacío/ilegible/`accessToken` en blanco | 503 (respuesta del PDP no confiable, fail-closed) | UNAVAILABLE / `BFF_SESSION_UNAVAILABLE` |
| **Cualquier 401 del PDP** (sesión muerta, o identidad técnica rechazada) | **401** | UNAUTHENTICATED / `TOKEN_INVALID` (sin cambio) |
| **503** | **503** + Retry-After | UNAVAILABLE / `BFF_SESSION_UNAVAILABLE` |
| Otros 5xx (500, 502, 504…) | 503 | ídem |
| Timeout, conexión rechazada/reseteada, DNS, TLS, error de deserialización | 503 | ídem |
| **4xx distintos de 401** (400, 403, 404, 429…) | **503** | ídem + log WARN `pep.bff-session.pdp-rejected` con `pdp.status` |
| Sin cookie `SECURITY_BASELINE_SESSION` | 401 (el `switchIfEmpty` del consumidor, sin cambio) | UNAUTHENTICATED / `TOKEN_INVALID` |

Justificación de los 4xx ≠ 401: la cookie es lo único que aporta el usuario y su invalidez ya se expresa con 401. Un 400/403/404 del endpoint interno significa que el PEP está mal configurado o desalineado con el PDP (scope, contrato, ruta): es un fallo del servidor, no del usuario. Un 401 aquí mandaría a la gente al login sin remedio y ocultaría el incidente. 503 mantiene fail-closed (nunca hay token → nunca se reenvía), alimenta la alerta de «tasa de 503 = salud de dependencias» (ADR-014) y el log WARN con el status permite diagnosticar. Mismo criterio que `WebClientDecisionAdapter` (`PDP_PROTOCOL_ERROR` → UNAVAILABLE). Un 429 del PDP también es 503 (el reintento es válido).

> **Decisión del responsable (compuerta 1):** no se distingue el 401 de identidad técnica por `WWW-Authenticate`. Era una inferencia sobre Spring Security que HU-028 dejó de garantizar (el resource server usa ahora `ApiAuthenticationEntryPoint`). Todo 401 del PDP sigue siendo 401 `TOKEN_INVALID`, como hoy. Distinguirlo de verdad exigiría un `code` propio en el 401 del canal interno del PDP: queda como mejora futura, no de esta historia.

## 4. ¿Rompe un contrato público del PEP hacia las aplicaciones?

**Sí, de forma acotada y deliberada.** Una petición con cookie BFF que antes recibía 401 `TOKEN_INVALID` ante cualquier fallo del canal al PDP, ahora puede recibir 503 `BFF_SESSION_UNAVAILABLE` (con `Retry-After: 5`) o 503 `PDP_SERVICE_IDENTITY_UNAVAILABLE`. Afecta al proxy y a `POST` del starter embebido. Los clientes que traten 401 como «reautenticar» y 503 como «reintentar» mejoran; los que asuman «solo 401 en fallo de sesión» deberán tolerar 503 (ya es un código que el PEP emite en el mismo endpoint por `PDP_UNAVAILABLE`, por lo que no es un código nuevo para el contrato). No cambian: forma del sobre, códigos 401/403 existentes, camino con `Authorization: Bearer`.

## 5. Reglas / invariantes

- Fail-closed: ninguna rama emite token salvo 200 con `accessToken` no vacío (criterio 3).
- El cuerpo del PDP nunca se refleja al cliente.
- Se libera siempre el cuerpo de la respuesta no-200 (`releaseBody`), sin `block()`.
- `onErrorMap(error -> !(error instanceof EnforcementFailure), …)` para no pisar los Failure ya clasificados.

## 6. SPEC (firmas)

- **[M]** `BffSessionTokenResolver` — firma pública **sin cambios** (`resolve(ServerWebExchange): Mono<String>`; constructor igual). Cambia solo el cuerpo (`exchangeToMono` + clasificación de la sección 3).
- **[M]** `ProblemWriter.write` — misma firma; añade la cabecera `Retry-After: 5` para `UNAVAILABLE` con `code == "BFF_SESSION_UNAVAILABLE"`.
- **[N]** Ninguna. No hay nuevos Kind, clases ni beans (`IntegrationConfiguration` sin cambios). **No se crean esqueletos; no se tocó código; no hizo falta compilar.**

## 7. Árbol de archivos

```
pep/src/main/java/co/edu/uco/seguridad/pep/ingress/infrastructure/adapter/secondary/http/BffSessionTokenResolver.java   [M]
pep/src/main/java/co/edu/uco/seguridad/pep/ingress/infrastructure/adapter/primary/web/ProblemWriter.java                [M]
pep/src/test/java/co/edu/uco/seguridad/pep/ingress/infrastructure/adapter/secondary/http/BffSessionTokenResolverTests.java   [N, tester]
pep/src/test/java/co/edu/uco/seguridad/pep/ingress/infrastructure/adapter/primary/web/ProblemWriterTests.java (o ampliar IngressLimitsHttpTests)  [N, tester]
```

## 8. Pruebas (descripción; no escritas)

`BffSessionTokenResolverTests` (estilo `PdpApplicationCredentialValidationAdapterTests`: `ExchangeFunction` lambda + `StepVerifier`, sin Mockito):
1. Sin cookie → vacío (sin llamar al PDP).
2. 200 con token → emite el token; verifica path `/internal/v1/bff-session-token`, `Authorization: Bearer <técnico>` y cabecera `Cookie` reenviada.
3. Cualquier 401 del PDP (con o sin `WWW-Authenticate`) → UNAUTHENTICATED/`TOKEN_INVALID`.
5. 503 → UNAVAILABLE/`BFF_SESSION_UNAVAILABLE`.
6. Parametrizada 500/502/504 → ídem.
7. 400/403/404/429 → UNAVAILABLE/`BFF_SESSION_UNAVAILABLE`.
8. `Mono.error(IllegalStateException)` y `WebClientRequestException` (red/timeout) → UNAVAILABLE.
9. 200 con cuerpo vacío / `accessToken` en blanco / JSON malformado → UNAVAILABLE.
10. Fallo de `PdpServiceTokenProvider` (ya UNAVAILABLE) se propaga sin reclasificar.

`ProblemWriter`/HTTP: `UNAVAILABLE/BFF_SESSION_UNAVAILABLE` → 503, `Content-Type: application/problem+json`, `Retry-After: 5`, `code`, `requestId`; `UNAVAILABLE/PDP_UNAVAILABLE` → 503 **sin** `Retry-After` (no regresión); 429 sigue con `Retry-After: 1`.

Integración (criterios 1–3 de la HU): con `FixtureServers` simular PDP interno devolviendo 401 / 503 / conexión cerrada y cookie BFF: verificar 401, 503 y 503, y que **el fixture de la aplicación protegida recibe 0 peticiones** en los tres casos.

Cierre: suite del PEP (`verify-pep.ps1`) en verde. `verificar.ps1` del PDP no se ve afectado (HU-031 no toca `pdp/`); correrlo solo como regresión.

## 9. Criterios de aceptación de la HU

1. PDP 401 → 401; PDP 503 → 503 con sobre y `Retry-After` — §3, pruebas 3/5.
2. Fallo de red al PDP → 503 — prueba 8.
3. Nada llega a la aplicación sin token válido — integración (0 peticiones).
4. Suite de `pep/` en verde — el tester/implementador la corre.

## 10. Fuera de alcance

PDP (HU-030), UI, reintentos en el PEP, circuit breaker, métrica nueva (se reutiliza el log `pep.enforcement.unavailable`).

## 11. Ambigüedades pendientes

- Valor de `Retry-After` (5 s) es constante en código; si el responsable lo quiere configurable, es una propiedad nueva (no pedida).

**Gate 1 — revisa el contrato.** Si las firmas de la sección 7 son correctas, el siguiente paso es escribir las pruebas contra ellas. Si algo del contrato está mal, es más barato arreglarlo ahora que después de tener pruebas y código escritos contra él.
