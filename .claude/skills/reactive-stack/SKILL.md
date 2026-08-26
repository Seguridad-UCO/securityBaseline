---
name: reactive-stack
description: >-
  Referencia del stack reactivo del PDP: Reactor (Mono/Flux), Spring WebFlux, Spring Security
  reactivo, Spring Modulith, Jackson 3, JUnit 5 + StepVerifier y Java 25. Incluye los operadores
  correctos por situación, las trampas específicas de Spring Boot 4 / Framework 7, y qué consultar
  en documentación externa antes de generar cada tipo de archivo. Usar en implementación y en
  generación de tests, después de cargar pdp-context.
---

# Skill: reactive-stack — Referencia del stack reactivo

> Complemento de `pdp-context`. Aquella dice **qué** construir; ésta dice **cómo** no
> equivocarse con la API del stack.

---

## Trampas específicas de este stack (leer siempre)

| Trampa | Detalle |
|---|---|
| **Jackson 3** | Spring Boot 4 usa `tools.jackson.databind.*`. Importar `com.fasterxml.jackson.*` compila pero rompe en runtime al mezclar `ObjectMapper`s. |
| **`RestTemplateBuilder` eliminado** | No existe en Boot 4. Para HTTP saliente usar `WebClient`. |
| **`@MockBean` eliminado** | En Boot 4 es `@MockitoBean` (paquete `org.springframework.test.context.bean.override.mockito`). |
| **No hay `HttpSession`** | WebFlux usa `WebSession` vía `exchange.getSession()` → retorna `Mono<WebSession>`. |
| **No hay `ThreadLocal` útil** | El request salta de hilo. Usar Reactor Context (`contextWrite` / `deferContextual`) o `ReactiveSecurityContextHolder`. |
| **`ReactiveJwtDecoder`** | `decode()` retorna `Mono<Jwt>`, no `Jwt`. |
| **Modulith sin Event Registry** | No hay backend de publicación persistente configurado. Usar `@EventListener`, **no** `@ApplicationModuleListener`. |
| **Java 25** | `--release 25`. Records, sealed, pattern matching y text blocks disponibles y recomendados. |

---

## Reactor — operadores por situación

| Necesitas | Operador | Nota |
|---|---|---|
| Transformar el valor | `.map(x -> y)` | Síncrono, sin I/O. |
| Encadenar otra operación reactiva | `.flatMap(x -> mono)` | Si usas `map` con algo que retorna `Mono`, terminas con `Mono<Mono<T>>`. |
| Valor por defecto si viene vacío | `.defaultIfEmpty(v)` | Para valores ya calculados. |
| Operación alternativa si viene vacío | `.switchIfEmpty(Mono.defer(() -> ...))` | **Siempre con `Mono.defer`**, si no la alternativa se evalúa con avidez aunque no haga falta. |
| Error si viene vacío | `.switchIfEmpty(Mono.error(new XNotFoundException(id)))` | Patrón estándar de "no encontrado". |
| Ejecutar y descartar el resultado | `.then(otro)` / `.then()` | Retorna `Mono<Void>` o el siguiente. |
| Ejecutar y conservar el original | `.flatMap(x -> otro.thenReturn(x))` | Ej: guardar y devolver lo guardado. |
| Varias operaciones en secuencia sobre una lista | `Flux.fromIterable(lista).flatMap(this::op).then()` | Para publicar N eventos. |
| Combinar dos independientes | `Mono.zip(a, b)` | Se ejecutan concurrentemente. |
| Efecto lateral sin cambiar el flujo | `.doOnNext(...)`, `.doOnError(...)` | Logging, métricas. Nunca lógica de negocio. |
| Recuperarse de un error concreto | `.onErrorResume(XException.class, e -> ...)` | No captures `Throwable` genérico. |
| Convertir vacío en error de dominio | `.switchIfEmpty(Mono.error(...))` | Preferido sobre `.blockOptional()`. |

### Anti-patrones Reactor (nunca los generes)

```java
// ❌ bloquea el event loop — rompe todo el modelo reactivo
var user = repository.findById(id).block();

// ❌ Mono anidado
return repository.findById(id).map(u -> repository.save(u));   // Mono<Mono<User>>

// ✅
return repository.findById(id).flatMap(repository::save);

// ❌ switchIfEmpty sin defer: crea el usuario SIEMPRE, aunque ya exista
.switchIfEmpty(createUser(dto))

// ✅
.switchIfEmpty(Mono.defer(() -> createUser(dto)))

// ❌ suscripción manual dentro de una cadena
mono.subscribe();

// ✅ devolver el Mono para que lo suscriba el framework
return mono;
```

---

## Spring WebFlux — controllers

```java
@RestController
@RequestMapping("/api/v1/{recurso}")
final class XController {

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<XWebResponse>>>> list(ServerWebExchange exchange) { }

    @GetMapping("/{id}")
    Mono<ResponseEntity<ApiResponse<XWebResponse>>> byId(@PathVariable String id,
            ServerWebExchange exchange) { }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<XWebResponse>>> create(@RequestBody XBodyRequest body,
            ServerWebExchange exchange) { }

    @PutMapping("/{id}")
    Mono<ResponseEntity<ApiResponse<XWebResponse>>> update(@PathVariable String id,
            @RequestBody XBodyRequest body, ServerWebExchange exchange) { }
}
```

- El controller **retorna** el `Mono`; nunca lo suscribe.
- `ServerWebExchange` como último parámetro cuando se necesita el `RequestContext`.
- Sin `@Valid`: la validación vive en el constructor compacto de los records de dominio y en los mappers. Los errores se traducen en el exception handler de `shared/web/exceptionhandler/`.

---

## Spring Security reactivo

```java
// Obtener el principal dentro de una cadena reactiva
SecurityContext.currentPrincipal()          // Mono<PdpPrincipal> — helper del proyecto
        .flatMap(principal -> useCase.execute(new XRequest(principal.tenantId(), ...)));

// Bajo el capó (no reimplementar):
ReactiveSecurityContextHolder.getContext()
        .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
        .map(Authentication::getPrincipal);
```

- **Nunca** `@AuthenticationPrincipal` en este proyecto (los use cases reciben un solo parámetro de negocio).
- **Nunca** leer tenant o subject del body/query — salen del token.

---

## Spring Modulith

```java
// package-info.java del módulo
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"commons", "tenants :: rule"})
package co.edu.uco.seguridad.pdp.{modulo};
```

- `"modulo"` → el paquete raíz del módulo (API pública).
- `"modulo :: nombre"` → una interfaz nombrada (sub-paquete exportado explícitamente).
- Verificación: `ModulithStructureTests`. Si falla, **no relajes la restricción** — reporta ambigüedad.

Listener de evento de dominio (en el módulo consumidor, capa infrastructure):

```java
@EventListener                      // NO @ApplicationModuleListener (sin registry configurado)
void on(XRegisteredEvent event) { ... }
```

---

## Testing reactivo — StepVerifier

```java
// Valor único
StepVerifier.create(useCase.execute(request))
        .assertNext(res -> assertThat(res.id()).isEqualTo(expectedId))
        .verifyComplete();

// Mono vacío
StepVerifier.create(repository.findByEmail(unknown))
        .verifyComplete();

// Error esperado
StepVerifier.create(useCase.execute(invalid))
        .expectErrorSatisfies(e -> assertThat(e)
                .isInstanceOf(TenantNotActiveException.class)
                .extracting(ex -> ((DomainException) ex).code())
                .isEqualTo("TENANT_NOT_ACTIVE"))
        .verify();

// Flux con varios elementos
StepVerifier.create(repository.findAll())
        .expectNextCount(3)
        .verifyComplete();

// Orden concreto
StepVerifier.create(useCase.execute())
        .assertNext(first -> assertThat(first.email()).isEqualTo("a@uco.edu.co"))
        .assertNext(second -> assertThat(second.email()).isEqualTo("b@uco.edu.co"))
        .verifyComplete();
```

### Fakes deterministas para los puertos

```java
private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

private final IdentifierGenerator identifiers = () -> FIXED_ID;
private final TimeProvider time = () -> FIXED_NOW;
```

### Controller con `@WebFluxTest`

```java
@WebFluxTest(controllers = XController.class)
class XControllerTests {

    @Autowired WebTestClient client;
    @MockitoBean XInteractor interactor;          // Boot 4: @MockitoBean, no @MockBean

    @Test
    void devuelveOkConElCatalogo() {
        when(interactor.execute()).thenReturn(Mono.just(List.of(sampleResponse())));

        client.get().uri("/api/v1/x")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("X_LISTED");
    }
}
```

---

## Java 25 — uso balanceado

| Feature | Cuándo sí | Cuándo no |
|---|---|---|
| `record` | Entidades, VOs, DTOs, eventos. **El caso por defecto.** | Cuando necesitas herencia o estado mutable (no debería pasar aquí). |
| `sealed interface` | Estados cerrados del dominio (`sealed interface EstadoPolitica permits Borrador, Vigente, Derogada`). | Conjuntos abiertos o que crecerán por configuración. |
| Pattern matching `switch` | Ramificación sobre un sealed type. | Un `if` simple de dos ramas. |
| Text blocks `"""` | Consultas SurrealQL de más de una línea. | Strings de una línea. |
| `var` | Cuando el tipo es evidente del lado derecho. | Cuando oculta el tipo de retorno de un método. |

**Regla de oro:** si la feature no aporta claridad al código concreto, no la uses.

---

## Consulta de documentación externa

Si tienes disponible el MCP **Context7** o la herramienta **WebFetch**, consulta antes de generar
código de una API que no domines. Referencias del stack:

| Tema | Dónde consultar |
|---|---|
| Reactor (Mono/Flux, operadores) | `projectreactor/reactor-core` · https://projectreactor.io/docs/core/release/reference/ |
| Spring WebFlux | https://docs.spring.io/spring-framework/reference/web/webflux.html |
| Spring Security reactivo | https://docs.spring.io/spring-security/reference/reactive/index.html |
| Spring Modulith | https://docs.spring.io/spring-modulith/reference/ |
| Spring Boot 4 (migración) | https://github.com/spring-projects/spring-boot/wiki |
| SurrealQL | https://surrealdb.com/docs/surrealql |
| StepVerifier | https://projectreactor.io/docs/core/release/reference/#testing |

**Antes de inventar una firma de método, consúltala.** Si no puedes consultarla y no estás
seguro, reporta la duda al usuario en vez de adivinar.
