# Pruebas de exclusiones

Una por exclusión, `<id>-<slug>.hurl`, mismo id que `../../rules/exclusions/<id>-<slug>.conf`.
Cada una reproduce el falso positivo exacto (misma ruta, mismo campo, mismo valor) y asserta que la
petición legítima pasa (2xx/4xx del PDP, nunca `REQUEST_REJECTED`). `tools/check-exclusions.sh`
falla si falta la pareja. Vacío hasta la fase de detección.
