# Integración PDP ↔ PEP ↔ OPA — estado real al unir las tres ramas

> **Qué es esto.** El [`HANDOFF`](HANDOFF-INTEGRACION-PEP-OPA.md) decidió el diseño del canal interno
> con el contrato del PEP a la vista. Este documento es lo que apareció **después**, al unir de
> verdad las tres ramas en un solo árbol y comparar los tres contratos entre sí.
>
> **Fecha:** 2026-09-10 · **Rama:** `integration/pdp-pep-opa`
>
> **Estado: las cuatro desalineaciones de §3 están resueltas.** En cada una se adoptó el estándar
> del componente que la tenía mejor resuelta y se adaptaron los otros dos; las decisiones, con su
> porqué y lo que cambió cada uno, están en [`contracts/README.md`](../../../../contracts/README.md).
> Este documento conserva el diagnóstico porque es la evidencia de por qué se decidió así — la
> sección 7 resume cómo quedó.

---

## 1. Qué se unió

| Aporte | Rama | Autor | Dónde vive |
|---|---|---|---|
| PDP — HU-002 y el handoff | `feature/hu-003-endpoint-interno-pep` | Sebastián | `src/` |
| PEP — módulo y contrato v1 | `feature/pep` | David Alzate | `pep/`, `contracts/pep-pdp/v1/` |
| OPA — motor de políticas | `feature/OPA` | Laura Agudelo | `security-policy-engine/` |
| Arreglo de `mapa.ps1` | local | — | `.claude/tools/` |

**Los tres componentes no se pisan**: cada uno vive en su propio árbol de primer nivel y `pep/` tiene
su propio POM, fuera del reactor raíz. Los únicos dos archivos compartidos que tocó más de una rama
fueron `ci/variables/{dev,prod}.yml` — y ahí estaba el problema de la sección 2.

---

## 2. La regresión que el merge limpio escondía

`feature/OPA` sale de `c956be1` (2026-08-20) y arrastra `4c76234`, el rollback que desactivó el
perfil `keycloak` en DEV y PROD mientras el deploy todavía no inyectaba los secretos.

**Ese rollback nunca entró a `main`.** Lo que entró después fue lo contrario: la reactivación de
Keycloak apuntando a su hostname HTTPS real (PR #22 y #23).

Git fusionó los dos `ci/variables` **sin reportar conflicto**, porque el rollback y la reactivación
tocan líneas distintas del mismo archivo. El resultado era un híbrido:

```yaml
springProfile: 'dev'                                     # ← del rollback: Keycloak APAGADO
keycloakIssuerUri: 'https://pdp-keycloak.../security-baseline'   # ← de develop: config presente
```

DEV y PROD habrían desplegado con la autenticación apagada, con toda la configuración de Keycloak
al lado, y sin que nada fallara ruidosamente. **Resuelto** conservando la versión de `develop` en
ambos archivos; `security-policy-engine/` entró intacto.

> **Para la próxima:** una rama que sale de un punto anterior a un rollback revertido arrastra el
> rollback. `git merge` no lo ve si las líneas no coinciden. Antes de integrar una rama vieja,
> mirar su `merge-base` y revisar a mano lo que toque de `ci/`.

---

## 3. El triángulo de contratos

Al momento del diagnóstico existían **tres** definiciones de contrato, cada una con su dueño:

| Contrato | Ruta entonces | Dueño | Estado |
|---|---|---|---|
| PEP → PDP | `contracts/pep-pdp/v1/` | PEP | Versionado, con schemas y ejemplos |
| PDP → OPA | `security-policy-engine/contracts/` — dentro del productor | OPA | Versionado, con schemas y ejemplos |
| El modelo del PDP | `pdp/authorization/` | PDP | Código: `AccessRequest`, `AccessDecision`, `ReasonCode` |

Los extremos están bien documentados cada uno por su lado. **Lo que nadie había comparado es si
encajan** — y hay cuatro puntos donde no.

> El contrato PDP↔OPA vive ahora en `contracts/pdp-opa/v1/`, en el árbol compartido. La ruta de la
> tabla es la que tenía cuando se hizo este diagnóstico.

### 3.1 La entrada de OPA exige dos hechos que el PDP no tiene

`input_validation.rego` exige, y si falta uno la decisión es `DENY` / `INVALID_INPUT`:

| Exige OPA | Tiene el PDP hoy | ¿Encaja? |
|---|---|---|
| `schemaVersion == "1.0"` | — | Lo pone el adaptador |
| `request.id` | **no existe** — `AccessRequest` solo lleva `correlationId` | ⛔ |
| `subject.id` | `subject` (el claim `sub`) | ✅ |
| `subject.type` | **no existe** — no hay noción de tipo de sujeto | ⛔ |
| `subject.tenantId` | `tenantId` | ✅ |
| `tenant.id` | `tenantId` (el mismo) | ✅ |
| `application.name` | nombre configurado de la aplicación; el PDP resuelve su UUID interno | ✅ |
| `resource.type` | `resourcePath` — una **ruta**, no un tipo | ⛔ |
| `action` | `action` (`HttpVerb`) | ✅ |

Además, todos los objetos del schema son `additionalProperties: false`: el PDP no puede mandar de
más para compensar.

**Consecuencia concreta:** si HU-005 serializa el `AccessRequest` de hoy contra OPA, `validation.valid`
es falso **en toda petición** y OPA responde `DENY` / `INVALID_INPUT` siempre. No es un fallo latente
que aparezca en un caso raro: es el camino normal.

Las tres piezas que faltan no son del mismo tipo de problema:

- **`request.id`** es plumbing: el PEP ya manda `requestId` y el PDP lo tira. **HU-003 debería
  meterlo en el modelo** aunque OPA todavía no exista — si no, HU-005 tendrá que tocar el contrato
  de HU-002, que es justo lo que D4 quiso evitar.
- **`subject.type`** necesita una decisión: hoy el único sujeto es un usuario de Keycloak. Puede ser
  una constante (`"USER"`) hasta que haya sujetos de servicio, pero **que sea una constante tiene que
  estar escrito**, no inventado por el adaptador.
- **`resource.type`** es una diferencia de modelo, no un campo que falte. El PDP piensa en
  `(ruta, verbo)`; OPA piensa en `(tipo, acción)` con `resource.id` opcional. Hay que decidir si la
  ruta va como `resource.id` con un `type` sintético, o si OPA acepta la ruta. **Es acuerdo entre
  Sebastián y Laura, no una decisión de implementación.**

### 3.2 Los vocabularios de `reasonCode` no se tocan en ningún punto

| OPA emite | PDP tiene (`ReasonCode`) |
|---|---|
| `INVALID_INPUT` | `NO_APPLICABLE_POLICY` |
| `EXPLICIT_DENY` | `POLICY_DENY` |
| `POLICY_OUTPUT_INVALID` | `TENANT_MISMATCH` |
| `POLICY_AMBIGUITY` | `TOKEN_INVALID` |
| `TENANT_ISOLATION_FAILED` | `CONTEXT_UNAVAILABLE` |
| `NO_POLICY_MATCH` | |
| `POLICY_ALLOWED` | |

**Cero coincidencias literales.** Semánticamente sí hay correspondencia para tres
(`NO_POLICY_MATCH`→`NO_APPLICABLE_POLICY`, `EXPLICIT_DENY`→`POLICY_DENY`,
`TENANT_ISOLATION_FAILED`→`TENANT_MISMATCH`), pero cuatro no tienen destino:

- `POLICY_AMBIGUITY` y `POLICY_OUTPUT_INVALID` son fallos del propio motor de políticas. No son
  `DENY` de negocio; se parecen más a `INDETERMINATE` / `CONTEXT_UNAVAILABLE`. Que OPA los marque
  `DENY` y el PDP los traduzca a `INDETERMINATE` es una decisión defendible, pero hay que tomarla.
- `INVALID_INPUT` acusa un defecto del PDP, no del usuario. Merece log y alerta, no un `DENY` mudo.
- `POLICY_ALLOWED` **no tiene dónde caer**: el enum del PDP no tiene ningún código para un `ALLOW`,
  porque hasta hoy el único adaptador es `DenyByDefaultPolicyDecisionAdapter` y solo deniega.
  `AccessDecision` exige `reasonCode` no nulo, así que un `ALLOW` de OPA no se puede representar
  sin ampliar el enum.

Nótese que **el cuello de botella es el enum del PDP, no el PEP**: el schema del PEP acepta cualquier
string que case `^[A-Za-z0-9_.:-]{1,128}$`, así que los códigos de OPA le pasarían tal cual.

### 3.3 Las obligaciones chocan de frente

| | OPA | PEP v1 |
|---|---|---|
| Tipo | objetos `{type, parameters}` | **strings** |
| Valores | `AUDIT`, `MASK_FIELDS`, `REQUIRE_MFA`, `READ_ONLY`, `LOG_SECURITY_EVENT` | ninguno |
| Cardinalidad | `allow_from` añade `AUDIT` en un `ALLOW` cross-tenant | *"Only absent/null/empty supported by PEP v1. Any non-empty list fails closed"* |

Son **dos incompatibilidades a la vez**: el tipo del elemento y la cardinalidad admitida.

**El escenario concreto:** OPA concede un acceso cross-tenant con evidencia y adjunta la obligación
`AUDIT`. El PDP la reenvía. El PEP ve una lista no vacía y **falla cerrado**. Un acceso que la
política concedió termina denegado en el borde, y el motivo no aparece en ningún log de política.

Las dos partes son coherentes por separado —OPA es más ambicioso, el PEP es deliberadamente mínimo en
v1— pero **la combinación no es coherente**. Hay que elegir: o el adaptador del PDP filtra las
obligaciones mientras el PEP esté en v1 (y entonces un `ALLOW` con `AUDIT` se sirve sin auditar, que
es exactamente lo que la obligación quería evitar), o el PEP saca v2 antes de que OPA se cablee.
La segunda es la correcta; la primera solo vale si se escribe como deuda.

### 3.4 `policyReferences`: una lista contra un solo id

El PEP exige `policyReferences: [{id, version}, ...]`, ambos obligatorios. OPA devuelve un
`policyId` suelto, sin versión. La doc de OPA es explícita en que `policyId` identifica **una**
política, no la lista de predicados que participaron.

La versión tendría que salir de la revisión del bundle, que la propia doc de OPA clasifica como
*"metadato operativo, no hecho de negocio"*. Convertir un metadato operativo en parte del contrato de
auditoría es una decisión, no un detalle de mapeo.

---

## 4. Qué implica para el roadmap

| Historia | Cambio respecto a lo planeado |
|---|---|
| **HU-003** | Sigue válida tal como la dejó el handoff. **Añadir `requestId` al modelo** (§3.1): el PEP ya lo manda, OPA lo va a exigir, y meterlo después obliga a tocar el contrato de HU-002 |
| **HU-005** | Deja de ser «escribir un adaptador». Es **cerrar cuatro contratos** (§3.1–3.4), y tres de ellos son acuerdos entre dos personas, no decisiones de implementación |

**Aplicado:** `contracts/pdp-opa/v1/` ya existe con el mismo estatus que `contracts/pep-pdp/v1/`.
Los schemas se movieron desde `security-policy-engine/contracts/` al árbol compartido —una sola
copia, no un duplicado— y `scripts/validate` los valida desde ahí en cada build.

Las cuatro desalineaciones fueron consecuencia de que el contrato PDP↔OPA vivía **solo del lado de
OPA**, escrito por quien produce y no acordado con quien consume; no de que alguien se equivocara.
Por eso la regla 1 de [`contracts/README.md`](../../../../contracts/README.md) es que un contrato se
acuerda antes de implementarse.

---

## 5. Limpieza pendiente

| Qué | Dónde | Por qué |
|---|---|---|
| `ANALYSIS.md`, `comparison_report.md` | raíz del repo, de `feature/pep` | Notas de trabajo de una sesión de análisis, no entregables. La raíz del repo no es el sitio |
| `.workspace/` | local, sin versionar | Carpeta vacía con dos `.gitkeep`; no está en `.gitignore` ni en el índice |

---

## 6. Verificación de esta rama

| Comprobación | Resultado |
|---|---|
| `mvnw test-compile` | ✅ verde |
| `consistencia.ps1` | ✅ los 5 slices con la misma forma, incluido `authorization` |
| `drift.ps1` | ✅ sin deriva (14 excepciones declaradas) |
| `mapa.ps1` | ✅ 264 clases, 6 slices, 5 puertos con su implementación |

---

## 7. Cómo quedó

Las decisiones completas, con los candidatos que perdieron y por qué, están en
[`contracts/README.md`](../../../../contracts/README.md) (D-U1 a D-U4). Resumen:

| Choque | Gana | Se adaptan |
|---|---|---|
| Forma de los hechos | **OPA** — categorías de atributos, versionadas, con doctrina de evidencia | PDP: `AccessRequest` y `AccessDecision` ganan `requestId`; `subject.type`/`resource.type` quedan como constantes escritas en el contrato |
| Vocabulario de `reasonCode` | **Mecanismo del PDP** (enum cerrado) + **nombres del más preciso** caso por caso | OPA renombra 2 y gana `INDETERMINATE`; el PDP añade 5 códigos |
| Obligaciones | **OPA** — objetos `{type, parameters}` y fallo cerrado ante tipo desconocido | PEP: pendiente v1.1. PDP: regla de transición escrita |
| `policyReferences` | **PEP** — `[{id, version}]`: sin versión la traza no reproduce la decisión | OPA emite la referencia versionada; los candidatos declaran `policyVersion` |

**Verificado:** `mvnw verify` 277 pruebas ✅ · `opa test` 12 pruebas, 87,9 % de cobertura ✅ ·
`opa check --strict` ✅ · `opa fmt --fail` ✅.

### Lo que sigue abierto, y de quién es

| Qué | De quién |
|---|---|
| `pep-pdp/v1.1`: `obligations` como objetos y fallo cerrado ante **tipo** desconocido, no ante lista no vacía | David — es su componente; el contrato ya dice qué tiene que hacer |
| Implementar la regla de transición de obligaciones en el adaptador de OPA | HU-005, cuando existan obligaciones que transportar |
| `ANALYSIS.md` y `comparison_report.md` en la raíz | Decidir si se retiran (§5) |

### Un hallazgo aparte: la validación de OPA estaba rota en Windows

`opa fmt --fail`, que corre dentro de `scripts/validate`, rechazaba **siete** archivos `.rego` sin
que nadie los hubiera tocado. En git están en LF, pero con `core.autocrlf=true` —el default de
Windows— salen en CRLF al hacer checkout. Los scripts `sh` tenían el mismo problema, y ahí es peor:
un `` en el shebang hace fallar el intérprete dentro del contenedor.

En un equipo mixto Mac/Windows eso significa que la validación pasaba para unos y fallaba para
otros, sin que el archivo cambiara. Resuelto fijando `*.rego` y `security-policy-engine/scripts/**`
a `eol=lf` en `.gitattributes`, y normalizando el árbol.
