# flows/authorize — POST /api/v1/authorize con cuerpo real atraviesa el edge y devuelve la
# decisión (200 ACCESS_EVALUATED; la decisión en sí es DENY mientras no haya política — ADR-014
# no la mapea a HTTP en este endpoint). PLAN-HU-027 §9.

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
  "name": "edge-authz-{{run_id}}",
  "description": "aplicación para authorize vía edge",
  "baseUrl": "https://authz.example.invalid"
}
HTTP 201
[Captures]
application_id: jsonpath "$.data.id"

POST {{edge_url}}/api/v1/authorize
Content-Type: application/json
X-XSRF-TOKEN: {{xsrf_token}}
{
  "applicationId": "{{application_id}}",
  "resourcePath": "/notes/77",
  "action": "read"
}
HTTP 200
[Asserts]
jsonpath "$.code" == "ACCESS_EVALUATED"
jsonpath "$.data" exists
jsonpath "$.correlationId" exists
