# Harness de IA — cómo se usa

Flujo de desarrollo asistido por agentes. El diseño y su justificación están en
[`docs/ai-harness/README.md`](../pdp/docs/ai-harness/README.md).

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Skills, herramientas (`mapa`, `verificar`, `drift`, `consistencia`), agentes 1-4, plantillas | ✅ Listo |
| 1b | Deriva doc↔código corregida y verificable · criterios en 22/23 | ✅ Listo |
| 1c | Skills rescatadas de la PR #24 (`sb-reactivo`, `sb-fuentes`) · `CLAUDE.md` | ✅ Listo |
| 1d | Consistencia arquitectónica: `consistencia.ps1` + 9 divergencias corregidas | ✅ Listo |
| 1e | Capa `application` aplanada (`primaryport`/`secondaryport`) · resiliencia de arranque · DEV saludable | ✅ Listo |
| Fase A | Reglas de negocio a `domain/{slice}/rule/`, puras y síncronas · `domain/` por categoría | ✅ Listo |
| HU-001 | Búsqueda con criterios y paginación + prueba HTTP end-to-end. Cierra 16-19 | ✅ Listo |
| HU-002 | `POST /api/v1/authorize`, canal BFF, denegación por defecto (277 pruebas) | ✅ Listo |
| 2 | Los cuatro agentes existen. **`1-planificador` y `4-validador` probados en HU-001; `2-tester-spec` y `3-implementador` nunca se han ejecutado** | 🟡 Parcial |
| 3 | Grafo del repositorio adoptado — ver [`docs/ai-harness/GRAFO.md`](../pdp/docs/ai-harness/GRAFO.md) | ✅ Listo |
| 4 | Slash commands, mutation testing y `5-entrega` | ⏳ Pendiente |

**Próximo paso:** **HU-003** — endpoint interno `POST /internal/v1/access-decisions` para el PEP.
Las decisiones de diseño, el contrato y las trampas verificadas están en
[`workspace/HANDOFF-INTEGRACION-PEP-OPA.md`](../pdp/docs/ai-harness/workspace/HANDOFF-INTEGRACION-PEP-OPA.md);
el orden de las historias, en [`workspace/ROADMAP-PDP.md`](../pdp/docs/ai-harness/workspace/ROADMAP-PDP.md).

---

## El ciclo, hoy

```
  workspace/HU-{ID}.md          la historia, escrita por un humano
     │
     ▼
  @1-planificador  →  PLAN-{ID}.md  +  esqueletos de la SPEC (compilan, sin lógica)
     │
     ▼
  [ GATE 1 — humano: ¿el contrato es correcto? ]     ← el gate que más ahorra
     │
     ▼
  @2-tester-spec   →  pruebas que fallan (rojo)      No toca src/main
     │
     ▼
  @3-implementador →  las hace pasar (verde)         No toca src/test. Nunca
     │
     ▼
  @4-validador     →  REPORTE-{ID}.md
     │
     ▼
  [ GATE 2 — humano: antes de que salga del repositorio ]
```

**Por qué los dos gates.** El primero es donde el humano corrige barato: cambiar un contrato en el
plan cuesta una frase; cambiarlo con veinte archivos escritos cuesta una tarde. El segundo existe
porque ningún agente aprueba su propio trabajo.

## Invocar los agentes

Uno por mensaje, en orden, esperando a que termine cada uno:

```
@1-planificador planifica HU-002
@2-tester-spec  escribe las pruebas de HU-002
@3-implementador implementa HU-002
@4-validador    valida HU-002
```

El agente lee `pdp/docs/ai-harness/workspace/HU-{ID}.md` por su cuenta: no hace falta pegar la historia
en el mensaje. Si la historia no existe todavía, escríbela antes — el planificador no la inventa.

### La separación que hace que esto funcione

| Agente | Puede tocar | Nunca toca |
|---|---|---|
| `1-planificador` | El plan y esqueletos que compilan sin lógica | Lógica real |
| `2-tester-spec` | `pdp/src/test` | `pdp/src/main` |
| `3-implementador` | `pdp/src/main` | `pdp/src/test` — **ni una línea** |
| `4-validador` | Nada: solo lee y ejecuta | Todo |

Que el implementador no pueda tocar las pruebas es lo que impide el fallo clásico del TDD agéntico:
ablandar la prueba hasta que pase. Si cree que una prueba está mal, lo reporta y para.

---

## Herramientas

Todas asumen que las ejecutas desde la raíz del repositorio, y operan sobre `pdp/` — el harness es especifico del PDP, no de `pep/` ni de `security-policy-engine/`.

**Mapa del proyecto** — inventario por slice y por rol, puertos, endpoints y pruebas.
Es el nivel 0 del grafo de conocimiento: se consulta antes de leer código.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/mapa.ps1
```

`-Check` sale con código 1 si el mapa quedó desactualizado respecto al código (útil en CI).

**Verificación** — corre Maven y devuelve solo el resumen, no las cientos de líneas del log.
Selecciona por su cuenta el JDK que exige el POM, aunque `JAVA_HOME` apunte a otro.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1
```

| Flag | Efecto |
|---|---|
| *(ninguno)* | `mvnw verify` — compila, prueba, cobertura, arquitectura |
| `-Rapido` | `mvnw test` — sin cobertura |
| `-Compilar` | `mvnw test-compile` — solo compila |
| `-Prueba X` | Una clase de prueba concreta |
| `-Lineas N` | Más contexto por fallo (por defecto 12) |

El log completo queda en `pdp/target/verificar-ultimo.log` (ignorado por git).

**Consistencia arquitectónica** — comprueba que todos los slices tengan la misma forma. ArchUnit
verifica la *dirección* de las dependencias y Modulith el mapa entre módulos; ninguno comprueba que
un slice se parezca al de al lado, y ahí es donde uno se pierde.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/consistencia.ps1
```

`-Estricto` sale con código 1 si hay hallazgos. Una divergencia legítima —una capacidad que ese
slice no necesita— se declara en `pdp/docs/ai-harness/consistencia-ignore.txt` con su razón.

**Deriva doc↔código** — comprueba que todo enlace de `pdp/docs/`, de la raíz `docs/` y de `.claude/` resuelva y que toda
clase citada exista.
Es la prueba de regresión del fallo histórico del proyecto: documentación que afirma lo que el código
no sostiene.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1
```

`-Estricto` sale con código 1 si hay hallazgos. Las excepciones se declaran en
`pdp/docs/ai-harness/drift-ignore.txt`, **solo** para lo que está pendiente a propósito y marcado como
tal en el documento que lo menciona.

---

## Skills

Se cargan solas cuando un agente las invoca. También puedes pedirlas por nombre.

| Skill | Cubre |
|---|---|
| `sb-arquitectura` | Slices, capas, contratos base, dónde vive cada archivo |
| `sb-estandares` | Idioma, value objects, excepciones, mensajes, DTOs, cableado |
| `sb-reactivo` | Trampas de Boot 4 / Jackson 3 / Java 25, operadores Reactor, Modulith, StepVerifier |
| `sb-testing` | JUnit + AssertJ + StepVerifier, fakes en vez de Mockito, antipatrones |
| `sb-criterios` | Los 23 criterios como contrato de aceptación, y cuáles no se cumplen |
| `sb-fuentes` | Qué hay en los repos hermanos y en qué orden consultarlo al planificar |

**Las skills se derivan del código, no de `docs/`.** Cuando cambies una convención en el código,
actualiza la skill en el mismo cambio — si no, los agentes multiplicarán la versión vieja.

---

## Artefactos generados

| Ruta | Qué es | Se versiona |
|---|---|---|
| `pdp/docs/ai-harness/PROJECT-MAP.md` | Mapa generado. **No editar a mano** | Sí |
| `pdp/docs/ai-harness/workspace/planes/` | Contratos por historia | Sí |
| `pdp/docs/ai-harness/workspace/reportes/` | Reportes de validación | Sí |
| `pdp/target/verificar-ultimo.log` | Log completo del último build | No |
