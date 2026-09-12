# Handoff — integración PDP ↔ PEP ↔ OPA

> **Este documento existe para arrancar en frío.** Si eres una sesión nueva sin contexto previo:
> léelo entero antes de tocar nada. Contiene el estado real del repositorio, las decisiones ya
> tomadas (con su porqué y las alternativas descartadas), las trampas verificadas que te van a
> costar un ciclo si las pisas, y el orden exacto en que arrancar con los agentes.
>
> **Fecha:** 2026-09-10 · **Autor:** sesión de Claude Code con Sebastián · **Rama:** `feature/hu-003-endpoint-interno-pep`

---

## 1. Cómo usar este documento

1. Lee [`AGENTS.md`](../../../../AGENTS.md) y [`CLAUDE.md`](../../../../CLAUDE.md) — convenciones del proyecto.
2. Lee este documento entero.
3. Lee [`ROADMAP-PDP.md`](ROADMAP-PDP.md) — la tabla de historias renumerada.
4. Arranca con `@1-planificador` sobre **HU-003** (sección 8 de abajo tiene el comando y el encargo).

**No re-decidas** lo que está en la sección 4. Ya se decidió, con su razón. Si crees que una decisión
está mal, dilo y espera — no la cambies a mitad de implementación.

---

## 2. Estado real del repositorio

| Hecho | Estado |
|---|---|
| **HU-002** — `POST /api/v1/authorize`, canal BFF, denegación por defecto | ✅ Implementada, validada y aprobada (277 pruebas verdes). **Aún NO mergeada a main** |
| Rama de HU-002 | `feature/hu-002-endpoint-decision` — 2 commits por delante de `origin/main`, pendiente de PR |
| **Rama del PEP** | `feature/pep` — módulo `pep/` completo, autónomo, con su propio POM. Tampoco mergeada |
| Esta rama (`feature/hu-003-endpoint-interno-pep`) | Sale de `feature/hu-002-endpoint-decision`, porque HU-003 depende del código de HU-002 |
| **HU-003** — endpoint interno para el PEP | ⛔ No empezada. Es lo que sigue |

### Lo que hay que mergear antes o en paralelo

`feature/hu-002-endpoint-decision` y `feature/pep` deberían entrar a `main` por PR. Mientras no
entren, cada rama vive con su propia copia del contrato y la deriva es cuestión de tiempo.

> **Sobre el pipeline:** esta rama trae el arreglo de `azure-pipelines.yml` que quita `feature/*` del
> trigger de push (el plan de SonarQube Cloud de la organización solo analiza `main` y pull requests;
> cada push a una feature branch terminaba en rojo por diseño). **Azure lee el trigger del YAML de la
> rama que recibe el push**, así que el arreglo solo protege a las ramas que ya lo tengan: hasta que
> llegue a `main` y las demás ramas lo integren, las ramas viejas seguirán disparando pipelines rojos.

---

## 3. El hallazgo que motiva HU-003

HU-002 publicó `POST /api/v1/authorize`: canal **BFF**, autenticado por sesión/cookie como el resto
del panel, con `AuthorizeRawRequest`/`AccessDecisionWebResponse` envueltos en `ApiResponse`.

El compañero del PEP, trabajando en paralelo y sin acceso al código del PDP (por diseño — ver
`pep/README.md`), construyó su cliente contra **`POST /internal/v1/access-decisions`**: canal
**interno**, autenticado con **mTLS del servicio PEP + el Bearer del usuario como evidencia**, con
contrato propio y versionado en `contracts/pep-pdp/v1/`.

Ninguno de los dos está mal — son canales distintos con distinto nivel de confianza. Lo que falta es
el segundo. Textual de `contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md`:
*"No hay una implementación de esa operación en el PDP actual, por lo que no se modificó el PDP."*

### El contrato, en una tabla

Fuente de verdad: `contracts/pep-pdp/v1/` en la rama `feature/pep` — `openapi.yaml`,
`request.schema.json`, `decision.schema.json` y tres ejemplos. **Léelos antes de planificar.**

| Petición (`SolicitudAcceso v1`) | Respuesta (`DecisionAcceso v1`) |
|---|---|
| `version` (const `"1"`) | `decision` (`ALLOW`/`DENY`/`INDETERMINATE`) |
| `requestId` (uuid), `correlationId`, `timestamp` | `decisionId`, `reasonCode` |
| `application.id`, `application.environment` | `policyReferences[]` (`id` + `version`) |
| `resource.path`, `resource.action` | `requestId`, `correlationId` (deben coincidir con la petición) |
| `context.method`, `context.channel` (const `"HTTP"`) | `obligations` — ausente/null/vacío en v1 |
| Headers `X-Request-Id`, `X-Correlation-Id` | Mismos headers de vuelta |

Códigos que el PEP espera: **200** evaluación completa · **400** contrato inválido · **401** evidencia
de usuario inválida · **403** PEP no admitido · **503** evaluación no disponible. Cualquier cosa que
no sea un `ALLOW` íntegro y sin obligaciones hace que el PEP falle cerrado y no reenvíe.

**Lo que el PEP NO manda, y por qué:** ni `tenant`, ni `subject`, ni roles. El PDP los resuelve desde
fuentes confiables. Textual de la guía: *"no confiar en roles, tenant ni atributos enviados por el PEP"*.

---

## 4. Decisiones tomadas

Cada una con su porqué y lo que se descartó. Están tomadas para que `@1-planificador` no las
renegocie: son entrada del plan, no preguntas del gate 1.

### D1 — Un segundo adaptador primario, no un segundo caso de uso de decisión

`/internal/v1/access-decisions` reutiliza el `AuthorizeUseCase` de HU-002. Gana su propio
controller, interactor, mapper y DTOs (crudo y de respuesta) dentro del slice `authorization`.
`/api/v1/authorize` **no se toca ni se retira**: sigue siendo el canal del panel.

*Por qué:* es el patrón que ya sostiene el proyecto — un caso de uso, varios adaptadores primarios.
Duplicar el caso de uso duplicaría la regla de decisión, que es justo lo que no puede divergir.

### D2 — mTLS: un solo listener con `client-auth=want` + filtro que exige certificado en `/internal/**`

**Descartado:** un segundo puerto con `client-auth=need`. Netty/WebFlux configura el `client-auth`
por conector, no por ruta, y Spring Boot arranca **un** servidor embebido: un segundo puerto exige
levantar un `HttpServer` a mano o un segundo contexto de aplicación, lo que rompe `@SpringBootTest`
y `WebTestClient` y añade mucha maquinaria para el beneficio que da.

**Decidido:** TLS del servidor con `client-auth=want` (opcional en el handshake) y un filtro propio
sobre `/internal/**` que **falla cerrado** si no hay certificado de cliente, o si el sujeto del
certificado no está en la lista de admitidos.

*Por qué es seguro:* con `want`, si el cliente presenta certificado, Netty ya valida la cadena contra
el trust store durante el handshake — un certificado no confiable ni siquiera llega. Si no presenta
ninguno, `SslInfo.getPeerCertificates()` viene vacío y el filtro rechaza con 403. Es decir: la
ausencia de certificado nunca es un permiso, es un rechazo explícito y testeable.

El certificado se lee de `ServerHttpRequest.getSslInfo()`. **No** de un header.

### D3 — El tenant sale del catálogo de aplicaciones, no del token

Esta es la decisión con más consecuencias. El PEP manda `application.id`; el PDP resuelve a partir
de ahí **qué tenant es dueño de esa aplicación**, y el `subject` sale del claim `sub` del JWT de
evidencia. Ni el tenant ni el sujeto se leen del cuerpo.

*Por qué:* el slice `applications` ya guarda el tenant dueño de cada aplicación. Es la única fuente
confiable disponible, y la guía del PEP lo exige explícitamente.

*Consecuencia:* hace falta un puerto/consulta que devuelva el tenant dueño de un `ApplicationId`.
Hoy no existe — existe `ApplicationOwnershipQuery(tenantId, applicationId)`, que **verifica** una
pertenencia que ya conoces, no la **resuelve**. Es trabajo nuevo de HU-003.

### D4 — Un caso de uso puente para el canal interno, sin tocar `AccessRequest`

`AuthorizeUseCase` recibe un `AccessRequest` que ya trae `tenantId`. Como en el canal interno el
tenant hay que resolverlo antes (D3), se añade un caso de uso propio del canal interno cuyo input
**no** trae tenant: resuelve el tenant dueño, arma el `AccessRequest` y delega en `AuthorizeUseCase`.

**Descartado:** (a) que el interactor haga la resolución — pondría lógica de negocio en
infraestructura; (b) hacer `AccessRequest.tenantId` opcional — rompería el contrato de HU-002 y sus
pruebas, que están bloqueadas.

El nombre exacto lo decide `@1-planificador`; lo que está decidido es la forma.

### D5 — La evidencia JWT se valida contra un conjunto de confianza configurado, no contra el emisor propio del PDP

El JWT que llega por el canal interno lo emitió el IdP de la **aplicación integrada**, no el PDP. En
HU-003 se valida firma, `exp`/`nbf`, `iss` y `aud` contra un conjunto configurado
(`pdp.security.internal.evidence.*`). Si falla → **401**, sin llegar al caso de uso.

**Aplazado a una historia propia de credenciales por aplicación** (sin número aún — ver
`ROADMAP-PDP.md`): que cada aplicación registrada declare su propio issuer/JWKS/audiencia y que la
validación sea *por aplicación*. Es lo correcto a futuro y lo pide la guía ("audiencia para
`application.id`"), pero exige extender el catálogo de aplicaciones. Cuando se escribió este handoff
eso cabía en «HU-004: roles y catálogo»; al partir roles/asignaciones el 2026-09-11 quedó fuera de
ambas, emparentado con HU-009 (administración por aplicación).

*Por qué se acepta el escalón:* la Etapa 1 del plan del compañero es explícitamente el "mínimo
seguro", y en el MVP del semillero los tokens vendrán del mismo Keycloak. Queda anotado como deuda
consciente, no como olvido.

### D6 — La respuesta del canal interno NO se envuelve en `ApiResponse`

El esquema del PEP es un objeto plano en la raíz. El canal interno devuelve el DTO desnudo.

*Por qué se documenta:* es una **desviación deliberada** de la convención del proyecto (todo el resto
de la API responde envuelto). `@4-validador` debe verla como decisión registrada aquí, no como
defecto. El `ApiErrorHandler` global sí sigue aplicando a los errores: el PEP no parsea el cuerpo de
un error, cualquier no-200 lo hace fallar cerrado, así que su forma actual sirve.

### D7 — Barreras de contrato del canal interno

Además de las tres barreras habituales de DTO seguro (criterio 14), el mapper del canal interno
rechaza con **400**:

- `version` distinto de `"1"`.
- `X-Request-Id` del header distinto de `requestId` del cuerpo (ídem correlación) — lo exige la
  Etapa 1 del plan del compañero: *"correlacionar request/correlation IDs de headers y cuerpo y
  rechazar inconsistencias"*.

`CorrelationWebFilter` ya lee `X-Request-Id`/`X-Correlation-Id` y los devuelve en la respuesta, así
que esa mitad del contrato ya está resuelta — no la reimplementes.

### D8 — `TOKEN_INVALID` no se emite en el cuerpo

Un token inválido se rechaza en la cadena de seguridad con **HTTP 401**, antes del caso de uso. El
contrato admite las dos formas (401, o 200 con `DENY`+`TOKEN_INVALID`); se elige la primera por ser
la que ya tiene el proyecto y la que no mezcla fallo de autenticación con decisión de política.

### D9 — HU-006 (OPA): el adaptador reemplaza a `DenyByDefaultPolicyDecisionAdapter`, no convive con él

`OpaPolicyDecisionAdapter` implementa `PolicyDecisionPort` con `WebClient` contra la API de OPA
(`pdp.opa.base-url`, `pdp.opa.decision-path`, `pdp.opa.timeout`). La denegación por defecto pasa a
vivir en la propia política Rego (`default allow = false`), que es su lugar natural.

*Por qué se borra la clase:* mantenerla detrás de un perfil sería un interruptor de compatibilidad
sin dueño. OPA caído **no** es `DENY`, es `INDETERMINATE` — y eso ya lo hace `AuthorizeUseCaseImpl`
en su `onErrorResume` final, así que no hace falta un respaldo.

### D10 — Nombrado de propiedades, simétrico al del PEP

El PEP ya usa `pep.pdp.*`. El PDP adopta:

| Prefijo | Para qué |
|---|---|
| `pdp.security.internal.mtls.*` | trust store, sujetos de certificado admitidos |
| `pdp.security.internal.evidence.*` | issuer/JWKS/audiencia del JWT de evidencia (D5) |
| `pdp.opa.*` | cliente OPA de HU-006 |

Mismo estilo que `JwtSecurityProperties`/`CorsProperties` ya existentes: un `record`
`@ConfigurationProperties`, sin Bean Validation.

---

## 5. Trampas verificadas — léelas o las pisas

### T1 — `PdpPrincipal` exige un claim `tenant` que el PEP no manda

`PdpPrincipal.from(Jwt)` hace `new TenantId(jwt.getClaimAsString("tenant"))`, y
`SecurityConfiguration` además **rechaza** cualquier token sin ese claim
(`new JwtClaimValidator<String>("tenant", ...)`). El README del PEP dice lo contrario, textual:
*"No requiere un tenant en el token"*.

**Por lo tanto:** el canal interno **no puede reutilizar** `PdpPrincipal` ni el bean
`ReactiveJwtDecoder` actual. Necesita su propio decoder (D5) y leer el `sub` directamente del `Jwt`.
Si intentas reutilizarlos, todo compila y todo revienta en la primera prueba de integración.

### T2 — El proyecto hoy no sirve TLS en absoluto

No hay una sola propiedad `server.ssl.*` en `src/main/resources/`. HU-003 tiene que introducir la
configuración TLS del servidor. Para las pruebas, **el PEP ya resolvió esto**: mira
`pep/src/test/java/co/edu/uco/seguridad/pep/infrastructure/config/PdpTlsIntegrationTests.java` y
`pep/src/test/java/co/edu/uco/seguridad/pep/fixture/FixtureServers.java` en la rama `feature/pep` —
generan certificados efímeros sin Docker. Copia ese enfoque en vez de inventar otro.

### T3 — En Azure App Service el TLS lo termina la plataforma

`SslInfo` no existe si el TLS muere en el borde: App Service reenvía el certificado del cliente en el
header `X-ARR-ClientCert`. Eso **contradice** la regla del ADR del PEP de no aceptar identidades por
header, salvo que se confíe explícitamente en la plataforma.

**Alcance:** HU-003 implementa el camino `SslInfo` (local, compose, contenedor con TLS directo). El
camino App Service es un problema de despliegue (Etapa 5 del plan del compañero) y queda **fuera** de
HU-003, anotado aquí para que nadie lo descubra en producción.

### T4 — La trampa de Modulith al cruzar un módulo por primera vez

Ya mordió dos veces en HU-002. El canal interno va a consumir `applications` (D3) y probablemente
`resources`. Está documentada en `.claude/agents/1-planificador.md`, sección Fase 5 — léela.

### T5 — `verificar.ps1 -Rapido` no corre `jacoco-check`

Un `-Rapido` verde no garantiza que el Quality Gate pase. Antes de cerrar, corre `verificar.ps1` sin
flags. Ya está documentado en `.claude/agents/4-validador.md`.

---

## 6. Alcance de HU-003

### Dentro

- `POST /internal/v1/access-decisions` conforme a `contracts/pep-pdp/v1/openapi.yaml`.
- Cadena de seguridad propia para `/internal/**`: mTLS (D2) + evidencia JWT (D5), separada de la
  cadena BFF existente. `SecurityConfiguration` documenta hoy que es "el único lugar que decide qué
  ruta necesita un token" — ese comentario hay que actualizarlo a "el único lugar **por canal**".
- Resolución de tenant desde el catálogo (D3) y caso de uso puente (D4).
- Barreras de contrato (D7) y respuesta plana (D6).
- `GET /actuator/health` accesible sin autenticación para la readiness del PEP — **ya está
  permitido** en `SecurityConfiguration`; solo hay que verificar que la cadena nueva no lo tape.
- Pruebas: contrato contra los JSON Schema, mTLS admitido y no admitido, JWT inválido, IDs
  inconsistentes, aplicación inexistente, y el camino DENY completo.

### Fuera (explícitamente)

- Validación de issuer/audiencia **por aplicación** → historia propia de credenciales por aplicación, sin número aún (D5).
- OPA → HU-006.
- Auditoría durable → HU-007.
- Despliegue, red, certificados por ambiente, `X-ARR-ClientCert` → Etapa 5, no es una historia del PDP.
- Tocar el módulo `pep/`. Es de otra persona. Si algo del contrato no cuadra, se reporta, no se edita.

### Criterios de la línea base que declara

Como mínimo 1, 2, 9, 11, 12, 21 y 22 (obligatorios), más **13 y 14** (DTOs y DTOs seguros — las
barreras de D7) y **4** (capacidades transversales: la cadena de seguridad nueva).

---

## 7. Alcance de HU-006 (OPA) — resumen, se planifica cuando toque

No la planifiques todavía: depende de HU-004 y HU-005 (roles y asignaciones vigentes), que es lo que le da a OPA algo que
evaluar. Lo decidido está en D9 y D10. Lo que habrá que producir además del adaptador:

- Forma del `input` que reciben las políticas Rego — es contrato con el compañero de OPA, igual que
  `contracts/pep-pdp/v1/` lo es con el del PEP. Merece su propia carpeta versionada.
- Mapeo de la respuesta de OPA a `DecisionState` + `ReasonCode` + `policyReferences` verificables.
- Error de OPA → `INDETERMINATE`, nunca `DENY` (ya lo hace el `onErrorResume` del caso de uso).

---

## 8. Cómo arrancar en la máquina nueva

```bash
git clone https://github.com/Seguridad-UCO/securityBaseline.git
cd securityBaseline
git checkout feature/hu-003-endpoint-interno-pep
```

Requisitos: **Java 25** (el POM lo exige; `verificar.ps1` resuelve el JDK aunque `JAVA_HOME` apunte a
otro), **Docker** corriendo (Testcontainers, necesario para `verify`), y abrir Claude Code con el
directorio de trabajo en **`securityBaseline/`**, no en la carpeta padre — si no, los cuatro agentes
no se registran como subagentes reales.

Comprueba el punto de partida antes de nada:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1
```

Debe salir **VERDE** con 277 pruebas. Si no, arregla eso primero: no se planifica sobre un build roto.

Luego, para ver el contrato del PEP sin cambiar de rama:

```bash
git show origin/feature/pep:contracts/pep-pdp/v1/openapi.yaml
git show origin/feature/pep:contracts/pep-pdp/v1/request.schema.json
git show origin/feature/pep:contracts/pep-pdp/v1/decision.schema.json
git show origin/feature/pep:contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md
git show origin/feature/pep:docs/plans/2026-09-06-security-platform-next-steps.md
```

### El encargo para el planificador

```
@1-planificador planifica HU-003: endpoint interno POST /internal/v1/access-decisions para el PEP.
El contrato, las decisiones ya tomadas y las trampas verificadas están en
docs/ai-harness/workspace/HANDOFF-INTEGRACION-PEP-OPA.md — léelo antes de la fase 0 y trata sus
decisiones D1..D10 como entrada del plan, no como preguntas del gate 1.
```

Después, el ciclo normal: `@2-tester-spec` → `@3-implementador` → `@4-validador`.

---

## 9. Lo que NO se decide sin Sebastián

- Mergear o cerrar `feature/hu-002-endpoint-decision` y `feature/pep`.
- Cualquier cambio dentro del módulo `pep/`.
- Cambiar una frontera de Modulith más allá de exportar un paquete que el plan necesite (eso exige
  ADR en `security-platform-architecture`).
- Declarar `contracts/pep-pdp/v1/` como fuente de verdad **en lugar de** `docs/05-contracts/rest/`
  del repo de arquitectura. Recomendación de esta sesión: que `contracts/pep-pdp/v1/` sea la fuente
  (ya está versionada y ya la consume un cliente real) y que el repo de arquitectura la referencie —
  pero es una decisión de equipo, no de una sesión.
- Cualquier `git push`, `commit`, PR o merge que no se haya pedido explícitamente.

---

## 10. Referencias

| Qué | Dónde |
|---|---|
| Contrato PEP↔PDP v1 | `contracts/pep-pdp/v1/` (rama `feature/pep`) |
| Guía de integración para el PDP | `contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md` |
| Plan de 7 etapas de la plataforma | `docs/plans/2026-09-06-security-platform-next-steps.md` (rama `feature/pep`) |
| ADR del PEP | `docs/architecture/adr-pep-v1.md` (rama `feature/pep`) |
| Estado y pruebas del PEP | `pep/README.md` (rama `feature/pep`) |
| Roadmap del PDP, renumerado | [`ROADMAP-PDP.md`](ROADMAP-PDP.md) |
| Contrato de trabajo de los agentes | [`AGENTS.md`](../../../../AGENTS.md) |
| Reporte de validación de HU-002 | [`reportes/REPORTE-HU-002.md`](reportes/REPORTE-HU-002.md) |
