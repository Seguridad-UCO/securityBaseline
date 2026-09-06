# El grafo del repositorio: qué aporta y qué no

Evaluación de **Graphify** sobre `securityBaseline`, con el grafo construido y consultado, no con
lo que promete su documentación. Es el punto 1 del diseño original del harness —el GraphRAG— que
quedó pendiente desde la fase 1.

> **Veredicto: se adopta, pero como localizador y análisis de impacto — no como grafo de llamadas
> ni como fuente de verdad arquitectónica.** El porqué está medido abajo.

---

## 1. Qué es

`graphify` es una herramienta externa (`pip install graphifyy`) que convierte una carpeta en un
grafo de conocimiento consultable. Sobre código usa **AST con tree-sitter**: determinista, sin LLM,
sin clave de API y sin coste. Solo la extracción de documentos e imágenes necesita un backend LLM
— y por eso aquí se usa siempre `--code-only`.

Instala `tree-sitter-java` (nuestro backend), `tree-sitter-powershell` (las herramientas del
harness) y `tree-sitter-typescript` (el frontend `securityBaseline-fr`, si algún día se une al
mismo grafo).

## 2. Lo que mide sobre este repositorio

```
337 archivos de código  →  1617 nodos, 5334 aristas, 103 comunidades
graph.json: 4,0 MB      →  jamás entra en contexto; se consulta con subcomandos
```

Construir el grafo completo tarda segundos. Se reconstruye solo tras cada commit si se instala el
hook (`graphify hook install`).

## 3. Dónde sí aporta

### Radio de impacto — la consulta que más rinde

`graphify god-nodes` ordena los nodos por conectividad:

| Nodo | Aristas |
|---|---:|
| `TenantId` | 139 |
| `ApplicationId` | 87 |
| `RequiredArgumentMessages` | 67 |
| `SurrealDbClient` | 52 |
| `ValueObjectMessages` | 44 |
| `UserId` | 44 |
| `ApplicationRepository` | 42 |

Esto **confirma con una consulta** lo que la reorganización de `domain/` descubrió a mano contando
imports uno por uno: mover `TenantId` toca 74 archivos. Cualquier cambio en `pdp/commons/model/` es
transversal por definición, no local.

También es una validación de la arquitectura: los nodos más conectados son el vocabulario
compartido y los catálogos de mensajes — **no hay ninguna clase de negocio actuando como god
class**, que es exactamente lo que se espera de un modelo bien factorizado.

### Localizar sin abrir archivos

`graphify query "concepto" --budget 500` devuelve los nodos y sus rutas con línea. En un repo de
337 archivos, encontrar el punto de entrada de un flujo sin cargar veinte archivos al contexto es
la ganancia diaria.

## 4. Dónde NO aporta — el límite propio de esta arquitectura

**`graphify path` entre un interactor y su repositorio no encuentra camino.** Comprobado:

```
$ graphify path "RegisterApplicationInteractorImpl" "SurrealApplicationRepository"
No directed path found.
```

No es un fallo de la herramienta: es consecuencia directa de nuestro diseño. El cableado es
explícito con `@Bean` en cada `{Slice}Configuration`, y `domain`/`application` no llevan ni una
anotación de Spring. El AST ve que el caso de uso depende de la **interfaz** `ApplicationRepository`
y que existe un `SurrealApplicationRepository` que la implementa, pero **quien los une es Spring en
tiempo de arranque** — y eso no está en el código como una llamada.

> La misma decisión que hace la arquitectura testeable sin mocks —cableado explícito, cero
> anotaciones en el núcleo— es la que impide que un grafo AST reconstruya la cadena runtime.
> Aceptar eso es más honesto que forzar la herramienta a un uso que no puede sostener.

## 5. Qué NO sustituye

El harness ya tenía tres herramientas ejecutables, y el grafo no reemplaza a ninguna:

| Herramienta | Pregunta que responde | ¿La sustituye el grafo? |
|---|---|---|
| `mapa.ps1` | ¿Qué clases existen y en qué capa? (inventario) | **No.** El grafo da relaciones, no censo |
| `consistencia.ps1` | ¿Todos los slices tienen la misma forma? | **No.** El grafo no conoce la convención |
| `drift.ps1` | ¿La documentación afirma algo que el código no sostiene? | **No.** |
| ArchUnit / Modulith | ¿La dirección de dependencias es legal? | **No.** Eso rompe el build; el grafo no rompe nada |

El grafo **describe**; las herramientas **verifican**. Un grafo nunca falla un build.

## 6. La razón estratégica: un solo estándar para dos herramientas

El equipo es heterogéneo — Claude Code y Codex — y hasta ahora cada uno leía instrucciones
distintas. [`AGENTS.md`](../../AGENTS.md) es el contrato común: Codex lo lee como sus instrucciones
de proyecto, y `CLAUDE.md` apunta a él en vez de repetirlo, para que no puedan divergir.

Esa es la aportación más valiosa del paquete `SecurityBaseline-AI-Compartible`, por encima del
grafo en sí.

## 7. Decisiones tomadas al adoptarlo

| Decisión | Por qué |
|---|---|
| **El skill de graphify no se versiona en el repo** | Es una herramienta externa que se actualiza sola. El paquete traía la 0.9.48 y ya iba por la 0.9.55: una copia congelada envejece en silencio. Cada quien corre `graphify install --platform <tool>` |
| **`graphify-out/` va al `.gitignore` versionado**, no a `.git/info/exclude` | El paquete lo excluía en local, lo que obliga a cada miembro a correr un instalador. En el `.gitignore` queda ignorado para todos sin hacer nada |
| **`AGENTS.md` se versiona** | El paquete lo mantenía local y con rutas absolutas del Mac de su autor (`/Users/davidalzate/...`), que no funcionan en otra máquina. Reescrito con rutas relativas y versionado, es un estándar de equipo en vez de una preferencia personal |
| **No se copió el `.claude/` del paquete** | Era un snapshot anterior: 2 agentes en vez de 4, sin `consistencia.ps1` y con las skills previas a las reglas puras en el dominio. Copiarlo habría sido una regresión |
| **`drift.ps1` ahora vigila `CLAUDE.md` y `AGENTS.md`** | Estaban en la raíz, fuera de las carpetas escaneadas. `CLAUDE.md` llegó a afirmar 18/23 criterios cuando ya eran 22/23, y nada lo detectaba |
| **Corregido un fallo silencioso en `drift.ps1`** | Al probar la ampliación apareció: con **exactamente un** hallazgo, `Sort-Object ... -Unique` devuelve un escalar en vez de un array, y `.Count` sobre un `PSCustomObject` suelto es `$null` en PowerShell 5.1 — el total daba 0 y el detector reportaba "SIN DERIVA" teniendo una clase ausente. Nunca se vio porque la deriva fue 22 → 2 → 3 → 0 y jamás pasó por 1. Arreglado envolviendo en `@(...)`, y verificado con el caso de un solo hallazgo |
| **`__MACOSX/` y `._*` al `.gitignore`** | Equipo mixto Mac/Windows: esa carpeta apareció al descomprimir el paquete en Windows. Que no vuelva a colarse |

## 8. Cómo se usa a diario

Las reglas operativas —presupuesto, número de consultas, qué no reconstruir— viven en
[`AGENTS.md`](../../AGENTS.md), sección 4, para no duplicarlas aquí.

En una frase: **dos consultas por sesión como máximo, `--budget 500`, y lo que el grafo diga se
confirma abriendo el archivo.**
