# Guía de verificación de la línea base

[← Evidencia](README.md)

## Prerrequisito

El POM exige Java 25.

```bash
./mvnw verify
```

`verify` ejecuta compilación, las pruebas y el reporte de cobertura en `target/site/jacoco/`.

## Qué demuestra cada prueba

| Prueba | Criterios | Evidencia |
|---|---|---|
| `ValueObjectTests` | 3, 14, 15, 21 | Invariantes de `TenantId`, `ApplicationName` e identificadores |
| `PageWindowTests` | 18, 19 | Límites de ventana, equivalencia página/rango, inmutabilidad de `ResultPage` |
| `ProtectedResourceDomainTests` | 3, 15, 17, 21 | Formatos kebab-case, partes obligatorias, specification |
| `TenantMustBeActiveRuleImplTests` | 3, 9 | Tenant inexistente frente a suspendido |
| `ApplicationRegistrationRuleTests` | 3, 9, 12 | Regla sin repositorio y regla con repositorio, aisladas |
| `ProtectedResourceRuleTests` | 3, 9, 12 | Consistencia de tenant y unicidad de concesión |
| `RegisterProtectedApplicationUseCaseImplTests` | 2, 7, 10, 11 | Orquestación, rollback y compensación entre módulos |
| `SearchProtectedApplicationsUseCaseImplTests` | 16, 17, 18, 19 | Consulta dinámica, total, páginas estables |
| `RegisterProtectedApplicationRequestMapperTests` | 6, 13, 14 | Raw → validado: válido, ausente, mal formado, fuera de rango |
| `SearchProtectedApplicationsRequestMapperTests` | 6, 17, 18, 19 | Combinaciones de ventana y sus rechazos |
| `ProtectedApplicationControllerMappingTests` | 11, 13 | El interactor mapea y no decide; el tenant viene del principal |
| `SecurityWebFilterChainTests` | 9 (ADR-0003) | 401 sin token/firma inválida/expirado/malformado; rutas públicas |
| `ProtectedApplicationHttpTests` | 5, 6, 9, 22 | Flujo completo autenticado sobre Netty; aislamiento entre tenants |
| `ProtectedResourceAuditListenerTests` | 4, 8 (ADR-0002) | Publicación/consumo del evento de registro |
| `ModulithStructureTests` | 1, 11, 12 | Dependencias de módulo permitidas |

## Demostración manual

```bash
./mvnw spring-boot:run
```

### Obtener un token de prueba

Desde ADR-0003, toda ruta salvo `/actuator/health` y `/actuator/info` exige
`Authorization: Bearer <jwt>`. No hay todavía un emisor HTTP (`/auth/token`): el emisor propio de
esta etapa solo valida, y las pruebas automatizadas firman sus propios tokens
(`TestJwtSupport`, en `src/test`). Para una demostración manual sin depender del código de pruebas,
un token HS256 se puede construir a mano con `openssl` y el mismo secreto de
`application.properties` (`pdp.security.jwt.secret`):

```bash
SECRET='dev-only-signing-key-not-for-production-use-please-change-1234'
ISSUER='https://security-baseline.pdp.local'

b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

mint_token() {
  local tenant="$1"
  local now=$(date +%s)
  local exp=$((now + 300))
  local header='{"alg":"HS256","typ":"JWT"}'
  local payload="{\"sub\":\"demo-user\",\"iss\":\"${ISSUER}\",\"tenant\":\"${tenant}\",\"iat\":${now},\"exp\":${exp}}"
  local signing_input="$(printf '%s' "$header" | b64).$(printf '%s' "$payload" | b64)"
  local signature=$(printf '%s' "$signing_input" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64)
  printf '%s.%s' "$signing_input" "$signature"
}

TOKEN=$(mint_token universidad-uco)
```

`SECRET` e `ISSUER` deben coincidir exactamente con `application.properties`; en `qa`/`prod` el
secreto real vive en Key Vault (`pdp-jwt-signing-key`, ver [`infra/README.md`](../../infra/README.md))
y este script deja de aplicar — ahí el token lo emite quien tenga la clave real, no un script local.

### Registro y consulta

Registro correcto:

```bash
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: demo-1' \
  -d '{"applicationName":"gestion-academica","resourceCode":"estudiantes","action":"consultar"}'
```

Devuelve `201`, código `APPLICATION_REGISTERED` y los identificadores de correlación. Repetir el
mismo POST devuelve `409 APPLICATION_ALREADY_EXISTS`.

Consulta con filtro y rango, en el mismo tenant del token:

```bash
curl "localhost:8080/api/v1/protected-applications?nameContains=academica&offset=0&limit=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Errores que conviene demostrar

```bash
# Sin token -> 401 UNAUTHORIZED
curl -i localhost:8080/api/v1/protected-applications
```

```bash
# Campo ausente -> 400 MISSING_REQUEST_FIELD con "field":"applicationName"
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"resourceCode":"estudiantes","action":"consultar"}'
```

```bash
# Formato inválido -> 400 MALFORMED_REQUEST_FIELD con "field":"resourceCode"
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"applicationName":"otra-app","resourceCode":"Estudiantes","action":"consultar"}'
```

```bash
# Nombre reservado -> 400 RESERVED_APPLICATION_NAME
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"applicationName":"admin","resourceCode":"estudiantes","action":"consultar"}'
```

```bash
# Tenant suspendido -> 400 TENANT_NOT_ACTIVE (el tenant sale del token, no del cuerpo)
SUSPENDED_TOKEN=$(mint_token colegio-suspendido)
curl -i -X POST localhost:8080/api/v1/protected-applications \
  -H "Authorization: Bearer $SUSPENDED_TOKEN" -H 'Content-Type: application/json' \
  -d '{"applicationName":"app-x","resourceCode":"estudiantes","action":"consultar"}'
# tenant desconocido -> TENANT_NOT_FOUND: mint_token tenant-inexistente
```

```bash
# Ventana ambigua -> 400 CONFLICTING_REQUEST_PARAMETERS
curl -i "localhost:8080/api/v1/protected-applications?offset=10" -H "Authorization: Bearer $TOKEN"
```

El punto de estas últimas: el mensaje que ve el cliente lo produce esta aplicación, con su código
estable y sus identificadores de correlación. Ninguno lo genera el framework antes de que el código
lo vea, que es exactamente lo que la estrategia de DTO en dos niveles vino a resolver — y el 401 lo
produce la frontera de seguridad antes de que la petición llegue siquiera al controlador.
