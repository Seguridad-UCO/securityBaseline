# REPORTE — HU-030 (renovación del token de la sesión BFF para el canal PEP) — 2026-10-28

**Veredicto: APROBADO**, con la prueba de Edge (Hurl con el token acortado) pendiente de ejecución en HU-027.

Implementa [ADR-033 §2](../../../../../../security-platform-architecture/docs/01-governance/adr/ADR-033-token-renewal-ownership.md).

## Cómo se validó

Flujo completo del harness: `@1-planificador` → compuerta 1 (revisión del contrato) → `@2-tester-spec` (pruebas en
rojo por compilación, esperable: las firmas nuevas no existían) → `@3-implementador` → validación:
`mvnw clean verify`, `consistencia.ps1`, `drift.ps1`, `mapa.ps1`, más lectura del diff de producción.

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | Token vencido + sesión viva → el endpoint interno devuelve uno nuevo y vigente y la sesión guarda la rotación | ✅ | `BffSessionTokenRenewalIntegrationTests` (Keycloak simulado que rota el refresh token) |
| 2 | Token vigente → no se llama a Keycloak; dentro del margen (skew) sí renueva | ✅ | integración + `BffSessionTokenRenewerTests` |
| 3 | Dos peticiones concurrentes → **un** solo canje | ✅ | integración (concurrencia = 1 canje) y unitaria de single-flight |
| 4 | `invalid_grant` → `401` y no se vuelve a llamar a Keycloak en la segunda petición | ✅ | integración |
| 5 | Keycloak caído / `5xx` / timeout → `503`; nunca se entrega un token vencido | ✅ | integración + unitarias (tiempo virtual) |
| 6 | Sin `RefreshTokenStorePort`, almacén Redis, tracker ni wrapper; el login funciona sin `refresh_expires_in` | ✅ | `grep` en `pdp/src/main`: cero referencias; `OidcAuthenticationSuccessHandlerTests` |
| 7 | El login sigue fallando explícito sin refresh token o sin `sid`; no escribe nada si falla | ✅ | `OidcAuthenticationSuccessHandlerTests` |
| 8 | El navegador no recibe tokens | ✅ | el controlador es interno (canal PEP→PDP) y no se añadió ninguna ruta pública |
| 9 | `verificar.ps1` completo, `consistencia`, `drift` | ✅ | **899 pruebas, 0 fallos, 0 errores**, JaCoCo ≥ 50 % por paquete; CONSISTENTE; SIN DERIVA |

## Desviación aceptada

`BffSessionTokenRenewer` usa un `subscribe()` deliberado dentro de `Mono.defer` + `Sinks.One`. Es una excepción a
"sin `subscribe()` en la cadena" (CLAUDE.md), aceptada porque la rotación del refresh token debe guardarse en la sesión
aunque el PEP corte la conexión; un `cache()` no bastaba (la prueba de cancelación del primer suscriptor lo demostró).
Queda documentada en el código y en la sección Implementación de ADR-033.

## No ejecutado (honesto)

- **Hurl de renovación en el Edge:** requiere acortar la vida del access token **solo** en la fixture
  `edge/tests/realm/security-baseline-ci-realm.json` (decisión ya tomada: nunca el realm real). Se ejecuta con HU-027.

## Límites conocidos

- Una sola instancia del PDP: el mapa de single-flight y la `WebSession` (`InMemoryWebSessionStore`) son locales.
  Sesiones distribuidas (Spring Session sobre Redis) quedan como decisión aparte.
- `BffSessionTokenResolver` del PEP convierte hoy el `503` del PDP en `401`: lo corrige **HU-031**.
- `DELETE /api/v1/session` con cookie ya no borra el refresh en Redis (no existe); el refresh muere con la
  `WebSession` invalidada y el logout RP-initiated cierra la sesión SSO.
