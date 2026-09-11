# Integración HTTP de OPA

Arranque el motor local:

```sh
docker compose up --build
```

El puerto se configura con `OPA_PORT` y es `8181` localmente. Use timeouts breves en el
cliente y falle cerrado cuando OPA no responda o expire; no convierta una indisponibilidad
técnica en un allow.

```sh
curl --fail --silent http://localhost:8181/health
jq -n --slurpfile input ../contracts/pdp-opa/v1/examples/valid/minimal-same-tenant.json '{input: $input[0]}' | \
  curl --fail --silent -X POST \
  http://localhost:8181/v1/data/security/authorization/decision \
  -H 'content-type: application/json' \
  --data-binary @-
```

La segunda llamada entrega el envelope nativo y, sin una política de aplicación, niega:

```json
{"result":{"effect":"DENY","reasonCode":"NO_APPLICABLE_POLICY","policyReferences":[{"id":"core.composition","version":"1.0"}],"obligations":[]},"decision_id":"..."}
```

`decision_id` y una revisión de bundle son metadatos operativos, no hechos de negocio.
No configure decision logs con cuerpos de entrada completos: identidades, atributos y
correlation IDs deben redactarse antes de salir del límite de confianza. Un bundle inválido
deja la instancia no lista; el despliegue debe validar, probar, firmar, publicar y verificar
la revisión antes de dirigirle tráfico.
