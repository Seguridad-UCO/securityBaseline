# attacks/content-type-mismatch — Content-Type fuera de tx.allowed_request_content_type
# (CRS 920420): la API solo acepta JSON, y el login OIDC form-urlencoded. PLAN-HU-027 §9.
POST {{edge_url}}/api/v1/applications
Content-Type: text/xml
```
<?xml version="1.0"?><!DOCTYPE x [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><app>&xxe;</app>
```
HTTP 403
[Asserts]
header "Content-Type" contains "application/problem+json"
jsonpath "$.code" == "REQUEST_REJECTED"
body not contains "rule"
