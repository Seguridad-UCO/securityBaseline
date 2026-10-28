# flows/correlation — E7: X-Request-Id / X-Correlation-Id se propagan si vienen y se generan si no,
# con los mismos nombres que lee CorrelationWebFilter. PLAN-HU-027 §9.
# Que el mismo id aparezca en el log del PDP lo verifica el pipeline sobre observability/logs/
# (hurl no lee archivos): ver ci/edge-pipeline.yml paso 3.
GET {{edge_url}}/actuator/health
X-Correlation-Id: hurl-corr-{{run_id}}
X-Request-Id: hurl-req-{{run_id}}
HTTP 200
[Asserts]
header "X-Correlation-Id" == "hurl-corr-{{run_id}}"
header "X-Request-Id" == "hurl-req-{{run_id}}"

GET {{edge_url}}/actuator/health
HTTP 200
[Asserts]
header "X-Correlation-Id" matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
header "X-Request-Id" matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
