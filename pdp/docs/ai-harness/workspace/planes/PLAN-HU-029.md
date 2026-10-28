# PLAN: Endpoint de refresco de token para aplicaciones externas (POST /api/v1/session/token/refresh)

## Metadata

- **ID:** HU-029
- **Slice:** `shared` (específicamente `shared/web/session` y `shared/auth`)
- **Tipo:** Mixto (intercambio con IdP / emisión / revocación)
- **Fecha:** 2026-10-21
- **Rama sugerida:** `feature/HU-029-refresh-token-externo`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-029.md`, `ADR-026-redis-cache-and-token-revocation.md`, `ADR-031-security-microfrontend-identity.md`, `PLAN-HU-028.md`, `TokenRevocationPort.java`, `KeycloakSecurityConfiguration.java`
- **Criterios de la línea base que toca:** 1 (Clean Architecture), 2 (Contratos de servicios), 9 (Mapeo de errores consistente), 11 (Interacción entre capas), 12 (Inyección de dependencias), 21 (Modelos inmutables), 22 (Reactivo sin block), 23 (Arquitectura y pruebas en verde).

## 1. Resumen funcional

Permite a las aplicaciones externas integradas (microfrontends `security-ui`, portales SPA) renovar su token de sesión consumiendo directamente la plataforma de seguridad con su token actual, sin tener que implementar flujos OIDC propios contra Keycloak.
Expone `POST /api/v1/session/token/refresh`. Al recibir el token actual, la plataforma valida la sesión, renueva el acceso contra Keycloak de forma reactiva, revoca el token anterior vía `TokenRevocationPort` para cerrar la ventana de reutilización, y responde con el nuevo access token y su tiempo de expiración.
Permite además que las aplicaciones gestionen sus propios mecanismos de inactividad: si expira el tiempo de inactividad, la aplicación invoca el cierre explícito (`DELETE /api/v1/session`, HU-028) o detecta si el token ya no puede refrescarse.
No expone el `refresh_token` a los clientes (el servidor actúa como intermediario seguro).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Refresco con token válido | Al enviar `POST /api/v1/session/token/refresh` con `Authorization: Bearer <token_actual>`, devuelve `200 OK` con `{ "accessToken": "...", "expiresIn": 300, "tokenType": "Bearer" }`. El token anterior queda revocado inmediatamente. |
| 2 | Intento de reusar token anterior | Una petición que utilice el token anterior tras haber sido refrescado es rechazada con `401 Unauthorized` por revocación en Redis. |
| 3 | Sesión expirada en IdP | Si la sesión en Keycloak ya no es renovable (refresh token vencido o usuario deshabilitado), responde `401 Unauthorized` con código `SESSION_EXPIRED`. |
| 4 | Fallo de disponibilidad de Keycloak | Si Keycloak no responde, responde `503 Service Unavailable`. El token actual NO se revoca (fail-closed seguro). |
| 5 | Confidencialidad de tokens de refresco | La respuesta nunca contiene `refresh_token` ni credenciales del cliente OIDC. |
| 6 | CORS para orígenes autorizados | El endpoint responde cabeceras CORS correctas a los orígenes definidos en `pdp.security.cors.allowed-origins`. |
| 7 | Verde | `verificar.ps1` en verde. |

## 3. Reglas de negocio

| # | Regla | Dónde vive (VO / Rule) | Puerto que trae el dato | Excepción ? HTTP |
|---|---|---|---|---|
| R1 | Petición requiere token bearer actual no revocado | `RevocationAwareJwtDecoder` / Filtro seguridad | `TokenRevocationPort` | `InvalidTokenException` ? 401 |
| R2 | Sesión no renovable en IdP | `TokenRefreshPort` (adaptador Keycloak) | Keycloak Token Endpoint | `SessionExpiredException` ? 401 |
| R3 | IdP inaccesible | `TokenRefreshPort` | Keycloak | `ServiceUnavailableException` ? 503 |

## 4. Modelo de dominio afectado

### Value objects / DTOs

| DTO / Objeto | Nuevo o existente | Invariantes | Vive en |
|---|---|---|---|
| `TokenRefreshResponse` | Nuevo | `accessToken` no nulo ni vacío, `expiresIn > 0`, `tokenType` por defecto "Bearer" | `shared/web/session/dto` |
| `TokenRefreshResult` | Nuevo | Resultado del puerto de IdP con nuevo access token y duración | `shared/auth/model` |

## 5. Persistencia
No toca base de datos relacional. Utiliza Redis vía `TokenRevocationPort` para invalidar el token anterior y opcionalmente almacenar la correlación de sesión si el perfil lo requiere.

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| POST | `/api/v1/session/token/refresh` | 200 OK | Vacío (usa `Authorization: Bearer <token>`) | `{ "accessToken": "...", "expiresIn": 300, "tokenType": "Bearer" }` |

- **Autorización:** Requiere autenticación `Bearer` (`authenticated()`), sujeto obtenido del contexto reactivo.
- **Errores esperados:**
  - Token inválido/revocado: 401
  - Sesión en IdP expirada: 401 (código `SESSION_EXPIRED`)
  - Keycloak no disponible: 503

## 7. SPEC — el contrato

### [N] `co.edu.uco.seguridad.shared.web.session.dto.TokenRefreshResponse`
```java
package co.edu.uco.seguridad.shared.web.session.dto;

public record TokenRefreshResponse(String accessToken, long expiresIn, String tokenType) {
    public TokenRefreshResponse {
        // Constructor compacto vacío en esqueleto
    }
}
```

### [N] `co.edu.uco.seguridad.shared.auth.port.TokenRefreshPort`
```java
package co.edu.uco.seguridad.shared.auth.port;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.session.dto.TokenRefreshResponse;
import reactor.core.publisher.Mono;

public interface TokenRefreshPort {
    Mono<TokenRefreshResponse> refreshToken(UserId userId, String currentTokenValue);
}
```

### [N] `co.edu.uco.seguridad.shared.web.session.TokenRefreshController`
```java
package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.shared.auth.port.TokenRefreshPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import co.edu.uco.seguridad.shared.web.session.dto.TokenRefreshResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
public final class TokenRefreshController {

    private final TokenRefreshPort tokenRefreshPort;
    private final TokenRevocationPort tokenRevocationPort;
    private final TimeProvider timeProvider;

    public TokenRefreshController(TokenRefreshPort tokenRefreshPort,
                                  TokenRevocationPort tokenRevocationPort,
                                  TimeProvider timeProvider) {
        this.tokenRefreshPort = Objects.requireNonNull(tokenRefreshPort, "tokenRefreshPort");
        this.tokenRevocationPort = Objects.requireNonNull(tokenRevocationPort, "tokenRevocationPort");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    @PostMapping("/api/v1/session/token/refresh")
    public Mono<TokenRefreshResponse> refresh(@AuthenticationPrincipal PdpPrincipal principal,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        throw new UnsupportedOperationException("pendiente: HU-029");
    }
}
```

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
+-- auth/
¦   +-- port/
¦       +-- [N] TokenRefreshPort.java
+-- web/
    +-- session/
        +-- [N] TokenRefreshController.java
        +-- dto/
            +-- [N] TokenRefreshResponse.java
```

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| Web / Controlador | `TokenRefreshControllerTests` | 1. Token válido: renueva exitosamente, revoca token previo e incrementa timestamp de revocación.<br>2. Fallo de Keycloak: propaga error 503 sin revocar el token actual.<br>3. Sesión de Keycloak expirada: responde 401 con código `SESSION_EXPIRED`. |
| Adaptador IdP | `KeycloakTokenRefreshAdapterTests` | 1. Intercambio exitoso contra endpoint `/protocol/openid-connect/token`.<br>2. Error 400 de Keycloak (grant invalid): mapea a `SessionExpiredException`.<br>3. Timeout o fallo de red: mapea a `ServiceUnavailableException`. |
| Integración HTTP | `TokenRefreshHttpTests` | Flujo completo: autenticación inicial ? refresh ? verificación de 200 con nuevo token ? intento con token antiguo falla con 401 por revocación. |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ? Generado | 2026-10-21 |
| Contrato aprobado (gate 1) | ? Pendiente | |
| Pruebas en rojo | ? Pendiente | |
| Implementación en verde | ? Pendiente | |
| Validación | ? Pendiente | |
| Entrega (gate 2) | ? Pendiente | |

## 11. Ambigüedades pendientes

1. **Gestión del refresh_token en el perfil Keycloak**: si el cliente externo no posee un `refresh_token` directo, el backend PDP almacena temporalmente en Redis (con TTL ligado a la sesión) el `refresh_token` emitido por Keycloak para ese `UserId`, permitiendo renovarlo en `POST /api/v1/session/token/refresh` de forma transparente. El implementador confirmará esta estrategia en el adaptador `KeycloakTokenRefreshAdapter`.
