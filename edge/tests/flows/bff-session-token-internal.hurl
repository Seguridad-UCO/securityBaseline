# flows/bff-session-token-internal — HU-030 (renovación del token de la sesión BFF para el PEP).
# El canal `/internal/v1/bff-session-token` NO atraviesa el edge (flows/internal-channel-blocked.hurl):
# el PEP lo llama directo al PDP con su identidad técnica (client_credentials, HU-003) + la cookie
# SECURITY_BASELINE_SESSION del usuario (BffSessionTokenResolver). Esta prueba hace lo mismo desde la
# red del Compose contra `{{pdp_url}}` (http://pdp:8080), sin pasar por el edge:
#   1. sin identidad técnica el canal responde 401;
#   2. con identidad técnica pero sin cookie BFF, 401;
#   3. con ambas, dos llamadas devuelven dos tokens DISTINTOS y vigentes (renovación en cada llamada);
#   4. tras el logout, la misma cookie ya no renueva (401).
# La renovación en cada llamada la fuerza edge/compose.pdp.yml con
# PDP_SECURITY_SESSION_TOKEN_RENEWAL_SKEW=3600s (mayor que los 300 s del access token del realm de CI)
# SOLO para las pruebas del edge; el default de producción (60 s) no cambia.
# Los tokens nunca se imprimen: solo se capturan claims decodificados (jti, iat, exp).
# El login sí pasa por el edge (así se obtiene la cookie, como en producción).

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
[Captures]
bff_cookie: cookie "SECURITY_BASELINE_SESSION"
[Asserts]
status >= 300
status < 400

GET {{edge_url}}/api/v1/session
HTTP 200
[Captures]
xsrf_token: cookie "XSRF-TOKEN"
subject: jsonpath "$.data.subject"
# --- fin del prólogo ---
# La cookie de sesión (capturada en el callback) se reenvía a mano al PDP: la cookie jar de Hurl es por host.
GET {{edge_url}}/api/v1/session
HTTP 200

# 1. Directo al PDP, sin identidad técnica.
GET {{pdp_url}}/internal/v1/bff-session-token
Cookie: SECURITY_BASELINE_SESSION={{bff_cookie}}
HTTP 401
[Asserts]
body not contains "accessToken"

# 2. Identidad técnica del PEP (realm de CI), igual que PdpServiceTokenProvider.
POST {{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/token
[Form]
grant_type: client_credentials
client_id: {{pep_internal_client_id}}
client_secret: {{pep_internal_secret}}
HTTP 200
[Captures]
tech_token: jsonpath "$.access_token"
tech_iat: jsonpath "$.access_token" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.iat"

# 3. Identidad técnica SIN cookie BFF: 401.
GET {{pdp_url}}/internal/v1/bff-session-token
Authorization: Bearer {{tech_token}}
HTTP 401

# 4. Primera llamada: identidad técnica + cookie BFF.
GET {{pdp_url}}/internal/v1/bff-session-token
Authorization: Bearer {{tech_token}}
Cookie: SECURITY_BASELINE_SESSION={{bff_cookie}}
HTTP 200
[Captures]
jti1: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.jti"
iat1: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.iat"
exp1: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.exp"
[Asserts]
jsonpath "$.accessToken" exists

# 5. Segunda llamada: el skew mayor que la vida del token fuerza otra renovación contra Keycloak.
GET {{pdp_url}}/internal/v1/bff-session-token
Authorization: Bearer {{tech_token}}
Cookie: SECURITY_BASELINE_SESSION={{bff_cookie}}
HTTP 200
[Captures]
jti2: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.jti"
iat2: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.iat"
exp2: jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.exp"
[Asserts]
variable "jti2" != {{jti1}}
variable "exp2" >= {{exp1}}
variable "iat2" >= {{iat1}}
# vigentes: ambos vencen después del momento en que se acuñó la identidad técnica (hace segundos)
variable "exp1" > {{tech_iat}}
variable "exp2" > {{tech_iat}}
jsonpath "$.accessToken" split "." nth 1 base64UrlSafeDecode decode "utf-8" jsonpath "$.azp" == "security-baseline-bff"

# 6. Tras el logout (por el edge) la misma cookie ya no renueva.
GET {{edge_url}}/api/v1/session/logout
HTTP *
[Asserts]
status >= 200
status < 400

GET {{pdp_url}}/internal/v1/bff-session-token
Authorization: Bearer {{tech_token}}
Cookie: SECURITY_BASELINE_SESSION={{bff_cookie}}
HTTP 401

# Nota: la vigencia se verifica por `exp` frente al `iat` de la identidad técnica recién acuñada; usar
# el token como bearer en /api/v1/** sigue pendiente de su prueba E2E (HANDOFF-VALIDACION-HU-027-029).
