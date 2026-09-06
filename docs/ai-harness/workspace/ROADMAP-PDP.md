# Roadmap del PDP — qué sigue y cómo se reparte con el equipo

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

| # | Historia | Por qué en ese lugar | Desbloquea |
|---|---|---|---|
| **HU-002** | Endpoint de decisión con el contrato completo, denegando por defecto | El PEP y OPA no pueden empezar sin el contrato **corriendo**. No decide nada real todavía: eso es deliberado | PEP y OPA, los dos |
| **HU-003** | Roles y asignaciones vigentes por inquilino (BC-04 + BC-08) | Es el **valor propio del PDP**: sin esto OPA no tiene atributos que evaluar. El trozo más grande y 100 % nuestro | HU-004 |
| **HU-004** | Adaptador real de OPA sobre `PolicyDecisionPort` | Sustituye la denegación por defecto por la decisión real | Cierra el flujo ALLOW |
| **HU-005** | `EventoAcceso` correlacionado hacia Auditoría | INV-AUD-01. Sin evidencia no hay cumplimiento | Auditoría |
| **HU-006** | Perfiles como agrupación de roles (BC-05) | Comodidad administrativa, no bloquea la decisión | — |
| **HU-007** | Criterio 10: cablear la saga de compensación | Único criterio abierto de la línea base, pero no bloquea a nadie del equipo | — |

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
