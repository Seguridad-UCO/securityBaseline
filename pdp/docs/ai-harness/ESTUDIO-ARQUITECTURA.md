# Estudio: dónde debería vivir cada cosa

Revisión completa de la arquitectura, contra el proyecto de referencia (`arquisoft-backend@develop`),
los ADRs y la teoría.

> **Estado: la Fase A está ejecutada (2026-08-31).** Las fases B y C siguen siendo plan. Lo que se
> hizo, y en qué se apartó de lo planeado, está al final en «Registro de ejecución».

Pregunta que lo origina: *«¿es correcta la arquitectura como está, o está muy regada?»*

**Respuesta corta:** no está mal, pero **está a medio camino entre dos diseños coherentes**, y esa
mezcla es lo que se ve desordenada. En tres puntos la referencia es claramente mejor, y en uno el
diseño actual es defendible por una razón que la referencia no tiene: somos reactivos.

---

## 1. El dato que cambia el diagnóstico

De los **69 archivos de `infrastructure`**, solo **12 tocan Spring**:

| Dónde | Cuántos | Anotaciones |
|---|---|---|
| `web/controller` | 4 | `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@RequestBody`, `@RequestParam` |
| `config` | 4 | `@Configuration`, `@Bean`, `@EnableConfigurationProperties` |
| `properties` | 3 | `@ConfigurationProperties` |
| `secondary/audit` | 1 | `@EventListener` |

Los otros **57 son Java puro**: interactores, mappers, entities, adaptadores de persistencia, schemas.
Y `application` tiene **cero** imports de Spring.

> **Esto significa que «infraestructura» no está agrupando por acoplamiento técnico.** Está
> agrupando por dos criterios distintos a la vez: *«toca un framework»* (12 archivos) y *«está en el
> borde / traduce»* (57). Dos criterios mezclados en un mismo paquete es exactamente lo que se
> percibe como desorden.

El acoplamiento real ya está muy contenido. **El problema no es de qué dependemos: es de dónde
ponemos las cosas y cómo las llamamos.**

---

## 2. Las tres divergencias con la referencia

### 2.1 Las `Rule` no son puras, y por eso no pueden vivir en el dominio

**Lo que hacemos:**

```java
// application/rule/impl/ApplicationMustExistForTenantRuleImpl.java
public Mono<Application> execute(ApplicationOwnershipQuery query) {
    return repository.findByTenantAndId(query.tenantId(), query.applicationId())   // ← I/O
            .switchIfEmpty(Mono.error(() -> new ApplicationNotFoundException(...)));
}
```

La regla **inyecta el repositorio y hace la consulta**. Como hace I/O, es reactiva; como es reactiva
y conoce un puerto, tiene que vivir en `application`.

**Lo que hace la referencia** — tres piezas, cada una en su sitio:

```java
// domain/{feature}/model/DisponibilidadNombreX.java     ← el dato YA RESUELTO
public record DisponibilidadNombreX(String nombre, boolean yaExiste) {}

// domain/{feature}/rules/XUnicoRule.java                ← contrato puro
public interface XUnicoRule extends DomainRule<DisponibilidadNombreX> {}

// domain/{feature}/rules/impl/XUnicoRuleImpl.java       ← DECISIÓN PURA, sin I/O, sin Mono
public void validar(DisponibilidadNombreX disponibilidad) {
    if (disponibilidad.yaExiste()) {
        throw new NombreXDuplicadoException(disponibilidad.nombre());
    }
}

// application/{feature}/command/finder/NombreXExisteFinder.java   ← trae SOLO lo necesario
public interface NombreXExisteFinder extends Finder<String, Boolean> {}
```

En la referencia hay **26 `Rule` y todas viven en `domain`**. Ninguna consulta nada.

**Las tres cosas que intuiste son correctas:**

1. **Sí, las reglas deberían ser puras y vivir en el dominio.** Una restricción de negocio es
   conocimiento del dominio; que hoy no pueda estar ahí es un accidente de que hace I/O.
2. **Sí, existe una estrategia para separarlo: el `Finder`.**
3. **Sí, el `Finder` trae exactamente lo que hace falta, no el agregado entero.**
   `Finder<String, Boolean>` responde «¿existe?» con un booleano. Nosotros hacemos
   `findByTenantAndId` y traemos la aplicación completa **para saber si existe**.

**Y en nuestro caso hay un beneficio extra que la referencia no tiene:** ellos son síncronos, así
que para ellos la regla pura es solo más limpia. Nosotros somos reactivos, así que una regla pura
**deja de ser `Mono` y pasa a ser una función normal**:

```java
// El validator (application) orquesta el I/O; la regla solo decide.
return finder.execute(name)                                  // Mono<Boolean>
        .map(existe -> new DisponibilidadNombre(name, existe))
        .doOnNext(rule::validar)                             // ← síncrono, puro, en domain
        .then();
```

Probar esa regla es `assertThatThrownBy(() -> rule.validar(new Disponibilidad("x", true)))`. Sin
`StepVerifier`, sin fakes, sin `Mono`.

### 2.2 El interactor no es un puerto primario: es un adaptador web

**ADR-016 lo llama «puerto primario explícito».** Un puerto lo define la capa que lo necesita, así
que debería estar en `application`. En la referencia está exactamente ahí:

```
application/{feature}/command/primaryport/
├── model/XCommand              el contrato de entrada del interactor
├── interactor/X + impl         recibe el Command ya tipado
└── mapper/XMapper              Command → dominio
```

Y el mapeo de **web → Command** lo hace el adaptador:

```
infrastructure/{feature}/command/primaryadapter/web/
├── XController                 @RestController, @PreAuthorize
├── dto/XRequestDTO
└── mapper/XRequestMapper       DTO web → Command
```

**Nuestro interactor hace las dos cosas a la vez:**

```java
// infrastructure/adapter/primary/web/interactor/impl/ListApplicationsInteractorImpl.java
return SecurityContext.currentPrincipal()                              // 1. lee el principal
        .map(p -> ListApplicationsRequestMapper.toRequest(raw, p.tenantId()))   // 2. mapea web→núcleo
        .flatMap(useCase::execute)                                     // 3. orquesta
        .map(page -> new PageResponse<>(...));                         // 4. mapea núcleo→web
```

Recibe un tipo **web** (`ListApplicationsRawRequest`) y devuelve un tipo **web** (`PageResponse`).
Con esa firma **no puede estar en `application`**: la capa quedaría conociendo tipos de transporte.

> **Respondiendo a tus dos preguntas:** el interactor **sí es orquestación**, pero hoy además hace
> traducción, y es la traducción lo que lo ancla a infraestructura. Y **debería recibir ya
> mapeado**: en la referencia recibe un `Command` tipado, y quien traduce del DTO web es el mapper
> del controller.

**Por qué acabó así aquí, y no es un descuido:** el ADR-016 dice que el interactor es «el lugar
natural para leer el `RequestContext` autenticado (tenant y sujeto) sin tocar el caso de uso». Leer
el principal exige `SecurityContext`, que es Spring Security. La referencia resuelve la autorización
con `@PreAuthorize` **en el controller**, así que su interactor nunca necesita el principal.

Es decir: **seguimos el ADR-016 al pie de la letra, y eso mismo empujó el interactor fuera de
`application`.** No es un error; es una consecuencia no prevista de la decisión.

### 2.3 El `Entity` de persistencia: está bien donde está, y no debe llevar anotaciones

Preguntabas si un `record` inmutable sin anotaciones debería estar en infraestructura, o si sería
mejor anotarlo.

```java
public record ApplicationEntity(String id, String tenantId, String name, String description,
        String baseUrl, String registeredAt) {}
```

**Se queda en infraestructura, sin anotaciones**, por tres razones:

1. **Su forma la dicta el almacén, no el dominio.** Es todo `String` porque SurrealDB devuelve JSON
   y las fechas llegan como texto. Si mañana cambia el almacén, cambia esta clase y nada más.
2. **Anotarlo exigiría un ORM que el proyecto descartó.** `@Entity`/`@Table`/`@Column` son de JPA, y
   el ADR-003 y el ADR-019 eligieron SurrealDB sin driver ni ORM. Añadir esas anotaciones sería
   arrastrar Jakarta Persistence al proyecto para decorar un objeto que nadie mapea automáticamente.
3. **Sin anotaciones sigue ganándose el sitio.** Separa «leer la fila» de «reconstruir el agregado»:
   los value objects validan en el mapper, en un solo punto. Sin `Entity`, el adaptador construiría
   VOs directamente desde el JSON — que es exactamente la divergencia que se corrigió el 2026-08-31
   en `applications` e `identity`.

> Un `Entity` sin anotaciones **no es un `Entity` incompleto**: es la forma correcta cuando no hay
> ORM. Lo que lo justifica es la separación de responsabilidades, no el framework.

---

## 3. Lo que ya está bien y no hay que tocar

| Pieza | Dónde está | Veredicto |
|---|---|---|
| `UseCase` + impl | `application/usecase` | ✅ Orquesta reglas y puertos. Es su sitio |
| `Repository` (puerto) | `application/secondaryport/repository` | ✅ Puerto de salida definido por quien lo necesita, implementado fuera. Correcto |
| Entidades y value objects | `domain` | ✅ Java puro, invariantes en el constructor |
| Controller, DTO web | `infrastructure/adapter/primary/web` | ✅ Es el borde. Aquí sí viven las anotaciones |
| Adaptador de persistencia | `infrastructure/adapter/secondary/persistence` | ✅ Implementa el puerto |
| `{Slice}Configuration` | `infrastructure/config` | ✅ La única clase Spring del módulo |

### Sobre los mappers en `crosscutting`

**No.** Hay dos mappers y cada uno pertenece a un lado distinto de una frontera:

- El **mapper web** (`raw → request tipado`) conoce tipos HTTP → vive con el adaptador web.
- El **mapper de persistencia** (`Entity → dominio`) conoce la forma de la fila → vive con el
  adaptador de persistencia.

Moverlos a un `crosscutting/mapper` común los juntaría solo porque comparten la palabra «mapper»,
que es agrupar por **sufijo del nombre** en vez de por **razón de cambio**. Un paquete así crece
hasta volverse un cajón de sastre que depende de todo. La referencia tampoco lo hace: tiene
`primaryadapter/web/mapper/`, `primaryport/mapper/` y `secondaryadapter/mapper/`, cada uno junto a
lo que traduce.

---

## 4. Plan

Tres cambios independientes, en orden de valor sobre coste. **Cada uno se puede hacer o no, por
separado.**

### Fase A — Reglas puras en el dominio, con `Finder`

**Valor: alto.** Es el que más ordena, el que más se acerca a la referencia y el único que además
mejora el rendimiento (deja de traer agregados enteros para responder «¿existe?»).

| Paso | Qué |
|---|---|
| A1 | `shared/contract`: añadir `DomainRule<T>` (`void validar(T)`) y `Finder<I, O>` (`Mono<O> find(I)`) |
| A2 | Por cada regla: un `record` de entrada en `domain/{slice}/model/` con el dato **ya resuelto** |
| A3 | Mover la regla a `domain/{slice}/rule/` + `impl/`, y hacerla **síncrona y pura** |
| A4 | Crear el `Finder` en `application/{slice}/secondaryport/finder/`, con la consulta **mínima** |
| A5 | El `RulesValidator` (application) hace el I/O: `finder → record → rule.validar` |
| A6 | Adelgazar los puertos: `existsByTenantAndName` en vez de `findByTenantAndId` donde solo se comprueba existencia |

**Coste:** 6 reglas, ~20 archivos nuevos o movidos, y sus pruebas se **simplifican** (dejan de
necesitar `StepVerifier` y fakes de repositorio).

**Riesgo:** bajo. Las reglas puras son el caso más fácil de probar; si algo se rompe, se rompe en
compilación.

**Ojo con una excepción:** `ApplicationMustExistForTenantRule` no solo comprueba existencia, también
**devuelve el agregado** que el use case usa después. Esa no es una regla: es una consulta con
rechazo. Debe quedarse como `Finder` + un `if` explícito en el use case, o partirse en dos.

### Fase B — Interactor como puerto primario

**Valor: medio.** Alinea con el ADR-016 y con la referencia, y saca 8 clases de infraestructura.

| Paso | Qué |
|---|---|
| B1 | El **controller** mapea el DTO web al request tipado (el mapper web se queda donde está) |
| B2 | El **interactor** pasa a `application/primaryport/interactor/` y recibe el request tipado |
| B3 | El interactor devuelve el DTO de `primaryport/response`, no `PageResponse`; el controller envuelve |
| B4 | **El tenant deja de leerse dentro del interactor**: lo pasa el controller, que sí es el borde |

**El punto que hay que decidir antes de tocar nada (B4):** hoy el interactor lee el principal, tal
como sugiere el ADR-016. Si se mueve a `application`, o bien sigue leyéndolo —y entonces
`application` conoce el contexto de seguridad, que es una capacidad técnica— o bien el controller
lo pasa como parámetro, y entonces **nos apartamos del punto 2 del ADR-016**.

> Apartarse de un ADR no es prohibido: es una decisión que necesita su propio ADR. Esta fase
> **no debería ejecutarse sin una enmienda a ADR-016** que diga dónde se lee el principal y por qué.

**Coste:** ~16 archivos. **Riesgo:** medio — toca la firma de todos los endpoints.

### Fase C — Separar «toca framework» de «está en el borde»

**Valor: bajo. Recomendación: no hacerlo.**

Sería mover los 57 archivos de infraestructura que son Java puro a otro sitio, para que
`infrastructure` contenga solo los 12 que tocan Spring. Suena coherente con el dato de la sección 1,
pero rompe algo más valioso: **la agrupación por adaptador**. Hoy, todo lo que sabe de HTTP está
junto y todo lo que sabe de SurrealDB está junto. Eso es lo que permite cambiar un almacén tocando
un solo árbol.

Agrupar por «tiene anotaciones» juntaría el controller con el `SurrealTenantSchemaInitializer` —que no tienen
nada que ver— y separaría el adaptador de su mapper. Es el mismo error que meter los mappers en
`crosscutting`, en grande.

**Lo que sí conviene hacer, y es de una línea:** documentar en `sb-arquitectura` que el criterio de
`infrastructure` es **«hacia dónde mira»** (al mundo exterior), no «de qué depende». Con eso, ver 57
archivos sin anotaciones ahí deja de ser una anomalía y pasa a ser lo esperado.

---

## 5. Recomendación

| | Hacer | Por qué |
|---|---|---|
| **Fase A** | ✅ Sí | Máximo orden por el menor riesgo. Las reglas van al dominio, se vuelven puras y síncronas, y los puertos dejan de traer de más. Es donde la referencia es más claramente mejor |
| **Fase B** | ⏸️ Después, y con ADR | Correcta, pero exige decidir dónde se lee el principal, y eso enmienda el ADR-016. No es un refactor: es una decisión de arquitectura |
| **Fase C** | ❌ No | El criterio actual (agrupar por adaptador) es mejor que el propuesto. Solo hay que documentarlo |

**Y una regla que sale de todo esto, para el harness:** cuando una pieza no encuentra su sitio, casi
siempre es porque **hace dos cosas**. La regla no cabía en el dominio porque además consultaba. El
interactor no cabe en `application` porque además traduce. Partir la responsabilidad resuelve la
ubicación sola.

---

## 6. Registro de ejecución — Fase A (2026-08-31)

`./mvnw verify` verde (247 pruebas, antes 234), `consistencia.ps1` y `drift.ps1` limpios. Los 9
E2E y los 5 de integración corrieron contra SurrealDB real, así que las consultas nuevas están
probadas contra la base, no contra un doble.

### Lo que quedó

```
domain/{slice}/
├── {X}Existence.java  {X}Availability.java  {X}Activation.java   el dato YA RESUELTO
├── rule/ + impl/                    la decisión: pura, síncrona, sin puertos ni Reactor
├── exception/                       la excepción que lanza, junto a quien la lanza
└── message/{Slice}Messages.java     su texto

application/{slice}/rule/validator/ + impl/    hace la E/S y aplica las reglas
                                               @NamedInterface("rule") — lo que ven otros módulos
```

**8 reglas, todas en `domain`.** Seis dejaron de ser reactivas; ninguna conoce ya un puerto.
Probarlas es `assertThatThrownBy(() -> rule.execute(...))`: sin `StepVerifier`, sin repositorio falso.

### Tres desviaciones respecto al plan, y por qué

1. **No se añadió `DomainRule<T>` ni `Finder<I, O>` a `shared/contract`** (el paso A1 desaparece).
   `OperationWithoutResult<I>` ya *es* `DomainRule` —misma firma exacta— y `ReactiveOperation<I, O>`
   ya *es* `Finder`. Los seis contratos existentes se nombran por **forma**; añadir dos nombres de
   rol para formas que ya existen habría dado dos maneras de decir lo mismo.

2. **No se creó una capa `Finder`.** En la referencia el `Finder` existe porque allí el validador es
   puro y quien consulta es el caso de uso. Aquí el validador es reactivo y puede consultar, así que
   un `Finder` que delegue en un método de repositorio con la misma firma sería indirección sin
   ganancia. El valor real del paso A4 estaba en A6 —adelgazar el puerto— y eso sí se hizo:

   | Antes | Ahora |
   |---|---|
   | `ApplicationRepository.findByTenantAndId` → `Mono<Application>` | `existsByTenantAndId` → `Mono<Boolean>` |
   | `TenantRepository.findById` → `Mono<Tenant>` | `findStatusById` → `Mono<TenantStatus>` |

3. **Lo que se publica entre módulos es el validador, no la regla.** Un consumidor no puede alimentar
   una regla pura: no tiene el repositorio ajeno, que es justo el motivo por el que pide la decisión
   prestada. Así que `tenants` publica `TenantMustBeActiveValidator` y `applications` publica
   `ApplicationMustExistForTenantValidator`, ambos con `@NamedInterface("rule")` mudado a
   `application/rule/validator`. **Ningún `allowedDependencies` cambió, así que no hizo falta ADR.**

### Lo que hubo que partir

El estudio nombraba una «consulta con rechazo»; eran **tres**, y las tres devolvían un agregado:

| Contrato | Devolvía | Quién lo usaba | Resultado |
|---|---|---|---|
| `ApplicationMustExistForTenantRule` | `Application` | `resources`, con `.then(...)` | **Nadie usaba el agregado.** Ahora `Mono<Void>` y el puerto responde un booleano |
| `TenantMustBeActiveRule` | `TenantResponse` | `applications` e `identity`, con `.then(...)` | Igual: `Mono<Void>`, y el puerto devuelve solo el estado |
| `UserMustExistRule` | `SecurityUser` | `AssignTenantUseCase`, y **sí lo usaba** | Partido: la regla pura decide, y el use case trae el usuario una sola vez porque lo necesita para reasignarlo |

Las dos primeras cargaban una fila entera para responder «¿existe?» y tiraban el resultado.

### Lo que se movió con las reglas

Excepciones de negocio y catálogos de mensajes bajaron a `domain/{slice}/`: una excepción vive donde
vive quien la lanza, y `domain` no puede importar `application`. `{slice}/application/exception/` y
`{slice}/application/message/` ya no existen en ningún slice.

### Lo que ahora vigila la herramienta

`consistencia.ps1` gana la regla 8: una `Rule` de `domain/rule/impl` que importe `reactor` o
`secondaryport` es un hallazgo. Es lo que impide que la separación se deshaga sola en la próxima
historia. Las reglas 2 y 5 pasaron a mirar `domain/rule` y `domain/message`.

### Pendiente

`security-platform-architecture/docs/07-engineering/repository-structure.md` describe todavía la
forma anterior (`application/rule`, `application/exception`). Es otro repositorio: hay que llevarlo
en su propio PR.
