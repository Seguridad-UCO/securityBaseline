# REPORTE — HU-029 (retiro del proxy de refresh) — 2026-10-28

**Veredicto: APROBADO**, con un criterio de Edge pendiente de ejecución (ver "No ejecutado").

Reemplaza al reporte anterior, que validaba el endpoint proxy que esta historia retira
([ADR-033](../../../../../../security-platform-architecture/docs/01-governance/adr/ADR-033-token-renewal-ownership.md)).

## Cómo se validó

Validación en un árbol **aislado**: los esqueletos sin implementar de HU-028 y HU-030 y sus planes se
apartaron con `git stash` para no contaminar ni la cobertura ni la deriva de esta historia. El primer intento
del validador, con esos esqueletos presentes, fue RECHAZADO por dos razones ajenas a HU-029: la cobertura
de `shared.auth.model` (0,42, por excepciones-esqueleto de HU-030 sin pruebas) y citas de planes futuros a
clases aún inexistentes.

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| A1 | No queda `TokenRefresh*` / `refreshSessionToken` | ✅ | `grep` en `pdp/src`, `securityBaseline-vue/src`, `security-ui/src`: sin coincidencias |
| A2 | La ruta solo existe como constante de prueba | ✅ | única aparición: `SecurityWebFilterChainTests` |
| A3 | Ningún controlador expuesto canjea `grant_type=refresh_token` | ✅ | `refresh_token` en `src/main` solo dentro del cliente OIDC del login y el almacén (HU-030) |
| A4 | Ruta autenticada → 404 `RESOURCE_NOT_FOUND`; anónima → 401 `UNAUTHORIZED` | ✅ | `SecurityWebFilterChainTests`, 12 pruebas, 0 fallos (guardas de regresión: el controlador era `@Profile("keycloak")` y las pruebas corren en el perfil por defecto) |
| A5 | El panel Vue no llama a la ruta; keepalive = `GET /api/v1/session` | ✅ | `useInactivityTracker.ts` → `refreshSession()` |
| A6 | Typecheck del Vue | ✅ | `npm run typecheck` (vue-tsc), exit 0 |
| A9 | `consistencia.ps1` y `drift.ps1` limpios | ✅ | CONSISTENTE; SIN DERIVA (2 clases retiradas declaradas en `drift-ignore.txt`) |
| A10 | `verificar.ps1` completo en verde | ✅ | `mvnw clean verify`: **837 pruebas, 0 fallos, 0 errores**, JaCoCo ≥ 50 % por paquete |
| A11 | `PROJECT-MAP.md` sin referencias | ✅ | regenerado con `mapa.ps1`; sin `TokenRefresh` |

## No ejecutado (honesto)

- **A7/A8 — Hurl del Edge:** `hurl` no está instalado en el shell y `oidc-login-session-logout.hurl` no tiene
  aún el paso `POST /api/v1/session/token/refresh` → 404. Se añade y se ejecuta con el resto de la suite de Edge
  en HU-027 (el stack Edge está levantado).

## Decisiones tomadas en la historia

- Ruta eliminada con `404`, sin `410` (ADR-033 §1).
- `RefreshTokenStorePort`, el wrapper de `refresh_expires_in` y el tracker se dejan intactos: los retira HU-030.
- Declaradas en `drift-ignore.txt` solo `KeycloakTokenRefreshAdapter` y `TokenRefreshControllerTests`.

## Observaciones

- Las pruebas de la ruta no pueden ser rojo→verde (la ruta nunca existió en el perfil de prueba). El 404 real
  con cookie BFF más CSRF queda cubierto por el paso A8 del Edge.
