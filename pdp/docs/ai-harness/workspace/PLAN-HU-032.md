# PLAN: HU-032 — Fronteras Modulith del PEP: `PepArchitectureTests` en verde sin relajar reglas

> Módulo: `pep/` (pom `pep/pom.xml`). El harness (`sb-*`, `verificar.ps1`) está afinado para `pdp/`; aquí manda el código del PEP
> y la versión real de Spring Modulith (**2.1.0**, `spring-modulith-bom`).
> **Todo lo de este plan se verificó ejecutando `ApplicationModules.of(PepApplication.class).verify()` sobre el código real,
> con los `package-info.java` propuestos creados de forma temporal; después se revirtió todo (`git status` del worktree: solo `HU-032.md`).
> No se creó ningún esqueleto ni se tocó `src/main` ni `src/test`.**

## Metadata

- **ID:** HU-032
- **Slice:** `pep/` — módulos `application`, `commons`, `enforcement`, `ingress`, `normalization` (no es un slice del PDP)
- **Tipo:** Estructural (anotaciones de paquete + una prueba de arquitectura). Sin lógica, sin endpoints, sin persistencia, sin cambio de comportamiento en ejecución.
- **Fecha:** 2026-10-30
- **Rama:** `fix/HU-032-fronteras-modulith-pep` (base `feature/seguridad-waf-sesiones`)
- **Fuentes:** `workspace/HU-032.md` (tabla «Decisiones tomadas», cerradas), ADR-035 (`security-platform-architecture/docs/01-governance/adr/ADR-035-pep-module-boundaries.md`,
  solo lectura), `pep/src/main/**` y `pep/src/test/**` reales, `pep/pom.xml`, `PepArchitectureTests`, los `package-info.java` del PDP
  (`commons`, `shared`, `applications`, `applications.application.usecase`, `…primaryport.request`), fuentes de `spring-modulith-core`/`-api` 2.1.0
  (`ApplicationModule`, `NamedInterfaces`, `ApplicationModule.AllowedDependency`), skills `sb-arquitectura`, `sb-estandares`, `sb-fuentes`, `sb-criterios`, `sb-reactivo` (sección Modulith).
- **Criterios de la línea base que toca:** 1 (Clean Architecture / fronteras de módulo), 11 (interacción entre capas), 12 (SOLID: dependencias declaradas, contratos mínimos) y 23 (arquitectura antes del negocio: la suite de arquitectura vuelve a ser una señal).
  El 2 no cambia de estado (los contratos funcionales de `application.contract` no se tocan). **No aplican** 3–10, 13–22: no hay reglas, excepciones, DTOs, persistencia, endpoints ni flujo reactivo nuevo; la HU no modifica una sola línea de comportamiento.

## 1. Resumen funcional

Se declaran las fronteras que el código del PEP ya usa: `application` (contratos funcionales) y `commons` quedan abiertos; `enforcement` y `normalization` se cierran y publican solo
`usecase` y `request`; cada módulo declara sus `allowedDependencies`. Una prueba nueva fija el conjunto de dependencias declaradas y de interfaces publicadas, porque `verify()` solo falla cuando
el *código* viola la lista, no cuando alguien *amplía* la lista. **No cubre:** lógica del PEP, el PDP, ni las reglas de capas de `PepArchitectureTests` (no se tocan ni se excluye ningún paquete).

## 2. Criterios de aceptación

| # | Criterio (de la HU) | Resultado esperado | Cómo se demuestra |
|---|---|---|---|
| 1 | `PepArchitectureTests` en verde sin relajar reglas ni excluir paquetes | `verify()` sin violaciones + las 3 reglas ArchUnit intactas | `./mvnw -f pep/pom.xml -q test -Dtest=PepArchitectureTests` en verde; diff de `PepArchitectureTests` vacío (salvo P1-B, ver §11) |
| 2 | Una dependencia no declarada (p. ej. `ingress` → interno de `normalization`) vuelve a fallar | La violación se detecta | Mutación temporal M1 (§9.3) + prueba de conjunto declarado (M5); resultado anotado en el reporte; **nada se sube** |
| 3 | ADR-035 publicada con fronteras y alternativas descartadas | Existe y es Accepted | Ya escrita (ver hallazgos H2 y H3: dos precisiones pendientes de reflejar en ella) |
| 4 | Suite completa de `pep/` en verde | Sin regresiones | `./mvnw -f pep/pom.xml verify` **en un entorno con sockets** (ver R6) |

## 3. Reglas de negocio

No hay. La única «regla» es de arquitectura y la hace cumplir Spring Modulith (§7).

## 4. Modelo de dominio afectado

Ninguno. No se toca `domain`, ni value objects, ni enums.

## 5. Persistencia / 6. Endpoint

No aplican.

## 7. SPEC — el contrato

> En esta HU el «contrato» son las anotaciones de paquete y la prueba. Todas las piezas son `package-info.java` (material del implementador, **[N]/[M] de configuración**, no esqueletos) o la prueba (tester). Se verificó que cada texto compila y que `verify()` + las 3 reglas ArchUnit quedan en verde con ellos.

### 7.1 Estado actual medido (`verify()` en rojo, 23 entradas de `Violations`)

Se ejecutó `JAVA_HOME=C:\Users\Sebastian\.jdks\temurin-25.0.4.1 ./mvnw -f pep/pom.xml -q test -Dtest=PepArchitectureTests`.

| Grupo | Entradas | Detalle |
|---|---:|---|
| `enforcement` → `application.contract` no expuesto | 4 | `DecisionPort`→`ReactiveOperation`; `EnforceAccessRequestMustBeCompleteRule`→`OperationWithoutResult`; `EnforceAccessRulesValidator`→`ReactiveOperationWithoutResult`; `EnforceAccessUseCase`→`ReactiveOperation` |
| `normalization` → `application.contract` no expuesto | 3 | `NormalizeAccessRequestMustBeCompleteRule`→`OperationWithoutResult`; `NormalizeAccessRulesValidator`→`ReactiveOperationWithoutResult`; `NormalizeAccessUseCase`→`ReactiveOperation` |
| `ingress` → `application.contract` no expuesto | 6 | `IntegrationCredentialMustMatchRule`→`ReactiveOperationWithoutResult`; `IntegrationRegistrationMustBeEnabledRule`→`OperationWithoutResult`; `RegisterIntegrationRulesValidator`→`ReactiveOperationWithoutResult`; `RegisterIntegrationUseCase`→`ReactiveOperation`; `RegisterIntegrationInteractor`→`ReactiveOperation`; `RegisterIntegrationInteractorImpl.execute` referencia `ReactiveOperation.execute` |
| `ingress` → `enforcement` interno | 10 | `EnforceAccessUseCase` (8: campo, parámetro de constructor, constructor declarado y llamada a `execute` en `PepHttpController` y `EmbeddedAccessDecisionController`) y `EnforceAccessRequest` (2: `new` en ambos controladores) |
| **Total** | **23** | |

Lo que **no** aparece (y por qué importa): `ingress` → `normalization.application.usecase.NormalizeAccessUseCase` y `…dto.request.NormalizeAccessRequest` no se reportan porque `normalization` es `OPEN`
(`isValidDependencyWithin` devuelve sin violación si `targetModule.isOpen()`), y `ingress` → `enforcement.ForwardHttpRequest` no se reporta porque está en el paquete raíz del módulo (API implícita).
`PepArchitectureTests` falla en la línea 13 (`verify()`); las 3 reglas ArchUnit de las líneas 14-21 ni siquiera llegan a ejecutarse hoy.

### 7.2 Mapa real de consumo de `ingress` (imports en `src/main`; `src/test` no cuenta, Modulith excluye tests)

| Tipo consumido | Módulo | Paquete | Quién lo usa en `ingress` | Publicación necesaria |
|---|---|---|---|---|
| `EnforceAccessUseCase` | enforcement | `application.usecase` | `PepHttpController`, `EmbeddedAccessDecisionController` | `@NamedInterface("usecase")` |
| `EnforceAccessRequest` | enforcement | `application.port.primary.dto.request` | ambos controladores | `@NamedInterface("request")` |
| `ForwardHttpRequest` | enforcement | **raíz** (`co.edu.uco.seguridad.pep.enforcement`) | `PepHttpController` (campo y constructor). La implementa `ReactiveProxyAdapter` (privado a `enforcement.infrastructure…http`) | Ninguna: interfaz **sin nombre** (raíz) |
| `NormalizeAccessUseCase` | normalization | `application.usecase` | `PepHttpController`, `EmbeddedAccessDecisionController` | `@NamedInterface("usecase")` |
| `NormalizeAccessRequest` | normalization | `application.port.primary.dto.request` | `CapturedAccess`, `HttpCaptureInteractor`, `EmbeddedAccessDecisionController` | `@NamedInterface("request")` |
| `ReactiveOperation`, `OperationWithoutResult`, `ReactiveOperationWithoutResult` | application | `contract` | reglas, validadores, use case e interactor propios | módulo `OPEN` |
| `EnforcementFailure`, `IdentityEvidence`, `ProxyTarget` | commons | raíz | 13 + 3 + 2 imports | raíz (ver P1) |

`enforcement` consume solo `application` y `commons`; `normalization` consume solo `application` y `commons`. Ninguno toca a `ingress` ni entre sí (si lo hicieran, `verify()` ya mostraría un ciclo: M4).

### 7.3 Sintaxis exacta que acepta Spring Modulith 2.1.0 (verificada contra el código fuente y ejecutando)

- `@ApplicationModule(type = ApplicationModule.Type.OPEN)` y `@ApplicationModule(allowedDependencies = {…})` van en `package-info.java` del paquete raíz del módulo (`Target` = `PACKAGE, TYPE`).
  El valor por defecto de `allowedDependencies` es un token centinela «abierto»; **declarar la lista la convierte en cerrada** (todo lo no listado falla).
- **Un nombre de módulo sin `::` apunta SOLO a su interfaz sin nombre (el paquete raíz)** en 2.1.0 (`AllowedDependency.of` → `getUnnamedInterface()`), no a «todo lo expuesto». Por eso `"enforcement"` basta para `ForwardHttpRequest`.
  En un módulo `OPEN` la interfaz sin nombre cubre todo el módulo (por eso `"application"` y `"commons"` bastan). La skill `sb-reactivo` dice lo mismo («`"modulo"` → el paquete raíz del módulo»).
- `"modulo :: nombre"` → interfaz nombrada. Separador `::`; se hace `split("::")` y `trim()`, así que `"enforcement :: usecase"` (con espacios, como en el PDP) y `"enforcement::usecase"` son equivalentes. Usar la forma con espacios (convención del PDP).
- `"modulo :: *"` → todas las interfaces nombradas (amplía la frontera: la prueba de conjunto debe prohibirlo, M5). `"modulo :: <<UNNAMED>>"` también se acepta (verificado), pero **no se usa**: es equivalente al nombre plano y menos legible.
- `@NamedInterface("usecase")` sobre `package-info.java` de un **paquete**: expone los tipos de ESE paquete; **no** arrastra los sub-paquetes aunque `propagate` valga `true` por defecto (verificado: `…application.usecase.impl.*` queda interno; `usecase` publica solo `EnforceAccessUseCase`).
- Los paquetes `package-info` cuyo paquete contenga `..application..` ya están excluidos de la regla 1 de `PepArchitectureTests` (`doNotHaveSimpleName("package-info")`); esos `package-info` pasan las 3 reglas (verificado).

### 7.4 Los `package-info.java` (texto exacto)

Ruta base: `pep/src/main/java/co/edu/uco/seguridad/pep/`. Se mantiene el estilo del único `package-info` existente del PEP (anotación + `package` + `import` al final). Un comentario Javadoc breve que cite ADR-035 antes de la anotación es opcional.

**[N] `application/package-info.java`** — el módulo `application` no tiene tipos en su raíz (solo `application.contract`); este archivo crea su única clase raíz y lo declara abierto:
```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package co.edu.uco.seguridad.pep.application;

import org.springframework.modulith.ApplicationModule;
```

**[N] `enforcement/package-info.java`**
```java
@ApplicationModule(allowedDependencies = {"application", "commons"})
package co.edu.uco.seguridad.pep.enforcement;

import org.springframework.modulith.ApplicationModule;
```

**[M] `normalization/package-info.java`** — hoy es `OPEN`; se cierra y declara dependencias:
```java
@ApplicationModule(allowedDependencies = {"application", "commons"})
package co.edu.uco.seguridad.pep.normalization;

import org.springframework.modulith.ApplicationModule;
```

**[N] `ingress/package-info.java`**
```java
@ApplicationModule(allowedDependencies = {
        "application", "commons",
        "enforcement", "enforcement :: usecase", "enforcement :: request",
        "normalization :: usecase", "normalization :: request"})
package co.edu.uco.seguridad.pep.ingress;

import org.springframework.modulith.ApplicationModule;
```
(`"enforcement"` plano = su interfaz sin nombre = `ForwardHttpRequest`; **no** se lista `"normalization"` plano: `ingress` no consume nada de su raíz y no debe poder hacerlo.)

**[N] publicación de los tipos de caso de uso y de petición** (4 archivos, mismo patrón):
```java
// enforcement/application/usecase/package-info.java
@NamedInterface("usecase")
package co.edu.uco.seguridad.pep.enforcement.application.usecase;

import org.springframework.modulith.NamedInterface;
```
```java
// enforcement/application/port/primary/dto/request/package-info.java
@NamedInterface("request")
package co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request;

import org.springframework.modulith.NamedInterface;
```
y los dos equivalentes de `normalization` (`normalization/application/usecase/` → `"usecase"`, `normalization/application/port/primary/dto/request/` → `"request"`).

**`commons`: ver §11, P1 (hallazgo H1). No se escribe hasta que el responsable decida.**

### 7.5 Decisión sobre `ForwardHttpRequest` (decisión 3 de la HU: «el planificador decide»)

**Basta la interfaz sin nombre (raíz de `enforcement`). No se publica una interfaz nombrada nueva ni se mueve el puerto.** Razones:
1. Es exactamente lo que Modulith define como API del módulo (paquete raíz); la entrada `"enforcement"` en `ingress` lo declara de forma explícita y la lista deja de ser implícita.
2. Moverla a un paquete con `@NamedInterface` sería un cambio [M] de código de producción (paquete + imports + wiring) sin ganar aislamiento; la HU pide «sin lógica nueva» y mínimo cambio (decisión 4).
3. El riesgo —que cualquier tipo futuro en la raíz de `enforcement` quede consumible por `ingress`— se cubre con la prueba: fija que la interfaz sin nombre de `enforcement` contiene **solo** `ForwardHttpRequest` (§7.6, prueba 5). Hoy la raíz de `normalization` está vacía y `ingress` no la declara.

### 7.6 La prueba de conjunto declarado (firma y contenido esperado)

**[N, tester]** `pep/src/test/java/co/edu/uco/seguridad/pep/PepModuleDependenciesTests.java` — clase **nueva**, no se modifica `PepArchitectureTests`.
Sin Mockito, AssertJ (ya en el classpath por `spring-boot-starter-test`), `ApplicationModules` en un `static final` (≈3 s por instancia). Ejecutada en el escenario de verificación: 5 pruebas, todas pasan con los `package-info` de §7.4 (variante P1-A).

```java
class PepModuleDependenciesTests {
    private static final ApplicationModules MODULES = ApplicationModules.of(PepApplication.class);

    @Test void the_pep_has_exactly_the_agreed_modules()
    //  MODULES.stream().map(m -> m.getIdentifier().toString())
    //  containsExactlyInAnyOrder("application","commons","enforcement","ingress","normalization")

    @Test void application_is_open_and_the_use_case_modules_are_closed()
    //  module("application").isOpen() == true; enforcement / normalization / ingress .isOpen() == false
    //  (commons: depende de P1 — A: no se afirma `isOpen()`, se afirma que todas sus interfaces son la sin nombre; B: isOpen() == true)

    @Test void each_module_declares_exactly_the_agreed_dependencies()
    //  declared("ingress")       containsExactlyInAnyOrder("application","commons","enforcement",
    //                              "enforcement :: usecase","enforcement :: request",
    //                              "normalization :: usecase","normalization :: request")
    //  declared("enforcement")   containsExactlyInAnyOrder("application","commons")
    //  declared("normalization") containsExactlyInAnyOrder("application","commons")

    @Test void the_open_modules_depend_on_no_other_module()
    //  module("application").getDirectDependencies(MODULES).isEmpty(); idem "commons"

    @Test void enforcement_and_normalization_publish_only_their_use_case_and_their_request()
    //  published("enforcement")   containsExactlyInAnyOrder("<<UNNAMED>> -> ForwardHttpRequest",
    //                               "usecase -> EnforceAccessUseCase","request -> EnforceAccessRequest")
    //  published("normalization") containsExactlyInAnyOrder("usecase -> NormalizeAccessUseCase",
    //                               "request -> NormalizeAccessRequest")      // sin tipos en la raíz

    // helpers (privados)
    private static ApplicationModule module(String name)   // MODULES.getModuleByName(name).orElseThrow()
    private static Set<String> declared(String name)       // module(name).getAllowedDependencies(MODULES).stream().map(Object::toString).collect(toSet())
    private static Set<String> published(String name)      // module(name).getNamedInterfaces().stream()
                                                            //   .flatMap(ni -> ni.asJavaClasses().map(t -> ni.getName() + " -> " + t.getSimpleName())).collect(toSet())
}
```
Notas: `AllowedDependency.toString()` (público) devuelve `"modulo"` para la interfaz sin nombre, `"modulo :: nombre"` para una nombrada y `"modulo :: *"` para el comodín; es el formato exacto de los literales de arriba. Alternativa más robusta ante cambios de formato: construir la cadena con `getTargetModule().getIdentifier()` y `getTargetNamedInterface()` (null = `*`, `isUnnamed()` = solo el módulo, otro = `modulo :: nombre`); equivalente por el código fuente leído, pero **solo la variante con `toString` se ejecutó**.
`<<UNNAMED>>` es `NamedInterface.getName()` de la interfaz sin nombre (verificado en la salida).

## 8. Árbol de archivos

> Rutas desde `pep/src/main/java/co/edu/uco/seguridad/pep/` salvo la prueba. **No hay `{Slice}Configuration` que tocar**: las anotaciones de paquete no son beans. No se crean esqueletos.

```
pep/
├── application/package-info.java                                              [N]  OPEN
├── commons/package-info.java                                                  [N]  SOLO si P1 = B  (ver §11)
├── enforcement/package-info.java                                              [N]  allowedDependencies {application, commons}
├── enforcement/application/usecase/package-info.java                          [N]  @NamedInterface("usecase")
├── enforcement/application/port/primary/dto/request/package-info.java         [N]  @NamedInterface("request")
├── normalization/package-info.java                                            [M]  OPEN → cerrado + allowedDependencies {application, commons}
├── normalization/application/usecase/package-info.java                        [N]  @NamedInterface("usecase")
├── normalization/application/port/primary/dto/request/package-info.java       [N]  @NamedInterface("request")
├── ingress/package-info.java                                                  [N]  allowedDependencies (7 entradas, §7.4)
pep/src/test/java/co/edu/uco/seguridad/pep/PepModuleDependenciesTests.java     [N]  tester (§7.6)
pep/src/test/java/co/edu/uco/seguridad/pep/PepArchitectureTests.java           sin cambios (salvo P1 = B: una línea)
```
No se modifica ninguna otra clase de `src/main`. Fuera de este repo: ADR-035 (ver H2, H3).

## 9. Casos de prueba esperados

### 9.1 Capa «arquitectura» (no hay pruebas de dominio/aplicación/infraestructura: no hay lógica)

| Clase | Casos | Estado esperado antes de implementar (rojo) |
|---|---|---|
| `PepArchitectureTests` (existente, sin tocar) | `module_boundaries_and_clean_layers_are_enforced`: `verify()` + 3 reglas ArchUnit | **Rojo** hoy por las 23 violaciones de §7.1 |
| `PepModuleDependenciesTests` (nueva) | los 5 casos de §7.6 | **Rojo** hoy: el único que pasa sin implementar es el de conjunto de módulos; `application.isOpen()`, listas declaradas (`none`) e interfaces publicadas fallan |

### 9.2 Orden recomendado

tester escribe `PepModuleDependenciesTests` (rojo) → implementador crea los `package-info.java` → ambas clases en verde → mutaciones (9.3) → suite completa.

### 9.3 Mutaciones temporales (el implementador las ejecuta una vez y anota el resultado en el REPORTE; se revierten, no se commitean)

Comando: `JAVA_HOME=C:\Users\Sebastian\.jdks\temurin-25.0.4.1 ./mvnw -f pep/pom.xml -q test -Dtest=PepArchitectureTests` (y `…,PepModuleDependenciesTests` para M5). Cada mutación es **un archivo sonda nuevo** (se borra después) o una edición de una línea; al terminar `git status` no debe mostrar residuos.

| # | Mutación | Qué demuestra | Resultado medido (mensaje) |
|---|---|---|---|
| **M1** (criterio 2 de la HU) | `ingress/MutationProbe.java` con `import co.edu.uco.seguridad.pep.normalization.application.rulesvalidator.NormalizeAccessRulesValidator;` y `interface MutationProbe { NormalizeAccessRulesValidator internal(); }` | `ingress` → interno de `normalization` falla | `Module 'ingress' depends on module 'normalization' via …ingress.MutationProbe -> …normalization.application.rulesvalidator.NormalizeAccessRulesValidator. Allowed targets: application, commons, enforcement, enforcement :: usecase, enforcement :: request, normalization :: usecase, normalization :: request.` |
| M2 | `ingress/MutationProbe.java` referenciando `enforcement.application.port.secondary.DecisionPort` | `ingress` → interno de `enforcement` falla | mismo formato, `depends on module 'enforcement' … DecisionPort` |
| M3 | `normalization/MutationProbe.java` referenciando `enforcement.application.usecase.EnforceAccessUseCase` | dependencia entre módulos no declarada (aunque el destino esté publicado) falla | `Module 'normalization' depends on named interface(s) 'enforcement :: usecase' via … Allowed targets: application, commons.` |
| M4 | `enforcement/MutationProbe.java` referenciando `ingress.CapturedAccess` | ciclo | `Cycle detected: Slice enforcement -> Slice ingress -> …` |
| **M5** | Editar `ingress/package-info.java` añadiendo `"normalization :: *"` (sin tocar código) | `verify()` **pasa** (nada viola la lista ampliada) pero `each_module_declares_exactly_the_agreed_dependencies` **falla** — razón de ser de la prueba nueva | `PepArchitectureTests` verde; `PepModuleDependenciesTests`: 1 fallo, `… the following elements were unexpected: ["normalization :: *"]` |
| M6 (opcional) | Añadir `@NamedInterface("rule")` a `normalization/application/rule/package-info.java` | publicar un paquete interno sin declararlo lo detecta la prueba de interfaces publicadas | `PepModuleDependenciesTests.enforcement_and_normalization_publish_only…` falla con `rule -> NormalizeAccessRequestMustBeCompleteRule` |

M1 es la mutación que cierra el criterio 2 literalmente; M5 demuestra que la prueba nueva aporta algo que `verify()` no da.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado y verificado contra el código (23 violaciones medidas; variante final ejecutada en verde y revertida) | 2026-10-30 |
| Contrato aprobado (gate 1) | ⏳ Pendiente — requiere decisión P1 | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

Trazabilidad decisión → pieza: D1 → `application/package-info.java` (+ P1 para `commons`) · D2 → `normalization/package-info.java` [M] + los 4 `@NamedInterface` · D3 → `enforcement/`, `normalization/`, `ingress/package-info.java` + §7.5 · D4/D5 → sin código (el caso de uso publicado se mantiene; `RegisterIntegrationInteractor` no cambia) · D6 → §7.6 + §9.3 · D7 → ADR-035 (ya escrita).
Criterios HU → evidencia: C1 → `PepArchitectureTests` verde · C2 → M1/M5 · C3 → ADR-035 (H2/H3) · C4 → `verify` completo (R6).

## 11. Ambigüedades pendientes y hallazgos que contradicen las decisiones cerradas (no resueltos por el planificador)

### H1 / P1 — `commons` `OPEN` explícito choca con la regla de capas de `PepArchitectureTests` (decisión 1 vs «no tocar las reglas de capas»)

Evidencia ejecutada: crear `commons/package-info.java` con `@ApplicationModule(type = OPEN)` hace que `verify()` pase pero la **regla 2** (`noClasses().that().resideInAnyPackage("..domain..", "..commons..").should().dependOnClassesThat().resideInAnyPackage(…, "org.springframework..", …)`) falle:
```
Class <co.edu.uco.seguridad.pep.commons.package-info> is annotated with <org.springframework.modulith.ApplicationModule>
Class <co.edu.uco.seguridad.pep.commons.package-info> has annotation member of type <org.springframework.modulith.ApplicationModule$Type>
```
(ArchUnit cuenta la anotación del `package-info` como dependencia de `commons` hacia `org.springframework..`). La regla 1 ya excluye `package-info` (`doNotHaveSimpleName("package-info")`); la regla 2 no. En el PDP esto no ocurre porque su prueba de capas no tiene esa regla sobre `commons`.
Dos salidas, ambas con coste distinto; **debe elegir el responsable técnico**:

| Opción | Qué se hace | Efecto | Contra |
|---|---|---|---|
| **A (recomendada)** | **No** crear `commons/package-info.java`. `"commons"` se sigue declarando en las listas | `commons` es hoy plano (todos sus tipos en la raíz = API implícita); `verify()` y las 3 reglas pasan (**ejecutado**). Más estricto que `OPEN`: un sub-paquete futuro de `commons` sería interno y se detectaría | No cumple al pie de la letra «commons `OPEN` explícito»; ADR-035 dice lo contrario (H2) |
| B | Crear `commons/package-info.java` `OPEN` **y** añadir `.and().doNotHaveSimpleName("package-info")` a la regla 2 de `PepArchitectureTests` (mismo patrón que la regla 1) | Cumple D1 al pie de la letra | Modifica una regla de capas (restricción cerrada de la HU); aunque no la relaja en la práctica, es tocarla |

El resto del plan es idéntico en ambas. La prueba §7.6 cambia en una línea (`commons.isOpen()` solo en B; en A se afirma que `commons` solo tiene la interfaz sin nombre).

### H2 — ADR-035 dice «`ingress` consume únicamente las interfaces nombradas», pero también depende de la raíz de `enforcement` (`ForwardHttpRequest`)
El plan lo resuelve con `"enforcement"` plano (§7.5), coherente con la decisión 3, pero el texto de la ADR debería citar esa entrada. Además, si se elige A en P1, la ADR dice «`commons` `OPEN`» y no sería exacta.

### H3 — Cuidado con la semántica del nombre plano en Modulith 2.1.0
`"modulo"` sin `::` apunta **solo a su paquete raíz**, no «a todo lo expuesto». Está probado y es lo que hace correcta la entrada `"enforcement"`; conviene que la ADR/skill no lo describan como «todo el módulo». (La nota de la FASE 5 del harness sobre exportar paquetes cuando un módulo gana su primer `@NamedInterface` es consistente con esto.)

## 12. Riesgos y notas para el implementador

- **R1 Entorno.** El POM exige Java 25 y el `JAVA_HOME` por defecto es Temurin 21 (`release version 25 not supported`). Usar `JAVA_HOME=C:\Users\Sebastian\.jdks\temurin-25.0.4.1` (también existe `temurin-25.0.4`). `verificar.ps1` está afinado para `pdp/`: para el PEP se usa Maven directo.
- **R2** `PepArchitectureTests` y `PepModuleDependenciesTests` no abren sockets y corren aquí (≈3 s cada una).
- **R3 Orden de los fallos.** Mientras `verify()` falle, las 3 reglas ArchUnit no se ejecutan. Al dejar `verify()` en verde pueden aparecer fallos de esas reglas ocultos hasta ahora; en el escenario verificado no apareció ninguno salvo H1.
- **R4 Un `package-info` nuevo bajo `..application..`** no viola la regla 1 (excluida por nombre). Uno nuevo bajo `commons` o `domain` **sí** violaría la regla 2 (H1). No añadir `package-info` en `domain` sin revisar.
- **R5 Fragilidad de la prueba.** Si Modulith cambia el formato de `AllowedDependency.toString()` (ver alternativa en §7.6) o la interfaz sin nombre (`<<UNNAMED>>`), la prueba falla con diferencia clara; es el comportamiento deseado, se ajusta con el bump de versión.
- **R6 Suite completa (criterio 4).** En esta herramienta `./mvnw -f pep/pom.xml test` da 41 errores **de entorno**: `BffSessionChannelIntegrationTests` (3), `PdpTlsIntegrationTests` (1), `IngressLimitsHttpTests` (2), `PepHttpIntegrationTests` (35), todos por `IllegalStateException: failed to create a child event loop` (Netty no puede abrir sockets/selectores). No son regresiones de esta HU (no la toca ningún comportamiento) pero **el criterio 4 no puede darse por cumplido hasta correr `./mvnw -f pep/pom.xml verify` donde haya sockets**. Las demás clases (≈32 pruebas: `ProblemWriterTests`, `BffSessionTokenResolverTests`, `KeycloakClientCredentialsTokenProviderTests`, `PdpApplicationCredentialValidationAdapterTests`, `IntegrationRegistrationControllerTests`, `RouteRegistryTests`, `PepArchitectureTests`) pasan con la variante propuesta. No se ejecutó la suite completa de base para comparar.
- **R7 Sin impacto en ejecución.** Las anotaciones de paquete no son beans; el escaneo de `@SpringBootApplication` y el cableado de `*Configuration` no cambian.
- **R8 Mejora opcional (no incluida)**: declarar `allowedDependencies = {}` en `application`/`commons` los haría «cerrados vacíos» (prohíben cualquier dependencia saliente, p. ej. `application` → `commons`, que hoy ni ciclo produce). No se incluye porque no está en las decisiones cerradas y la prueba `the_open_modules_depend_on_no_other_module` ya lo vigila.
- **R9** Los tests (`src/test`) que importan internos de `enforcement` (`PdpTlsIntegrationTests`, etc.) no cuentan para Modulith (`ApplicationModules.of` excluye tests): sin cambios.
