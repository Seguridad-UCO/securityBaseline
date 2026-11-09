# Realm de CI

`security-baseline-ci-realm.json` es el realm que el overlay `edge/compose.pdp.yml` monta en
`/opt/keycloak/data/import/` para que la suite `hurl` pueda ejecutar el flujo OIDC real del BFF.

- **El secreto del client y la contraseña del usuario son de prueba**, iguales a los defaults de
  `application-keycloak.properties`; no existen fuera de este Compose. Si alguna vez se reutilizan en
  un ambiente real, eso es el defecto, no este archivo.
- `redirectUris` apunta a `edge:8088` (la suite corre dentro de la red del Compose) y a
  `localhost:8088` (uso manual desde el host).
- El mapper `self-audience` añade `security-baseline-bff` a `aud`: es lo que
  `pdp.security.jwt.audience=${PDP_KEYCLOAK_CLIENT_ID}` exige en el perfil `keycloak`.
