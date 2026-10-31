# flows/applications-register-list — cuerpos JSON reales con caracteres que CRS mira de cerca
# (comillas simples, acentos, guiones dobles) deben pasar; listado paginado con query. PLAN-HU-027 §9.
# Candidato conocido a falso positivo: 942100 (libinjection) sobre ARGS:name — si dispara, la
# exclusión va en rules/exclusions/ con su prueba gemela, no aquí.

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
  "name": "edge-O'Higgins-{{run_id}}",
  "description": "Descripción con acentos, ñ, comillas 'simples' y guiones -- dobles",
  "baseUrl": "https://ohiggins.example.invalid/app?tab=1&x=y"
}
HTTP 201
[Captures]
application_id: jsonpath "$.data.id"
[Asserts]
jsonpath "$.code" exists
jsonpath "$.data.id" exists
jsonpath "$.data.name" == "edge-O'Higgins-{{run_id}}"
jsonpath "$.correlationId" exists

# Listado paginado con filtros por query (criterios 16-19 del PDP, a través del edge).
GET {{edge_url}}/api/v1/applications
[Query]
page: 0
size: 10
name: edge-O'Higgins-{{run_id}}
HTTP 200
[Asserts]
jsonpath "$.data" exists

# Combinación ambigua de paginación: el 400 debe venir del PDP (criterio 19), no del edge.
GET {{edge_url}}/api/v1/applications
[Query]
page: 0
size: 10
offset: 5
HTTP 400
[Asserts]
jsonpath "$.code" != "REQUEST_REJECTED"
