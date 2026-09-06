# AGENTS.md — reglas para cualquier agente sobre este repositorio

Contrato **común a todas las herramientas**: Codex lo lee como instrucciones del proyecto, Claude
Code llega aquí desde [`CLAUDE.md`](CLAUDE.md), y cualquier otro agente debería empezar por aquí.

> **La arquitectura, las convenciones y los comandos están en [`CLAUDE.md`](CLAUDE.md).**
> Este archivo no los repite: cubre solo cómo un agente debe *trabajar* — qué puede tocar, cómo
> consultar el repositorio sin quemar contexto, y qué nunca hace sin permiso.

---

## 1. La fuente de verdad es el código

- El grafo, la documentación y estas instrucciones sirven para **localizar**; no sustituyen leer el
  archivo. Confirma todo hallazgo contra el código, los contratos y las pruebas actuales.
- No inventes endpoints, contratos, roles, permisos, políticas ni reglas de negocio que el
  repositorio no respalde.
- No des por cumplido un criterio porque una tabla lo diga. `drift.ps1` existe precisamente porque
  este proyecto ya tuvo documentación afirmando lo que el código no sostenía.

## 2. Qué nunca haces sin que te lo pidan

- **No haces `commit`, `push`, `merge` ni despliegue.** Preparas el cambio y lo reportas.
- No añades pruebas nuevas salvo petición explícita o necesidad estricta del cambio.
- No relajas un `allowedDependencies` de Modulith para que algo compile: **cambiar una frontera es
  una decisión de arquitectura y necesita su ADR** en `security-platform-architecture`.
- No ejecutas extracción semántica del grafo (documentos e imágenes): necesita un backend LLM y
  consume tokens. El grafo de código es AST puro, determinista y gratis.

## 3. Antes de cerrar un cambio

Las tres herramientas del harness son ejecutables, no opiniones. Corren sin argumentos:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1     # compila, prueba, cobertura, arquitectura
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/consistencia.ps1  # todos los slices tienen la misma forma
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1         # la doc no afirma lo que el codigo no sostiene
```

`verificar.ps1` resuelve por su cuenta el JDK que exige el POM y devuelve un resumen de cuatro
líneas en vez de cientos de líneas de log. Prefiérelo a invocar Maven directamente.

---

## 4. Graphify — el grafo del repositorio

El grafo vive en `graphify-out/` (ignorado por Git: es derivado del código y se reconstruye).

### Para qué sirve aquí, y para qué no

Medido sobre este repositorio: **1617 nodos, 5334 aristas, 337 archivos de código.**

| Úsalo para | No lo uses para |
|---|---|
| Localizar dónde vive algo sin abrir 20 archivos | Trazar la cadena de llamadas en runtime |
| **Radio de impacto** antes de refactorizar (`god-nodes`) | Sustituir a `mapa.ps1`, que es el inventario |
| Ver qué módulos tocan un concepto | Sustituir a `consistencia.ps1` ni a ArchUnit |
| Encontrar el punto de entrada de un flujo | Afirmar una relación sin confirmarla en el código |

**Límite propio de este proyecto, comprobado:** el cableado es explícito con `@Bean` en cada
`{Slice}Configuration`, y `domain`/`application` no llevan una sola anotación. Por eso el AST **no
ve la cadena runtime a través de un puerto**: `graphify path` entre un interactor y su repositorio
no encuentra camino, porque el enlace real lo hace Spring en tiempo de arranque. Es consecuencia de
la arquitectura, no un fallo del grafo — y significa que aquí el grafo es un **localizador**, no un
grafo de llamadas.

### Disciplina de consulta (obligatoria)

- **Nunca cargues `graph.json` en el contexto.** Pesa unos 4 MB. Usa siempre los subcomandos.
- **Máximo dos consultas por sesión**, con `--budget 500`. Si la respuesta llega truncada, **estrecha
  la pregunta**; no subas el presupuesto por costumbre.
- Prefiere el grafo existente. No lances `extract`, `update` ni `cluster-only` durante una consulta
  normal: solo con autorización explícita.
- Si el grafo falta o está desactualizado, **lee el código**; reconstruirlo es una decisión del
  usuario, no tuya.
- Después de consultar, abre solo los archivos focales que confirmen la respuesta.

```bash
graphify query "pregunta estrecha" --budget 500   # relaciones de un concepto
graphify explain "NombreDeClase"                  # un nodo y sus vecinos
graphify god-nodes                                # lo mas conectado = mayor radio de impacto
```

`god-nodes` es la consulta que más rinde aquí: revela que `TenantId` tiene **139 aristas** y
`ApplicationId` 87. Son el vocabulario compartido de `pdp/commons/model/`, así que tocarlos es un
refactor transversal — no local.

### Instalación (una vez por equipo)

Graphify **no se versiona en este repositorio**: es una herramienta externa que se actualiza sola, y
una copia congelada aquí envejecería en silencio. Cada quien la instala en su máquina:

```bash
pip install graphifyy                 # o: uv tool install graphifyy
graphify install --platform claude    # o codex, cursor, gemini...
graphify extract . --code-only        # primer grafo: AST, sin LLM, sin coste
graphify hook install                 # reconstruye el grafo tras cada commit
```

`--code-only` es deliberado: salta los 75 documentos y 2 imágenes del repo, que son los que
necesitarían un LLM.

---

## 5. Cómo reportas

Termina con: qué quedó, qué archivos, con qué lo validaste, y qué riesgo o pendiente queda. Si algo
no se pudo hacer, dilo explícitamente en vez de reducir el alcance en silencio.
