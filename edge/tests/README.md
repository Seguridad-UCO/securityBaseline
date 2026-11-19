# Suite hurl del edge

Corre **dentro** del Compose como servicio `hurl` (misma red: resuelve `edge` y `keycloak`).
Contrato de carpetas y casos: PLAN-HU-027 §7.6 y §9.

| Carpeta | Regla |
|---|---|
| `attacks/` | Deben recibir `403` `application/problem+json` con `code = REQUEST_REJECTED` en `blocking`; nunca `rule` en el cuerpo |
| `flows/` | Deben pasar en `detection` **y** en `blocking`, y dejar el audit log de Coraza vacío |
| `flows/internal-channel-blocked.hurl` | Regla de enrutamiento (no detección), **default-deny**: todo `/internal/*` responde `404` sin cuerpo hacia el PDP en `detection` y `blocking` (incluidas variantes con mayúsculas, `//`, `%69nternal`, `;x=1`, `.json`). Solo con upstream PEP (`EDGE_INTERNAL_ALLOW_PEP=true`) se permiten `PUT /internal/v1/integrations/*/*` y `POST /internal/v1/embedded-access-decisions` |
| `flows/bff-session-token-internal.hurl` | HU-030: login por el edge, pero el canal `/internal/v1/bff-session-token` se llama **directo al PDP** (`pdp_url`, como el PEP) con la identidad técnica `security-pep-internal` + cookie BFF; token distinto y vigente entre llamadas (requiere `PDP_SECURITY_SESSION_TOKEN_RENEWAL_SKEW=3600s`, solo en `compose.pdp.yml`) y 401 sin identidad/cookie/tras logout |
| `allowlist/` | `internal-allowlist-pep.hurl`: SOLO para la topología PEP (`EDGE_INTERNAL_ALLOW_PEP=true`); fuera de `flows/` para que `flows/*.hurl` pase en la topología PDP, donde todo `/internal/*` es `404` |
| `ratelimit/` | `429` sobre el umbral desde una IP; sin `429` repartido en dos (`hurl-second-ip`) |
| `exclusions/` | Una por exclusión, mismo id que `rules/exclusions/` |
| `realm/` | Realm de CI para Keycloak (`security-baseline-ci-realm.json`), solo para el Compose; el secreto del client es de prueba |
| `fixtures/` | `broken-rule.conf` para la prueba de fail-closed |

Ejecutar siempre con `--jobs 1 --path-as-is` (el traversal codificado se normaliza si no). Los ataques
llevan `X-Correlation-Id: edge-attack-<run_id>` para que el pipeline compruebe que no llegan al log del PDP.
