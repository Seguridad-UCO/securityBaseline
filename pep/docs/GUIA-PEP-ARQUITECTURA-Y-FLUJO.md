# Guía técnica completa del PEP

## Propósito y límites

El **Policy Enforcement Point (PEP)** es un proxy inverso reactivo e independiente. Es la entrada pública a una
aplicación protegida: valida identidad, convierte la solicitud HTTP en una solicitud canónica de autorización,
consulta una decisión remota y reenvía al backend únicamente con un `ALLOW` válido.

El PEP no es el motor de políticas. No administra roles, perfiles, tenants, catálogo de recursos, reglas Rego,
OPA ni auditoría de decisiones: todo ello corresponde al **Policy Decision Point (PDP)**. Tampoco es una
librería que proteja controladores locales; el backend tiene que estar en una red privada accesible solamente
desde el PEP.

La regla general es *fail closed*: si la identidad es inválida, la ruta no existe, el PDP falla o su decisión no
es aplicable, el backend no recibe la solicitud.

## Componentes

| Componente | Responsabilidad                                                                                                     |
|------------|---------------------------------------------------------------------------------------------------------------------|
| Cliente    | Obtiene un Bearer JWT de su IdP y usa la URL pública del PEP.                                                       |
| PEP        | Autentica JWT, aplica límites, normaliza acceso, consulta PDP y hace proxy tras `ALLOW`.                            |
| PDP        | Autentica el PEP por mTLS, vuelve a validar evidencia, resuelve contexto confiable, consulta catálogo/OPA y audita. |
| Backend    | Ejecuta lógica de negocio solo cuando el PEP reenvía una solicitud autorizada.                                      |

```mermaid
sequenceDiagram
    participant C as Cliente
    participant P as PEP
    participant D as PDP
    participant B as Backend privado
    C->>P: GET /apps/academic/api/courses + Bearer JWT
    P->>P: límites, ruta, JWT y audiencia
    P->>D: POST /internal/v1/access-decisions (mTLS + Bearer)
    D->>D: identidad, tenant, catálogo, OPA y auditoría
    D-->>P: ALLOW / DENY / INDETERMINATE
    alt ALLOW válido y sin obligaciones
        P->>B: GET /api/courses + headers saneados
        B-->>P: respuesta
        P-->>C: respuesta del backend
    else otro resultado
        P-->>C: error local; no reenvía
    end
```

## Rutas protegidas

Cada ruta registra un prefijo público, `application-id`, entorno, origen interno y audiencias JWT permitidas.
Puede declararse estáticamente con `pep.ingress.routes[]` o mediante alta dinámica.

```properties
pep.ingress.routes[0].prefix=/apps/academic
pep.ingress.routes[0].application-id=academic
pep.ingress.routes[0].environment=prod
pep.ingress.routes[0].target=https://academic.internal
pep.ingress.routes[0].audiences[0]=academic-api
```

Para ese ejemplo, `/apps/academic/api/courses` identifica `academic`, mientras el backend recibe
`/api/courses`. El destino se configura, nunca se obtiene de query, headers o datos del cliente. Los prefijos no
se solapan; una ruta desconocida devuelve `404 ROUTE_NOT_FOUND`.

## Flujo detallado de una solicitud

### 1. Filtro de entrada, IDs y límites

`IngressLimitsFilter`, con prioridad máxima, genera `X-Request-Id` UUID. Conserva `X-Correlation-Id` solo si
cumple `[A-Za-z0-9_.:-]{1,128}`; si no, genera un UUID. Ambos vuelven al cliente y atraviesan el flujo.

Antes de autenticar aplica límite global de concurrencia, token bucket local por IP inmediata, límite por
`Content-Length`, límite de cuerpo en streaming y validación de protocolo. Solo admite `GET`, `POST`, `PUT`,
`PATCH`, `DELETE`, `HEAD` y `OPTIONS`; rechaza `Upgrade`, SSE y más de un `Authorization`.

Los estados relevantes son: capacidad agotada `503`, tasa excedida `429` con `Retry-After: 1`, cuerpo grande
`413`, solicitud ambigua `400` y protocolo no soportado `501`. Los endpoints de salud propios no pasan por el
control de rutas.

### 2. JWT, audiencia y ruta

El `SecurityWebFilterChain` es un Resource Server reactivo. Descarga el JWKS configurado y valida issuer,
firma, validadores JWT por defecto y presencia de `sub`. Para una ruta protegida requiere un
`JwtAuthenticationToken` autenticado y que al menos una audiencia del JWT coincida con la ruta.

El PEP no usa roles, tenant, perfiles ni headers de identidad enviados por el cliente para decidir. JWT sin
validez, issuer, audiencia o firma correctos produce `401 TOKEN_INVALID`; un fallo remoto de JWKS produce
`503 IDENTITY_UNAVAILABLE`. CORS se calcula solamente para rutas existentes y para los orígenes configurados.

### 3. Captura y contrato canónico

`HttpCaptureInteractor` resuelve el prefijo confiable y obtiene application ID, entorno, ruta relativa, verbo,
timestamp e IDs. Conserva el Bearer original aparte, como `IdentityEvidence`. Luego
`NormalizeAccessUseCaseImpl` crea la solicitud v1:

```json
{
  "version": "1",
  "requestId": "<uuid PEP>",
  "correlationId": "<id>",
  "application": {"id": "academic", "environment": "prod"},
  "resource": {"path": "/api/courses", "action": "GET"},
  "context": {"method": "GET", "channel": "HTTP"}
}
```

La query y el cuerpo se conservan para el backend, pero no son atributos de autorización en v1. Si una política
depende de ellos, hay que extender el contrato antes de usar esa operación.

### 4. Delegación al PDP

`WebClientDecisionAdapter` llama `POST /internal/v1/access-decisions` con JSON, Bearer original en
`Authorization`, y `X-Request-Id`/`X-Correlation-Id`. No manda roles, tenant, subject ni token dentro del JSON.

Para HTTPS, el cliente del PDP configura CA, certificado y clave cliente. Fuera de desarrollo los tres son
obligatorios para mTLS. No sigue redirecciones, limita respuesta y aplica timeout. HTTP requiere la propiedad
explícita `pep.pdp.allow-insecure-http=true`.

`DecisionApplicability` acepta solo HTTP 200 JSON con decisión conocida, IDs seguros y los mismos IDs de la
solicitud. Un `ALLOW` con `TOKEN_INVALID`, obligaciones no vacías, respuesta nula, JSON inválido, campos
obligatorios ausentes, IDs distintos o errores de transporte nunca se pueden aplicar.

| Resultado PDP/integración                         |     Respuesta PEP | ¿Reenvía? |
|---------------------------------------------------|------------------:|-----------|
| `ALLOW` correlacionado y sin obligaciones         | respuesta backend | Sí        |
| `DENY`                                            |               403 | No        |
| `DENY` con `TOKEN_INVALID` o HTTP 401 PDP         |               401 | No        |
| `INDETERMINATE`, 5xx, timeout o contrato inválido |               503 | No        |

### 5. Proxy tras el ALLOW

`ReactiveProxyAdapter` compone `origen + ruta relativa + query original`; no sigue redirecciones y transmite el
cuerpo de forma reactiva, aplicando otra vez el límite de bytes. Timeout del backend devuelve `504`, y fallo de
conexión `502`.

No reenvía por defecto `Authorization`, `Cookie` ni `Set-Cookie`; una ruta puede habilitarlos explícitamente
con `forward-bearer` y `forward-cookies`. Elimina headers hop-by-hop, `Forwarded`, `X-Forwarded-*` del cliente,
headers de identidad, roles y tenant. Luego define sus propios `X-Request-Id`, `X-Correlation-Id`,
`X-Decision-Id`, `X-Forwarded-For` y `X-Forwarded-Proto`.

## Salud, errores y operación

Los errores locales son `application/problem+json`, `no-store`, con código seguro e IDs de correlación; no
exponen token, cookies, query, payload ni referencias de política. `X-Decision-Id` solo aparece cuando el PDP
entregó una decisión identificada.

Liveness representa el proceso. Readiness incluye `GET /actuator/health` al PDP. Un PDP caído debe dejar
readiness abajo y devolver 503 para solicitudes protegidas, no reiniciar el PEP ni permitir acceso. Las métricas
son `pep.http.duration`, `pep.pdp.duration` y `pep.pdp.decisions`.

| Grupo   | Propiedades principales                                                             |
|---------|-------------------------------------------------------------------------------------|
| Ingress | `pep.ingress.issuer`, `jwks-uri`, rutas, límites, CORS.                             |
| PDP     | `pep.pdp.base-url`, CA/certificado/clave mTLS, timeouts, límite de respuesta.       |
| Proxy   | `pep.proxy.connect-timeout`, `timeout`, `max-body-bytes`.                           |
| Alta    | `pep.integration.enabled`, `registry-file`, `public-base-url`, credenciales BCrypt. |

En producción use HTTPS para listener, IdP/JWKS, PDP y backend. Las propiedades `allow-insecure-http` son solo
para desarrollo. La alta dinámica se persiste atómicamente en archivo local: esta versión es de una única
réplica PEP, salvo que se añada sincronización externa.

## Qué falta del PDP

El PDP debe implementar el contrato v1, autenticación mTLS del PEP, validación de evidencia JWT, resolución de
contexto confiable, catálogo/OPA, auditoría y health. La guía exacta está en
[PDP-INTEGRATION-GUIDE.md](../../contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md). Mientras eso no exista, el
PEP responderá intencionalmente `503` y no expondrá el backend.
