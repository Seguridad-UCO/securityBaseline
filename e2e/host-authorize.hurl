# E2E de la decision de acceso (lo que hace el PEP por una aplicacion protegida): administrar -> asignar -> autorizar.
# Variables: edge_url, kc_url, kc_realm, tester_user, tester_password, run_id.
POST {{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/token
[Form]
grant_type: password
client_id: e2e-host
username: {{tester_user}}
password: {{tester_password}}
HTTP 200
[Captures]
t: jsonpath "$.access_token"

GET {{edge_url}}/api/v1/users
Authorization: Bearer {{t}}
HTTP 200
[Captures]
user_id: jsonpath "$.data[?(@.email=='edge-tester@ci.invalid')].id" nth 0

POST {{edge_url}}/api/v1/applications
Authorization: Bearer {{t}}
Content-Type: application/json
{"name":"e2e-notas-{{run_id}}","description":"App de ejemplo","baseUrl":"https://notas.example.invalid"}
HTTP 201
[Captures]
app_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/applications/{{app_id}}/resources
Authorization: Bearer {{t}}
Content-Type: application/json
{"path":"/notas","method":"GET"}
HTTP 201
[Captures]
res_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/roles
Authorization: Bearer {{t}}
Content-Type: application/json
{"name":"lector-{{run_id}}","scope":"APPLICATION","applicationId":"{{app_id}}"}
HTTP 201
[Captures]
role_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/roles/{{role_id}}/resources
Authorization: Bearer {{t}}
Content-Type: application/json
{"resourceId":"{{res_id}}"}
HTTP *
[Asserts]
status < 300

# Antes de asignar el rol: DENY.
POST {{edge_url}}/api/v1/authorize
Authorization: Bearer {{t}}
Content-Type: application/json
{"applicationId":"{{app_id}}","resourcePath":"/notas","action":"GET"}
HTTP 200
[Asserts]
jsonpath "$.data.state" == "DENY"

POST {{edge_url}}/api/v1/roles/{{role_id}}/assignments
Authorization: Bearer {{t}}
Content-Type: application/json
{"userId":"{{user_id}}","applicationId":"{{app_id}}"}
HTTP *
[Asserts]
status < 300

# Despues de asignar: lo esperado es ALLOW.
POST {{edge_url}}/api/v1/authorize
Authorization: Bearer {{t}}
Content-Type: application/json
{"applicationId":"{{app_id}}","resourcePath":"/notas","action":"GET"}
HTTP 200
[Asserts]
jsonpath "$.data.state" == "ALLOW"
