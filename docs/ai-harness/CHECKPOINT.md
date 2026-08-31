# Checkpoint — 2026-08-31

Estado del trabajo para retomarlo en cualquier máquina. Se actualiza al cerrar cada tramo.

---

## Dónde estamos

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Skills, herramientas, `1-planificador`, `4-validador`, plantillas | ✅ |
| 1b | Deriva doc↔código corregida y verificable · criterios realineados a 18/23 | ✅ |
| 1c | Skills rescatadas de la PR #24 (`sb-reactivo`, `sb-fuentes`) · `CLAUDE.md` | ✅ |
| HU-001 | Plan aprobado (gate 1) · esqueletos [N] materializados | 🟡 en curso |
| 2 | `2-tester-spec`, `3-implementador`, slash commands, mutation testing, `5-entrega` | ⏳ |
| 3 | Grafo nivel 1 y 2 | ⏳ |

**Lo siguiente:** escribir las pruebas de HU-001 en rojo, contra la SPEC de la sección 7 del plan.
Ese es el momento de decidir si se construye el agente `2-tester-spec` o se escriben a mano primero
para validar el ciclo.

---

## Retomar en otra máquina

Todo el estado vive en el repositorio: no hace falta arrastrar ninguna conversación.

```bash
git clone https://github.com/Seguridad-UCO/securityBaseline.git
cd securityBaseline
git checkout feature/harness-agentes-ia
```

Clona también los repos hermanos **al lado**, porque las skills los referencian por ruta relativa:

```
Semillero/
├── securityBaseline/
├── securityBaseline-fr/                 github.com/Seguridad-UCO/securityBaseline-fr
├── security-platform-architecture/      github.com/Seguridad-UCO/security-platform-architecture
└── artefactos-referencia/               (sin repositorio remoto — cópiala a mano)
```

### Requisitos del entorno

| Requisito | Por qué |
|---|---|
| **JDK 25** | El POM lo exige. `verificar.ps1` lo busca solo en `~/.jdks` y en `Program Files`, así que basta con tenerlo instalado aunque `JAVA_HOME` apunte a otro |
| **Docker** | `mvnw verify` levanta SurrealDB con Testcontainers |
| **PowerShell** | Las tres herramientas son `.ps1`. En Linux/macOS haría falta portarlas |
| **`gh` autenticado** | Solo para PRs |

### Comprobar que todo está sano

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1   # VERDE, 189 pruebas
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/mapa.ps1 -Check # al día
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1       # sin deriva
```

### Retomar la conversación

Las sesiones de Claude Code son **locales a cada máquina**: no se sincronizan. Pero el harness está
diseñado justo para que eso no importe — el estado está en el repositorio, no en el historial de
chat. En la máquina nueva basta con abrir el proyecto y decir:

> «Lee `docs/ai-harness/CHECKPOINT.md` y continuamos con HU-001.»

`CLAUDE.md` se carga solo al arrancar y da el contexto del proyecto; las skills y los agentes de
`.claude/` quedan registrados en cuanto arranca la sesión.

---

## Decisiones tomadas (no volver a abrirlas sin motivo)

| Decisión | Resuelto | Dónde está el porqué |
|---|---|---|
| Harness en Claude Code, no LangGraph | Un nodo de LangGraph es un prompt + herramientas + transición; eso ya es un subagente | `docs/ai-harness/README.md` §2 |
| Sin agente orquestador | Un router LLM cuesta un turno para decidir lo que el humano ya sabe | §3 |
| Sin agente que persista reportes ni `test-validator` | Un agente por artefacto; la calidad de las pruebas la mide el build | §3 |
| Validador de 4 juicios, no de 13 niveles | Lo que una prueba puede ejecutar no se razona | §3.1 |
| Grafo escalonado; nivel 0 ya | Con 226 archivos, Neo4j no resuelve un problema que tengamos | §4 |
| Alcance solo backend | El frontend tiene 6 archivos | §10 |
| Planes y reportes versionados en el repo | — | §10 |
| Modelos: opus para planificar y validar, estándar para ejecutar | El juicio está en los extremos | §10 |
| Dos gates humanos | Contrato, y salida del repositorio | §10 |
| Español en agentes y skills | — | §10 |
| `[N]` / `[M]` en la SPEC | El planificador no puede romper contratos existentes sin tocar `src/test` | `.claude/agents/1-planificador.md` |
| `Optional` como componente en `ApplicationCriteria` | La alternativa obliga a sobrescribir el accessor y desalinea `equals` | Javadoc de la clase |
| PR #24 cerrada, con rescate | Sus 6 agentes seguían un diseño ya descartado; su conocimiento de stack sí valía | Abajo |

---

## Qué se rescató de la PR #24

Aquella rama traía 6 agentes, 3 skills y un `CLAUDE.md`. Los agentes seguían el diseño que este
trabajo descartó con razones (orquestador, validador monolítico, agente de persistencia). Pero
**tres piezas tenían conocimiento real** que no estaba en ningún otro sitio, y se rescataron
verificándolas una a una contra el código:

| Pieza | Destino | Corrección al rescatarla |
|---|---|---|
| `reactive-stack` | `sb-reactivo` | Ninguna: todas sus afirmaciones se verificaron (Jackson 3, `@EventListener`, `ReactiveJwtDecoder`…) |
| `docs-reader` | `sb-fuentes` | **Sí**: describía `docs/09-artefactos`, que no existe. Los artefactos están en el repo hermano `artefactos-referencia` |
| `CLAUDE.md` | `CLAUDE.md` | Actualizado al flujo actual, a las herramientas del harness y al estado real de 18/23 |

Se descartó `pdp-context` (464 líneas) por solaparse con `sb-arquitectura` y `sb-estandares`, que
además están verificadas contra el código.

---

## Deudas conocidas

| Deuda | Dónde |
|---|---|
| Criterios 10, 16 y 17 no se cumplen; 18 y 19 parciales | `docs/criteria-compliance-matrix.md`. 16-19 los cierra HU-001; el 10 necesita su propia historia |
| No hay prueba HTTP end-to-end | La repone HU-001 |
| Las herramientas son solo PowerShell | Si entra alguien en Linux/macOS, hay que portarlas |
| `securityBaseline-fr` no tiene `.idea/` en su `.gitignore` | Ensucia el `git status` de ese repo |
| El agente `5-entrega` no existe | Los commits y PRs se hacen a mano, con los dos gates igualmente |
