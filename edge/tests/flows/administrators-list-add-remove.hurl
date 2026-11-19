# flows/administrators-list-add-remove — HU-020 a través del edge. PLAN-HU-027 §9 y §11.4.
# La topología de pruebas del edge arranca el PDP con PDP_SECURITY_MFA_ENABLED=false (compose.pdp.yml):
# el realm de CI no emite MFA y con el default fail-closed el listado respondería MFA_REQUIRED. Así el
# listado se afirma estricto (200); POST/DELETE son negocio del PDP (duplicado, último administrador)
# y lo que se garantiza es que el edge no los rechazó (nunca REQUEST_REJECTED, nunca 5xx).

# --- prólogo: login OIDC (idéntico a oidc-login-session-logout.hurl pasos 1-5) ---
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
[Asserts]
status >= 300
status < 400

GET {{edge_url}}/api/v1/session
HTTP 200
[Captures]
xsrf_token: cookie "XSRF-TOKEN"
subject: jsonpath "$.data.subject"
# --- fin del prólogo ---

POST {{edge_url}}/api/v1/applications
Content-Type: application/json
X-XSRF-TOKEN: {{xsrf_token}}
{
  "name": "edge-admins-{{run_id}}",
  "description": "aplicación para probar administradores vía edge",
  "baseUrl": "https://admins.example.invalid"
}
HTTP 201
[Captures]
application_id: jsonpath "$.data.id"

# Quien registra es el primer administrador (HU-015): el listado responde 200 a través del edge.
GET {{edge_url}}/api/v1/applications/{{application_id}}/administrators
HTTP 200
[Asserts]
jsonpath "$.data" exists

# Agregar (el propio sujeto: lo que responda el PDP es negocio suyo — duplicado o MFA).
POST {{edge_url}}/api/v1/applications/{{application_id}}/administrators
Content-Type: application/json
X-XSRF-TOKEN: {{xsrf_token}}
{
  "userId": "{{subject}}"
}
HTTP *
[Asserts]
status < 500
status != 401
jsonpath "$.code" != "REQUEST_REJECTED"

# Quitar (mismo criterio: último administrador o MFA, nunca el edge).
DELETE {{edge_url}}/api/v1/applications/{{application_id}}/administrators/{{subject}}
X-XSRF-TOKEN: {{xsrf_token}}
HTTP *
[Asserts]
status < 500
status != 401
jsonpath "$.code" != "REQUEST_REJECTED"
