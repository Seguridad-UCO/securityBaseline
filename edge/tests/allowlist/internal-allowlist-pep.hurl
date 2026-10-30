# allowlist/internal-allowlist-pep — SOLO para la topología PEP (EDGE_INTERNAL_ALLOW_PEP=true, compose.pep.yml).
# Lista blanca de /internal: únicamente PUT /internal/v1/integrations/*/* y
# POST /internal/v1/embedded-access-decisions atraviesan el edge hacia el PEP; el resto sigue en 404 sin
# cuerpo. NO ejecutar en la topología PDP (allí todo /internal es 404: internal-channel-blocked.hurl).
# `status != 404` en las permitidas: lo que responda el backend (401/400/…) es negocio suyo; el 404 del
# edge es el único que se descarta. Verificado contra un edge de prueba con upstream PDP (ver EVIDENCIA).

POST {{edge_url}}/internal/v1/embedded-access-decisions
Content-Type: application/json
{"applicationId":"demo","resourcePath":"/x","action":"read"}
HTTP *
[Asserts]
status != 404
status < 500

PUT {{edge_url}}/internal/v1/integrations/demo/local
Content-Type: application/json
{"enabled":true}
HTTP *
[Asserts]
status != 404
status < 500

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
