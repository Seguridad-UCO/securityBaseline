# Keycloak local setup

Esta carpeta versiona el theme de login `security-baseline` usado por el realm local `security-baseline`.

## Levantar Keycloak

```bash
docker compose up keycloak surrealdb
```

Keycloak queda en `http://localhost:9090`, persiste sus datos en el volumen Docker `keycloak_data`, monta el theme desde `./keycloak/themes` y deja preparada la importacion de realms desde `./keycloak/import`.

## Persistencia local

- `docker compose up keycloak` reutiliza la configuracion si el volumen `keycloak_data` ya existe.
- `docker compose down` ya no deberia borrarte el realm porque los datos quedan en ese volumen.
- si ejecutas `docker compose down -v` o eliminas manualmente el volumen `keycloak_data`, Keycloak vuelve a quedar vacio.

## Import de realm

El contenedor arranca con `--import-realm`. Si luego agregas un export valido en `./keycloak/import`, Keycloak puede crear el realm automaticamente en una instancia nueva.

Para versionar ese estado mas adelante, guarda ahi un archivo como `security-baseline-realm.json`.

## Que debes reconfigurar ahora

Como la instancia local anterior ya se perdio, esta vez debes recrear manualmente al menos esta configuracion en la consola de admin de Keycloak.

### 1. Realm

- Crear el realm `security-baseline`.

### 2. Realm settings -> Login

- `User registration`: `ON`
- `Login theme`: `security-baseline`

Efecto esperado:

- Keycloak permite registro local con `user/password`.
- El theme personalizado deja `Google` visible solo en `login`.
- La vista `Crear cuenta` queda reservada para registro local, no para proveedores federados.

### 3. Cliente `security-baseline-bff`

Configurar el cliente OIDC que usa el backend BFF.

- `Client ID`: `security-baseline-bff`
- `Valid redirect URIs`:
  - `http://localhost:8080/login/oauth2/code/keycloak`
- `Valid post logout redirect URIs`:
  - `http://localhost:5173`
  - `http://localhost:5173?registered=success`

Notas:

- El callback tecnico OIDC del backend es solo `http://localhost:8080/login/oauth2/code/keycloak`.
- El valor `http://localhost:5173?registered=success` es obligatorio para que el logout automatico post-registro no falle con `invalid uri`.
- Si falta ese URI, Keycloak no completa el cierre de sesion despues del registro y el usuario puede quedar autenticado por error.

### 4. Mapper del cliente `security-baseline-bff-dedicated`

- Conservar el claim `identity_provider`.

Ese claim se usa en el backend para distinguir si el acceso vino de `keycloak-local`, `google` u otro broker configurado.

### 5. Identity provider `google`

- Mantenerlo activo para login federado.
- Registrar en Google Cloud el callback exacto que Keycloak muestre para el broker.

Regla funcional esperada:

- `Google` se usa solo para `Iniciar sesion`.
- `Google` no debe usarse como mecanismo de `Crear cuenta`.
- Si un usuario entra por Google por primera vez, el backend lo aprovisiona localmente en el primer login exitoso, pero eso sigue siendo flujo de `login`, no de `register`.

### 6. Endpoints publicos del BFF

Estos endpoints no se configuran dentro de Keycloak, pero conviene validarlos en la integracion local:

- Login:
  - `http://localhost:8080/oauth2/authorization/keycloak`
- Registro:
  - `http://localhost:8080/oauth2/authorization/keycloak/register`
- Callback tecnico:
  - `http://localhost:8080/login/oauth2/code/keycloak`
- Logout BFF:
  - `http://localhost:8080/api/v1/session/logout`

### 7. Comportamiento esperado para validar la configuracion

#### Login local o con Google

- Desde `http://localhost:5173`, `Iniciar sesion` lleva a Keycloak.
- En login deben verse:
  - formulario local `user/password`
  - boton `Google`
- Si el login es exitoso, el backend crea o actualiza la sesion local y redirige a `http://localhost:5173`.

#### Registro local

- Desde `http://localhost:5173`, `Crear cuenta` lleva al formulario de registro de Keycloak.
- En Keycloak `26.7.x`, `prompt=create` existe como estandar OIDC, pero en este proyecto el flujo publico de registro usa la ruta OIDC `/protocol/openid-connect/registrations`.
- La razon practica es evitar que `prompt=create` se propague al broker de Google cuando el usuario vuelve desde `Create account` a `Sign in`.
- El BFF ya no depende de `kc_action=register` para abrir la vista de registro.
- En register no debe verse `Google`.
- Al completar el registro:
  - Keycloak autentica tecnicamente el callback
  - el BFF no crea sesion local
  - el BFF dispara logout OIDC automatico
  - Keycloak vuelve a `http://localhost:5173?registered=success`
  - la SPA queda anonima
  - el usuario debe iniciar sesion manualmente despues

#### Logout

- `GET /api/v1/session/logout` invalida la sesion BFF
- luego completa el logout OIDC de Keycloak
- finalmente vuelve a `http://localhost:5173`

## Flujo esperado

- `http://localhost:5173` inicia login en `securityBaseline`
- `securityBaseline` redirige a Keycloak
- el registro local ocurre desde `GET /oauth2/authorization/keycloak/register`
- el BFF transforma ese endpoint publico en una autorizacion OIDC hacia `/protocol/openid-connect/registrations`
- el primer login exitoso crea `security_user` y `external_identity` en SurrealDB
- `GET /api/v1/session/logout` invalida la sesion BFF y redirige al logout OIDC de Keycloak

## Verificacion rapida del theme

El theme real queda montado desde `./keycloak/themes/security-baseline` e incluye:

- `login/theme.properties`
- `login/resources/css/security-baseline.css`
- `login/resources/js/security-baseline-nav.js`
- `login/resources/img/security-baseline-grid.svg`
- `login/messages/messages_es.properties`
- `login/messages/messages_en.properties`
