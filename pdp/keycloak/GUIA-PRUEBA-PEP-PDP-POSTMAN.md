# Guía local: clientes Keycloak para probar PEP → PDP → OPA

Esta guía prepara dos identidades distintas dentro del realm local `security-baseline`:

| Identidad | Cliente | Uso |
| --- | --- | --- |
| Técnica | `security-pep-internal` | El PEP valida ante el PDP la credencial de una aplicación registrada. |
| Humana de prueba | `security-postman-test` | Postman obtiene el Bearer de cada usuario para invocar el backend protegido. |

No reutilices el token de una persona como identidad técnica del PEP. Tampoco copies secretos ni
access tokens a `application.properties` ni los subas al repositorio.

## Antes de empezar

1. Levanta Keycloak y entra a `http://localhost:9090/admin`.
2. Cambia del realm `master` al realm `security-baseline` en el selector superior izquierdo.
3. Comprueba que ya existe el cliente `security-baseline-bff`, que usa el frontend/BFF.

> Esta guía es solamente para local. El PDP local tiene `PDP_INTERNAL_MTLS_ENABLED=false` por
> defecto para facilitar esta prueba; los perfiles `dev` y `prod` requieren mTLS.

## 1. Crear la identidad técnica del PEP

1. Ve a **Clients** y selecciona **Create client**.
2. Elige **OpenID Connect** y usa como **Client ID**: `security-pep-internal`.
3. Pulsa **Next**. En **Capability config** activa:
   - **Client authentication**: `On`.
   - **Service accounts roles**: `On`.
   - **Standard flow**: `Off`.
   - **Direct access grants**: `Off`.
4. Pulsa **Save**.
5. Abre la pestaña **Credentials** y copia el **Client secret**. Guárdalo en un gestor de secretos
   local; Keycloak solo permite regenerarlo, no recuperarlo después.

### Agregar la audiencia que espera el PDP

El PDP valida actualmente que `aud` contenga `security-baseline-bff`. Por eso el token técnico del
PEP debe llevar esa audiencia aun cuando el cliente se llame `security-pep-internal`.

1. Dentro de `security-pep-internal`, abre **Client scopes**.
2. En el scope dedicado del cliente, selecciona **Add mapper** → **By configuration** →
   **Audience**.
3. Configura:
   - **Name**: `audience-security-baseline-bff`.
   - **Included Client Audience**: `security-baseline-bff`.
   - **Add to access token**: `On`.
4. Guarda el mapper.

No son necesarios realm roles ni client roles para este token técnico: el canal interno actual
solo valida emisor, sujeto y audiencia.

## 2. Configurar la renovación automática en el PEP

El PEP solicita el token con `client_credentials`, lo mantiene solo en memoria y lo renueva 30
segundos antes de vencer. No copies el `access_token` en ninguna propiedad.

En la configuración de ejecución del PEP en IntelliJ agrega únicamente:

```text
PEP_KEYCLOAK_CLIENT_SECRET=<secret de security-pep-internal>
```

Los demás valores ya tienen estos defaults locales:

```text
PEP_KEYCLOAK_TOKEN_URI=http://localhost:9090/realms/security-baseline/protocol/openid-connect/token
PEP_KEYCLOAK_CLIENT_ID=security-pep-internal
```

Si Keycloak no puede emitir el token, el PEP deja un `WARN` sanitizado y responde `503` a las
solicitudes protegidas. No expone el secreto ni permite la solicitud. Puedes verificar el token
generado temporalmente con la petición `client_credentials` de Postman, pero no necesitas copiarlo
al PEP.

## 3. Crear el cliente de Postman

Este cliente entrega tokens de personas para probar autorización. No es el cliente técnico del PEP.

1. En **Clients**, selecciona **Create client**.
2. Elige **OpenID Connect** y usa **Client ID**: `security-postman-test`.
3. En **Capability config** define:
   - **Client authentication**: `Off` (cliente público para PKCE).
   - **Standard flow**: `On`.
   - **Direct access grants**: `Off`.
   - **Implicit flow**: `Off`.
4. En **Login settings** agrega:
   - **Valid redirect URIs**: `https://oauth.pstmn.io/v1/callback`
   - **Web origins**: `https://oauth.pstmn.io`
5. Guarda.
6. Repite el mapper de audiencia del paso 1, con el mismo valor:
   `security-baseline-bff` y **Add to access token** activado.

## 4. Crear dos usuarios locales

Usar usuarios locales hace la prueba repetible; no dependes de Google ni de contraseñas federadas.

Para cada usuario, ve a **Users** → **Add user**:

| Usuario sugerido | Finalidad |
| --- | --- |
| `notes-allowed` | Recibirá el perfil/rol que permite `GET /api/notes`. |
| `notes-denied` | No recibirá ese perfil/rol. |

En ambos casos:

1. Define username y email; activa **Email verified** si no quieres validar correo.
2. Guarda el usuario.
3. Abre **Credentials** → **Set password**.
4. Define una contraseña local y deja **Temporary** en `Off`.

Después inicia sesión al menos una vez por el BFF con cada usuario. Así el PDP aprovisiona su
identidad local. En el frontend/PDP, asigna al usuario `notes-allowed` el perfil que agrupa el rol
del recurso `GET /api/notes`; no asignes ese perfil a `notes-denied`.

## 5. Obtener un Bearer de usuario en Postman

En Postman abre **Authorization** → tipo **OAuth 2.0** → **Get New Access Token**:

| Campo | Valor |
| --- | --- |
| Token name | `notes-allowed-local` o `notes-denied-local` |
| Grant type | `Authorization Code (With PKCE)` |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Auth URL | `http://localhost:9090/realms/security-baseline/protocol/openid-connect/auth` |
| Access Token URL | `http://localhost:9090/realms/security-baseline/protocol/openid-connect/token` |
| Client ID | `security-postman-test` |
| Client secret | Vacío |
| Code challenge method | `S256` |
| Scope | `openid profile email` |

Pulsa **Get New Access Token**, inicia sesión con uno de los usuarios locales y selecciona
**Use Token**. Antes de usarlo confirma que el `aud` contiene `security-baseline-bff`.

## 6. Prueba esperada al terminar la conexión PEP/PDP

Con PEP y PDP configurados para Keycloak (issuer/JWKS) y con el token técnico del PEP cargado:

```text
GET http://localhost:18081/api/notes
Authorization: Bearer <token de notes-denied>
→ 403 ACCESS_DENIED

GET http://localhost:18081/api/notes
Authorization: Bearer <token de notes-allowed>
→ 200
```

Sin `Authorization` debe permanecer en `401 TOKEN_INVALID`. Si el PEP o PDP no puede verificar la
solicitud, el resultado debe ser `503`, nunca `200`.

## Por qué el token del login del frontend no sirve en Postman

El frontend usa un BFF: el navegador conserva una cookie de sesión y el backend no expone el access
token de Keycloak a JavaScript. Eso limita el impacto de XSS y evita que un token reutilizable quede
en el navegador. El cliente `security-postman-test` usa su propio flujo Authorization Code + PKCE
solo para pruebas de API y entrega un access token directamente a Postman.
