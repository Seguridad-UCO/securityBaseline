# `reasonCode` — vocabulario único

Un solo listado, tres implementaciones: enum cerrado en el PDP
(`pdp/authorization/domain/model/ReasonCode.java`), literales en Rego
(`security-policy-engine/policies/`), y string libre en el PEP, que acepta cualquiera de estos.

**Añadir un código es un cambio de contrato**: se acuerda aquí primero, y después se implementa en
los tres. La decisión que fijó esta lista es la D-U2 de [`README.md`](README.md).

---

## Los produce la política (OPA los emite, el PDP los reenvía)

| Código | Cuándo | `decision` |
|---|---|---|
| `POLICY_ALLOWED` | Una política de aplicación concedió el acceso | `ALLOW` |
| `POLICY_DENY` | Una política denegó explícitamente | `DENY` |
| `NO_APPLICABLE_POLICY` | Ninguna política aplicó al `application.id` de la petición | `DENY` |
| `TENANT_ISOLATION_FAILED` | El guard de tenant rechazó: acceso cruzado sin evidencia que lo respalde | `DENY` |

## El motor no pudo decidir (OPA los emite; son defectos, no denegaciones)

| Código | Cuándo | `decision` |
|---|---|---|
| `INVALID_INPUT` | El conjunto de hechos no pasó `input_validation.rego` | `INDETERMINATE` |
| `POLICY_AMBIGUITY` | Más de un candidato `ALLOW` para la misma petición | `INDETERMINATE` |
| `POLICY_OUTPUT_INVALID` | Una política emitió un candidato mal formado (p. ej. una obligación de tipo desconocido) | `INDETERMINATE` |

> Estos tres **no son `DENY`**. El efecto para el PEP es el mismo —cualquier cosa que no sea un
> `ALLOW` íntegro hace que falle cerrado—, pero el motivo es honesto y se puede alertar sobre él.
> `INVALID_INPUT`, en particular, acusa un defecto del **PDP**, no del usuario: merece log y alerta.

## Los produce el PDP, antes o alrededor de la política

| Código | Cuándo | `decision` |
|---|---|---|
| `TENANT_MISMATCH` | La aplicación no pertenece al tenant del principal (catálogo, antes de consultar la política) | `DENY` |
| `TOKEN_INVALID` | La evidencia JWT no es válida. Por D8 esto se responde **401**, no en el cuerpo; el código existe para el log | `DENY` |
| `CONTEXT_UNAVAILABLE` | El motor de políticas no respondió, expiró o falló | `INDETERMINATE` |

---

## Mapeo con lo que OPA emitía antes de la unificación

Referencia para leer logs o bundles anteriores al 2026-09-10:

| Antes (OPA) | Ahora | Motivo |
|---|---|---|
| `NO_POLICY_MATCH` | `NO_APPLICABLE_POLICY` | Término de XACML (*NotApplicable*) |
| `EXPLICIT_DENY` | `POLICY_DENY` | Simetría con `POLICY_ALLOWED` |
| `INVALID_INPUT` con `effect: DENY` | `INVALID_INPUT` con `effect: INDETERMINATE` | Es un defecto, no una denegación |
| `POLICY_AMBIGUITY` con `effect: DENY` | `POLICY_AMBIGUITY` con `effect: INDETERMINATE` | Ídem |
| `POLICY_OUTPUT_INVALID` con `effect: DENY` | `POLICY_OUTPUT_INVALID` con `effect: INDETERMINATE` | Ídem |

`TENANT_ISOLATION_FAILED` y `POLICY_ALLOWED` no cambian.
