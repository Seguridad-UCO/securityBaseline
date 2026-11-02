# PLAN: Revocaci�n de token al cerrar sesi�n (BFF y bearer)

## Metadata

- **ID:** HU-028
- **Slice:** `shared` (espec�ficamente `shared/web/session` y `shared/security/revocation`)
- **Tipo:** Escritura
- **Fecha:** 2026-10-21
- **Rama sugerida:** `feature/HU-028-revocacion-al-logout`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-028.md`, `ADR-026-redis-cache-and-token-revocation.md`,
  `PLAN-HU-022.md`, `KeycloakLogoutController.java`, `TokenRevocationPort.java`, `SecurityConfiguration.java`,
  `KeycloakSecurityConfiguration.java`
- **Criterios de la l�nea base que toca:** 1 (Clean Architecture), 2 (Contratos de servicios), 9 (Mapeo de errores
  consistente), 11 (Interacci�n entre capas), 12 (Inyecci�n de dependencias), 21 (Modelos expl�citos e inmutables), 22 (
  Reactivo sin block), 23 (Arquitectura y pruebas en verde).

## 1. Resumen funcional

Conecta la infraestructura de revocaci�n en Redis (HU-022) con el ciclo de vida del logout del usuario.
Hoy, `KeycloakLogoutController` invalida la sesi�n BFF pero no revoca el token JWT, permitiendo que un token extra�do
siga siendo v�lido hasta su expiraci�n.
Esta historia realiza dos cosas:

1. En el flujo BFF (`KeycloakLogoutController.logout`), extrae el sujeto autenticado (`UserId`) antes de invalidar la
   sesi�n WebFlux y ejecuta `TokenRevocationPort.revokeAllSince(userId, timeProvider.now())`.
2. Expone el endpoint `DELETE /api/v1/session` para clientes bearer (SPA o microfrontends externos), revocando
   inmediatamente todos los tokens del sujeto autenticado emitidos hasta ese instante y respondiendo `204 No Content`.
   No cubre el refresco de tokens (HU-029) ni configuraciones del servidor Keycloak.

## 2. Criterios de aceptaci�n

| # | Criterio                        | Resultado esperado                                                                                                                                                                                                   |
|---|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Revocaci�n en logout BFF        | Al llamar `GET /api/v1/session/logout`, se invoca `TokenRevocationPort.revokeAllSince` para el `UserId` de la sesi�n antes de invalidarla. Una petici�n posterior con un token de esa sesi�n es rechazada con `401`. |
| 2 | Endpoint DELETE /api/v1/session | Una petici�n `DELETE /api/v1/session` con token Bearer v�lido revoca todos los tokens del sujeto hasta ese momento y devuelve `204 No Content`. Peticiones posteriores con ese token devuelven `401`.                |
| 3 | Fail-closed ante fallo de Redis | Si Redis no responde al momento de revocar en el logout BFF o en el DELETE, la operaci�n falla con `503 Service Unavailable` (`ApiErrorHandler`). En el BFF, la sesi�n no se invalida para permitir reintento.       |
| 4 | Sin afectaci�n a otros usuarios | La revocaci�n de un sujeto no afecta los tokens ni sesiones de otros usuarios (`UserId` distinto).                                                                                                                   |
| 5 | Sesiones an�nimas en logout     | Si `GET /api/v1/session/logout` es invocado sin sesi�n autenticada previa, redirige normalmente a Keycloak/login sin lanzar error.                                                                                   |
| 6 | Verde                           | `verificar.ps1` en verde (compilaci�n, pruebas, Modulith, ArchUnit y cobertura).                                                                                                                                     |

## 3. Reglas de negocio

| #  | Regla                                                        | D�nde vive (VO / Rule)                         | Puerto que trae el dato         | Excepci�n ? HTTP                                     |
|----|--------------------------------------------------------------|------------------------------------------------|---------------------------------|------------------------------------------------------|
| R1 | Petici�n de revocaci�n bearer requiere autenticaci�n         | Filtro de seguridad (`SecurityWebFilterChain`) | Contexto de seguridad reactivo  | `AuthenticationCredentialsNotFoundException` ? 401   |
| R2 | Fallo en almac�n de revocaci�n es fail-closed                | `TokenRevocationPort` (adaptador Redis)        | Redis                           | `DataAccessException` / `RedisSystemException` ? 503 |
| R3 | Si no hay usuario en sesi�n BFF, logout contin�a sin revocar | `KeycloakLogoutController` (s�ncrona en flujo) | `SecurityContext` de WebSession | Ninguna (contin�a con redirect)                      |

## 4. Modelo de dominio afectado

### Entidad / agregado

No se introducen entidades de dominio nuevas. Utiliza el modelo compartido `UserId` (
`co.edu.uco.seguridad.pdp.commons.model.UserId`).

### Value objects

Ninguno nuevo. Utiliza `UserId`.

## 5. Persistencia

No toca base de datos relacional ni SurrealDB. Utiliza Redis a trav�s de
`TokenRevocationPort.revokeAllSince(UserId subject, Instant since)` (clave `revocation:{subject}`, sem�ntica SET con
expiraci�n).

## 6. Endpoint

| Verbo  | Ruta                     | C�digo de �xito      | Cuerpo de entrada | Cuerpo de salida |
|--------|--------------------------|----------------------|-------------------|------------------|
| GET    | `/api/v1/session/logout` | 302 Found (Redirect) | Ninguno           | Ninguno          |
| DELETE | `/api/v1/session`        | 204 No Content       | Ninguno           | Vac�o            |

- **Autorizaci�n:**
    - `GET /api/v1/session/logout`: permitido a todos (`permitAll()`), si existe sesi�n activa procesa la revocaci�n del
      usuario asociado.
    - `DELETE /api/v1/session`: requiere autenticaci�n Bearer (`authenticated()`), el sujeto se extrae del
      `Authentication` reactivo (`PdpPrincipal` o `LocalUserPrincipal`).
- **Errores esperados:**
    - Sin token en DELETE: `401 Unauthorized`
    - Fallo en Redis: `503 Service Unavailable`

## 7. SPEC � el contrato

### [M] `co.edu.uco.seguridad.shared.web.session.KeycloakLogoutController`

```java
package co.edu.uco.seguridad.shared.web.session;

// Modificaci�n: inyecta TokenRevocationPort y TimeProvider
public final class KeycloakLogoutController {
    // Constructor modificado
    KeycloakLogoutController(KeycloakOidcSessionService oidcSessionService,
                             TokenRevocationPort tokenRevocationPort,
                             TimeProvider timeProvider,
                             @Value("${pdp.frontend.origin}") String frontendOrigin) { ...}
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

## 8. �rbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/shared/
+-- web/
�   +-- session/
�       +-- [M] KeycloakLogoutController.java
�       +-- [N] SessionRevocationController.java
```

## 9. Casos de prueba esperados

| Capa              | Clase de prueba                    | Casos                                                                                                                                                                                                                                                                                          |
|-------------------|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Web / Controlador | `KeycloakLogoutControllerTests`    | 1. Logout con sesi�n autenticada: invoca `revokeAllSince`, remueve `KEYCLOAK_ID_TOKEN`, invalida sesi�n y redirige.<br>2. Logout con sesi�n an�nima: no invoca revocaci�n, redirige normalmente.<br>3. Fallo de Redis en revocaci�n: emite error y no invalida sesi�n.                         |
| Web / Controlador | `SessionRevocationControllerTests` | 1. `DELETE /api/v1/session` autenticado con `PdpPrincipal`: invoca `revokeAllSince(principal.userId(), now)` y responde 204.<br>2. `DELETE /api/v1/session` autenticado con `LocalUserPrincipal`: invoca con su `userId` y responde 204.<br>3. Fallo de Redis: propaga error al handler (503). |
| Integraci�n HTTP  | `SessionRevocationHttpTests`       | 1. Con token bearer v�lido: `DELETE /api/v1/session` devuelve 204; subsiguiente llamada con el mismo token devuelve 401.<br>2. Sin token: devuelve 401.                                                                                                                                        |

## 10. Trazabilidad

| Fase                       | Estado      | Fecha      |
|----------------------------|-------------|------------|
| Plan                       | ? Generado  | 2026-10-21 |
| Contrato aprobado (gate 1) | ? Pendiente |            |
| Pruebas en rojo            | ? Pendiente |            |
| Implementaci�n en verde    | ? Pendiente |            |
| Validaci�n                 | ? Pendiente |            |
| Entrega (gate 2)           | ? Pendiente |            |

## 11. Ambig�edades pendientes

Ninguna. La firma de `TokenRevocationPort.revokeAllSince` y el manejo reactivo en WebFlux ya est�n probados en
`RemoveApplicationAdministratorUseCaseImpl` y `RedisTokenRevocationAdapter`.
