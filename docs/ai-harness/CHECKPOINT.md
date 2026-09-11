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
| 1d | Consistencia arquitectónica: `consistencia.ps1` + 9 divergencias corregidas | ✅ |
| 1e | Capa `application` aplanada · resiliencia de arranque · DEV saludable | ✅ |
| Fase A | Reglas de negocio movidas a `domain/{slice}/rule/`, puras y síncronas · `domain/` reorganizado por categoría | ✅ |
| 2 | `2-tester-spec` y `3-implementador` ✅ · slash commands, mutation testing, `5-entrega` ⏳ | 🟡 |
| 3 | Grafo nivel 1 y 2 | ⏳ |

**Lo siguiente:** estrenar `2-tester-spec` y `3-implementador` con la historia que cierre el
criterio 10 (cablear la saga de compensación), que es la única deuda de la línea base.

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

## La consistencia, verificada (2026-08-31)

`ArchUnit` comprueba la **dirección** de las dependencias y Modulith el **mapa** entre módulos.
Ninguno comprueba que un slice tenga la misma **forma** que los demás: se puede resolver el mismo
problema de tres maneras distintas sin romper una sola regla de capas. Eso es lo que hacía que
entrar en un módulo no se pareciera a entrar en el de al lado.

`.claude/tools/consistencia.ps1` lo convierte en comprobación ejecutable. La primera pasada encontró
**9 divergencias reales**, todas corregidas:

| Slice | Divergencia | Corrección |
|---|---|---|
| `applications`, `identity` | El adaptador construía el agregado directamente desde el JSON, mientras `tenants` y `resources` pasaban por `Entity` + `Mapper` | `ApplicationEntity`, `SecurityUserEntity`, `ExternalIdentityEntity` y sus mappers |
| `identity` | Sin `application/message`: el texto vivía como literal dentro de la excepción | `IdentityMessages` |
| `identity` | `AssignTenantUseCaseImpl` hacía «busca o lanza» inline; en `tenants` eso es una `Rule` | `UserMustExistRule` |
| `resources` | Consultaba el repositorio de `applications` y lanzaba su excepción inline | `applications` publica `ApplicationMustExistForTenantValidator` y `resources` lo consume |
| `identity` | `toUser` / `toIdentity` frente a `toDomain` en el resto | `toSecurityUser` / `toExternalIdentity` |

El caso de `resources` **estrecha** la frontera de Modulith en vez de ampliarla: pasa de necesitar
`applications :: repository` a `applications :: rule`. Es el mismo patrón con el que `tenants`
publica `TenantMustBeActiveValidator` — una implementación inyectada, no una comprobación copiada.

Evidencia tomada del proyecto de referencia, como se pidió: en `arquisoft-backend@develop` **todos**
los contextos usan `entity` + `mapper` + `repository` juntos, sin excepción. Y los ADRs 008, 009 y
016 del repo de arquitectura respaldan las tres decisiones.

---

## El despliegue a DEV, y por qué fallaba (2026-08-31)

El pipeline daba **503 durante 30 intentos** y su mensaje culpaba a SurrealDB. Eran **dos causas
distintas, ninguna en el código, y ninguna era la que el mensaje señalaba**:

1. **Keycloak apagado.** `vm-pdp-keycloak-shared` estaba *deallocated* — se apaga a propósito para
   ahorrar crédito, y el propio `ci/variables/dev.yml` ya lo documentaba. Spring resuelve el issuer
   OIDC al construir el contexto, así que sin Keycloak la aplicación no llega a arrancar.
2. **SurrealDB colgado.** La VM estaba *running* y el NSG permitía las 32 IPs de salida del App
   Service, pero el contenedor llevaba **20 días «Up» y `unhealthy`**: el puerto publicado, y el
   proceso sin responder ni a `localhost`. Su último log era del 11 de agosto.

Encendida la VM y reiniciado el contenedor —los datos son `rocksdb` sobre volumen, no se pierden—,
**DEV responde `status: UP`**.

### Lo que se cambió para que no vuelva a ser mudo

| Cambio | Por qué |
|---|---|
| `SurrealSchemaInitializer` | Los cuatro inicializadores hacían `.block()` **sin plazo** en un `ApplicationRunner`. Una base que no responde dejaba el arranque colgado. Ahora se espera 15 s y un fallo se registra en vez de tumbar el contexto |
| `SurrealDbHealthIndicator` | No existía: `/actuator/health` no podía decir que la base estaba caída. Sigue dando `DOWN` —un deploy contra una base caída debe fallar— pero ahora **dice por qué** |
| Mensaje del pipeline | Nombra a Keycloak como causa más probable, da el comando para encenderla, y vuelca el cuerpo de `/actuator/health` antes de salir |

**Rutina de entorno:** antes de un deploy a DEV, `az vm start -g rg-pdp-shared-v1 -n vm-pdp-keycloak-shared`.

---

## La capa `application`, aplanada (2026-08-31)

Tenía **11 paquetes para 23 archivos** y profundidad de 5 (`port/primary/dto/request`), con
`rulesvalidator` colgando suelto al lado de `rule`. Se tomó el empaquetado del proyecto de
referencia, que agrupa por dirección del puerto:

| Antes | Ahora |
|---|---|
| `application/port/primary/dto/request` | `application/primaryport/request` |
| `application/port/primary/dto/response` | `application/primaryport/response` |
| `application/port/secondary/repository` | `application/secondaryport/repository` |
| `application/rulesvalidator` | `application/rule/validator` |

La ruta de un DTO de entrada pasa de cinco segmentos a tres, y el coordinador de reglas queda
dentro de lo que coordina. Los nombres de las interfaces nombradas de Modulith (`dto`, `repository`,
`rule`) **no cambian**, así que ningún `allowedDependencies` se toca.

95 archivos con `package`/`import` reescritos, 234 pruebas en verde, Modulith y ArchUnit intactos.

---

## Las reglas de negocio, movidas al dominio — Fase A (2026-08-31)

`docs/ai-harness/ESTUDIO-ARQUITECTURA.md` preguntaba si la arquitectura estaba «muy regada» tras
compararla con `arquisoft-backend@develop`. Diagnóstico: no estaba mal, pero las 8 `Rule` hacían I/O
(inyectaban el repositorio) y por eso no podían vivir en `domain`, que no conoce puertos ni Reactor.

**Lo que se hizo:** cada regla se partió en dos piezas, una por capa —

```
domain/{slice}/rule/ + impl/          decisión pura y síncrona, sin puertos ni Reactor
application/{slice}/rule/validator/   hace la E/S (el finder) y aplica la regla
```

Con eso, `domain/` se reorganizó por categoría: `exception/`, `message/`, `event/`, `model/` (value
objects) y `rule/` + `rule/model/` (el hecho ya resuelto que cada regla recibe). El agregado y el
`Criteria` se quedan en la raíz de `domain/`.

**Resultado:** 8 reglas, todas en `domain`, ninguna conoce ya un repositorio. `./mvnw verify` en
**247 pruebas** (antes 234), `consistencia.ps1` y `drift.ps1` en verde. Lo que se publica entre
módulos sigue siendo el *validador* (`TenantMustBeActiveValidator`,
`ApplicationMustExistForTenantValidator`), no la regla pura — un consumidor no tiene el repositorio
ajeno para alimentarla. Ningún `allowedDependencies` de Modulith cambió.

**Lo que quedó fuera del plan original, y por qué:** no se creó `Finder<I,O>` como contrato nuevo
(los contratos reactivos existentes ya tienen esa forma) ni una capa `Finder` separada (aquí el
validador puede consultar directamente porque es reactivo; en la referencia hace falta porque su
validador es puro). El detalle completo, con las tres desviaciones razonadas, está en el §6
(«Registro de ejecución») del ESTUDIO.

**En el repo hermano:** `security-platform-architecture`, rama `docs/repository-structure`, ya tiene
los dos commits que hacían falta — `f61df32` refleja el aplanado de `application` (1e) y `6a522cf`
la Fase A (`domain/rule`, `domain/model`, `application/rule/validator`, y la regla 9: una `Rule` de
`domain/rule/impl` no importa Reactor ni un `secondaryport`). Pendiente de PR: la PR #2 de esa rama
ya se mergeó, así que estos dos commits necesitan una PR nueva.

### `mapa.ps1`, corregido para el layout nuevo (2026-08-31)

La tabla de roles de `.claude/tools/mapa.ps1` seguía las rutas de antes de 1e y de la Fase A
(`application/port/secondary/`, `application/rulesvalidator/`, `application/rule/impl/` para las
reglas). Como `mapa.ps1 -Check` solo compara el mapa contra sí mismo, el drift pasó desapercibido:
la tabla **«Puertos de salida y sus implementaciones» salía vacía**, y las reglas nuevas de
`domain/rule/` cayeron en «Otro» junto con los value objects. Reescrita para las rutas reales
(`domain/rule/`, `domain/rule/model/`, `application/secondaryport/repository/`,
`application/primaryport/{request,response}/`, `application/rule/validator/`) y se añadieron
patrones para `commons/{exception,message,model}/`, que antes caían enteros en «Otro». Regenerado:
`mapa.ps1 -Check` en verde, `drift.ps1` y `consistencia.ps1` sin novedad.

---

## Deudas conocidas

| Deuda | Dónde |
|---|---|
| **Criterio 10** — la operación compensatoria existe y ningún flujo la invoca | `docs/criteria-compliance-matrix.md`. Necesita su propia historia |
| Las herramientas son solo PowerShell | Si entra alguien en Linux/macOS, hay que portarlas |
| `repository-structure.md` del repo de arquitectura sigue siendo un stub | Ahora que la estructura está verificada por herramienta, se puede elaborar |
| El agente `5-entrega` no existe | Los commits y PRs se hacen a mano, con los dos gates igualmente |
