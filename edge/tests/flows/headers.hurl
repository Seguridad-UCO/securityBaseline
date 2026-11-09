# flows/headers — criterio 7: cabeceras de seguridad en toda respuesta a través del edge, y sin
# `Server` (E6: el edge no se identifica). PLAN-HU-027 §9.
GET {{edge_url}}/_edge/health
HTTP 200
[Asserts]
header "Strict-Transport-Security" contains "max-age="
header "X-Content-Type-Options" == "nosniff"
header "Referrer-Policy" == "no-referrer"
header "Cache-Control" contains "no-store"
header "Server" not exists

# También en una respuesta proxiada desde el PDP.
GET {{edge_url}}/actuator/health
HTTP 200
[Asserts]
header "Strict-Transport-Security" contains "max-age="
header "X-Content-Type-Options" == "nosniff"
header "Referrer-Policy" == "no-referrer"
header "Cache-Control" contains "no-store"
header "Server" not exists
