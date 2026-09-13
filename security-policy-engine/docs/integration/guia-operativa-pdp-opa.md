# Guía operativa: PDP → OPA

Esta guía permite a otra persona levantar OPA localmente, entender qué recibe y probar una decisión sin PEP. El PDP es la fuente de verdad de usuarios, roles, profiles, recursos y asignaciones. OPA no persiste ni consulta esos datos.

## Modelo mental

Un rol es un cargo (`EDITOR`), un recurso es una puerta (`GET /orders`) y un entitlement es el pase concreto para esa puerta (`resource:<ResourceId>`).

En cada autorización el PDP lee el estado vigente de SurrealDB, calcula los pases del usuario y envía hechos a OPA. OPA compara el pase con la puerta solicitada y devuelve una decisión. Por ello crear, revocar o modificar roles, profiles, recursos o asignaciones no requiere recrear el bundle ni reiniciar OPA.

```text
PDP/SurealDB ── hechos actuales por HTTP ──> OPA ── ALLOW/DENY ──> PDP
```

El script Python `scripts/compile-policy-definitions` no está en ese flujo. Se usa únicamente durante construcción para validar archivos de políticas y producir `build/data/policies.json` para el bundle.

## Requisitos

- Docker Desktop en ejecución.

No se instala OPA, Python ni `jsonschema` en la máquina. El servicio temporal `opa-tools` los
incluye y se usa para validar, probar, compilar las definiciones y construir `bundle.tar.gz`.

## Ejecución local en Mac — solo Docker Desktop

Abra Terminal en la raíz clonada del repositorio (`securityBaseline/`). Confirme que está allí con
`ls pdp security-policy-engine`. Luego ejecute:

```sh
# Dependencias del PDP: reutiliza los contenedores existentes o crea solo los que falten.
if docker container inspect surrealdb >/dev/null 2>&1; then
  docker start surrealdb
else
  docker compose -f pdp/docker-compose.yml up -d surrealdb
fi

if docker container inspect keycloak >/dev/null 2>&1; then
  docker start keycloak
else
  docker compose -f pdp/docker-compose.yml up -d keycloak
fi

docker ps --filter "name=^/surrealdb$" --filter "name=^/keycloak$"

# Validar, probar y construir el bundle usando solo el contenedor temporal opa-tools
cd security-policy-engine
docker compose run --rm opa-tools ./scripts/validate
docker compose run --rm opa-tools ./scripts/test
docker compose run --rm opa-tools ./scripts/bundle

# Iniciar OPA con el bundle recién generado
docker compose up -d --build opa
docker compose ps
curl -fsS http://localhost:8181/health
```

El contenedor de runtime se verá como `security-policy-engine-opa-1` en Docker Desktop. Es normal
que muestre CPU `0%`: OPA está esperando solicitudes. Lo importante es que `docker compose ps`
muestre `opa` como `healthy` y que el `curl` responda correctamente.

Los servicios del PDP usan los nombres fijos `surrealdb` y `keycloak`. Por eso la guía primero
comprueba si ya existen: si existen los inicia; si no, Docker Compose los crea. Así se evita el
conflicto `container name "/surrealdb" is already in use` y no se eliminan datos existentes.

Para iniciar el PDP Java, abra una segunda Terminal:

```sh
# Desde la raíz de securityBaseline
./mvnw -f pdp/pom.xml spring-boot:run
```

El PDP usa OPA en `http://localhost:8181` y SurrealDB en `localhost:8000`.

Para apagar los contenedores:

```sh
# Desde la raíz de securityBaseline
docker compose -f security-policy-engine/docker-compose.yml down
docker compose -f pdp/docker-compose.yml down
```

Detenga el PDP Java con `Ctrl+C` en su Terminal.

## Qué envía PDP

El endpoint es:

```text
POST /v1/data/security/authorization/decision
Content-Type: application/json
{ "input": <PolicyEvaluationInput> }
```

PDP conserva `resource.id` como el path HTTP por compatibilidad. Además envía el UUID real del recurso y su llave de entitlement en atributos.

```json
{
  "input": {
    "schemaVersion": "1.0",
    "request": { "id": "request-123", "correlationId": "correlation-123" },
    "subject": {
      "id": "user-123",
      "type": "USER",
      "tenantId": "tenant-a",
      "roles": ["EDITOR"],
      "profiles": ["OPERATIONS"],
      "entitlements": ["resource:550e8400-e29b-41d4-a716-446655440000"],
      "groups": [],
      "attributes": {}
    },
    "tenant": { "id": "tenant-a", "attributes": {} },
    "application": { "id": "app-a", "attributes": {} },
    "resource": {
      "type": "http",
      "id": "/orders",
      "attributes": {
        "resourceId": "550e8400-e29b-41d4-a716-446655440000",
        "entitlementKey": "resource:550e8400-e29b-41d4-a716-446655440000",
        "path": "/orders",
        "method": "GET"
      }
    },
    "action": "GET",
    "relationships": [],
    "context": {},
    "security": {}
  }
}
```

`roles` y `profiles` son evidencia para políticas adicionales. `entitlements` son los recursos efectivos: se derivan en runtime de asignaciones directas y de los roles actuales de profiles activos. No se persisten.

La policy base `core.resource-grant` permite solamente cuando:

```text
resource.attributes.entitlementKey IN subject.entitlements
```

También aplica el aislamiento de tenant. Una policy adicional restrictiva debe ser `DENY`, ya que los DENY prevalecen sobre el ALLOW base.

## Prueba HTTP

Con OPA arriba:

```sh
curl -sS http://localhost:8181/v1/data/security/authorization/decision \
  -H 'Content-Type: application/json' \
  --data @- <<'JSON'
{"input":{"schemaVersion":"1.0","request":{"id":"smoke-allow"},"subject":{"id":"user-1","type":"USER","tenantId":"tenant-a","roles":["EDITOR"],"profiles":["OPERATIONS"],"entitlements":["resource:550e8400-e29b-41d4-a716-446655440000"]},"tenant":{"id":"tenant-a"},"application":{"id":"app-a"},"resource":{"type":"http","id":"/orders","attributes":{"resourceId":"550e8400-e29b-41d4-a716-446655440000","entitlementKey":"resource:550e8400-e29b-41d4-a716-446655440000","path":"/orders","method":"GET"}},"action":"GET"}}
JSON
```

Debe responder `result.effect = ALLOW` y `result.reasonCode = POLICY_ALLOWED`. Cambie `entitlements` a `[]`: debe responder `DENY / NO_APPLICABLE_POLICY`. Cambie `tenant.id` por `tenant-b` manteniendo el entitlement: debe responder `DENY / TENANT_ISOLATION_FAILED`.

## Datos nuevos en PDP

Al crear un recurso y concederlo a un rol, y luego asignar ese rol o un profile que lo contiene a un usuario, la siguiente solicitud debe incluir su `resource:<UUID>` en `subject.entitlements`. El mismo bundle genérico permitirá el acceso. No se crea una policy por aplicación, usuario, rol, profile ni recurso.

## Cambios que sí requieren bundle

Solo se reconstruye al modificar lógica de políticas, por ejemplo MFA obligatorio, un profile requerido, horario, riesgo, ownership o un DENY explícito. Flujo:

```sh
docker compose run --rm opa-tools ./scripts/bundle
docker compose up -d --build opa
```

Las fuentes viven en `policy-definitions/{core,global,tenants,applications}/`; no edite manualmente `build/data/policies.json` ni `bundle.tar.gz`.

## Diagnóstico rápido

| Síntoma | Causa / solución |
|---|---|
| `COPY bundle.tar.gz ... not found` | Ejecutar y corregir `docker compose run --rm opa-tools ./scripts/bundle` antes de iniciar OPA. |
| `INVALID_INPUT` | Revisar los campos mínimos: schemaVersion, request.id, subject, tenant, application, resource.type y action. |
| `NO_APPLICABLE_POLICY` | Falta el entitlement del recurso solicitado o ninguna policy coincide. |
| `TENANT_ISOLATION_FAILED` | `subject.tenantId` y `tenant.id` no coinciden y no hay evidencia explícita cross-tenant. |
