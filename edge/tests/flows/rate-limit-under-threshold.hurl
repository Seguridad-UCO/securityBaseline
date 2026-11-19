# flows/rate-limit-under-threshold — por debajo del umbral de la zona `authorize` nadie recibe 429.
# El umbral inicial es 60/1m (PLAN-HU-027 §6.1, EDGE_RATE_AUTHORIZE); `repeat` es literal porque
# [Options] no interpola variables — si el umbral cambia, cambia aquí y en ratelimit/over-threshold.
# 50 y no 59: authorize.hurl y las demás flows también consumen la zona en la misma ventana.
# Sin sesión a propósito: el 401 del PDP es la respuesta legítima y no es 429.
POST {{edge_url}}/api/v1/authorize
Content-Type: application/json
[Options]
repeat: 50
{
  "applicationId": "no-importa",
  "resourcePath": "/x",
  "action": "read"
}
HTTP *
[Asserts]
status != 429
status < 500
