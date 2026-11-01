# PLAN: Revocación de token al cerrar sesión (BFF y bearer)

## Metadata

- **ID:** HU-028
- **Slice:** `shared` (específicamente `shared/web/session` y `shared/security/revocation`)
- **Tipo:** Escritura
- **Fecha:** 2026-10-21
- **Rama sugerida:** `feature/HU-028-revocacion-al-logout`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-028.md`, `ADR-026-redis-cache-and-token-revocation.md`, `PLAN-HU-022.md`, `KeycloakLogoutController.java`, `TokenRevocationPort.java`, `SecurityConfiguration.java`, `KeycloakSecurityConfiguration.java`
- **Criterios de la línea base que toca:** 1 (Clean Architecture), 2 (Contratos de servicios), 9 (Mapeo de errores consistente), 11 (Interacción entre capas), 12 (Inyección de dependencias), 21 (Modelos explícitos e inmutables), 22 (Reactivo sin block), 23 (Arquitectura y pruebas en verde).

## 1. Resumen funcional

Conecta la infraestructura de revocación en Redis (HU-022) con el ciclo de vida del logout del usuario.
Hoy, `KeycloakLogoutController` invalida la sesión BFF pero no revoca el token JWT, permitiendo que un token extraído siga siendo válido hasta su expiración.
Esta historia realiza dos cosas:
1. En el flujo BFF (`KeycloakLogoutController.logout`), extrae el sujeto autenticado (`UserId`) antes de invalidar la sesión WebFlux y ejecuta `TokenRevocationPort.revokeAllSince(userId, timeProvider.now())`.
2. Expone el endpoint `DELETE /api/v1/session` para clientes bearer (SPA o microfrontends externos), revocando inmediatamente todos los tokens del sujeto autenticado emitidos hasta ese instante y respondiendo `204 No Content`.
No cubre el refresco de tokens (HU-029) ni configuraciones del servidor Keycloak.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Revocación en logout BFF | Al llamar `GET /api/v1/session/logout`, se invoca `TokenRevocationPort.revokeAllSince` para el `UserId` de la sesión antes de invalidarla. Una petición posterior con un token de esa sesión es rechazada con `401`. |
| 2 | Endpoint DELETE /api/v1/session | Una petición `DELETE /api/v1/session` con token Bearer válido revoca todos los tokens del sujeto hasta ese momento y devuelve `204 No Content`. Peticiones posteriores con ese token devuelven `401`. |
| 3 | Fail-closed ante fallo de Redis | Si Redis no responde al momento de revocar en el logout BFF o en el DELETE, la operación falla con `503 Service Unavailable` (`ApiErrorHandler`). En el BFF, la sesión no se invalida para permitir reintento. |
| 4 | Sin afectación a otros usuarios | La revocación de un sujeto no afecta los tokens ni sesiones de otros usuarios (`UserId` distinto). |
| 5 | Sesiones anónimas en logout | Si `GET /api/v1/session/logout` es invocado sin sesión autenticada previa, redirige normalmente a Keycloak/login sin lanzar error. |
| 6 | Verde | `verificar.ps1` en verde (compilación, pruebas, Modulith, ArchUnit y cobertura). |

## 3. Reglas de negocio

| # | Regla | Dónde vive (VO / Rule) | Puerto que trae el dato | Excepción ? HTTP |
|---|---|---|---|---|
| R1 | Petición de revocación bearer requiere autenticación | Filtro de seguridad (`SecurityWebFilterChain`) | Contexto de seguridad reactivo | `AuthenticationCredentialsNotFoundException` ? 401 |
| R2 | Fallo en almacén de revocación es fail-closed | `TokenRevocationPort` (adaptador Redis) | Redis | `DataAccessException` / `RedisSystemException` ? 503 |
| R3 | Si no hay usuario en sesión BFF, logout continúa sin revocar | `KeycloakLogoutController` (síncrona en flujo) | `SecurityContext` de WebSession | Ninguna (continúa con redirect) |

## 4. Modelo de dominio afectado

### Entidad / agregado
No se introducen entidades de dominio nuevas. Utiliza el modelo compartido `UserId` (`co.edu.uco.seguridad.pdp.commons.model.UserId`).

### Value objects
Ninguno nuevo. Utiliza `UserId`.

## 5. Persistencia
No toca base de datos relacional ni SurrealDB. Utiliza Redis a través de `TokenRevocationPort.revokeAllSince(UserId subject, Instant since)` (clave `revocation:{subject}`, semántica SET con expiración).

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| GET | `/api/v1/session/logout` | 302 Found (Redirect) | Ninguno | Ninguno |
| DELETE | `/api/v1/session` | 204 No Content | Ninguno | Vacío |

- **Autorización:** 
  - `GET /api/v1/session/logout`: permitido a todos (`permitAll()`), si existe sesión activa procesa la revocación del usuario asociado.
  - `DELETE /api/v1/session`: requiere autenticación Bearer (`authenticated()`), el sujeto se extrae del `Authentication` reactivo (`PdpPrincipal` o `LocalUserPrincipal`).
- **Errores esperados:**
  - Sin token en DELETE: `401 Unauthorized`
  - Fallo en Redis: `503 Service Unavailable`

## 7. SPEC — el contrato

### [M] `co.edu.uco.seguridad.shared.web.session.KeycloakLogoutController`
```java
package co.edu.uco.seguridad.shared.web.session;

// Modificación: inyecta TokenRevocationPort y TimeProvider
public final class KeycloakLogoutController {
    // Constructor modificado
    KeycloakLogoutController(KeycloakOidcSessionService oidcSessionService,
                             TokenRevocationPort tokenRevocationPort,
                             TimeProvider timeProvider,
                             @Value("${pdp.frontend.origin}") String frontendOrigin) { ... }
}
```

### [N] `co.edu.uco.seguridad.shared.web.session.SessionRevocationController`
```java
package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
public final class SessionRevocationController {

    private final TokenRevocationPort tokenRevocationPort;
    private final TimeProvider timeProvider;

    public SessionRevocationController(TokenRevocationPort tokenRevocationPort, TimeProvider timeProvider) {
        this.tokenRevocationPort = Objects.requireNonNull(tokenRevocationPort, "tokenRevocationPort");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    @DeleteMapping("/api/v1/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revokeSession(@AuthenticationPrincipal Principal principal) {
        throw new UnsupportedOperationException("pendiente: HU-028");
    }
}
```

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
+-- web/
¦   +-- session/
¦       +-- [M] KeycloakLogoutController.java
¦       +-- [N] SessionRevocationController.java
```

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| Web / Controlador | `KeycloakLogoutControllerTests` | 1. Logout con sesión autenticada: invoca `revokeAllSince`, remueve `KEYCLOAK_ID_TOKEN`, invalida sesión y redirige.<br>2. Logout con sesión anónima: no invoca revocación, redirige normalmente.<br>3. Fallo de Redis en revocación: emite error y no invalida sesión. |
| Web / Controlador | `SessionRevocationControllerTests` | 1. `DELETE /api/v1/session` autenticado con `PdpPrincipal`: invoca `revokeAllSince(principal.userId(), now)` y responde 204.<br>2. `DELETE /api/v1/session` autenticado con `LocalUserPrincipal`: invoca con su `userId` y responde 204.<br>3. Fallo de Redis: propaga error al handler (503). |
| Integración HTTP | `SessionRevocationHttpTests` | 1. Con token bearer válido: `DELETE /api/v1/session` devuelve 204; subsiguiente llamada con el mismo token devuelve 401.<br>2. Sin token: devuelve 401. |

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

Ninguna. La firma de `TokenRevocationPort.revokeAllSince` y el manejo reactivo en WebFlux ya están probados en `RemoveApplicationAdministratorUseCaseImpl` y `RedisTokenRevocationAdapter`.
