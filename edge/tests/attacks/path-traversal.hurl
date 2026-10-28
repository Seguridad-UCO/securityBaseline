# attacks/path-traversal — traversal codificado y archivos restringidos (CRS 930xxx). PLAN-HU-027 §9.
# El traversal va codificado a propósito: un cliente HTTP normaliza `..` en claro antes de enviarlo.
GET {{edge_url}}/api/v1/%2e%2e/%2e%2e/etc/passwd
HTTP 403
[Asserts]
jsonpath "$.code" == "REQUEST_REJECTED"

GET {{edge_url}}/.env
HTTP 403
[Asserts]
jsonpath "$.code" == "REQUEST_REJECTED"

GET {{edge_url}}/.git/config
HTTP 403
[Asserts]
jsonpath "$.code" == "REQUEST_REJECTED"
body not contains "rule"
