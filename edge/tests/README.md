# Suite hurl del edge

Corre **dentro** del Compose como servicio `hurl` (misma red: resuelve `edge` y `keycloak`).
Contrato de carpetas y casos: PLAN-HU-027 §7.6 y §9.

| Carpeta | Regla |
|---|---|
| `attacks/` | Deben recibir `403` `application/problem+json` con `code = REQUEST_REJECTED` en `blocking`; nunca `rule` en el cuerpo |
| `flows/` | Deben pasar en `detection` **y** en `blocking`, y dejar el audit log de Coraza vacío |
| `ratelimit/` | `429` sobre el umbral desde una IP; sin `429` repartido en dos (`hurl-second-ip`) |
| `exclusions/` | Una por exclusión, mismo id que `rules/exclusions/` |
| `realm/` | Realm de CI para Keycloak (`security-baseline-ci-realm.json`), solo para el Compose; el secreto del client es de prueba |
| `fixtures/` | `broken-rule.conf` para la prueba de fail-closed |
