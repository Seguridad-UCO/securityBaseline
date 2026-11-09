# Harness de IA para securityBaseline

Plan de adopción de un flujo de desarrollo asistido por agentes: grafo de conocimiento del código,
agentes especializados, herramientas atómicas y desarrollo dirigido por contrato.

Documento de decisión. Las ocho decisiones del §10 están **tomadas** (2026-08-31) y la **fase 1 está
implementada** — ver `.claude/README.md` para el manual de uso.

---

## 1. Diagnóstico (medido, no supuesto)

| Hecho                                    | Valor                                                                                                                                                             | Consecuencia para el diseño                                                                                |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| Java de producción                       | 226 archivos · 5 976 LOC                                                                                                                                          | El código **cabe** en un mapa de texto. Un grafo en base de datos no resuelve hoy un problema que tengamos |
| Tests                                    | 60 archivos                                                                                                                                                       | Base suficiente para un ciclo rojo→verde                                                                   |
| Verificación arquitectónica ya existente | `LayeredArchitectureTests` (3 reglas ArchUnit) + `ModulithStructureTests`                                                                                         | Lo que una prueba ya ejecuta, **un agente no debe volver a razonarlo**                                     |
| Contrato de aceptación ya existente      | `docs/criteria-compliance-matrix.md` (23 criterios) + `docs/evidence/verification-guide.md` (prueba ↔ criterio)                                                   | Ya tenemos la especificación formal que exige el enfoque spec-driven. No hay que inventarla                |
| Stack                                    | Spring Boot 4.1.0 · Java 25 · WebFlux · Modulith · SurrealDB · Keycloak                                                                                           | Stack por delante del corte de conocimiento de cualquier modelo → Context7 no es un lujo, es un requisito  |
| Estructura                               | `seguridad.shared` (técnico) + `seguridad.pdp` (negocio: `applications`, `identity`, `resources`, `tenants`, `commons`)                                           | 4 slices verticales, no un monolito plano                                                                  |
| Deriva doc↔código                        | `docs/` referencia `pdp/aplicaciones`, `pdp/recursos` y `ProtectedApplicationCriteria`; el código tiene `pdp/applications`, `pdp/resources` y esa clase no existe | Ver §9. Es la falla que el harness debe impedir, no repetir                                                |

Proyecto de referencia (`arquisoft-backend@develop`): 6 agentes (`1-planificador`, `2-implementador`,
`3-tester`, `4a-validator-analyze`, `4b-validator-report`, `4c-commit`), 5 skills, 2 plantillas.
**No tiene orquestador**: el humano enruta invocando `@n-agente` y aprueba entre fases.

---

## 2. Decisión 1 — Dónde vive el harness

**Claude Code (subagentes + skills + slash commands). No LangGraph, no CrewAI — todavía.**

Un nodo de LangGraph es, en la práctica, un prompt + un conjunto de herramientas + una condición de
transición. Eso es exactamente un subagente de Claude Code, sin servidor de estado, sin checkpointer,
sin despliegue. La referencia lleva meses probando que el patrón funciona así.

LangGraph se justifica cuando el harness deje de tener un humano al teclado: un bot que revisa PRs en
CI, una demo autónoma, o un entregable del semillero que deba correr fuera del IDE. Ese día se elige
LangGraph sobre CrewAI (máquina de estados explícita y control del bucle de feedback, frente a la
abstracción de "roles" de CrewAI, que oculta justo lo que necesitamos controlar). El paso intermedio
es el Claude Agent SDK: mismos primitivos, en código.

**Regla:** los prompts de los agentes se escriben en Markdown plano y sin dependencias del runtime,
para que migrar a LangGraph sea copiar texto, no reescribir el diseño.

---

## 3. Decisión 2 — Roster de agentes

Criterio de admisión: **un agente existe solo si aporta juicio que ni un script ni una prueba pueden
dar.** Todo lo demás es un script.

### Se adoptan

| # | Agente            | Qué hace                                                                                                                                                          | Por qué se justifica                                                                                                                                                                                       |
|---|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | `1-planificador`  | Consulta el grafo, hace preguntas de clarificación, produce **PLAN + SPEC** (interfaces compilables, firmas, endpoints, criterios que aplican). No escribe lógica | Es donde se previenen las violaciones de arquitectura, en vez de detectarlas después. El agente de mayor retorno del flujo                                                                                 |
| 2 | `2-tester-spec`   | Escribe pruebas **contra la SPEC**, antes de que exista implementación. Deben fallar (rojo)                                                                       | Aislar quién escribe las pruebas de quién escribe el código es la razón real para tener multi-agente aquí: el implementador **no puede** reescribir la prueba para que pase, porque no está en su contexto |
| 3 | `3-implementador` | Escribe código hasta poner las pruebas en verde. Prohibido tocar `src/test`                                                                                       | Contrato duro. Si una prueba está mal, se detiene y reporta; no la edita                                                                                                                                   |
| 4 | `4-validador`     | Corre `./mvnw verify` y juzga **solo lo que ninguna prueba puede juzgar**                                                                                         | Ver §3.1: es un validador reducido, no el de 13 niveles de la referencia                                                                                                                                   |

### Se descartan (y por qué)

| Candidato                                         | Veredicto        | Razón                                                                                                                                                                                                                                                                                                                                                                                                |
|---------------------------------------------------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Orquestador / Router**                          | ❌ No             | Un router LLM añade un turno completo (latencia + tokens) para decidir algo que tú ya sabes cuando escribes la instrucción. La referencia enruta con `@1-planificador` y funciona. El enrutamiento correcto aquí son **slash commands** (`/plan`, `/spec-test`, `/impl`, `/verify`): deterministas, coste cero. Un orquestador se justifica cuando la entrada es una cola de tickets, no una persona |
| **`4b-validator-report`** (persistir el reporte)  | ❌ No             | Es un agente entero cuyo trabajo es escribir un archivo en disco. En la referencia tiene sentido por aislamiento de contexto; aquí el validador escribe su propio reporte. Un agente, un artefacto                                                                                                                                                                                                   |
| **`test-validator`** ("¿son buenas las pruebas?") | ❌ No como agente | Esa pregunta la responde **mutation testing (PIT) + umbral JaCoCo + lista de antipatrones**, de forma determinista y reproducible. Un agente que lee pruebas y opina es exactamente el modo de fallo que queremos evitar: caro y alucinable. Se convierte en un **check de la skill `sb-testing`** más un plugin en el build                                                                         |
| **`revisor` / code reviewer**                     | ❌ No al inicio   | Se solapa con `4-validador`. Si más adelante hace falta, ya existe `/code-review` en el propio Claude Code                                                                                                                                                                                                                                                                                           |

### Se adopta condicionado

| Candidato                     | Veredicto         | Razón                                                                                                                                                                                                                                                                                       |
|-------------------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`5-entrega`** (commit + PR) | ⚠️ Fase 2         | Su valor no es `git commit` (trivial), sino los **dos gates de confirmación** antes de acciones hacia afuera (push/PR) y el armado del cuerpo del PR con evidencia del reporte de validación. Se copia el protocolo de `4c-commit`. Se activa cuando el ciclo 1→4 esté estable              |
| **`drift` / evidencia**       | ⚠️ Primero script | El problema es real y está medido (§9), pero el 90 % es verificable con un script: ¿resuelve cada enlace de "Ubicación verificable"? ¿existe cada clase que la doc afirma? Solo el resto —"¿lo que dice el texto sigue siendo cierto?"— necesita juicio, y ese cabe dentro de `4-validador` |

### 3.1 Por qué el validador es más pequeño aquí que en la referencia

La referencia valida en 13 niveles porque su arquitectura no está verificada por el build. La nuestra
**sí lo está**: ArchUnit cubre la dirección de dependencias, Modulith cubre el mapa de módulos, JaCoCo
cubre el umbral, y los 23 criterios están mapeados prueba por prueba.

> **Regla:** cada check que una prueba pueda ejecutar, se escribe como prueba, no como prompt.

Al validador le quedan cuatro cosas, todas de juicio:

1. ¿El código cumple los **criterios de aceptación** del PLAN (no solo compila)?
2. ¿Convención de idioma? (inglés técnico / español de negocio — la propia doc admite que ninguna prueba la verifica)
3. ¿**Deriva doc↔código** introducida por este cambio?
4. ¿Altitud/cohesión: la lógica quedó en la capa correcta?

Y una acción determinista: correr `./mvnw verify` y leer el resumen.

---

## 4. Decisión 3 — El grafo, por niveles

Cada nivel debe **ganarse** el siguiente. No se salta al nivel 2 porque suene mejor.

### Nivel 0 — `PROJECT-MAP.md` generado (≈1 hora)

Un script regenera un mapa de ~200 líneas: módulos, slices, e inventario por rol
(`UseCase`, `Rule`, `Port`, `Adapter`, `Controller`, `Mapper`, VOs) con su ruta. Determinista,
versionable, diffeable.

Con 226 archivos, esto responde el 80 % de "¿qué existe y dónde va lo nuevo?" a coste casi nulo.
**Es el paso con mejor relación valor/esfuerzo de todo el documento.**

### Nivel 1 — Grafo en archivo + herramienta acotada (≈1 día)

Un script parsea `imports`, `implements`, `extends` y tipos de constructor a `graph.json`
(nodos: clase; aristas: `IMPLEMENTS`, `EXTENDS`, `USES`). Se consulta con una CLI:

```
consultar_grafo --clase TenantRepository --rel implementado-por --depth 1 --limit 5
```

Sin base de datos, sin Docker, versionable en git, y ya permite lo que `grep` hace mal:
impacto transitivo, huérfanos, "quién usa este puerto".

### Nivel 2 — jQAssistant + Neo4j + MCP (cuando duela)

`jQAssistant` escanea **bytecode** (no hay que escribir un parser) hacia Neo4j como parte del build;
`mcp-neo4j-cypher` expone la consulta al agente. Disparadores para subir a este nivel: aparecen 2+
contextos nuevos, o el nivel 1 empieza a dar respuestas incompletas. Pasos concretos en §8, Fase 3.

### La regla que hace que el grafo funcione

> **El límite lo impone la herramienta, no el prompt.**

"El prompt del agente dirá: máximo 5 nodos adyacentes" es una sugerencia que un LLM puede ignorar.
`--depth 1 --limit 5` como tope **físico** de la herramienta es una garantía. Lo mismo con Cypher:
el agente **no escribe Cypher libre**; elige una consulta con nombre de un catálogo, parametrizada y
con `LIMIT` incrustado. Esa es la diferencia entre un grafo fiable y uno que alucina.

---

## 5. Decisión 4 — Herramientas atómicas

| Herramienta               | Firma                                                                                | Devuelve                                                |
|---------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------------|
| `mapa`                    | `--refresh`                                                                          | Regenera `PROJECT-MAP.md`                               |
| `consultar_grafo`         | `--clase X --rel {usa,usado-por,implementa,implementado-por} --depth 1..2 --limit 5` | JSON acotado                                            |
| `leer_firma`              | `--clase X [--metodo m]`                                                             | Solo firmas públicas, **sin cuerpos**                   |
| `buscar_implementaciones` | `--interfaz X`                                                                       | Rutas de implementaciones                               |
| `verificar`               | `[--modulo m] [--test T]`                                                            | **Solo el resumen de fallos** de `./mvnw verify`        |
| `drift`                   | —                                                                                    | Enlaces rotos y clases afirmadas por doc que no existen |

**`verificar` es la herramienta más rentable de la lista.** El mayor sumidero de contexto en un bucle
agéntico sobre Java no es el código fuente: es la salida de Maven. Un wrapper que devuelve
"3 fallos: A, B, C + 20 líneas de cada traza" ahorra más tokens que el grafo entero.

Reglas de diseño: salida **acotada por construcción**, formato estructurado (JSON), determinista.
Una herramienta capaz de devolver 500 líneas es un defecto, no una funcionalidad.

---

## 6. Decisión 5 — Skills

Skill ≠ herramienta. La skill es **conocimiento** que se carga bajo demanda; la herramienta **ejecuta**.

Patrón que se copia de la referencia y que funciona: **una skill no pega bloques de código, apunta al
archivo real.** Un snippet dentro de una skill envejece en silencio; una ruta rota falla ruidosamente.

| Skill             | Cubre                                                                                                                  |
|-------------------|------------------------------------------------------------------------------------------------------------------------|
| `sb-arquitectura` | Slices del modulith, roles por capa, dependencias permitidas, dónde vive cada tipo de artefacto                        |
| `sb-estandares`   | Idioma, jerarquía de excepciones, catálogo de mensajes, DTOs en dos niveles, firmas reactivas (`Mono`/`Flux`), logging |
| `sb-criterios`    | Los 23 criterios como contrato de aceptación + qué prueba demuestra cada uno                                           |
| `sb-testing`      | JUnit 5 + reactor-test + Testcontainers + ArchUnit; aislamiento por capa; antipatrones; presupuesto                    |
| `sb-grafo`        | Catálogo de consultas con nombre y sus topes                                                                           |
| `sb-mcps`         | Context7 (obligatorio: Spring Boot 4.1 / Java 25), GitHub, Neo4j en nivel 2                                            |

---

## 7. Decisión 6 — El ciclo dirigido por contrato

```
  HU / requerimiento
        |
        v
  1-planificador --> PLAN-{ID}.md       (funcional: criterios de aceptación, reglas)
        |            SPEC-{ID}/          (formal: interfaces Java compilables + firmas + endpoints)
        |                                consulta el grafo · pregunta lo ambiguo
        v
  [ GATE HUMANO: ¿el contrato es correcto? ]
        |
        v
  2-tester-spec --> src/test/...         escribe contra la SPEC · NO ve implementación
        |                                ./mvnw test  ->  ROJO (obligatorio)
        v
  3-implementador --> src/main/...       escribe hasta VERDE · PROHIBIDO tocar src/test
        |                                <-- bucle de feedback con `verificar`
        v
  4-validador --> REPORTE-{ID}.md        ./mvnw verify + los 4 juicios de §3.1
        |
        v
  [ GATE HUMANO ] --> 5-entrega (fase 2): commit -> push -> PR
```

La SPEC es **código que compila**: interfaces con firmas reactivas reales lanzando
`UnsupportedOperationException`. Así el contrato existe y compila antes que la lógica, y el tester
puede compilar contra él. Es lo que convierte "spec-driven" en algo verificable en vez de en una
intención.

---

## 8. Pasos a ejecutar

### Fase 1 — Base (sin infraestructura nueva) — ✅ implementada 2026-08-31

1. ✅ `.claude/{agents,skills,tools,templates}/`.
2. ✅ Las 4 skills base (`sb-arquitectura`, `sb-estandares`, `sb-criterios`, `sb-testing`), **derivadas del código real
   **, no de `docs/`.
3. ✅ `tools/mapa.ps1` → `docs/ai-harness/PROJECT-MAP.md` (Nivel 0). 211 clases de producción, 60 de prueba, 5 slices, 4
   puertos, 12 endpoints.
4. ✅ `tools/verificar.ps1` — envoltorio de Maven. Reduce 329 líneas de log a 4, y **resuelve el JDK que exige el POM**
   por su cuenta.
5. ✅ Agentes `1-planificador` (opus) y `4-validador` (opus).
6. ✅ Plantillas `PLAN.md` (con la sección SPEC) y `REPORTE.md`.
7. ⏳ **Probar el flujo en una HU pequeña real** antes de escribir un agente más.

Línea base al cerrar la fase: `./mvnw test` en **verde, 189 pruebas**.

### Fase 2 — Ciclo TDD agéntico

8. Agentes `2-tester-spec` y `3-implementador` con los contratos duros de §3.
9. Slash commands `/plan`, `/spec-test`, `/impl`, `/verify`.
10. Añadir PIT (mutation testing) al build y fijar umbrales.
11. `tools/drift` + agente `5-entrega`.

### Fase 3 — Grafo Nivel 1 → 2

12. `tools/consultar_grafo` sobre `graph.json` (Nivel 1) + skill `sb-grafo`.
13. Cuando se cumpla el disparador de §4:
    - Levantar Neo4j: `docker run -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/<pass> neo4j`
    - Añadir el plugin de jQAssistant al `pom.xml` **en un perfil `graph`** (nunca en el build por defecto: no puede
      frenar CI)
    - `./mvnw -Pgraph verify` → escanea `target/classes` hacia Neo4j
    - Instalar el MCP `mcp-neo4j-cypher` con credenciales **de solo lectura**
    - **Paso que casi todos se saltan:** una pasada Cypher de "conceptos" que etiquete los nodos con *nuestro*
      vocabulario (`:UseCase`, `:Port`, `:Adapter`, `:Rule`, `:Slice`). Sin eso hay un grafo de clases Java genérico, no
      un grafo de arquitectura
    - Reescribir el catálogo de consultas de `sb-grafo` en Cypher parametrizado

---

## 8b. Fase 1b — Deriva corregida (2026-08-31)

La auditoría con `tools/drift.ps1` encontró **181 hallazgos**, de dos clases muy distintas:

| Clase                                                                    | Cantidad     | Qué se hizo                                                                                                                                                                                                                        |
|--------------------------------------------------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Renombrados** — la funcionalidad existe, la doc apunta a la ruta vieja | ~107 enlaces | Sustitución mecánica: `aplicaciones`→`applications`, `recursos`→`applications`/`resources`, `RegisterProtectedApplication*`→`RegisterApplication*`, `crosscutting/messages`→`pdp/commons/message`, `shared/rule`→`shared/contract` |
| **Ausentes** — la funcionalidad nunca existió o se desconectó            | 5 criterios  | Se declaró el estado real. Ver abajo                                                                                                                                                                                               |

Resultado: **cero enlaces rotos, cero clases citadas inexistentes**, con 10 excepciones declaradas
y justificadas en `drift-ignore.txt` (documentos históricos y pendientes de HU-001).

### Los criterios que no se cumplían

`docs/criteria-compliance-matrix.md` declaraba **23 de 23**. El estado real es **18 de 23**:

| #  | Criterio               | Estado      | Causa                                                                                         |
|----|------------------------|-------------|-----------------------------------------------------------------------------------------------|
| 10 | Transacciones          | ⛔ No cumple | `RemoveApplicationUseCase` existe como operación compensatoria y **nadie la invoca**          |
| 16 | Repositorios dinámicos | ⛔ No cumple | Ningún puerto expone `findBy(criteria, window)`                                               |
| 17 | Consultas dinámicas    | ⛔ No cumple | `ProtectedApplicationCriteria` nunca se creó                                                  |
| 18 | Paginación             | ⚠️ Parcial  | `PageWindow`/`ResultPage`/`PageResponse` existen y están probados, pero **son código muerto** |
| 19 | Rangos                 | ⚠️ Parcial  | La lógica existe y está probada, pero ningún endpoint la expone                               |

Además, de las 15 pruebas que `evidence/verification-guide.md` mapeaba a criterios, **9 no existían**;
la tabla se reconstruyó contra las pruebas reales.

Esto no es un descuido menor: es **la recaída exacta del hallazgo transversal de la auditoría de
agosto** ("la evidencia no correspondía al código"), reintroducida por la refactorización que
renombró los paquetes. La diferencia es que ahora la comprobación es ejecutable en vez de manual.

### La primera prueba del harness arregla el hallazgo que el harness encontró

`docs/ai-harness/workspace/HU-001.md` — consulta de aplicaciones con filtros y paginación, más la
prueba HTTP end-to-end. Cierra los criterios 16 a 19 y repone la evidencia de 5, 6, 9 y 22.

## 9. Hallazgo: deriva entre documentación y código

Verificado hoy sobre `main`:

| La doc dice                                                                                        | El código tiene                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pdp/aplicaciones`, `pdp/recursos` (`naming-conventions.md`, "Ubicación verificable")              | `pdp/applications`, `pdp/resources`                                                                                                                                                                                                                                                                                                                                                                                              |
| `ProtectedApplicationCriteria` como clase existente (`criteria-compliance-matrix.md`, criterio 15) | No existe en `src/main`                                                                                                                                                                                                                                                                                                                                                                                                          |
| 15 pruebas mapeadas a criterios (`evidence/verification-guide.md`)                                 | **9 de esas 15 no existen con ese nombre**: `ProtectedResourceDomainTests`, `ProtectedResourceRuleTests`, `RegisterProtectedApplicationUseCaseImplTests`, `SearchProtectedApplicationsUseCaseImplTests`, `RegisterProtectedApplicationRequestMapperTests`, `SearchProtectedApplicationsRequestMapperTests`, `ProtectedApplicationControllerMappingTests`, `ProtectedApplicationHttpTests`, `ProtectedResourceAuditListenerTests` |

No es anecdótico: el propio `criteria-compliance-matrix.md` documenta que el hallazgo transversal de
la auditoría de agosto fue *"la evidencia no correspondía al código"*. La deriva volvió tras el
renombrado a inglés.

**Implicación directa para el harness:** las skills **no** pueden derivarse de `docs/`. Se derivan del
código y citan a `docs/` como contexto. Y `tools/drift` deja de ser un adorno: es la prueba de
regresión de esta falla concreta.

---

## 10. Decisiones tomadas (2026-08-31)

| # | Decisión                | Resuelto                                                                                                                |
|---|-------------------------|-------------------------------------------------------------------------------------------------------------------------|
| 1 | Alcance                 | **Solo backend.** El frontend tiene 6 archivos; no hay patrón que extraer todavía                                       |
| 2 | Planes y reportes       | Se versionan en el repo, bajo `docs/ai-harness/workspace/{planes,reportes}/`                                            |
| 3 | Origen de las historias | Las dicta el usuario, o vienen de `security-platform-architecture` / `artefactos-referencia` por ruta local. Sin `gh`   |
| 4 | Modelo por agente       | `1-planificador` y `4-validador`: **opus** (juicio). `2-tester-spec` y `3-implementador`: **sonnet** (contrato acotado) |
| 5 | Gates                   | Dos gates humanos: tras el contrato y antes de salir del repositorio. Relajar solo con confianza medida                 |
| 6 | Cobertura y mutación    | Se mantiene el umbral vigente de JaCoCo (**50 % de líneas por paquete**). PIT en fase 2, primero en modo reporte        |
| 7 | Grafo                   | Nivel 0 ya (`PROJECT-MAP.md`). Nivel 1 en fase 3. Nivel 2 solo con el disparador cumplido                               |
| 8 | Idioma                  | Español en agentes, skills y plantillas                                                                                 |

## 11. Hallazgo de entorno

El POM exige **Java 25** y el `JAVA_HOME` de la máquina apunta a **Java 21**, así que `./mvnw` falla
con `release version 25 not supported` aunque el código esté bien. Hay un `temurin-25.0.4` instalado
en `~/.jdks`.

`tools/verificar.ps1` **resuelve el JDK correcto por su cuenta**: lee `<java.version>` del POM, busca
un JDK que la cumpla y lo usa. Se absorbe en la herramienta a propósito — si no, cada agente gasta un
ciclo diagnosticando el entorno en vez del código.
