# Guía local: integración de un frontend con BFF, PEP, PDP y OPA

Esta guía define el patrón para las aplicaciones web que usan la plataforma de seguridad. La
aplicación de ejemplo es `../../../notes-security-demo`, que consume el backend
`../../../pep-webflux-sample`.

El canal BFF conserva una cookie HttpOnly y nunca expone su token. Todas las aplicaciones web
integradas reutilizan esa sesión central: el navegador manda la cookie con `credentials: include`;
el PEP intercambia la evidencia solo por su canal técnico con el PDP.

```text
SPA nueva ──cookie──> backend de la aplicación ──canal técnico──> PEP ──> PDP/OPA
   │                         │                                      │
   └── sin sesión ─> BFF ─> Keycloak ─> BFF ── returnTo seguro ──────┘
```

## 1. Qué se configura una vez y qué configura cada aplicación

| Ámbito | Responsabilidad | Frecuencia |
| --- | --- | --- |
| Keycloak/BFF central | Cliente `security-baseline-bff`, audiencia y URL de retorno permitida | Una vez por entorno/origen nuevo |
| PEP | Identidad técnica `security-pep-internal` | Una vez por entorno; ver la guía de Postman |
| PDP/OPA | Aplicación, recursos, roles, perfiles y asignaciones | Una vez por aplicación y recurso |
| Backend integrado | Starter PEP, identificador/credencial de la aplicación y CORS | Por aplicación |
| Frontend integrado | Chequeo de sesión BFF y llamadas con `credentials: include` | Por frontend |

No se crea un cliente Keycloak por aplicación ni por microfrontend. Cada frontend solo registra su
origen y retorno exactos en la configuración del BFF.

## 2. Configuración única de Keycloak para el BFF

En el realm `security-baseline`, el cliente `security-baseline-bff` debe tener:

- **Standard flow** activado.
- Redirect URI: `http://localhost:8080/login/oauth2/code/keycloak`.
- El mapper `identity_provider` que ya utiliza el BFF.
- Un mapper de tipo **Audience** que incluya `security-baseline-bff` en el access token.

### Scope de audiencia

Es preferible usar un client scope reutilizable, por ejemplo
`security-baseline-pdp-audience`. En el scope cree el mapper:

| Campo | Valor |
| --- | --- |
| Type | `Audience` |
| Name | `audience-security-baseline-bff` |
| Included Client Audience | `security-baseline-bff` |
| Add to access token | Activado |

Después, desde **Clients → security-baseline-bff → Client scopes**, agréguelo en **Default client
scopes**. Crearlo desde el menú global **Client scopes** no lo asocia al BFF por sí solo.

Tras modificar un mapper o scope, cierre la sesión BFF y vuelva a iniciar sesión: los tokens ya
guardados en la sesión no cambian. El access token nuevo debe incluir:

```json
"aud": ["security-baseline-bff", "account"]
```

## 3. Registrar el origen y el retorno del frontend

El BFF solo puede regresar a orígenes declarados explícitamente. Para la demo local use:

```properties
pdp.security.cors.allowed-origins[0]=http://localhost:5174
pdp.security.login.allowed-return-origins[0]=http://localhost:5174
```

También se puede suministrar la lista mediante las variables correspondientes al arrancar el PDP:

```sh
PDP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174 \
PDP_LOGIN_ALLOWED_RETURN_ORIGINS=http://localhost:5174 \
SPRING_PROFILES_ACTIVE=keycloak \
./mvnw -f pdp/pom.xml spring-boot:run
```

No use comodines para los retornos. El BFF valida `returnTo` y rechaza esquemas, hosts u orígenes
no permitidos para evitar open redirects.

## 4. Patrón mínimo del frontend

La SPA consulta la sesión central al iniciar. Si recibe `401`, redirige al login central incluyendo
su URL actual como `returnTo`:

```ts
const bffUrl = "http://localhost:8080";
const loginUrl = "http://localhost:5173/login";

async function session() {
  const response = await fetch(`${bffUrl}/api/v1/session`, { credentials: "include" });
  if (response.status === 401) {
    location.assign(`${loginUrl}?returnTo=${encodeURIComponent(location.href)}`);
    return;
  }
  if (!response.ok) throw new Error("No fue posible consultar la sesión.");
  return response.json();
}
```

Las peticiones del host al backend propio y del microfrontend al PDP envían credenciales de navegador.
El token de Keycloak nunca llega al frontend:

```ts
fetch("http://localhost:18081/api/notes", {
  credentials: "include",
});
```

El token de sesión `SECURITY_BASELINE_SESSION` es HttpOnly y no debe leerse ni copiarse desde
JavaScript. Si una SPA invoca un endpoint mutante **del BFF** directamente, debe aplicar el CSRF
del BFF (`XSRF-TOKEN` y header `X-XSRF-TOKEN`). CORS permite únicamente los orígenes explícitos
configurados.

Mapee las respuestas de seguridad a mensajes funcionales. Por ejemplo:

| Estado | Comportamiento de la SPA |
| --- | --- |
| `401` | Redirigir al login central y conservar `returnTo` |
| `403` | Alerta: “Usted no tiene permisos para acceder a este recurso.” |
| `503` | Alerta: “El servicio de seguridad no está disponible. Intente más tarde.” |

## 5. Configurar el backend de la aplicación

La aplicación de negocio instala el starter `security-pep-integration-spring-boot-starter` y define
su registro en PDP:

```properties
security.pep.enforcement.enabled=true
security.pep.enforcement.pep-url=http://localhost:8081
security.pep.enforcement.application-id=<id registrado en PDP>
security.pep.enforcement.environment=local
security.pep.enforcement.application-credential=<credencial de la aplicación>
security.pep.enforcement.public-paths=/health
```

Cuando llega una cookie BFF, el starter solicita al PEP la decisión; el PEP, con su identidad
técnica, recupera internamente el access token de la sesión BFF en PDP. El navegador no participa
en ese intercambio. El Bearer se reserva para clientes no navegadores como Postman.

### CORS del backend

El backend debe permitir el origen exacto del frontend y credenciales. En WebFlux registre un
`CorsWebFilter` anterior al filtro PEP:

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
CorsWebFilter corsWebFilter() {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of("http://localhost:5174"));
    cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Correlation-Id"));
    cors.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return new CorsWebFilter(source);
}
```

No combine `allowCredentials(true)` con `*` en `allowedOrigins`.

## 6. Política de ejemplo para notas

Registre recursos distintos en PDP/OPA para ambos métodos:

| Perfil | `GET /api/notes` | `POST /api/notes` |
| --- | --- | --- |
| Lector | Permitido | Denegado (`403`) |
| Escritor | Según la política definida | Permitido |
| Administrador | Permitido | Permitido |

La asignación del perfil pertenece al PDP. Los roles de Keycloak no sustituyen esa asignación.

## 7. Arranque y validación local

1. Levante Keycloak en `9090`.
2. Levante PDP con el perfil `keycloak` en `8080`.
3. Levante PEP en `8081`; la credencial local de `security-pep-internal` ya está configurada para
   la demostración.
4. Instale el starter localmente: `./mvnw -f pep/starter/pom.xml install`.
5. Levante `pep-webflux-sample` en `18081`.
6. Ejecute `npm run dev` en `../../../notes-security-demo` y abra `http://localhost:5174`.

Con `notes-allowed`, la consulta de notas debe devolver `200` y mostrar un `X-Decision-Id`. El
intento de crear una nota sin el permiso de escritura debe dar `403` y la alerta de permisos.

## 8. Diagnóstico rápido

| Síntoma | Causa probable | Verificación/corrección |
| --- | --- | --- |
| `401 TOKEN_INVALID` desde la SPA | Token BFF sin audiencia | Verifique `aud` y que el scope esté asociado como Default client scope |
| `401` después de cambiar un mapper | Sesión conserva token anterior | Cierre sesión BFF e inicie sesión de nuevo |
| `CORS error` | Falta `CorsWebFilter` o el origen no coincide | Permita el origen exacto y `allowCredentials(true)` |
| `403 ACCESS_DENIED` | PDP/OPA denegó el recurso | Revise perfiles, roles, recursos y asignaciones en PDP |
| `503` | PEP/PDP o identidad técnica no disponible | Revise PEP, PDP y `PEP_KEYCLOAK_CLIENT_SECRET` |

## 9. Postman

Para probar API directamente con Bearer, use
[GUIA-PRUEBA-PEP-PDP-POSTMAN.md](GUIA-PRUEBA-PEP-PDP-POSTMAN.md). Postman es un flujo distinto al
BFF: no debe fabricar ni compartir cookies de sesión para simular un frontend.
