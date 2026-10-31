# flows/oidc-login-session-logout — el flujo BFF completo atraviesa el edge sin una sola detección:
# inicio → login en Keycloak por formulario → callback → cookie de sesión → /api/v1/session →
# CSRF (rechazo sin token, aceptación con token) → logout. PLAN-HU-027 §9, criterio 2.
# Criterio 2 exige además que el audit log de Coraza quede vacío: lo comprueba el pipeline.

# 1. El PDP inicia el flujo y redirige a Keycloak.
GET {{edge_url}}/oauth2/authorization/keycloak
HTTP 302
[Captures]
auth_url: header "Location"
[Asserts]
header "Location" startsWith "{{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/auth"
header "Location" contains "client_id=security-baseline-bff"
header "Location" contains "redirect_uri="
cookie "SECURITY_BASELINE_SESSION" exists

# 2. Página de login de Keycloak; se captura la acción del formulario (lleva session_code/execution).
GET {{auth_url}}
HTTP 200
[Captures]
login_action: xpath "string(//form[@id='kc-form-login']/@action)"
[Asserts]
xpath "string(//form[@id='kc-form-login']/@action)" startsWith "{{kc_url}}/"

# 3. Credenciales de prueba → Keycloak redirige al callback del PDP a través del edge.
POST {{login_action}}
[Form]
username: {{tester_user}}
password: {{tester_password}}
credentialId:
HTTP 302
[Captures]
callback_url: header "Location"
[Asserts]
header "Location" startsWith "{{edge_url}}/login/oauth2/code/keycloak"
header "Location" contains "code="
header "Location" contains "state="

# 4. El callback pasa por el edge (query con `code` y `state`, candidatos a falso positivo) y deja
#    la sesión; el PDP redirige al frontend, que no se sigue.
GET {{callback_url}}
HTTP *
[Asserts]
status >= 300
status < 400
cookie "SECURITY_BASELINE_SESSION" exists

# 5. Sesión autenticada.
GET {{edge_url}}/api/v1/session
HTTP 200
[Captures]
xsrf_token: cookie "XSRF-TOKEN"
subject: jsonpath "$.data.subject"
[Asserts]
jsonpath "$.data.subject" exists
jsonpath "$.data.email" == "edge-tester@ci.invalid"
cookie "XSRF-TOKEN" exists

# 6. CSRF sigue vigente a través del edge: un POST sin X-XSRF-TOKEN lo rechaza el PDP (FORBIDDEN),
#    no el edge (REQUEST_REJECTED). Es el 403 "del otro lado" y el `code` los distingue (§11.4).
POST {{edge_url}}/api/v1/applications
Content-Type: application/json
{
  "name": "edge-csrf-{{run_id}}",
  "description": "no debe crearse: falta el token CSRF",
  "baseUrl": "https://csrf.example.invalid"
}
HTTP 403
[Asserts]
jsonpath "$.code" == "FORBIDDEN"
jsonpath "$.code" != "REQUEST_REJECTED"

# 7. Con el token, el mismo POST es aceptado: la cadena cookie + XSRF atraviesa el edge intacta.
POST {{edge_url}}/api/v1/applications
Content-Type: application/json
X-XSRF-TOKEN: {{xsrf_token}}
{
  "name": "edge-oidc-{{run_id}}",
  "description": "creada por la suite del edge",
  "baseUrl": "https://oidc.example.invalid"
}
HTTP 201
[Asserts]
jsonpath "$.data.id" exists
jsonpath "$.data.name" == "edge-oidc-{{run_id}}"

# 7b. HU-029 retiró POST /api/v1/session/token/refresh (cada app renueva directo con Keycloak): con
#     cookie válida y CSRF correcto el endpoint ya no existe. 404 es del PDP, no del edge.
POST {{edge_url}}/api/v1/session/token/refresh
Content-Type: application/json
X-XSRF-TOKEN: {{xsrf_token}}
{}
HTTP 404

# 8. Logout: el PDP cierra la sesión y redirige al logout de Keycloak.
GET {{edge_url}}/api/v1/session/logout
HTTP *
[Asserts]
status >= 200
status < 400

# 9. Sin sesión, /api/v1/session ya no autentica.
GET {{edge_url}}/api/v1/session
HTTP *
[Asserts]
status >= 300
status < 500

# 10. Sin cookie de sesión (host alterno encaminado al mismo edge: la cookie jar es por host), el
#     endpoint retirado responde 401, no 404: la autenticación va antes que el enrutado.
POST http://nocookie.invalid:8088/api/v1/session/token/refresh
Content-Type: application/json
[Options]
connect-to: nocookie.invalid:8088:edge:8088
{}
HTTP 401
[Asserts]
jsonpath "$.code" != "REQUEST_REJECTED"
