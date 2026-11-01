# Handoff de revisión — HU-027, HU-028 y HU-029

**Actualizado:** 2026-10-28
**Rama inspeccionada:** `feature/seguridad-waf-sesiones`
**HEAD base antes del commit:** `8fd1593` (`docs(ai-harness): actualizar mapa, roadmap y checkpoint tras cierre de HU-027, HU-028 y HU-029`)
**Propósito:** registrar evidencia de validación y preparar una revisión independiente con agentes especializados. Este documento no certifica las tres historias ni reemplaza sus criterios de aceptación.

## Resumen ejecutivo

| HU | Estado comprobado | Qué sí pasó | Qué impide marcarla cerrada |
|---|---|---|---|
| HU-027 — WAF | **Validación local parcial aprobada; aceptación global pendiente** | 8/8 flujos Hurl (89 solicitudes), 8/8 archivos de ataque (11 solicitudes) en `blocking`; rate limit 61/1 IP y 50/otra IP pasó | Modo `detection`/audit log, overlay PEP completo, pipeline CI registrado y ejecutado, observabilidad requerida, mediciones p50/p95 y sidecar Azure pendientes |
| HU-028 — logout/revocación | **Implementación y pruebas focalizadas; integración bearer real pendiente** | Flujo BFF OIDC→sesión→logout pasó; tests focalizados 28/28 para los componentes de refresh/logout/revocación; Redis revocation 7/7 | Falta una prueba E2E con JWT bearer emitido por el IdP, logout y reutilización del mismo JWT rechazado en el servidor integrado |
| HU-029 — refresh externo | **Contrato/diseño sin cerrar; no validada para apps externas** | El callback BFF ahora conserva la expiración Keycloak; pruebas relacionadas 7/7 dentro de la suite y tests del adaptador/controlador entre los 28 | El endpoint depende del refresh BFF almacenado por `sid`, mientras la app externa debe autenticarse y refrescar con su cliente propio. Decisión más reciente del usuario: cada app renueva directamente con Keycloak; el contrato HU-029 debe revisarse antes de implementar o aprobarlo |

**Conclusión:** no usar los antiguos reportes “APROBADO” de HU-028/HU-029 ni el checkpoint anterior como autorización de entrega. La rama contiene trabajo funcional valioso, pero el estado actual debe presentarse como parcial/no validado donde corresponda.

## Decisión actual de arquitectura de HU-029

La conversación de definición concluyó en que las aplicaciones externas:

1. Obtienen su propio bearer por OIDC y conservan la responsabilidad de renovar directamente con Keycloak.
2. No entregan el refresh token de su cliente al navegador ni a PDP.
3. Pueden usar los endpoints de revocación/logout compatibles con su identidad/token, sujeto a validar el alcance con un token real del cliente.

Esto **no coincide con el código actual**: `KeycloakTokenRefreshAdapter` usa la sesión de refresh en Redis asociada al `sid` y el registro/credenciales Keycloak de PDP (`keycloak`/BFF), y `TokenRefreshController` espera un JWT bearer cuyo `sid` corresponda a esa sesión almacenada. Un refresh token vinculado a otro `client_id` no se debe enviar al token endpoint autenticándose como el cliente BFF.

Antes de cerrar HU-029, el implementador/arquitecto debe aclarar si:

- se elimina o redefine el endpoint HU-029 porque la app renueva directamente con Keycloak; o
- se mantiene como proxy y se diseña explícitamente un enrolamiento server-to-server, almacenamiento seguro, relación `client_id`/`sid`, credenciales confidenciales por aplicación y rotación/revocación.

La última elección explícita fue **renovación directa por cada aplicación**, no enrolamiento del refresh token en PDP. No se debe cambiar Keycloak ni habilitar password grant para “hacer pasar” la prueba local sin nueva aprobación.

## Evidencia ejecutada en esta sesión

### WAF / HU-027

Stack local existente del Compose `edge` reutilizado; no se inició Redis adicional ni se borraron volúmenes. Edge quedó en modo `blocking`.

| Prueba | Resultado |
|---|---|
| `edge/tests/flows/*.hurl`, secuencial (`hurl --test --jobs 1`) | **8 archivos, 89 solicitudes, 8/8 correctos** |
| `edge/tests/attacks/*.hurl`, `--path-as-is` | **8 archivos, 11 solicitudes, 8/8 correctos** |
| `ratelimit/over-threshold.hurl` desde un servicio | **61 solicitudes; última limitada, prueba correcta** |
| `flows/rate-limit-under-threshold.hurl` desde el segundo servicio/IP | **50 solicitudes, prueba correcta**, concurrente con la anterior |
| `authorize.hurl` | Se corrigió `action: read` a `action: GET`; el contrato PDP requiere verbo HTTP |

El orden secuencial es necesario para los flujos que reutilizan `edge-tester`: la ejecución paralela de Hurl produjo bloqueo anti-fuerza-bruta del usuario de pruebas. El bloqueo se limpió únicamente para esa cuenta local.

### Sesiones / HU-028 y HU-029

| Prueba | Resultado |
|---|---|
| `OidcAuthenticationSuccessHandlerTests` + nuevo `KeycloakAuthorizationCodeTokenResponseClientTests` | **7/7** |
| Tests focalizados de refresh, callback OIDC, logout, revocación y JWT decoder | **32/32** |
| `RedisTokenRevocationAdapterTests` con Testcontainers | **7/7** |
| `flows/oidc-login-session-logout.hurl` contra el stack Edge/Keycloak real | **9 solicitudes, correcto** |

La prueba OIDC real había fallado porque Spring Security construye `OAuth2RefreshToken` sin `expiresAt`; el response Keycloak contiene `refresh_expires_in`. Se añadió el wrapper `KeycloakAuthorizationCodeTokenResponseClient` y el `KeycloakRefreshTokenExpirationTracker`, que conserva temporalmente la duración real recibida y la vincula al refresh token por SHA-256. Si no llega una duración positiva, el login falla explícitamente. No hay TTL de reserva.

### Validación no ejecutada o no aprobatoria

- `verificar.ps1` ejecutado con Java 25 y OpenSSL de Git en `PATH`: `mvnw clean verify` compiló y corrió **794 tests, 0 fallos y 1 error**, y terminó **ROJO** en `ModulithStructureTests`. La violación reportada es una dependencia de `authorization` hacia repositorios de `assignments`; no relajar `allowedDependencies` sin ADR.
- `consistencia.ps1` ejecutado: encontró **7 inconsistencias preexistentes** de excepciones lanzadas directamente desde usecases en `applications`, `profiles`, `resources` y `roles`.
- `drift.ps1` ejecutado: encontró **1 referencia histórica no resuelta** a `SurrealAdministrationEventSchemaInitializer` en `pdp/docs/ai-harness/CHECKPOINT.md`.
- Suites Hurl contra el Edge activo en `blocking`: **8/8 flujos, 89 solicitudes; 8/8 ataques, 11 solicitudes; rate limit, 61 solicitudes desde una IP y 50 desde otra en paralelo**. La suite de flows usa `--jobs 1`; los resultados no cubren `detection`.
- No se validó el endpoint HU-029 con una aplicación/cliente OAuth distinto del BFF.
- No se probó en servidor integrado que un bearer real quede rechazado después de refresh/logout.
- No se pasó la suite de flows en modo `detection`; el health/audit log en ese modo había fallado previamente.
- No se ejecutó el overlay PEP end-to-end ni pipeline de Azure DevOps.
- No se verificaron criterios completos de telemetría (audit log hacia Loki/Collector con los campos exigidos), p50/p95, memoria ni sidecar Azure.
- El typecheck Vue había pasado en una ejecución anterior; no se modificó Vue en esta sesión y no se repitió.
- Las pruebas Hurl dejaron datos de prueba en el stack local; no se limpiaron ni se borraron volúmenes o claves Redis.

## Cambios auditados e incluidos en el commit

Los cambios revisados se registraron en `ce5fbdb` (`fix(security): track Keycloak refresh expiry and update validation`) y se publicaron en `origin/feature/seguridad-waf-sesiones` sin trailer de coautor.

### Cambios editados al continuar el trabajo, sin commit

- WAF: `edge/Caddyfile`, `edge/rules/85-audit-on-detection.conf`, `edge/tests/flows/authorize.hurl`.
- Expiración del refresh OIDC BFF: `pdp/src/main/java/co/edu/uco/seguridad/shared/auth/service/KeycloakAuthorizationCodeTokenResponseClient.java`, `KeycloakRefreshTokenExpirationTracker.java`, `OidcAuthenticationSuccessHandler.java`, `KeycloakSecurityConfiguration.java`; pruebas correspondientes en `pdp/src/test/java/.../KeycloakAuthorizationCodeTokenResponseClientTests.java` y `OidcAuthenticationSuccessHandlerTests.java`.

`OidcAuthenticationSuccessHandler.java`, `KeycloakSecurityConfiguration.java` y
`OidcAuthenticationSuccessHandlerTests.java` ya estaban modificados antes de agregar el seguimiento
de `refresh_expires_in`. Las diffs de esta continuación se mezclan con esos cambios anteriores: revisar
cada archivo completo y preservar las partes previas al preparar commits.

### Cambios previos del árbol de trabajo

Estos cambios también se revisaron y quedaron incluidos en el commit:

- `pdp/src/main/java/co/edu/uco/seguridad/shared/config/RedisConfiguration.java`
- `pdp/src/main/java/co/edu/uco/seguridad/shared/web/exceptionhandler/ApiErrorHandler.java`
- `pdp/src/main/resources/application-keycloak.properties`
- `pdp/src/test/java/co/edu/uco/seguridad/pdp/assignments/application/usecase/impl/RemoveApplicationAdministratorUseCaseImplTests.java`
- `pdp/keycloak/import/security-baseline-realm.json` (no rastreado y **vacío, 0 bytes**; no incluirlo)

El archivo `pdp/keycloak/import/security-baseline-realm.json` sigue sin seguimiento, vacío (0 bytes), y se excluyó. No se limpiaron datos de prueba ni se detuvieron o borraron contenedores/volúmenes. No incluir claves, tokens de prueba, configuración privada ni datos del volumen Redis en documentos o commits.

## Issues observados que el validador debe resolver o clasificar

1. **Token BFF y token de aplicación externa son contratos distintos.** Revisar audience, `client_id`, `sid`, `sub`, `jti`, `exp`, rotación del refresh token y ownership del logout.
2. **HU-029 necesita decisión de alcance coherente** con renovación directa en Keycloak; no declarar como validado el endpoint actual contra mocks únicamente.
3. **HU-028 requiere prueba HTTP E2E del Bearer real**: `DELETE /api/v1/session` exitoso, luego el mismo JWT devuelve `401`, otro sujeto no se afecta y un fallo Redis conserva el comportamiento fail-closed.
4. **HU-027 conserva criterios pendientes** aunque los flujos locales en `blocking` pasaron. No mover `EDGE_MODE` predeterminado ni afirmar despliegue CI/Azure validado.
5. **Full verification**: ejecutar el comando oficial, separar fallos preexistentes vs. introducidos, confirmar Modulith/ArchUnit/cobertura y actualizar reportes con logs/resultados reales.
6. **Cambios de configuración de seguridad**: revisar CORS, resource server/BFF y el realm fixture con un revisor de seguridad antes de commit.
7. **Residuos de prueba**: durante la comprobación había 7 claves `oidc-refresh-token:*` en `edge-redis`; no se inspeccionaron valores ni se eliminaron. Dejar expirar por TTL. No detener ni borrar contenedores/volúmenes sin autorización.

## Secuencia recomendada para la validación especializada

1. **Revisión de seguridad (read-only):** revisar toda la diff de HU-028/029, revocación, JWT decoder, manejo de refresh token, logout, CORS y realm/configuración; reportar hallazgos con archivo/línea y severidad. No aplicar cambios durante esta pasada.
2. **Revisión de código (read-only):** revisar coherencia de las tres HUs, contratos, edge cases reactivos, idempotencia, arquitectura y posibles cambios no relacionados en el worktree.
3. **Agente implementador:** solo después de cerrar HU-029 y priorizar hallazgos, implementar los gaps aprobados y agregar pruebas de integración que prueben el contrato externo real.
4. **Agente validador:** ejecutar validación completa en un árbol limpio/reproducible, suites Edge en detection y blocking, suites bearer/logout/refresh, `verificar.ps1`, consistencia, drift y mapas; no cambiar código.
5. **Commit/push:** realizado en `ce5fbdb`. No afirmar que los tres HUs están “cerrados” hasta que los criterios pendientes tengan evidencia.

## Guía para la siguiente sesión

- Empezar leyendo este handoff y `git status --short`; no volver a iniciar Redis en `6379`.
- El Compose Edge usa `edge-redis`; el contenedor `redis` publica el puerto local 6379. Conservar los volúmenes y datos.
- La fixture de Edge es `edge/tests/realm/security-baseline-ci-realm.json`; no copiar su secreto a logs.
- Para ejecutar la suite Hurl, usar `--jobs 1` para flujos autenticados; incluir `--path-as-is` para los ataques con URL codificada.
- Mantener HU-027/HU-028/HU-029 como estados diferenciados; un test unitario verde no demuestra por sí solo el flujo integrado.
