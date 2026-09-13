# Security policy engine

> Panorama de los tres componentes (PDP, PEP, OPA) y cómo encajan hoy:
> [`../docs/PLATAFORMA.md`](../docs/PLATAFORMA.md).

Motor de políticas OPA independiente del PDP actual. El PDP entrega hechos confiables,
OPA toma una decisión lógica y el PEP aplica esa decisión y sus obligaciones.

Hay dos entrypoints públicos:

```text
POST /v1/data/security/authorization/decision
{"input": <PolicyEvaluationInput>}
```

```text
POST /v1/data/security/administration/decision
{"input": {"subject": {...}, "tenant": {...}, "application": {...}}}
```

El primero decide acceso a un recurso externo (PEP); el segundo decide si un sujeto
administra el catálogo del PDP para una aplicación (HU-009) — deliberadamente separado del
contrato `pdp-opa/v1`, sin `resource`/`action`. Ver
[`docs/policies/administration.md`](docs/policies/administration.md).

El core no incluye una política de una aplicación ficticia. Por diseño, una entrada válida
sin una política de aplicación registrada devuelve `DENY / NO_APPLICABLE_POLICY`.

## Inicio local

Solo se requiere Docker Desktop. OPA, Python y el validador JSON Schema se ejecutan en el
contenedor temporal `opa-tools`; no es necesario instalarlos en macOS, Windows o Linux.

```sh
docker compose run --rm opa-tools ./scripts/bundle
docker compose up -d --build opa
```

El servicio queda en `http://localhost:8181`; consulte la guía de HTTP en
[`docs/integration/opa-http.md`](docs/integration/opa-http.md). Los contratos y la frontera
PDP–OPA están documentados en [`docs/contracts/pdp-opa.md`](docs/contracts/pdp-opa.md).
El contrato operativo y el algoritmo de un adapter PDP están en
[`docs/contracts/pdp-http-integration-contract.md`](docs/contracts/pdp-http-integration-contract.md).

Para levantarlo y entender el flujo de hechos dinámicos PDP → OPA, consulte la
[`guía operativa PDP → OPA`](docs/integration/guia-operativa-pdp-opa.md).
