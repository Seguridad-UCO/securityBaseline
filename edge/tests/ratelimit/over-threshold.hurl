# ratelimit/over-threshold — criterio 6: N+1 peticiones a /api/v1/authorize desde UNA IP en la
# ventana → la última recibe 429 problem+json RATE_LIMITED con Retry-After. PLAN-HU-027 §9.
# El caso "dos IPs no disparan": hurl no tiene condicionales, así que lo produce el pipeline
# ejecutando flows/rate-limit-under-threshold.hurl desde el servicio `hurl-second-ip` EN PARALELO
# con este archivo. Si el edge contara por algo distinto de la IP cliente, el segundo servicio
# vería 429 y fallaría.
# Umbral literal 60/1m (EDGE_RATE_AUTHORIZE) — [Options] no interpola variables; si cambia el
# umbral, cambia aquí y en flows/rate-limit-under-threshold.hurl.
POST {{edge_url}}/api/v1/authorize
Content-Type: application/json
[Options]
repeat: 60
{
  "applicationId": "no-importa",
  "resourcePath": "/x",
  "action": "read"
}
HTTP *
[Asserts]
status < 500

POST {{edge_url}}/api/v1/authorize
Content-Type: application/json
{
  "applicationId": "no-importa",
  "resourcePath": "/x",
  "action": "read"
}
HTTP 429
[Asserts]
header "Content-Type" contains "application/problem+json"
header "Retry-After" exists
jsonpath "$.status" == 429
jsonpath "$.code" == "RATE_LIMITED"
jsonpath "$.type" == "urn:security-baseline:error:rate-limited"
jsonpath "$.correlationId" exists
