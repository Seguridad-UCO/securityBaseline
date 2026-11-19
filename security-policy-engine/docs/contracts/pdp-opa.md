# Contratos PDP–OPA

La integración conserva tres modelos diferentes:

| Contrato | Propósito | Propietario |
| --- | --- | --- |
| `PolicyEvaluationInput` | Hechos ya resueltos y confiables para evaluar una política. | PDP conceptual → OPA |
| `PolicyDecision` | Resultado lógico del core de políticas. | Core OPA |
| `OpaResponse` | Envelope nativo de la API REST de OPA. | Adapter HTTP |

El adapter HTTP transforma `OpaResponse` en una decisión PDP; el core Rego no conoce el
envelope HTTP. `policyReferences` contiene las policies decisivas, ordenadas de manera
determinista; una DENY puede referenciar varias policies que coincidieron.

## Hechos y tenant

`subject.tenantId` es el tenant de pertenencia o contexto del sujeto y `tenant.id` es el
tenant objetivo. Ambos son obligatorios. `resource.tenantId` no existe: la única fuente
del tenant objetivo es `tenant.id`.

Los roles, perfiles y entitlements son evidencias: ninguno concede acceso por sí solo.
Una ausencia de contexto, seguridad, relación o atributo nunca cuenta como evidencia
positiva. El PDP resuelve y protege esos hechos antes de enviarlos a OPA.

## Obligaciones

Aunque el contrato enumera tipos de obligation, actualmente no se publican obligaciones:
PDP aún no las transporta y PEP rechaza una lista no vacía. Una PolicyDefinition con
obligations no vacías debe ser rechazada durante validación de bundle.
