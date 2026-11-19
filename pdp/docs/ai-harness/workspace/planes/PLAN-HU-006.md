# PLAN: HU-006 — Adaptador real de OPA sobre `PolicyDecisionPort`

## Metadata

- **ID:** HU-006
- **Slice:** `authorization` (existente)
- **Tipo:** Escritura de configuración / integración saliente (no toca persistencia propia)
- **Fecha:** 2026-09-12
- **Rama sugerida:** `feature/HU-006-adaptador-opa`
- **Fuentes:**
    - `pdp/docs/ai-harness/workspace/HANDOFF-INTEGRACION-PEP-OPA.md` §4 (D9, D10) y §7 (alcance ya
      acordado — tratado como entrada del plan, no como pregunta de gate 1)
    - `contracts/pdp-opa/v1/policy-evaluation-input.schema.json`, `opa-response.schema.json`,
      `policy-decision.schema.json`, `obligation.schema.json`, `examples/valid/minimal-same-tenant.json`
    - `contracts/reason-codes.md` (vocabulario cerrado, ya implementado 1:1 en `ReasonCode`)
    - `security-policy-engine/policies/application/*.rego` (composición, guards, validación de entrada)
    - `security-policy-engine/README.md` (endpoint real: `POST /v1/data/security/authorization/decision`)
    - Código real: `PolicyDecisionPort`, `AccessRequest`, `AccessDecision`, `DenyByDefaultPolicyDecisionAdapter`,
      `AuthorizeUseCaseImpl`, `EvaluateInternalAccessUseCaseImpl`, `AuthorizationConfiguration`,
      `SurrealDbConfiguration`/`SurrealDbClient` (precedente de cliente `WebClient` + Jackson 3 del proyecto)
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 9, 11, 12, 21, 22 (ver §"Criterios" abajo)

## 0. Hallazgo antes de planificar — por qué el alcance NO incluye roles todavía

`assignments` (HU-005) ya puede resolver los roles activos de un usuario
(`AssignmentRepository.findActiveRoleIdsFor`), y el schema de entrada de OPA sí tiene un campo
opcional `subject.roles`. Parecía natural que HU-006 los conectara. **No lo hace**, por dos hechos
verificados en el código, no supuestos:

1. **No hay ningún `UserId` resoluble en el punto donde se construye `AccessRequest`.** En el canal
   BFF, `subject` es el `sub` crudo del JWT de Keycloak (`PdpPrincipal.subject()`); resolverlo a
   `UserId` exige `SecurityUserRepository.findIdentity(issuer, subject)`, y ni `AccessRequest` ni
   `PdpPrincipal` llevan el `issuer`. En el canal interno (PEP), `subject` es el `sub` del **JWT de
   evidencia** que autentica al PEP como llamador — no hay ningún usuario final en
   `contracts/pep-pdp/v1/request.schema.json` (el `SolicitudAcceso` no tiene campo de usuario en
   absoluto). Resolver esto de verdad es un cambio de contrato con el compañero que lleva el PEP —
   fuera del alcance de una historia del PDP en solitario.
2. **Ninguna política Rego consume roles todavía.** `policies/application/composition.rego` tiene
   `allow_candidates`/`deny_candidates` como *extension points* vacíos (`false` literal) — hoy OPA
   deniega siempre por `NO_APPLICABLE_POLICY` sin importar qué lleve `subject`. Enviar roles reales
   no cambiaría ninguna decisión observable hasta que exista una política de aplicación real.

Por eso el alcance de HU-006 es **el adaptador completo y correcto contra el contrato que ya existe**,
enviando los campos que `AccessRequest` sí puede rellenar sin inventar nada (`request`, `subject.id`,
`subject.type`, `subject.tenantId`, `tenant`, `application`, `resource`, `action`), y dejando
`subject.roles`/`profiles`/`entitlements`/`groups`, `relationships`, `context` y `security` sin
enviar (son opcionales en el schema). Cuando exista una política real que los necesite, esa historia
resuelve primero la identidad de usuario extremo a extremo — anotado en la sección 11.

## 1. Resumen funcional

`OpaPolicyDecisionAdapter` implementa `PolicyDecisionPort` con un `WebClient` real contra
`POST {pdp.opa.base-url}{pdp.opa.decision-path}` (hoy `http://localhost:8181/v1/data/security/authorization/decision`).
Traduce `AccessRequest` → `PolicyEvaluationInput` (contrato `contracts/pdp-opa/v1`), envía
`{"input": ...}` (convención HTTP estándar de OPA), y traduce la respuesta (`effect`, `reasonCode`,
`policyReferences`) a `AccessDecision`. Sustituye a `DenyByDefaultPolicyDecisionAdapter`, que se
elimina (D9 del handoff): la denegación por defecto pasa a vivir en la política Rego, no en el PDP.
Cualquier fallo de red, timeout o de mapeo (p. ej. un `reasonCode` fuera del vocabulario cerrado) se
propaga como error y `AuthorizeUseCaseImpl` ya lo convierte en `INDETERMINATE`/`CONTEXT_UNAVAILABLE`
en su `onErrorResume` final — **no se toca ese use case**.

**No cubre:** resolución de identidad de usuario para `subject.roles` (ver §0), políticas Rego reales
de aplicación (`policies/applications/`, corresponde a quien las publique), auditoría de la decisión
(HU-007), circuit breaker/retry avanzado (un timeout simple basta para este alcance).

## 2. Criterios de aceptación

| #  | Criterio                                                                                                                                                                                                                                                                                        | Resultado esperado |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| 1  | `PolicyDecisionPort` sigue siendo el único punto de contacto; `AuthorizeUseCaseImpl`/`EvaluateInternalAccessUseCaseImpl` no cambian una línea                                                                                                                                                   |
| 2  | Con OPA respondiendo `{"result":{"effect":"ALLOW","reasonCode":"POLICY_ALLOWED","policyReferences":[{"id":"...","version":"..."}],"obligations":[]}}`, `AuthorizeUseCase.execute` devuelve `AccessDecision` con `state=ALLOW`, `reasonCode=POLICY_ALLOWED` y esa `policyReferences`             |
| 3  | Con OPA respondiendo `DENY`/`NO_APPLICABLE_POLICY` (el caso de hoy, sin políticas publicadas), la decisión sigue siendo `DENY`/`NO_APPLICABLE_POLICY` — mismo comportamiento observable que con el adaptador anterior, pero ahora es una respuesta real de OPA, no un valor fijo en el PDP      |
| 4  | OPA caído, con timeout, o respondiendo un cuerpo que no cumple el contrato (p. ej. `reasonCode` fuera del enum) → `AccessDecision` con `state=INDETERMINATE`, `reasonCode=CONTEXT_UNAVAILABLE` (vía el `onErrorResume` ya existente de `AuthorizeUseCaseImpl`, no lógica nueva en el adaptador) |
| 9  | Ninguna excepción cruda llega al cliente: el adaptador no atrapa nada, deja que el error suba                                                                                                                                                                                                   |
| 11 | El adaptador no decide nada de negocio: solo traduce ida y vuelta                                                                                                                                                                                                                               |
| 21 | Los DTO de transporte hacia/desde OPA son `record` inmutables, sin Lombok                                                                                                                                                                                                                       |
| 22 | `WebClient` reactivo, sin `block()`; timeout con `.timeout(Duration)`, no `Thread.sleep`                                                                                                                                                                                                        |

## 3. Reglas de negocio

Ninguna regla nueva. El adaptador no valida ni decide — traduce. La única regla existente que se ve
involucrada es la ya implementada en `ReasonCode` (enum cerrado): un `reasonCode` de OPA que no está
en el vocabulario lanza `IllegalArgumentException` al mapear (`ReasonCode.valueOf(...)`), que sube
como error técnico y cae en el `onErrorResume` general de `AuthorizeUseCaseImpl` → `INDETERMINATE`.
No se envuelve esa excepción en una regla de dominio propia: es un defecto de contrato entre
componentes, no una decisión de negocio del PDP.

| # | Regla           | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|-----------------|------------|-------------------------|------------------|
| — | (ninguna nueva) | —          | —                       | —                |

## 4. Modelo de dominio afectado

Ninguno. `DecisionState`, `ReasonCode`, `PolicyReference`, `AccessDecision`, `AccessRequest` ya
existen y no cambian de forma (son contratos existentes — el planificador no los toca).

### Value objects

| VO                   | Nuevo o existente | Invariantes | Vive en |
|----------------------|-------------------|-------------|---------|
| (ninguno de dominio) | —                 | —           | —       |

Los tipos nuevos de esta historia son DTO de **transporte de infraestructura** (la forma exacta del
JSON que OPA espera/devuelve), no value objects de dominio — ver sección 7.

## 5. Persistencia

No aplica — sin sección 5.

## 6. Endpoint

No aplica — HU-006 no expone HTTP nuevo. Reutiliza `POST /api/v1/authorize` (BFF) y
`POST /internal/v1/access-decisions` (HU-003) tal cual están. Solo cambia qué hay detrás de
`PolicyDecisionPort`.

## 7. SPEC — el contrato

> Firmas exactas. Sin cuerpos de lógica: los `[N]` se materializan como esqueletos que lanzan
> `UnsupportedOperationException`; los `[M]` los aplica el implementador.

### DTO de transporte hacia OPA — `infrastructure/adapter/secondary/policy/dto/` [N]

Mapean 1:1 `contracts/pdp-opa/v1/policy-evaluation-input.schema.json`, solo los campos requeridos
más `resource.id` (opcional pero disponible sin costo). El resto de campos opcionales del schema
(`relationships`, `context`, `security`, `subject.roles/profiles/entitlements/groups/attributes`) se
omiten a propósito (ver §0) — Jackson 3 no los exige porque no están anotados como obligatorios.

```java
// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaRequestInfo.java
public record OpaRequestInfo(String id, String correlationId) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaSubject.java
public record OpaSubject(String id, String type, String tenantId) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaTenant.java
public record OpaTenant(String id) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaApplication.java
public record OpaApplication(String id) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaResource.java
public record OpaResource(String type, String id) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaEvaluationInput.java
public record OpaEvaluationInput(String schemaVersion, OpaRequestInfo request, OpaSubject subject,
        OpaTenant tenant, OpaApplication application, OpaResource resource, String action) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaEvaluationRequest.java
// Envoltorio {"input": ...} — convención HTTP estándar de OPA, ver security-policy-engine/README.md.
public record OpaEvaluationRequest(OpaEvaluationInput input) {
}
```

### DTO de transporte desde OPA — `infrastructure/adapter/secondary/policy/dto/` [N]

Mapean `contracts/pdp-opa/v1/opa-response.schema.json` / `policy-decision.schema.json`. Se omite
`decision_id` (el PDP genera el suyo con `IdentifierGenerator`, igual que hacía
`DenyByDefaultPolicyDecisionAdapter`) y `obligations` (fuera de alcance — ver resumen funcional;
Jackson 3 no falla por un campo del JSON que el record no declara, salvo que algo lo configure
explícitamente, y este proyecto no lo hace).

```java
// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaPolicyReference.java
public record OpaPolicyReference(String id, String version) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaPolicyDecisionPayload.java
public record OpaPolicyDecisionPayload(String effect, String reasonCode, java.util.List<OpaPolicyReference> policyReferences) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaResponse.java
public record OpaResponse(OpaPolicyDecisionPayload result) {
}
```

### Propiedades de configuración — `infrastructure/properties/` [N]

Simétrico a `pep.pdp.*` del PEP (D10 del handoff).

```java
// pdp/authorization/infrastructure/properties/OpaProperties.java
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "pdp.opa")
public record OpaProperties(String baseUrl, String decisionPath, java.time.Duration timeout) {
}
```

### El adaptador — `infrastructure/adapter/secondary/policy/` [N]

```java
// pdp/authorization/infrastructure/adapter/secondary/policy/OpaPolicyDecisionAdapter.java
public final class OpaPolicyDecisionAdapter implements PolicyDecisionPort {

    public OpaPolicyDecisionAdapter(org.springframework.web.reactive.function.client.WebClient webClient,
            tools.jackson.databind.ObjectMapper objectMapper, OpaProperties properties,
            co.edu.uco.seguridad.shared.port.IdentifierGenerator identifiers,
            co.edu.uco.seguridad.shared.port.TimeProvider time) {
        throw new UnsupportedOperationException("pendiente: HU-006");
    }

    @Override
    public reactor.core.publisher.Mono<AccessDecision> execute(AccessRequest input) {
        throw new UnsupportedOperationException("pendiente: HU-006");
    }
}
```

> Nota para el implementador (no es lógica, es la forma del contrato ya decidida en D9): el cuerpo
> POST es `{"input": <OpaEvaluationInput>}`, la ruta es `properties.baseUrl() + properties.decisionPath()`,
> el timeout se aplica con `.timeout(properties.timeout())` sobre el `Mono`, y el mapeo de vuelta es
> `DecisionState.valueOf(payload.effect())` / `ReasonCode.valueOf(payload.reasonCode())` /
> `payload.policyReferences().stream().map(r -> new PolicyReference(r.id(), r.version())).toList()`,
> con `identifiers.next()` y `time.now()` para `decisionId`/`decidedAt` — exactamente como hacía
> `DenyByDefaultPolicyDecisionAdapter`, solo que los tres primeros campos vienen de OPA en vez de ser
> fijos.

### Modificaciones a piezas existentes [M]

```java
// shared/message/RequiredArgumentMessages.java — dos constantes nuevas, mismo patrón que SURREALDB_WEBCLIENT/SURREALDB_PROPERTIES
public static final String OPA_WEB_CLIENT = "se requiere el WebClient de OPA";
public static final String OPA_PROPERTIES = "se requieren las propiedades de OPA";
```

```java
// pdp/authorization/infrastructure/config/AuthorizationConfiguration.java
// Reemplaza el @Bean policyDecisionPort actual (que construye DenyByDefaultPolicyDecisionAdapter)
// por uno que arma el WebClient (mismo patrón que SurrealDbConfiguration.surrealDbClient) y
// construye OpaPolicyDecisionAdapter. Añade @EnableConfigurationProperties(OpaProperties.class).
```

**Eliminación** (parte del contrato D9, no una modificación):
`pdp/authorization/infrastructure/adapter/secondary/policy/DenyByDefaultPolicyDecisionAdapter.java`
se borra. No tiene prueba dedicada (`grep` confirmó cero referencias fuera de sí mismo y de
`AuthorizationConfiguration`), así que no hay archivo de `pdp/src/test` que tocar por esto.

## 8. Árbol de archivos

```
pdp/authorization/
├── infrastructure/
│   ├── adapter/secondary/policy/
│   │   ├── OpaPolicyDecisionAdapter.java                          [N]
│   │   ├── DenyByDefaultPolicyDecisionAdapter.java                [M] — se elimina (D9)
│   │   └── dto/
│   │       ├── OpaEvaluationRequest.java                          [N]
│   │       ├── OpaEvaluationInput.java                            [N]
│   │       ├── OpaRequestInfo.java                                [N]
│   │       ├── OpaSubject.java                                    [N]
│   │       ├── OpaTenant.java                                     [N]
│   │       ├── OpaApplication.java                                [N]
│   │       ├── OpaResource.java                                   [N]
│   │       ├── OpaResponse.java                                   [N]
│   │       ├── OpaPolicyDecisionPayload.java                      [N]
│   │       └── OpaPolicyReference.java                            [N]
│   ├── properties/
│   │   └── OpaProperties.java                                     [N]
│   └── config/
│       └── AuthorizationConfiguration.java                        [M] — nuevo @Bean policyDecisionPort, @EnableConfigurationProperties
shared/message/
└── RequiredArgumentMessages.java                                  [M] — OPA_WEB_CLIENT, OPA_PROPERTIES
pdp/src/main/resources/
├── application.properties                                        [M] — pdp.opa.base-url/decision-path/timeout (valores locales)
```

No hay cruce de módulo Modulith nuevo: el adaptador solo usa tipos de `authorization` (propios) y
`commons`/`shared` (ya `OPEN`). `allowedDependencies` de `authorization` no cambia.

## 9. Casos de prueba esperados

> Presupuesto y convenciones: skill `sb-testing`. Sin Mockito — el `WebClient` real se prueba contra
> un servidor HTTP embebido de prueba (patrón ya usado en `pep/src/test/.../FixtureServers.java` y en
> las pruebas de integración de `SurrealDbClient`, si existen; si no, un `MockWebServer`-equivalente
> mínimo con `WebClient` apuntando a `http://localhost:{puerto efímero}` basta — no hace falta
> Testcontainers, esto no es persistencia).

| Capa                                                    | Clase de prueba                                                          | Casos                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------------------------------------------------|--------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| infrastructure (adaptador, con servidor HTTP de prueba) | `OpaPolicyDecisionAdapterTests`                                          | ALLOW con `policyReferences` no vacío → `AccessDecision.state=ALLOW` con esas referencias; DENY/`NO_APPLICABLE_POLICY` → `state=DENY`; OPA responde 500/conexión rechazada → el `Mono` termina en error (no en `AccessDecision`, eso lo decide `AuthorizeUseCaseImpl` aguas arriba); timeout superado → el `Mono` termina en error; `reasonCode` fuera del vocabulario cerrado → error de mapeo (no una `AccessDecision` silenciosa); el cuerpo POST enviado es exactamente `{"input": {...}}` con los campos esperados (schemaVersion, request.id/correlationId, subject.id/type/tenantId, tenant.id, application.id, resource.type/id, action) |
| application (sin tocar, ya cubierto)                    | `AuthorizeUseCaseImplTests`                                              | Ninguno nuevo — ya prueba el `onErrorResume` con un `PolicyDecisionPort` fake; no depende del adaptador real                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| integración de arranque                                 | (extender `LayeredArchitectureTests`/`ModulithStructureTests` si aplica) | Ninguna prueba nueva dedicada; correr las existentes basta — no se cruza ningún módulo nuevo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |

Presupuesto total estimado: **5–7 pruebas**, todas en una sola clase nueva. Es deliberadamente
pequeño: no hay reglas de negocio nuevas, solo una traducción de ida y vuelta.

## 10. Trazabilidad

| Fase                       | Estado                                                                           | Fecha      |
|----------------------------|----------------------------------------------------------------------------------|------------|
| Plan                       | ✅ Generado                                                                       | 2026-09-12 |
| Contrato aprobado (gate 1) | ⏳ Pendiente                                                                      |            |
| Pruebas en rojo            | ⏳ Pendiente                                                                      |            |
| Implementación en verde    | ✅ Verde (471 pruebas, 0 fallos)                                                  | 2026-09-12 |
| Validación                 | ✅ APROBADO (segunda pasada — primera rechazada por deriva doc↔código, corregida) | 2026-09-12 |
| Entrega (gate 2)           | ⏳ Pendiente                                                                      |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee este plan. Una decisión de alcance **documentada, no preguntada** (ver §0):
`subject.roles` queda vacío/ausente hasta que exista (a) una política Rego real que lo consuma y
(b) una forma de resolver la identidad del usuario final en ambos canales (BFF e interno) — lo
segundo es, en el canal interno, un cambio de contrato con el PEP y no se puede decidir unilateralmente
desde el PDP. Cuando llegue esa historia, candidatos a nombre: "HU-008 — identidad de usuario final en
el canal interno" o similar; no se numera aquí para no inventar una historia que el usuario no pidió.
