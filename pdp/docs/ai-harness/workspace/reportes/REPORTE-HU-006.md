# Reporte de validación — HU-006

## Metadata

- **Slice:** `authorization`
- **Fecha:** 2026-09-12
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-006.md`
- **Rama:** `feature/HU-006-adaptador-opa`

## Resultado del build

> Segunda pasada, tras corregir el bloqueante de la primera (ver "Historial de esta validación"
> abajo). Salida de la corrida final.

```
ESTADO: VERDE  (mvnw clean verify, 62,4s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 471, Failures: 0, Errors: 0, Skipped: 0
```

```
CONSISTENTE: todos los slices siguen la misma forma.
Slices verificados: applications, assignments, authorization, identity, resources, roles, tenants
```

```
CLASES CITADAS QUE NO EXISTEN: 1
  PepRegistrationProperties  (pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md)

Excepciones declaradas activas: 16 (ver drift-ignore.txt)
```

`PepRegistrationProperties` es la excepción preexistente ya conocida de sesiones anteriores
(`drift.ps1` no escanea `pep/`, donde esa clase sí existe) — no la introdujo este cambio.

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 471 pruebas, 0 fallos (6 nuevas en `OpaPolicyDecisionAdapterTests`, 6 existentes de `AuthorizationHttpTests` reverificadas) |
| Cobertura (≥ 50 % por paquete) | ✅ (`jacoco-check` pasó dentro de `clean verify`) |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |

## Estado final

> ✅ APROBADO — sin bloqueantes.

## Historial de esta validación

**Primera pasada (RECHAZADO):** `drift.ps1` marcó 4 citas nuevas de `DenyByDefaultPolicyDecisionAdapter`
en `docs/PLATAFORMA.md`, `HANDOFF-INTEGRACION-PEP-OPA.md`, `INTEGRACION-PDP-PEP-OPA.md` y
`REPORTE-HU-002.md` — la clase existía cuando se escribieron esos cuatro documentos, y HU-006 la
borró (D9 del handoff, correctamente aplicado; no era un error de código). Un solo bloqueante basta
para rechazar, así que el resto de los juicios y criterios de esa pasada ya salían ✅ pero no
alcanzaba.

**Corrección aplicada por `@3-implementador`:**
- `docs/PLATAFORMA.md` — el único de los cuatro que describe **estado actual** — actualizado:
  diagrama de contenedores (§2), prosa (§3), diagrama de secuencia (§4) y la fila de HU-006 en la
  tabla de prioridades (§6), todas reemplazando `DenyByDefaultPolicyDecisionAdapter` por
  `OpaPolicyDecisionAdapter` con su comportamiento real (llama a OPA; OPA deniega porque no hay
  política de aplicación publicada, no porque el PDP no llame a nadie).
- `docs/ai-harness/drift-ignore.txt` — se retiró la excepción obsoleta de `OpaPolicyDecisionAdapter`
  (ya existe, tal como el propio plan pedía al cerrar la historia que lo crea); se añadieron
  excepciones para `HANDOFF-INTEGRACION-PEP-OPA.md` e `INTEGRACION-PDP-PEP-OPA.md`, que se
  autodeclaran diagnóstico/decisión **fechados** ("Este documento conserva el diagnóstico porque es
  la evidencia de por qué se decidió así" — cita literal de `INTEGRACION-PDP-PEP-OPA.md`); y se
  amplió la excepción de documentos históricos para cubrir todo `pdp/docs/ai-harness/workspace/reportes/*`,
  ya que un `REPORTE-{HU|HT}-*.md` de este mismo agente es un acta fechada por naturaleza — mismo
  criterio que ya aplicaba a los planes en `workspace/planes/*`.

**Segunda pasada (esta):** `drift.ps1` vuelve a mostrar solo la excepción preexistente. Los tres
juicios que ya estaban ✅ (1, 2, 4) no cambiaron — ningún archivo de `pdp/src/main` ni `pdp/src/test`
se tocó en la corrección — así que no se re-evidencian abajo con detalle nuevo, solo se confirma que
siguen vigentes.

## Bloqueantes

Ninguno.

## Observaciones menores

Mi propia primera pasada clasificó mal `INTEGRACION-PDP-PEP-OPA.md` como "documento vivo a
corregir" en el bloqueante original. Es, por su propio encabezado, un diagnóstico fechado —
la corrección correcta era una excepción en `drift-ignore.txt`, no una edición de texto, y así
quedó en la segunda pasada. Queda anotado para que quien lea el historial de esta validación no
repita el mismo diagnóstico equivocado con otro documento fechado.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | Fila 1 (PolicyDecisionPort sigue siendo el único contacto): `AuthorizeUseCaseImpl.java`/`EvaluateInternalAccessUseCaseImpl.java` no aparecen en el diff de esta historia. Fila 2 (ALLOW con policyReferences): `OpaPolicyDecisionAdapterTests.maps_an_allow_response_with_policy_references_to_an_allow_decision`. Fila 3 (DENY/NO_APPLICABLE_POLICY): `OpaPolicyDecisionAdapterTests.maps_a_deny_response_with_no_applicable_policy_to_a_deny_decision` + `AuthorizationHttpTests.denies_by_default_when_application_and_resource_are_known` (e2e, contra un `OpaFixtureServer` embebido). Fila 4 (OPA caído/timeout/inválido → INDETERMINATE/CONTEXT_UNAVAILABLE): las tres pruebas de error de `OpaPolicyDecisionAdapterTests` (500, timeout, `reasonCode` fuera del vocabulario) demuestran que el adaptador propaga el error sin decidir nada, y la conversión a `INDETERMINATE`/`CONTEXT_UNAVAILABLE` ya la cubre la prueba preexistente `AuthorizeUseCaseImplTests` (línea ~123-129, con un `PolicyDecisionPort` fake que simula "OPA unreachable") — la cadena completa queda evidenciada sin duplicar esa prueba |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Clases nuevas: `OpaPolicyDecisionAdapter`, `OpaEvaluationInput`, `OpaRequestInfo`, `OpaSubject`, `OpaTenant`, `OpaApplication`, `OpaResource`, `OpaEvaluationRequest`, `OpaResponse`, `OpaPolicyDecisionPayload`, `OpaPolicyReference`, `OpaProperties`, `OpaFixtureServer` — todas en inglés. Javadoc de las once clases de producción y de los tres mensajes nuevos de `RequiredArgumentMessages` (`OPA_WEB_CLIENT`, `OPA_PROPERTIES`, `OPA_BASE_URL`, `OPA_DECISION_PATH`, `OPA_TIMEOUT`) en español. Métodos de prueba en `snake_case` inglés descriptivo (`maps_an_allow_response_with_policy_references_to_an_allow_decision`, etc.) |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | Segunda pasada: `drift.ps1` vuelve a mostrar solo la excepción preexistente (`PepRegistrationProperties`). Ver "Historial de esta validación" para la corrección aplicada entre pasadas |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | Sin `if`/negocio en ningún use case (ninguno se tocó). El adaptador no decide: traduce — el `ALLOW`/`DENY` sale de la respuesta de OPA, no de una condición en el PDP. `OpaProperties` (`@ConfigurationProperties`) vive en `infrastructure/properties/`, no en `domain`/`application`. Cero anotaciones de Spring fuera de `infrastructure`. `allowedDependencies` de `authorization` no cambió (`package-info.java` intacto) — no hizo falta relajar ninguna frontera de Modulith porque el adaptador solo usa tipos propios del slice y de `commons`/`shared`, ya abiertos |

## Criterios de la línea base

> Solo los que el plan declaró. Los marcados 🤖 los resuelve el build, no la lectura.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | 🤖 ✅ | `LayeredArchitectureTests`/`ModulithStructureTests` en verde dentro de `clean verify` |
| 2 | Contratos de servicios | ✅ | `PolicyDecisionPort` (puerto existente) no cambió de forma; `OpaPolicyDecisionAdapter` es una implementación nueva, no un contrato nuevo |
| 3 | Reglas e integridad | ✅ (N/A) | Sin reglas de negocio nuevas — el plan lo declaró explícitamente en su sección 3 y el código lo respeta: cero clases `Rule` añadidas |
| 4 | Capacidades transversales | ✅ | `OpaPolicyDecisionAdapter(WebClient, ObjectMapper, OpaProperties, IdentifierGenerator identifiers, TimeProvider time)` — `identifiers.next()`/`time.now()` en `toDecision(...)`, cero `UUID.randomUUID()`/`Instant.now()` en línea |
| 9 | Excepciones | ✅ | El adaptador no atrapa nada (`.map`/`.timeout` sin `onErrorResume` propio); un `reasonCode` fuera del vocabulario cerrado lanza `IllegalArgumentException` desde `ReasonCode.valueOf(...)` — comportamiento ya documentado en el javadoc del enum, no nuevo — y nunca llega al cliente como error crudo: lo intercepta el `onErrorResume` general de `AuthorizeUseCaseImpl`, que responde 200 con `INDETERMINATE` |
| 11 | Interacción entre capas | ✅ | No se tocó ningún controller ni interactor; la cadena Controller→Interactor→UseCase→Puerto sigue intacta |
| 12 | SOLID | ✅ | Constructor con cinco dependencias inyectadas contra interfaces/tipos concretos mínimos (`WebClient`, `ObjectMapper`, `OpaProperties`, `IdentifierGenerator`, `TimeProvider`), cada una validada con `Objects.requireNonNull` |
| 21 | Modelo refinado | ✅ | Los doce tipos nuevos son `record`, sin Lombok; `OpaProperties` con invariantes en el constructor compacto (`Objects.requireNonNull` por componente) |
| 22 | Arquitectura reactiva | ✅ | `Mono.defer(...)` envolviendo la construcción de la petición + `.timeout(properties.timeout())`; cero `block()` en `execute(...)` |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `pdp/authorization/infrastructure/properties/OpaProperties.java` | Constructor compacto vacío (esqueleto del planificador) | El implementador añadió `Objects.requireNonNull` por componente, con tres constantes nuevas en `RequiredArgumentMessages` | Sí — mismo patrón que `SurrealDbProperties`, el único otro `@ConfigurationProperties` del proyecto con el mismo rol (cliente HTTP externo). Es exactamente la corrección que el propio `3-implementador.md` pide hacer aparte del grep de `UnsupportedOperationException`, y no cambia ninguna firma de la SPEC (sección 7) |

Sin otras desviaciones: el árbol de archivos final coincide exactamente con la sección 8 del plan
(los doce `[N]` existen y compilan con lógica real, los cuatro `[M]` se aplicaron tal cual, y
`DenyByDefaultPolicyDecisionAdapter.java` se eliminó según D9).

## Datos para la entrega

- **Mensaje de commit:** `feat(authorization): adaptador real de OPA sobre PolicyDecisionPort (HU-006)`
- **Cuerpo:** `OpaPolicyDecisionAdapter` reemplaza a `DenyByDefaultPolicyDecisionAdapter` (D9 del
  handoff): traduce `AccessRequest` al contrato `contracts/pdp-opa/v1` vía `WebClient` y la respuesta
  de vuelta a `AccessDecision`. Sin endpoint nuevo — reutiliza `POST /api/v1/authorize` y
  `POST /internal/v1/access-decisions` tal cual. Incluye la actualización de `docs/PLATAFORMA.md` y
  `docs/ai-harness/drift-ignore.txt` para que la documentación describa el adaptador vigente.
- **Rama:** `feature/HU-006-adaptador-opa`
- **Archivos a incluir:** los listados en "Desviaciones" y en el árbol de la sección 8 del plan bajo
  `pdp/src/main` y `pdp/src/test`, más `docs/PLATAFORMA.md` y `pdp/docs/ai-harness/drift-ignore.txt`
  (la corrección del bloqueante de esta validación) — no el plan ni este reporte, que se versionan
  aparte en `pdp/docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega).
