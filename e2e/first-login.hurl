# Primer ingreso de un usuario al PDP por el panel propio (cookie BFF): aprovisiona su identidad local.
# Hoy el canal Bearer NO aprovisiona identidades (ver README, hallazgo 2): un usuario que nunca entro recibe 401.
# Variables: edge_url, login_user, login_password.
GET {{edge_url}}/oauth2/authorization/keycloak
HTTP 302
[Captures]
auth_url: header "Location"

GET {{auth_url}}
HTTP 200
[Captures]
login_action: xpath "string(//form[@id='kc-form-login']/@action)"

POST {{login_action}}
[Form]
username: {{login_user}}
password: {{login_password}}
credentialId:
HTTP 302
[Captures]
callback_url: header "Location"

GET {{callback_url}}
HTTP *

GET {{edge_url}}/api/v1/session
HTTP 200
