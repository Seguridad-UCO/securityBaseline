# attacks/method-not-allowed — método fuera de tx.allowed_methods (CRS 911100). PLAN-HU-027 §9.
# Lo rechaza el edge, nunca el PDP: por eso se exige el sobre REQUEST_REJECTED y no un 405 de Spring.
TRACE {{edge_url}}/api/v1/session
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
jsonpath "$.code" == "REQUEST_REJECTED"
body not contains "rule"
