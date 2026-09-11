# Contratos de la Plataforma Central de Seguridad

Aquí viven los contratos **entre componentes**. Un contrato está aquí, y no dentro del componente
que lo produce, por una razón concreta: cuando el contrato vive del lado del productor, se escribe
solo desde su punto de vista y el consumidor se entera al integrar. Eso ya pasó una vez — ver
[`INTEGRACION-PDP-PEP-OPA.md`](../docs/ai-harness/workspace/INTEGRACION-PDP-PEP-OPA.md).

| Contrato | Dirección | Consumidor | Productor |
|---|---|---|---|
| [`pep-pdp/v1/`](pep-pdp/v1/) | PEP → PDP | PDP | PEP |
| [`pdp-opa/v1/`](pdp-opa/v1/) | PDP → OPA | OPA | PDP |

Vocabularios compartidos por los tres, con una sola definición:

- [`reason-codes.md`](reason-codes.md) — por qué se decidió lo que se decidió
- [`obligations.md`](obligations.md) — qué debe hacer el PEP antes de servir un `ALLOW`

## Reglas

1. **Un cambio de contrato se acuerda antes de implementarse**, entre productor y consumidor. Un
   schema que solo firma una parte no es un contrato, es una expectativa.
2. **Versión en la ruta** (`v1/`). Un cambio incompatible abre `v2/`; el `v1/` sigue vivo hasta que
   el último consumidor migre.
3. **Los ejemplos son parte del contrato**, no ilustración. `security-policy-engine/scripts/validate`
   valida los de `pdp-opa/v1/examples/` contra su schema en cada build.

---

# Las cuatro decisiones de unificación (2026-09-10)

Al unir las tres ramas aparecieron cuatro choques. En cada uno se adoptó **el estándar del componente
que lo tenía mejor resuelto**, y los otros dos se adaptaron. Ninguna se resolvió «por mayoría» ni por
quién llegó primero.

## D-U1 — La forma de los hechos: gana el modelo de OPA

| Candidato | Valoración |
|---|---|
| **OPA `PolicyEvaluationInput`** | ✅ **Gana.** Categorías de atributos al estilo XACML (`subject`/`resource`/`action`/`context`/`security`), `schemaVersion` propio, `additionalProperties:false`, y separa el tenant de pertenencia del sujeto (`subject.tenantId`) del tenant objetivo (`tenant.id`). Es el único que escribe la doctrina de evidencia: *roles, perfiles y entitlements son evidencia; ninguno concede por sí solo* |
| PEP `SolicitudAcceso v1` | Bien versionado, pero deliberadamente delgado: es una petición, no un conjunto de hechos. No manda sujeto ni tenant a propósito |
| PDP `AccessRequest` | El más débil: record plano, sin versión y sin `requestId` |

**Lo que cambia cada uno:**

- **PDP** — `AccessRequest` gana `requestId`. El PEP ya lo manda como campo obligatorio y OPA lo exige
  como `request.id`; el PDP era el único de los tres que lo tiraba. *Aplicado.*
- **PDP** — los dos hechos que OPA exige y el PDP no tenía se fijan **como constantes del contrato**,
  no como invención del adaptador:

  | Hecho de OPA | Valor hoy | Por qué es constante, y hasta cuándo |
  |---|---|---|
  | `subject.type` | `"USER"` | El único sujeto hoy es una persona autenticada en Keycloak. Cuando existan sujetos de servicio, deja de ser constante |
  | `resource.type` | `"HTTP_ENDPOINT"` | El catálogo solo protege endpoints HTTP. `resource.id` lleva la ruta |

- **OPA** — no cambia su schema. Se documenta que, para `resource.type = "HTTP_ENDPOINT"`,
  `resource.id` es la ruta HTTP: eso reconcilia el modelo `(tipo, acción)` de OPA con el
  `(ruta, verbo)` del PDP y del PEP sin que ninguno de los dos ceda su modelo.

> **Por qué no se adoptó el modelo del PDP.** Era el más cómodo (no cambiar nada), pero es el único
> sin versión y sin categorías. Adoptarlo habría significado que el motor de políticas no puede
> recibir un atributo nuevo sin romper el contrato.

## D-U2 — El vocabulario de `reasonCode`: gana el mecanismo del PDP, gana la precisión de OPA

Ninguno de los dos era mejor entero, así que se separó **mecanismo** de **nombres**:

- **Mecanismo: el del PDP.** Un enum cerrado. No puede derivar en silencio: si OPA emite un código
  que el PDP no conoce, el compilador y el mapeo lo dicen. La alternativa del PEP —string libre que
  case un patrón— acepta cualquier cosa, incluida una errata.
- **Nombres: caso por caso, gana el término más preciso.** Dos renombra OPA, cinco añade el PDP.

La lista única está en [`reason-codes.md`](reason-codes.md). Los dos renombrados:

| OPA decía | Ahora | Por qué gana el otro |
|---|---|---|
| `NO_POLICY_MATCH` | `NO_APPLICABLE_POLICY` | Es el término de XACML (*NotApplicable*). Cuando existe un término de industria, se usa |
| `EXPLICIT_DENY` | `POLICY_DENY` | Simétrico con `POLICY_ALLOWED`, y «explícito» sobra: el caso implícito ya es `NO_APPLICABLE_POLICY` |

**Y un cambio de fondo: OPA gana `INDETERMINATE`.** `POLICY_AMBIGUITY`, `POLICY_OUTPUT_INVALID` e
`INVALID_INPUT` no son denegaciones de negocio: son defectos del conjunto de políticas o del
llamador. Marcarlos `DENY` es seguro pero miente sobre el motivo, y los deja indistinguibles del
tráfico normal justo cuando hay que alertar. `DecisionState` del PDP y `decision` del PEP **ya**
tienen los tres valores; OPA era el único con dos.

## D-U3 — Obligaciones: gana el modelo de OPA

| Candidato | Valoración |
|---|---|
| **OPA** | ✅ **Gana.** Objetos `{type, parameters}` con lista cerrada de cinco tipos, y la regla correcta: *un consumidor que reciba una obligación desconocida junto a un `ALLOW` debe cerrar la solicitud* |
| PEP v1 | `items: {type: "string"}` y «solo se admite vacío». No es un modelo, es un aplazamiento: un string no puede llevar parámetros |

**Lo que cambia cada uno:**

- **PEP** — el contrato pasa a `v1.1`: `obligations` es un array de `{type, parameters}` y se conserva
  el fallo cerrado, pero ante **tipo desconocido**, no ante lista no vacía. ⚠️ *Pendiente: es cambio
  en el componente de David.*
- **PDP** — mientras el PEP no las implemente, **no puede descartarlas silenciosamente**. Ver la regla
  de transición en [`obligations.md`](obligations.md).

> **La regla de transición es la parte importante.** Lo cómodo sería que el PDP filtrara las
> obligaciones para que el PEP acepte el `ALLOW`. Eso serviría la petición **sin auditar**, que es
> exactamente lo que la obligación `AUDIT` existía para impedir. Un `ALLOW` con una obligación que
> nadie va a cumplir no es un `ALLOW`: es `INDETERMINATE`.

## D-U4 — `policyReferences`: gana el del PEP

| Candidato | Valoración |
|---|---|
| **PEP `[{id, version}]`** | ✅ **Gana.** Una decisión auditable tiene que decir **qué versión** de qué política decidió. Sin versión, la traza no reproduce la decisión |
| OPA `policyId` suelto | Un identificador sin versión |

**Lo que cambia cada uno:**

- **OPA** — la decisión emite `policyReferences`, una lista de `{id, version}`. Sigue siendo **una
  sola** referencia, que es la semántica que OPA eligió a propósito (*«`policyId` identifica la
  política, no la lista de predicados que participaron»*): una lista de un elemento respeta esa
  semántica y satisface el schema del PEP. Los candidatos declaran `policyVersion`.
- **PDP** — ya lo tenía bien: `AccessDecision.policyReferences` es `List<PolicyReference>`.

---

## Estado de adopción

| Decisión | PDP | OPA | PEP |
|---|---|---|---|
| D-U1 forma de los hechos | ✅ `requestId` añadido | ✅ sin cambio | — |
| D-U2 vocabulario | ✅ enum ampliado | ✅ renombrado + `INDETERMINATE` | ✅ acepta cualquiera |
| D-U3 obligaciones | ✅ regla de transición escrita | ✅ sin cambio | ⚠️ **pendiente: v1.1** |
| D-U4 `policyReferences` | ✅ ya lo tenía | ✅ emite `{id, version}` | ✅ ya lo tenía |

**Lo único que queda fuera de este árbol es la implementación del cliente del PEP para D-U3.** Es
código de otra persona; el contrato ya dice qué tiene que hacer.
