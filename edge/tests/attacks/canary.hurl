# attacks/canary — el healthcheck del motor, visto desde fuera (PLAN-HU-027 §7.4).
# /_edge/canary responde 200 estático DESPUÉS del WAF: si un payload trivial llega al 200, el motor
# no está bloqueando aunque el proxy esté vivo.
GET {{edge_url}}/_edge/canary
HTTP 200

GET {{edge_url}}/_edge/canary
X-Correlation-Id: edge-attack-{{run_id}}
[Query]
p: <script>alert(1)</script>
HTTP 403
[Asserts]
jsonpath "$.code" == "REQUEST_REJECTED"
jsonpath "$.correlationId" == "edge-attack-{{run_id}}"
body not contains "rule"
