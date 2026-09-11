# Contrato de consumo HTTP: PDP → Security Policy Engine

Este documento define una integración genérica. No presupone una aplicación, un proveedor
de identidad, un lenguaje, ni un dominio de negocio. Cualquier PDP que pueda entregar
hechos confiables puede consumir el motor.

## Límite de responsabilidades

```text
PDP/adapter: autentica, resuelve hechos y valida el contrato de entrada
Security Policy Engine (OPA): evalúa políticas y devuelve PolicyDecision
PEP: aplica DENY/ALLOW y ejecuta obligations
```

El motor no valida JWT, no consulta bases de datos, no busca permisos remotos y no ejecuta
obligations. Esas tareas dependen del consumidor y deben ocurrir antes o después de la
evaluación, nunca dentro de una regla Rego.

## Endpoint y contenido

```text
POST /v1/data/security/authorization/decision
Content-Type: application/json
Accept: application/json
```

El cuerpo es siempre un envelope OPA:

```json
{"input": {"schemaVersion": "1.0", "...": "PolicyEvaluationInput"}}
```

`PolicyEvaluationInput` se define formalmente en
`contracts/pdp-opa/v1/policy-evaluation-input.schema.json`. Antes de hacer la llamada, el adapter
debe validar ese schema y rechazar localmente datos malformados. Solo el PDP puede
proveer hechos: el cliente final nunca debe poder enviar directamente este objeto.

Respuesta HTTP exitosa de OPA:

```json
{"result":{"effect":"DENY","reasonCode":"NO_APPLICABLE_POLICY","policyReferences":[{"id":"core.composition","version":"1.0"}],"obligations":[]},"decision_id":"opa-generated-id"}
```

Un `200` representa que OPA evaluó correctamente; no representa autorización. La decisión
está en `result.effect`. El adapter valida `result` con
`contracts/pdp-opa/v1/policy-decision.schema.json`, conserva `decision_id` solo como trazabilidad y
no lo usa como hecho de autorización.

## Algoritmo obligatorio del adapter

```text
1. Autenticar la solicitud y obtener la identidad.
2. Resolver desde fuentes confiables el tenant, roles, entitlements, relaciones y recurso.
3. Construir PolicyEvaluationInput y validarlo contra el JSON Schema.
4. POST a OPA con un timeout corto y configurable.
5. Si existe respuesta HTTP 200, validar OpaResponse.result.
6. Si effect = DENY, denegar.
7. Si effect = ALLOW, validar que todas las obligations sean conocidas y ejecutables.
8. Aplicar las obligations antes de entregar el recurso o completar la operación.
```

Fallo cerrado es obligatorio: timeout, error de red, estado HTTP no exitoso, JSON inválido,
schema inválido o una obligation desconocida producen una denegación técnica. No se debe
reintentar automáticamente una decisión en el camino crítico salvo que el consumidor tenga
un presupuesto de latencia explícito; OPA es una dependencia de seguridad, no un fallback
de allow.

## Invariantes de aislamiento

- `application.id` identifica al consumidor de la política y se compara con el
  `applicationId` declarado por el candidato de política.
- Una política de `applicationId: "A"` no puede autorizar ni denegar solicitudes para
  `application.id: "B"`.
- `subject.tenantId` y `tenant.id` son conceptos distintos y ambos son obligatorios.
- Cross-tenant necesita tanto una política declarada `CROSS_TENANT` como evidencia
  explícita resuelta por el PDP; produce obligación `AUDIT`.
- Más de un candidato ALLOW aplicable es ambiguo y el core devuelve
  `DENY / POLICY_AMBIGUITY`.

## Adaptador de referencia (pseudocódigo)

```text
decision = denyTechnical("OPA_UNAVAILABLE")
input = factsResolver.resolve(authenticatedRequest)
validate(PolicyEvaluationInputSchema, input)

response = http.post(opaUrl + "/v1/data/security/authorization/decision", { input }, timeout)
validate(OpaResponseSchema, response)
decision = response.result

if decision.effect == "ALLOW" and obligationsAreKnownAndExecutable(decision.obligations):
    pep.apply(decision.obligations)
    return decision
return deny(decision)
```

## Versionado compatible

El producer debe enviar `schemaVersion: "1.0"`. Una versión incompatible se deniega. Los
cambios incompatibles requieren un nuevo schemaVersion, contratos publicados y soporte
simultáneo durante la migración. Nunca se reutiliza una versión para cambiar la semántica
de campos existentes.

