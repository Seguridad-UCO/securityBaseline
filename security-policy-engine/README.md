# Security policy engine

Motor de políticas OPA independiente del PDP actual. El PDP entrega hechos confiables,
OPA toma una decisión lógica y el PEP aplica esa decisión y sus obligaciones.

El único entrypoint público es `security/authorization/decision`:

```text
POST /v1/data/security/authorization/decision
{"input": <PolicyEvaluationInput>}
```

El core no incluye una política de una aplicación ficticia. Por diseño, una entrada válida
sin una política de aplicación registrada devuelve `DENY / NO_APPLICABLE_POLICY`.

## Inicio local

```sh
make validate
make test
docker compose up --build
```

El servicio queda en `http://localhost:8181`; consulte la guía de HTTP en
[`docs/integration/opa-http.md`](docs/integration/opa-http.md). Los contratos y la frontera
PDP–OPA están documentados en [`docs/contracts/pdp-opa.md`](docs/contracts/pdp-opa.md).
El contrato operativo y el algoritmo de un adapter PDP están en
[`docs/contracts/pdp-http-integration-contract.md`](docs/contracts/pdp-http-integration-contract.md).
