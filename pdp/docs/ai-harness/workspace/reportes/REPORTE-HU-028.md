# REPORTE — HU-028 (logout con revocación acotada por canal) — 2026-10-28

**Veredicto: APROBADO.** Reemplaza al reporte anterior, que daba por buena una revocación global también por
bearer y no tenía prueba de extremo a extremo con un bearer real
([ADR-034](../../../../../../security-platform-architecture/docs/01-governance/adr/ADR-034-logout-and-revocation-scope.md)).

## Cómo se validó

Árbol aislado (los esqueletos de HU-030 apartados con `git stash`). `mvnw clean verify` completo, `consistencia.ps1`,
`drift.ps1` y `mapa.ps1`. Revisión manual del diff de producción por seguridad. Las pruebas las escribió
`@2-tester-spec` antes de la implementación y se vieron en **rojo por la razón correcta** (adaptador:
`UnsupportedOperationException`; decoder, controlador y E2E: aserción de comportamiento).

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | Logout del panel (cookie) revoca todo el sujeto, borra refresh e invalida la sesión | ✅ | `SessionRevocationControllerTests` (canal cookie) |
| 2 | `DELETE /api/v1/session` con bearer: `204`; ese JWT y los del mismo cliente (`azp`) anteriores a T reciben `401` | ✅ | E2E `SessionLogoutBearerIntegrationTests` (servidor integrado, perfil `keycloak`, JWT RS256 firmado con JWKS local) |
| 2b | Otro cliente del mismo sujeto sigue válido; la sesión BFF y su refresh no se tocan; bearer sin `azp` solo revoca su `jti` | ✅ | E2E + `SessionRevocationControllerTests` |
| 3 | Redis falla al escribir → `503`; con Redis totalmente caído el decoder responde `401` (fail-closed) | ✅ | E2E con decorador `@Primary` que falla solo las escrituras |
| 4 | Otro sujeto no se afecta; `iat` posterior a T tampoco | ✅ | E2E con `iat` explícitos, sin `sleep` |
| 5 | El panel sigue igual | ✅ | rama cookie sin cambios de comportamiento |
| 6 | Un bearer inválido, expirado, de firma alterada o revocado recibe `401` con el sobre `UNAUTHORIZED` (`application/problem+json`) | ✅ | E2E (3 casos). Era un defecto: respondía cuerpo vacío |
| 7 | Sin identidad local → `401` (antes `500`) | ✅ | `SessionRevocationControllerTests` |
| 8 | `verificar.ps1` completo, `consistencia`, `drift` | ✅ | **867 pruebas, 0 fallos, 0 errores**, JaCoCo ≥ 50 % por paquete; CONSISTENTE; SIN DERIVA |

## Cambios de producción (solo `pdp/src/main/.../shared`)

- `TokenRevocationPort`: `revokeClientSince` / `isClientRevoked` (sin `default`: un doble sin implementarlos no compila).
- `RedisTokenRevocationAdapter`: clave `revoked-client-since:{userId}:{clientId}`, misma retención y comparación `<=`.
- `RevocationAwareJwtDecoder`: tercera comprobación por `azp`, solo si el token lo trae; fail-closed.
- `SessionRevocationController`: canal por tipo de principal; borrado del refresh en `Mono.defer`.
- `KeycloakSecurityConfiguration` y `SecurityConfiguration`: `ApiAuthenticationEntryPoint` y `ApiAccessDeniedHandler`
  conectados al resource server (también el perfil no-keycloak tenía el mismo defecto).

## Límites conocidos

- La precisión de `iat` es de segundos y la comparación es `<=`: un token emitido en el mismo segundo que el logout
  del mismo cliente cae revocado. Es la semántica que ya tenía `revokeAllSince`; aceptable.
- El logout por bearer no termina la sesión SSO de Keycloak (nunca lo hizo): lo hace el cliente con su propio flujo.
- `RefreshTokenStorePort` se conserva aquí; lo retira HU-030.
