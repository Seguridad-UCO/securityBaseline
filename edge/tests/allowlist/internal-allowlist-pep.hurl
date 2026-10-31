# allowlist/internal-allowlist-pep — SOLO para la topología PEP (EDGE_INTERNAL_ALLOW_PEP=true, compose.pep.yml).
# Lista blanca de /internal: únicamente PUT /internal/v1/integrations/*/* y
# POST /internal/v1/embedded-access-decisions atraviesan el edge hacia el PEP; el resto sigue en 404 sin
# cuerpo. NO ejecutar en la topología PDP (allí todo /internal es 404: internal-channel-blocked.hurl).
# En las permitidas, lo que responda el PEP (401/400/503…) es negocio suyo y depende de qué tenga detrás
# (en la topología de fixtures el PDP no existe y el PEP responde 503): lo que se afirma es que la petición
# LLEGÓ al PEP, es decir, que no es el 404 vacío del edge y que la respuesta es un problem+json del PEP con su
# `requestId`. Un `status < 500` aquí era una afirmación sobre el backend, no sobre el edge.

POST {{edge_url}}/internal/v1/embedded-access-decisions
Content-Type: application/json
{"applicationId":"demo","resourcePath":"/x","action":"read"}
HTTP *
[Asserts]
status != 404
header "Content-Type" contains "application/problem+json"
jsonpath "$.requestId" exists

PUT {{edge_url}}/internal/v1/integrations/demo/local
Content-Type: application/json
{"enabled":true}
HTTP *
[Asserts]
status != 404
header "Content-Type" contains "application/problem+json"
jsonpath "$.requestId" exists

# El resto de /internal sigue denegado, también con método de la lista blanca en otra ruta.
GET {{edge_url}}/internal/v1/bff-session-token
HTTP 404
[Asserts]
body == ""

POST {{edge_url}}/internal/v1/access-decisions
Content-Type: application/json
{"applicationId":"demo"}
HTTP 404
[Asserts]
body == ""

GET {{edge_url}}/internal/v1/embedded-access-decisions
HTTP 404
[Asserts]
body == ""

POST {{edge_url}}/internal/v1/integrations/demo/local
Content-Type: application/json
{"enabled":true}
HTTP 404
[Asserts]
body == ""

PUT {{edge_url}}/internal/v1/inventada
Content-Type: application/json
{}
HTTP 404
[Asserts]
body == ""
