# Exclusiones CRS

Una exclusión por archivo, `<id>-<slug>.conf` con id en 900000–999999, **siempre** acotada a una ruta
(`SecRule REQUEST_URI "@beginsWith /…" "…,ctl:ruleRemoveTargetById=<regla>;<target>"`) y **siempre**
con su prueba gemela `../../tests/exclusions/<id>-<slug>.hurl` que reproduce el falso positivo.
`tools/check-exclusions.sh` falla el pipeline si falta cualquiera de las dos, o si un archivo de aquí
desactiva una regla globalmente o toca el umbral/paranoia. Contrato: PLAN-HU-027 §7.5.

Vacío a propósito: las exclusiones aparecen durante la fase de detección, no antes.
