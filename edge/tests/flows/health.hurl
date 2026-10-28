# flows/health — el edge está vivo y proxya. PLAN-HU-027 §9.
GET {{edge_url}}/_edge/health
HTTP 200

# Ruta pública del PDP a través del edge (permitAll en ambas cadenas de seguridad).
GET {{edge_url}}/actuator/health
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"
