#!/bin/sh
# PDP de pruebas (HU-034): handler de `nc -l -p 8080 -e`. Lee la peticion, escribe UNA linea JSON con la forma
# de security-pdp.json (mismos campos que el PDP real) con el X-Correlation-Id recibido y responde 200.
# No es el PDP: solo permite probar "la peticion que el edge deja pasar queda en los logs del PDP".
# El id se sanea ([A-Za-z0-9._:-]) para que la linea sea siempre JSON valido.
cid=""
while IFS= read -r line; do
  line=$(printf '%s' "$line" | tr -d '\r')
  [ -z "$line" ] && break
  case "$line" in [Xx]-[Cc]orrelation-[Ii]d:*) cid=$(printf '%s' "${line#*:}" | tr -cd 'A-Za-z0-9._:-') ;; esac
done
ts=$(date -u +%Y-%m-%dT%H:%M:%S.000000000Z)
printf '{"@timestamp":"%s","level":"INFO","message":"Petición HTTP finalizada","correlationId":"%s","event.name":"http.request.completed","http.response.status_code":200,"service.name":"security-pdp","deployment.environment.name":"test"}\n' \
  "$ts" "$cid" >> /var/log/security/security-pdp.json
printf 'HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok'
