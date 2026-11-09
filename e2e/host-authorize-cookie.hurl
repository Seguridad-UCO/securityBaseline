# Misma cadena que host-authorize.hurl pero por la cookie BFF (canal del panel).
# Primer ingreso de un usuario al PDP por el panel propio (cookie BFF): aprovisiona su identidad local.
# Hoy el canal Bearer NO aprovisiona identidades (ver README, hallazgo 2): un usuario que nunca entro recibe 401.
# Variables: edge_url, tester_user, tester_password.
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
username: {{tester_user}}
password: {{tester_password}}
credentialId:
HTTP 302
[Captures]
callback_url: header "Location"

GET {{callback_url}}
HTTP *

GET {{edge_url}}/api/v1/session
HTTP 200
[Captures]
xsrf: cookie "XSRF-TOKEN"

GET {{edge_url}}/api/v1/users
X-XSRF-TOKEN: {{xsrf}}
HTTP 200
[Captures]
user_id: jsonpath "$.data[?(@.email=='edge-tester@ci.invalid')].id" nth 0

POST {{edge_url}}/api/v1/applications
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"name":"e2e-notas-{{run_id}}","description":"App de ejemplo","baseUrl":"https://notas.example.invalid"}
HTTP 201
[Captures]
app_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/applications/{{app_id}}/resources
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"path":"/notas","method":"GET"}
HTTP 201
[Captures]
res_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/roles
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"name":"lector-{{run_id}}","scope":"APPLICATION","applicationId":"{{app_id}}"}
HTTP 201
[Captures]
role_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/roles/{{role_id}}/resources
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"resourceId":"{{res_id}}"}
HTTP *
[Asserts]
status < 300

# Antes de asignar el rol: DENY.
POST {{edge_url}}/api/v1/authorize
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"applicationId":"{{app_id}}","resourcePath":"/notas","action":"GET"}
HTTP 200
[Asserts]
jsonpath "$.data.state" == "DENY"

POST {{edge_url}}/api/v1/roles/{{role_id}}/assignments
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"userId":"{{user_id}}","applicationId":"{{app_id}}"}
HTTP *
[Asserts]
status < 300

# Despues de asignar: lo esperado es ALLOW.
POST {{edge_url}}/api/v1/authorize
X-XSRF-TOKEN: {{xsrf}}
Content-Type: application/json
{"applicationId":"{{app_id}}","resourcePath":"/notas","action":"GET"}
HTTP 200
[Asserts]
jsonpath "$.data.state" == "ALLOW"
