# Roadmap del PDP — qué sigue y cómo se reparte con el equipo

> **Si vas a empezar HU-003, lee primero [`HANDOFF-INTEGRACION-PEP-OPA.md`](HANDOFF-INTEGRACION-PEP-OPA.md).**
> Tiene el contrato del PEP, las decisiones de diseño ya tomadas y las trampas verificadas.

> El PDP es el contenedor del medio: recibe del PEP, consulta a OPA, lee SurrealDB y publica a
> Auditoría. Eso lo convierte en **el único que toca los dos contratos** del diagrama C4, y por eso
> el orden de abajo no es una lista de deseos: es el orden que desbloquea a los otros dos.

## El reparto

| Componente | Responsable | Depende de que el PDP publique… |
|---|---|---|
| **PEP** — punto de aplicación | compañero/a | El esquema de `SolicitudAcceso` y `DecisionAcceso`, y un endpoint vivo contra el cual integrar |
| **OPA** — motor de políticas | compañero/a | El esquema de `input` que reciben las políticas Rego y la forma de la decisión que deben devolver |
| **PDP** — servicio de autorización | **Sebastián** | — (es quien los publica) |

**Consecuencia práctica:** mientras el contrato no exista y no esté corriendo, los otros dos
codifican contra una suposición. Por eso HU-002 es primero aunque no tome ninguna decisión real.

## Hallazgo (2026-09-10): el contrato de HU-002 no es el que consume el PEP

HU-002 publicó `POST /api/v1/authorize` — un endpoint BFF, autenticado por sesión/cookie como el
resto del panel. El compañero del PEP, en paralelo y sin acceso al código del PDP (por diseño, ver
`pep/README.md`), construyó su cliente contra `POST /internal/v1/access-decisions`: un canal
**interno**, autenticado con **mTLS del servicio PEP + el Bearer del usuario como evidencia**, con su
propio contrato versionado en `contracts/pep-pdp/v1/` (`openapi.yaml`, `request.schema.json`,
`decision.schema.json` — forma distinta a `AuthorizeRawRequest`/`AccessDecisionWebResponse`).

Ninguno de los dos está mal: son canales distintos con distinta confianza. Lo que falta es el
segundo. Verificado abriendo `contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md` en la rama
`feature/pep`: *"No hay una implementación de esa operación en el PDP actual"*, textual.

**Reconciliación, no rediseño:** `/internal/v1/access-decisions` reutiliza el mismo
`AuthorizeUseCase`/`AccessRequest`/`AccessDecision` de dominio que ya existe — es un **segundo
adaptador primario** (su propio controller/interactor/mapper, mapeando el JSON Schema del PEP en
vez del `AuthorizeRawRequest` del BFF), exactamente el mismo patrón por el que `tenants` o
`applications` podrían tener dos entradas HTTP para un mismo caso de uso. `/api/v1/authorize` no se
toca ni se retira: sigue siendo el canal del panel/BFF.

Esto además reordena el roadmap: la etapa que desbloquea al PEP no es HU-003 (roles) como decía la
versión anterior de esta tabla, es un endpoint nuevo. El plan completo de 7 etapas del compañero
está en `docs/plans/2026-09-06-security-platform-next-steps.md` (rama `feature/pep`) — la tabla de
abajo es la porción que le corresponde al PDP, renumerada para encajar con sus Etapas 1 y 3.

## Lo que ya está aceptado y NO hay que inventar

El vocabulario y el flujo están cerrados en `security-platform-architecture`:

| Artefacto | Dónde |
|---|---|
| `SolicitudAcceso`, `DecisionAcceso`, `EventoAcceso` como *published language* | `docs/02-domain/04-context-map.md` |
| **UC-01 EvaluarAcceso** — flujo ALLOW, DENY e INDETERMINATE, precondiciones y criterios | `docs/02-domain/08-use-cases.md` |
| `DecisionAcceso` es **tri-estado** y estructurada, nunca un booleano (INV-POL-04) | `docs/02-domain/06-invariants.md` |
| Denegación por defecto si no hay política aplicable | ADR-012 |
| Mapeo HTTP: token inválido → 401 · resto de DENY → 403 · INDETERMINATE → **503** | ADR-014 |
| El inquilino sale del principal, nunca del cuerpo ni de la query | ADR-018 |

Lo que **sí** está vacío y hay que producir: `docs/05-contracts/rest/`, `docs/05-contracts/policies/`
y `docs/06-security/opa/`. Eso lo produce HU-002 y es lo que consumen los otros dos.

---

## Orden de prioridad

| # | Historia | Por qué en ese lugar | Desbloquea | Etapa del plan del PEP |
|---|---|---|---|---|
| **HU-002** | Endpoint de decisión con el contrato completo, denegando por defecto (`/api/v1/authorize`, canal BFF) | Publica `AccessRequest`/`AccessDecision` como *published language* y el flujo tri-estado. **✅ Hecho** — pero es el canal BFF, no el que consume el PEP (ver hallazgo arriba) | Base de dominio para HU-003 | Etapa 0 (parcial) |
| **HU-003** | Endpoint interno `POST /internal/v1/access-decisions` con mTLS del PEP + Bearer del usuario como evidencia, conforme a `contracts/pep-pdp/v1` | Es lo único que de verdad desbloquea al PEP — hoy no tiene nada contra qué integrar. Reutiliza el `AuthorizeUseCase` de HU-002 con un segundo adaptador primario; responde `INDETERMINATE`/error mientras no haya política real, nunca `ALLOW` provisional | PEP | **Etapa 1** |
| **HU-004** | Roles y asignaciones vigentes por inquilino (BC-04 + BC-08) — resolución de recurso/catálogo para el contexto de decisión | Es el **valor propio del PDP**: sin esto OPA no tiene atributos que evaluar. El trozo más grande y 100 % nuestro | HU-005 | Etapa 2 |
| **HU-005** | Adaptador real de OPA sobre `PolicyDecisionPort` | Sustituye la denegación por defecto por la decisión real | Cierra el flujo ALLOW | Etapa 3 |
| **HU-006** | `EventoAcceso` correlacionado hacia Auditoría | INV-AUD-01. Sin evidencia no hay cumplimiento | Auditoría | Etapa 4 (parcial — diseño de evento/outbox; almacenamiento y consulta puede ser otra historia) |
| **HU-007** | Perfiles como agrupación de roles (BC-05) | Comodidad administrativa, no bloquea la decisión | — | — |
| **HU-008** | Criterio 10: cablear la saga de compensación | Único criterio abierto de la línea base, pero no bloquea a nadie del equipo | — | — |

> Las etapas 5 (despliegue seguro), 6 (adopción/starter) y 7 (observabilidad y gobierno) del plan del
> compañero son transversales a varias historias y no mapean a un HU único del PDP — se revisan
> cuando cada pieza esté lista, no antes.

---

## Qué necesita el PDP para que estas dos integraciones sean consistentes

Lista corta, deliberadamente — lo mínimo que hay que decidir **antes** de planificar HU-003 con
`@1-planificador`, para no descubrirlo a mitad de la implementación como pasó con
`IdentifierGenerator` en HU-002:

1. **Puerto/listener separado para el canal interno.** WebFlux sobre Netty configura
   `client-auth` (mTLS) por conector, no por ruta — no se puede exigir certificado de cliente solo en
   `/internal/**` sobre el mismo puerto que sirve `/api/**` sin certificado. Necesita un segundo
   puerto (p. ej. `pdp.internal-server.port`) con su propio `Ssl` de servidor. Esto es una decisión de
   arquitectura, no un detalle de implementación — probablemente merece su propio ADR corto antes de
   HU-003.
2. **Un segundo `SecurityWebFilterChain`**, acotado a `/internal/**` y separado del que ya existe en
   `SecurityConfiguration` (que es explícitamente "el único lugar que decide" — ese comentario hay
   que actualizarlo para decir "el único lugar *por canal*"). Valida certificado de cliente (CN
   contra un allowlist) y, aparte, el Bearer del usuario como evidencia — probablemente reutilizando
   el mismo `ReactiveJwtDecoder`/`JwtSecurityProperties` que ya existen, sin el `WebSessionServerSecurityContextRepository`
   (el PEP no manda cookie).
3. **Nombrado de properties simétrico al del PEP.** El PEP ya usa `pep.pdp.*`
   (`pep.pdp.base-url`, `pep.pdp.ca-certificate`, etc.). El lado PDP debería adoptar algo como
   `pdp.security.internal.*` para el mTLS entrante y `pdp.opa.*` para el cliente OPA de HU-005 —
   mismo estilo que `JwtSecurityProperties`/`CorsProperties` ya existentes.
4. **Un segundo adaptador primario, no un segundo caso de uso.** `AuthorizationConfiguration` gana
   un nuevo `@Bean` de interactor/mapper para el canal interno; `AuthorizeUseCase` no cambia.
5. **Fuente de verdad del contrato.** `contracts/pep-pdp/v1/` (en este repo, rama `feature/pep`) ya
   tiene el OpenAPI y los JSON Schema. El roadmap actual apunta a `docs/05-contracts/rest/` en
   `security-platform-architecture` como destino "vacío, por producir" — hay que decidir cuál es la
   fuente real antes de que diverjan. Lo más simple: `contracts/pep-pdp/v1/` es la fuente (ya
   versionada, ya consumida por el cliente PEP real) y `security-platform-architecture` la referencia.
6. **`ci/pep-pipeline.yml` sigue sin registrar en Azure** (según el propio ADR del PEP) — no es tuyo
   que resolverlo, pero si al fusionar `feature/pep` nadie lo hace, el módulo `pep/` quedará sin CI
   propio. Vale la pena mencionarlo al compañero.

Lo que **no** hace falta decidir todavía: diseño de auditoría durable (Etapa 4, después de HU-005),
despliegue/red (Etapa 5), ni el starter (Etapa 6) — son del compañero o de una historia futura.

### Por qué HU-002 deniega a propósito

Un endpoint que responde `DENY` con `reasonCode` estable **no es un placeholder**: es el
comportamiento correcto según ADR-012 mientras no haya política publicada. El compañero del PEP
puede integrar y probar de verdad su camino de rechazo (que es el que más importa en seguridad) y
el contrato de transporte completo. Cuando HU-004 conecte OPA, el camino ALLOW se abre **sin
cambiar el contrato**.

Eso es lo contrario de un mock: el mock miente y hay que quitarlo; esto es *fail-closed*, y se queda.

---

## La regla de diseño que atraviesa todo el roadmap

> **Ni un solo `if (rol == ...)` en un caso de uso o un controlador.**

Roles y perfiles no desaparecen: dejan de ser la lógica de decisión y pasan a ser **atributos de
entrada** para las políticas. El PDP resuelve el contexto confiable —sujeto, tenant, roles vigentes,
recurso, acción, atributos— y OPA decide sobre él.

`PdpPrincipal` se queda como está: identidad autenticada y nada más. El contexto de autorización es
un tipo aparte, que se construye por HU-003 y viaja a OPA en HU-004.
