# E2E del canal Bearer — una aplicación host contra la plataforma real

Prueba de punta a punta de lo que haría una aplicación con su propio login OIDC (ADR-031): token de **su** cliente de Keycloak →
edge/WAF → PDP → OPA → SurrealDB/Redis. No sustituye las suites unitarias; confirma que las piezas funcionan juntas.

## Parte automática (ejecutada el 2026-10-30)

Requiere el stack del edge levantado (`docker compose -p edge -f pdp/docker-compose.yml -f edge/compose.pdp.yml up -d --build --wait`; ver
`edge/README.md`) y que el `pdp` sea la imagen del código actual (`--build`).

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File e2e/run.ps1
```

`run.ps1` aprovisiona en el Keycloak **de prueba** (nunca el realm real) un cliente `e2e-host` (público, audience `security-baseline-pdp`,
claim `tenant`) y un usuario `e2e-nonadmin`, hace el primer ingreso de ese usuario por el panel y ejecuta `host-bearer.hurl`:

| Paso | Qué demuestra | Esperado |
|---|---|---|
| 1 | Sin token | 401 |
| 2–3 | Login del host + lectura y registro de una aplicación solo con `Authorization: Bearer` (sin cookie ni CSRF) | 200 / 201 |
| 4 | El registrador administra su aplicación: define un rol de esa aplicación | 201 |
| 5 | HU-036: quien administra una aplicación puede definir un rol TENANT | 201 |
| 6 | HU-036: un usuario sin aplicaciones **no** puede | 403 `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` |
| 7 | Ni administrar la aplicación ajena; el dueño sí | 403 / 200 |
| 8 | Logout Bearer (ADR-034, revocación en Redis): el mismo token deja de valer | 2xx, luego 401 |
| 9 | El cierre fue acotado: el otro usuario sigue con su sesión | 200 |

Resultado: 13 solicitudes, todas pasan. Contra la imagen anterior a HU-036 el paso 6 devolvía 201: así se comprobó el hueco real.

## Parte de navegador (microfrontend con Bearer) — ejecutada el 2026-10-30

Contra el stack del edge (Keycloak de **prueba**, nunca el realm real), en el Browser pane. El Keycloak del edge no publica puerto y su `iss`
es `http://keycloak:8080/...`, así que `browser/` resuelve las dos cosas sin tocar el PDP:

1. `docker compose -p edge -f pdp/docker-compose.yml -f edge/compose.pdp.yml -f e2e/browser/override.yml up -d --no-build --no-deps --wait keycloak pdp`
   (publica el Keycloak de prueba en `127.0.0.1:9091` y permite CORS desde `http://localhost:5190`).
2. `bash e2e/provision-keycloak.sh` ya ejecutado por `run.ps1` (cliente `e2e-host`, usuario `edge-tester` del realm de CI).
3. `node e2e/browser/serve-int.mjs` y abrir `http://localhost:5190`: el servidor reenvía `/kc-token` al Keycloak con `Host: keycloak:8080`
   (para que el `iss` del token coincida con el que valida el PDP) y sirve `security-ui/dist`. «Entrar y montar» registra una aplicación y monta el panel.

Resultado: el panel monta y carga resumen, roles, perfiles, recursos y administradores; toda llamada lleva `Authorization: Bearer`, sin CSRF y con
`credentials: omit`; el host entrega el token antes de **cada** petición (`getAccessToken`); tras «Cerrar sesión Bearer» (204, revocación en Redis) la
siguiente llamada da 401 y `onError` entrega `unauthenticated` al host. No se probó la caducidad natural del token (renovación por el host).

## Hallazgos de esta prueba

0. **El bundle del microfrontend no arrancaba en un navegador sin bundler** (`process is not defined`: Vue dejaba 184 `process.env.NODE_ENV`). Corregido en `security-ui` (`define` en `vite.config.ts`).

1. **El token del host necesita el claim `tenant`.** El PDP lo toma del claim (nunca del cuerpo); sin él toda llamada da `400 INVALID_TENANT_ID`.
   La cookie BFF usa el tenant por defecto y por eso el laboratorio no lo mostraba. Ya está en la guía de integración.
2. **El canal Bearer no aprovisiona identidades** (HU-026, criterio 5, pendiente): un usuario que nunca ingresó por el panel recibe `401` en todo,
   porque `RevocationAwareJwtDecoder` necesita su identidad local para comprobar la revocación. Solución propuesta: HU-038 (aprovisionar en el decoder
   con los claims mínimos, fail-closed).
3. **El canal Bearer nunca obtiene `ALLOW`** (`host-authorize.hurl`): el principal Bearer no lleva `userId`, el PDP omite roles y entitlements y responde
   siempre `DENY/NO_APPLICABLE_POLICY`. La misma cadena por cookie (`host-authorize-cookie.hurl`) sí da `ALLOW`. Se amplía HU-038.
4. El `#` dentro de un valor de formulario en Hurl abre un comentario: usar variables (fallo del propio script, corregido).
