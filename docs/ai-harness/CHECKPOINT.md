# Checkpoint — 2026-08-31

Estado del trabajo para retomarlo en cualquier máquina. Se actualiza al cerrar cada tramo.

---

## Dónde estamos

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Skills, herramientas, `1-planificador`, `4-validador`, plantillas | ✅ |
| 1b | Deriva doc↔código corregida y verificable · criterios realineados | ✅ |
| 1c | Skills rescatadas de la PR #24 (`sb-reactivo`, `sb-fuentes`) · `CLAUDE.md` | ✅ |
| HU-001 | Implementada: 225 pruebas, criterios 16-19 cerrados, 22/23 | ✅ |
| 2 | `2-tester-spec`, `3-implementador`, slash commands, mutation testing, `5-entrega` | ⏳ |
| 3 | Grafo nivel 1 y 2 | ⏳ |

**Lo siguiente:** decidir si se construye `2-tester-spec` con lo aprendido en HU-001, y abrir la
historia que cierre el criterio 10 (cablear la saga de compensación).

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
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1   # VERDE, 225 pruebas
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/mapa.ps1 -Check # al día
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1       # sin deriva
```

### Retomar la conversación

Las sesiones de Claude Code son **locales a cada máquina**: no se sincronizan. Pero el harness está
diseñado justo para que eso no importe — el estado está en el repositorio, no en el historial de
chat. En la máquina nueva basta con abrir el proyecto y decir:

> «Lee `docs/ai-harness/CHECKPOINT.md` y seguimos.»

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

## Lo que enseñó HU-001, la primera historia por el flujo

| Hallazgo | Ajuste |
|---|---|
| El planificador no puede materializar cambios de firma sin romper `src/test`, que tiene prohibido tocar | Distinción **[N]** / **[M]** en la SPEC |
| `drift.ps1` marcaba el propio plan | Un plan nombra por definición lo que aún no existe: `doc:…/planes/*` en el ignore |
| Publicar esqueletos solos tumbó el pipeline: el Quality Gate exige ≥ 80 % de cobertura en código nuevo y un esqueleto no tiene ninguna | El ciclo completo llega junto a la rama; los esqueletos son estado local |
| `drift.ps1` solo miraba `docs/`, así que una skill desactualizada pasaba desapercibida | Ampliado a `.claude/`: encontró dos hallazgos en las propias skills |
| Al retirar un método del puerto se rompen todos los fakes que lo doblan | Documentado en `sb-testing` como trabajo del implementador |

La fase 2 del planificador funcionó como se esperaba: descubrió que el endpoint ya existía, que
`ApplicationName.contains()` ya resolvía el filtro y que `PageWindow` estaba completo. Eso convirtió
la historia en cableado en vez de construcción, y evitó tres preguntas al usuario.

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
| `CLAUDE.md` | `CLAUDE.md` | Actualizado al flujo actual, a las herramientas del harness y al estado real de la línea base |

Se descartó `pdp-context` (464 líneas) por solaparse con `sb-arquitectura` y `sb-estandares`, que
además están verificadas contra el código.

---

## Veredicto sobre `2-tester-spec`, tras escribir las pruebas a mano

**Sí vale la pena, con un reparto distinto del que preveía el diseño.**

Lo que se vio al hacerlo a mano:

- Escribir las pruebas contra la SPEC fue **directo**: las firmas estaban completas y no hubo que
  inventar nada. Los 14 casos del mapper salieron de la tabla de resolución de ventana del plan.
  Eso es exactamente la tarea acotada y verificable que justifica un modelo estándar.
- El rojo fue limpio y significativo: `UnsupportedOperationException: pendiente: HU-001`, no un
  fallo de compilación ni una aserción mal escrita.
- Pero **la mitad del trabajo no fue escribir pruebas**: fue aplicar las firmas `[M]` y arreglar los
  cuatro fakes que dejaron de compilar. Trabajo mecánico, sin juicio, y ahí es donde un agente rinde.

### El reparto que se propone

El diseño original dejaba las `[M]` al implementador, pero eso choca con su propia prohibición de
tocar `src/test`: cambiar una firma rompe los fakes, y arreglarlos es tocar pruebas.

| Agente | Hace | Deja |
|---|---|---|
| `2-tester-spec` | Aplica las firmas **[M]** con cuerpos que lanzan · escribe las pruebas de la sección 9 · arregla los fakes que el cambio rompió | **ROJO**, y solo por `UnsupportedOperationException` |
| `3-implementador` | Rellena cuerpos en `src/main`. **No toca `src/test` jamás** | **VERDE** |

Así cada uno tiene un contrato limpio y una condición de terminado medible:
`verificar.ps1 -Prueba X` en rojo por la razón correcta para el primero, en verde para el segundo.
Y se conserva lo que hace valiosa la separación: el implementador no puede reescribir una prueba
para que pase, porque no la tiene en su contexto.

---

## Deudas conocidas

| Deuda | Dónde |
|---|---|
| **Criterio 10** — la operación compensatoria existe y ningún flujo la invoca | `docs/criteria-compliance-matrix.md`. Necesita su propia historia |
| Las herramientas son solo PowerShell | Si entra alguien en Linux/macOS, hay que portarlas |
| El agente `5-entrega` no existe | Los commits y PRs se hacen a mano, con los dos gates igualmente |
