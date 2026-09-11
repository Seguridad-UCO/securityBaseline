# Contratos PDP–OPA

La integración conserva tres modelos diferentes:

| Contrato | Propósito | Propietario |
| --- | --- | --- |
| `PolicyEvaluationInput` | Hechos ya resueltos y confiables para evaluar una política. | PDP conceptual → OPA |
| `PolicyDecision` | Resultado lógico del core de políticas. | Core OPA |
| `OpaResponse` | Envelope nativo de la API REST de OPA. | Adapter HTTP |

Un adapter futuro transforma el resultado HTTP en `PolicyDecision`; el core Rego no
importa ni conoce `OpaResponse`. `policyId` identifica la política de aplicación (o el
componente core que negó), no una lista de predicados que participaron en la decisión.

## Hechos y tenant

`subject.tenantId` es el tenant de pertenencia o contexto del sujeto y `tenant.id` es el
tenant objetivo. Ambos son obligatorios. `resource.tenantId` no existe: la única fuente
del tenant objetivo es `tenant.id`.

Los roles, perfiles y entitlements son evidencias: ninguno concede acceso por sí solo.
Una ausencia de contexto, seguridad, relación o atributo nunca cuenta como evidencia
positiva. El PDP resuelve y protege esos hechos antes de enviarlos a OPA.

## Obligaciones

Los únicos tipos admitidos son `AUDIT`, `MASK_FIELDS`, `REQUIRE_MFA`, `READ_ONLY` y
`LOG_SECURITY_EVENT`, con parámetros objeto. OPA solamente las declara; el PEP las
ejecuta. Un consumidor que reciba una obligación desconocida con un `ALLOW` debe cerrar
la solicitud. El contrato completo está en `contracts/pdp-opa/v1/obligation.schema.json`.

