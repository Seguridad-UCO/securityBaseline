# attacks/scanner-user-agent — User-Agent de scanner conocido (CRS 913xxx). PLAN-HU-027 §9.
# La ruta es legítima a propósito: lo único hostil es el agente.
GET {{edge_url}}/actuator/health
X-Correlation-Id: edge-attack-{{run_id}}
User-Agent: sqlmap/1.7.2#stable (https://sqlmap.org)
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
jsonpath "$.code" == "REQUEST_REJECTED"
jsonpath "$.correlationId" exists
body not contains "rule"
