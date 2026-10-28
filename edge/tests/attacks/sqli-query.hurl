# attacks/sqli-query — inyección SQL en la query string (CRS 942xxx). PLAN-HU-027 §9.
# Esperado en blocking: 403 problem+json REQUEST_REJECTED, sin id de regla en el cuerpo (E6).
GET {{edge_url}}/api/v1/applications
[Query]
name: ' OR 1=1--
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
header "X-Correlation-Id" exists
jsonpath "$.status" == 403
jsonpath "$.code" == "REQUEST_REJECTED"
jsonpath "$.title" == "REQUEST_REJECTED"
jsonpath "$.type" == "urn:security-baseline:error:request-rejected"
jsonpath "$.correlationId" exists
jsonpath "$.requestId" exists
body not contains "rule"
body not contains "942"
body not contains "score"
