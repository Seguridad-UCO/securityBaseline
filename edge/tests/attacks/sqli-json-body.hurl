# attacks/sqli-json-body — inyección en un cuerpo JSON: prueba que el procesador JSON está activo
# (SecRequestBodyAccess On + JSON). PLAN-HU-027 §9.
POST {{edge_url}}/api/v1/applications
X-Correlation-Id: edge-attack-{{run_id}}
Content-Type: application/json
{
  "name": "x' UNION SELECT credentialHash FROM application--",
  "description": "sqli en cuerpo json",
  "baseUrl": "https://app.example.invalid"
}
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
jsonpath "$.code" == "REQUEST_REJECTED"
jsonpath "$.correlationId" exists
body not contains "rule"
body not contains "score"
