# attacks/xss-query — XSS reflejado en la query (CRS 941xxx). PLAN-HU-027 §9.
GET {{edge_url}}/api/v1/applications
[Query]
q: <script>alert(1)</script>
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
jsonpath "$.code" == "REQUEST_REJECTED"
jsonpath "$.correlationId" exists
body not contains "rule"
