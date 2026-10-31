# flows/internal-channel-blocked — regla de ENRUTAMIENTO del edge, no detección del WAF: en la
# topología PDP TODO /internal/* es 404 sin cuerpo (default-deny), en `detection` y en `blocking`, y no
# deja detecciones en el audit. El PEP llama su canal con el PDP directo (ver bff-session-token-internal.hurl).
# Únicas excepciones (lista blanca, solo con upstream PEP): allowlist/internal-allowlist-pep.hurl.

GET {{edge_url}}/internal/v1/bff-session-token
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/bff-session-token/x
HTTP 404
[Asserts]
body == ""

POST {{edge_url}}/internal/v1/access-decisions
Content-Type: application/json
{"applicationId":"demo","resourcePath":"/x","action":"read"}
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/access-decisions/subruta
HTTP 404
[Asserts]
body == ""

POST {{edge_url}}/internal/v1/applications/names/demo/credential-validations
Content-Type: application/json
{"credential":"x"}
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/applications/00000000-0000-0000-0000-000000000000/administrators
HTTP 404
[Asserts]
body == ""

# Una ruta inventada bajo /internal también es 404 del edge (default-deny, no lista negra).
GET {{edge_url}}/internal/v1/inventada
HTTP 404
[Asserts]
body == ""

# Las rutas del starter embebido solo pasan con upstream PEP: en la topología PDP son 404.
POST {{edge_url}}/internal/v1/embedded-access-decisions
Content-Type: application/json
{"applicationId":"demo","resourcePath":"/x","action":"read"}
HTTP 404
[Asserts]
body == ""

PUT {{edge_url}}/internal/v1/integrations/demo/local
Content-Type: application/json
{"enabled":true}
HTTP 404
[Asserts]
body == ""

# Variantes de mayúsculas, barras repetidas, `;param` y sufijo.
GET {{edge_url}}/Internal/V1/BFF-Session-Token
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}//internal//v1//bff-session-token
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/bff-session-token;x=1
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/access-decisions.json
HTTP 404
[Asserts]
body == ""

# Codificación de la primera letra (%69 = i): el edge la decodifica antes de enrutar.
GET {{edge_url}}/%69nternal/v1/bff-session-token
HTTP 404
[Asserts]
body == ""
