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

Como la instancia local anterior ya se perdio, esta vez debes recrear manualmente al menos:

1. Realm `security-baseline`
2. `Realm settings -> Login`
   - `User registration`: `ON`
   - `Login theme`: `security-baseline`
3. Cliente `security-baseline-bff`
   - `Valid redirect URIs`: `http://localhost:8080/login/oauth2/code/keycloak`
   - `Valid post logout redirect URIs`: `http://localhost:5173`
4. Mapper del cliente `security-baseline-bff-dedicated`
   - conservar el claim `identity_provider`
5. Identity provider `google`
   - mantenerlo activo para login federado
   - registrar en Google Cloud el callback exacto que Keycloak muestre para el broker
6. Endpoint público de registro del BFF
   - `http://localhost:8080/oauth2/authorization/keycloak/register`

## Flujo esperado

- `http://localhost:5173` inicia login en `securityBaseline`
- `securityBaseline` redirige a Keycloak
- el registro local ocurre desde `GET /oauth2/authorization/keycloak/register`
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
