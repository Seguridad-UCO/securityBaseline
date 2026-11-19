# E2E canal Bearer (ADR-031/026/034, HU-026/036): lo que hace una aplicacion host con su propio login OIDC.
# Todo pasa por el edge (WAF) -> PDP -> OPA -> SurrealDB/Redis. Requiere provision-keycloak.sh.
# Variables: edge_url, kc_url, kc_realm, run_id (hurl.env.compose + --variable run_id=...).

# 1. Sin token: el PDP no deja pasar.
GET {{edge_url}}/api/v1/applications
HTTP 401

# 2. Login del host (password grant, SOLO laboratorio) con el cliente e2e-host: token con audience del PDP.
POST {{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/token
[Form]
grant_type: password
client_id: e2e-host
username: {{tester_user}}
password: {{tester_password}}
HTTP 200
[Captures]
admin_token: jsonpath "$.access_token"
[Asserts]
jsonpath "$.refresh_token" exists

# 3. Con Bearer y sin cookie ni CSRF: lectura y escritura.
GET {{edge_url}}/api/v1/applications
Authorization: Bearer {{admin_token}}
HTTP 200

POST {{edge_url}}/api/v1/applications
Authorization: Bearer {{admin_token}}
Content-Type: application/json
{"name":"e2e-host-{{run_id}}","description":"Aplicacion de la prueba E2E","baseUrl":"https://e2e.example.invalid"}
HTTP 201
[Captures]
app_id: jsonpath "$.data.id"

# 4. El registrador queda como administrador: define un rol de ESA aplicacion.
POST {{edge_url}}/api/v1/roles
Authorization: Bearer {{admin_token}}
Content-Type: application/json
{"name":"e2e-lector-{{run_id}}","scope":"APPLICATION","applicationId":"{{app_id}}"}
HTTP 201

# 5. HU-036: como administra una aplicacion, puede definir un rol de alcance TENANT.
POST {{edge_url}}/api/v1/roles
Authorization: Bearer {{admin_token}}
Content-Type: application/json
{"name":"e2e-tenant-{{run_id}}","scope":"TENANT"}
HTTP 201

# 6. Segundo usuario sin ninguna aplicacion: NO puede definir catalogo TENANT.
POST {{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/token
[Form]
grant_type: password
client_id: e2e-host
username: e2e-nonadmin
password: {{other_password}}
HTTP 200
[Captures]
other_token: jsonpath "$.access_token"

POST {{edge_url}}/api/v1/roles
Authorization: Bearer {{other_token}}
Content-Type: application/json
{"name":"e2e-intruso-{{run_id}}","scope":"TENANT"}
HTTP 403
[Asserts]
jsonpath "$.code" == "NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG"

# 7. ...ni administrar la aplicacion del primero.
GET {{edge_url}}/api/v1/applications/{{app_id}}/security/summary
Authorization: Bearer {{other_token}}
HTTP 403

GET {{edge_url}}/api/v1/applications/{{app_id}}/security/summary
Authorization: Bearer {{admin_token}}
HTTP 200

# 8. Logout con Bearer (ADR-034): revoca el token presentado en Redis. Reutilizarlo ya no sirve.
DELETE {{edge_url}}/api/v1/session
Authorization: Bearer {{admin_token}}
HTTP *
[Asserts]
status >= 200
status < 300

GET {{edge_url}}/api/v1/applications
Authorization: Bearer {{admin_token}}
HTTP 401

# 9. El cierre fue acotado al cliente: el otro usuario sigue con su sesion.
GET {{edge_url}}/api/v1/applications
Authorization: Bearer {{other_token}}
HTTP 200
