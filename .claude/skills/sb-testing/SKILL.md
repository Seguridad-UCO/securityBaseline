---
name: sb-testing
description: Convenciones de prueba de securityBaseline — JUnit 5 + AssertJ + StepVerifier, fakes en vez de Mockito, aislamiento por capa, Testcontainers, antipatrones y presupuesto. Cargar antes de escribir o validar cualquier prueba.
---

# Skill: sb-testing

**Derivada del código real** de `pdp/src/test`. Umbral vigente: JaCoCo exige **50 % de líneas por
paquete** en `verify` (`jacoco-check`). Las clases `*Configuration`, `package-info` y
`PdpApplication` están excluidas de cobertura: son cableado, no lógica.

---

## La regla que más se incumple: aquí no se usa Mockito

**Cero usos de Mockito en todo el proyecto.** No es un descuido: los contratos de `shared/contract`
son `@FunctionalInterface`, así que un doble de prueba es una **lambda** o una **clase anónima**.

| Qué quiero doblar | Cómo se hace aquí |
|---|---|
| Un `UseCase`, `Rule` o `Interactor` | Una **lambda**: `raw -> Mono.just(expected)` |
| Un puerto con varios métodos (`TenantRepository`) | Clase **anónima** que implementa lo que el test usa y lanza `UnsupportedOperationException` en el resto |
| Verificar que algo se guardó | Una `List<T>` capturadora que el fake rellena, y luego `assertThat(saved).hasSize(1)` |

Ver el patrón exacto en
`pdp/src/test/java/co/edu/uco/seguridad/pdp/tenants/application/usecase/impl/CreateTenantUseCaseImplTests.java`
(método privado `fakeRepository(boolean, List<Tenant>)`).

> `UnsupportedOperationException` en los métodos no usados **es intencional**: si el use case empieza
> a llamar un método que no debería, el test falla ruidosamente en vez de recibir `null`.

---

## Aislamiento por capa

Un test que necesita contexto de Spring para probar `domain` o `application` es señal de que la
lógica está en la capa equivocada. **Detente y reporta**, no escribas el workaround.

| Capa | Herramientas permitidas | Contexto de Spring |
|---|---|---|
| `domain` (entidades, VOs, enums) | JUnit + AssertJ. Java puro | Nunca |
| `application` (use cases, rules) | JUnit + AssertJ + `StepVerifier` + fakes | Nunca |
| `infrastructure` — mappers | JUnit + AssertJ | Nunca |
| `infrastructure` — controllers | JUnit + AssertJ + `MockServerWebExchange`, con el controller **construido a mano** | Nunca |
| `infrastructure` — adaptador de persistencia | `AbstractSurrealDbIntegrationTest` (Testcontainers) | Sí |
| `shared` — seguridad, CORS, arranque | `@SpringBootTest` | Sí, y solo aquí |

Solo cuatro clases de prueba en todo el repo levantan contexto de Spring. Si estás escribiendo la
quinta, justifícalo.

---

## Cómo se prueba un controller sin Spring

Los interactores son interfaces funcionales, así que el controller se instancia con `new` pasando
lambdas, y el intercambio se simula con `MockServerWebExchange`. Hay que **sembrar el contexto de
correlación** o `CorrelationWebFilter.context(exchange)` devuelve `null`:

```
exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
```

Ver `pdp/tenants/infrastructure/adapter/primary/web/controller/TenantControllerTests.java`.

Se afirma el **código de estado** y el **`data()` del envoltorio**, no el JSON serializado.

---

## Nomenclatura y forma

- Clase: `{ClaseBajoPrueba}Tests` — **en plural**. Nunca `Test` ni `*IT`.
- Método: `snake_case` en inglés, describiendo el comportamiento, no el método invocado.
  - ✅ `creates_a_tenant_that_does_not_exist_yet`, `refuses_a_tenant_id_that_already_exists`
  - ❌ `testCreateTenant`, `shouldWork`, `test1`
- Sin `@DisplayName`: el nombre del método ya es la descripción.
- Constantes compartidas del test como `private static final` al inicio (`ID`, `NAME`).
- La clase de prueba es **package-private** (`class X {`), igual que en el resto del repo.

---

## Aserciones

| Situación | Forma |
|---|---|
| Valor síncrono | `assertThat(x).isEqualTo(y)` (AssertJ) |
| Excepción síncrona | `assertThatThrownBy(() -> new TenantName(null)).isInstanceOf(InvalidTenantNameException.class)` |
| `Mono` con valor | `StepVerifier.create(...).assertNext(r -> { … }).verifyComplete()` |
| `Mono<Void>` que completa | `StepVerifier.create(...).verifyComplete()` |
| `Mono` vacío | `StepVerifier.create(...).verifyComplete()` sin `assertNext` |
| Error reactivo | `StepVerifier.create(...).expectError(XException.class).verify()` |
| Efecto lateral | Lista capturadora + `assertThat(saved).hasSize(1)` |

Nunca `assertEquals` de JUnit: el proyecto usa AssertJ de forma uniforme.
Nunca `.block()` para probar un `Mono`: se usa `StepVerifier`.

---

## Qué probar en cada capa

**`domain`** — un test por invariante del VO: normalización (trim), `null`, vacío, y cada límite de
formato/longitud. Más las factorías y el comportamiento de la entidad (`register` deja `ACTIVE`,
`isActive()`), y el comportamiento del enum.

**`application`** — por caso de uso: el camino feliz (afirmando el DTO devuelto **y** el efecto en el
fake) y **un test por regla que puede rechazar**, afirmando la excepción concreta. Por regla con
repositorio: encontrado / no encontrado / estado inválido.

**`infrastructure`** — mappers: campo ausente → `MissingRequestFieldException`; mal formado →
`MalformedRequestFieldException`; válido → el VO correcto. Controllers: delega al interactor y
responde el estado esperado. Adaptador de persistencia: solo con Testcontainers.

---

## Antipatrones — no generar nunca

| Antipatrón | Por qué |
|---|---|
| Usar Mockito | El proyecto no lo usa. Rompe la uniformidad y no hace falta con interfaces funcionales |
| `@SpringBootTest` para probar un use case | La lógica no necesita contexto; si lo necesita, está mal ubicada |
| Afirmar que una operación devuelve **500** | Un 500 es un fallo no previsto. Si el test lo espera, falta una excepción de negocio |
| Probar el mapeo HTTP dentro del test del use case | Eso es del `ApiErrorHandler`, que ya tiene su prueba |
| Un test que solo verifica que un mock fue llamado | Verifica el comportamiento, no la implementación |
| Reafirmar en `application` lo que ya afirma el test del VO | Duplicación; el VO ya está probado |
| Probar `{Slice}Configuration` | Es cableado, y está excluido de cobertura a propósito |
| Un test que necesita orden de ejecución | Cada test se sostiene solo |
| `Thread.sleep` en un test reactivo | `StepVerifier` ya controla el tiempo |

---

## Al cambiar la firma de un puerto

Los fakes son clases anónimas que implementan el puerto **completo**, así que **añadir o quitar un
método rompe la compilación de todos los tests que lo doblan**. Es intencional: el compilador te
obliga a mirarlos en vez de dejar un doble que miente.

Cuando eso pase, el método nuevo se implementa en cada fake con
`throw new UnsupportedOperationException();` salvo en la prueba que sí lo ejercita. En HU-001,
retirar `findAllByTenant` del puerto tocó cuatro clases de prueba de tres slices distintos.

**Es trabajo del implementador, no del planificador**, que tiene prohibido tocar `pdp/src/test`.

## Presupuesto orientativo

Para una historia con un caso de uso de escritura, un endpoint y dos reglas:

| Capa | Pruebas |
|---|---|
| `domain` | 3–5 por value object nuevo; 1–2 por entidad/enum |
| `application` | 1 camino feliz + 1 por rechazo posible (≈ 3–4) |
| `infrastructure` | 3–4 del request mapper, 1 del response mapper, 1–2 del controller |

Un total de **10–15 pruebas** es lo normal. Muchas más suele indicar duplicación entre capas; muchas
menos, que falta cubrir un rechazo.

---

## Integración con Testcontainers

Un test de persistencia extiende `AbstractSurrealDbIntegrationTest`, que arranca
`surrealdb/surrealdb` en memoria y publica la URL con `@DynamicPropertySource`. **Requiere Docker
en marcha**; si Docker no está disponible, el fallo es del entorno, no del código — repórtalo como
tal en vez de intentar sortearlo.

---

## Verificación arquitectónica — ya automatizada

No dupliques estas comprobaciones en lenguaje natural: **ya son pruebas**.

| Prueba | Qué garantiza |
|---|---|
| `LayeredArchitectureTests` | `application` ⊁ `infrastructure`; `domain` ⊁ `infrastructure`; `domain` ⊁ `application` |
| `ModulithStructureTests` | El mapa de dependencias entre módulos de `pdp` |

Si una historia introduce una violación de capa, la descubre `./mvnw -f pdp/pom.xml verify`, no el revisor.

---

## Reglas invariantes

1. Nada de Mockito: fakes anónimos y lambdas.
2. Un test de `domain` o `application` no levanta Spring.
3. `StepVerifier` para todo lo reactivo; nunca `block()`.
4. AssertJ en todas las aserciones.
5. `{Clase}Tests`, métodos en snake_case inglés descriptivo.
6. Ningún test afirma un 500.
7. Los métodos no usados de un fake lanzan `UnsupportedOperationException`.
8. Si probar algo exige un workaround de framework, la lógica está en la capa equivocada: detente y reporta.
