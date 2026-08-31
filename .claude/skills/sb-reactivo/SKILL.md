---
name: sb-reactivo
description: Referencia del stack reactivo de securityBaseline — trampas de Spring Boot 4 / Jackson 3 / Java 25, operadores Reactor por situación, antipatrones, Spring Security reactivo, fronteras de Spring Modulith y StepVerifier. Cargar al implementar o al escribir pruebas de cualquier flujo reactivo.
---

# Skill: sb-reactivo

`sb-arquitectura` dice **qué** construir y **dónde**; esta dice **cómo no equivocarse con la API**
del stack. Rescatada del trabajo previo del equipo y **verificada contra el código real** el
2026-08-31.

---

## Trampas de este stack — leer siempre

Son cosas que compilan y luego fallan, o que un modelo entrenado con Spring Boot 3 da por buenas.

| Trampa | Detalle |
|---|---|
| **Jackson 3** | Spring Boot 4 usa `tools.jackson.databind.*`. Importar `com.fasterxml.jackson.*` **compila** pero rompe en runtime al mezclar `ObjectMapper`s. Verificado: los tres adaptadores Surreal importan `tools.jackson.databind.JsonNode`, y no hay un solo `com.fasterxml` en el repo |
| **`@MockBean` eliminado** | En Boot 4 es `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). Aquí da igual casi siempre: **este proyecto no usa Mockito** (ver `sb-testing`) |
| **`RestTemplateBuilder` eliminado** | Para HTTP saliente, `WebClient` |
| **No hay `HttpSession`** | WebFlux usa `WebSession` vía `exchange.getSession()` → devuelve `Mono<WebSession>` |
| **No hay `ThreadLocal` útil** | La petición salta de hilo. Usa el Contexto de Reactor (`contextWrite`/`deferContextual`) o `ReactiveSecurityContextHolder`. Para logs ya está `ReactiveLogContext` |
| **`ReactiveJwtDecoder`** | `decode()` devuelve `Mono<Jwt>`, no `Jwt` |
| **Modulith sin Event Registry** | No hay backend de publicación persistente. Usa `@EventListener`, **nunca** `@ApplicationModuleListener`. El porqué está documentado en `InMemoryAuditAdapter` |
| **Java 25** | `--release 25`. Records, sealed, pattern matching y text blocks disponibles y preferidos |

---

## Reactor — operador por situación

| Necesitas | Operador | Nota |
|---|---|---|
| Transformar el valor | `.map(x -> y)` | Síncrono, sin I/O |
| Encadenar otra operación reactiva | `.flatMap(x -> mono)` | Con `map` sobre algo que devuelve `Mono` acabas con `Mono<Mono<T>>` |
| Valor por defecto si viene vacío | `.defaultIfEmpty(v)` | Para valores ya calculados |
| Operación alternativa si viene vacío | `.switchIfEmpty(Mono.defer(() -> …))` | **Siempre con `defer`** |
| Error si viene vacío | `.switchIfEmpty(Mono.error(() -> new XNotFoundException(id)))` | Patrón de «no encontrado». El supplier evita construir la excepción en el camino feliz |
| Ejecutar y descartar | `.then(otro)` / `.then()` | Devuelve `Mono<Void>` o el siguiente |
| Ejecutar y conservar el original | `.flatMap(x -> otro.thenReturn(x))` | Guardar y devolver lo guardado |
| Diferir la construcción | `Mono.fromSupplier(() -> …)` | Construir la entidad solo después de que pasen las reglas |
| N operaciones en secuencia | `Flux.fromIterable(lista).concatMap(this::op).then()` | Publicar N eventos en orden |
| Combinar dos independientes | `Mono.zip(a, b)` | Concurrentes |
| Efecto lateral | `.doOnNext(…)`, `.doOnError(…)` | Logs y métricas. **Nunca lógica de negocio** |
| Recuperarse de un error concreto | `.onErrorResume(XException.class, e -> …)` | Nunca captures `Throwable` |

### Antipatrones — nunca los generes

```java
// ❌ bloquea el event loop
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
// ✅ devuelve el Mono y deja que lo suscriba el framework
return mono;
```

**Excepción única a la prohibición de `block()`:** un `ApplicationRunner` de arranque
(`Surreal{X}SchemaInitializer`), que no está en el camino de ninguna petición.

---

## Spring Security reactivo

```java
SecurityContext.currentPrincipal()      // Mono<PdpPrincipal> — helper del proyecto
        .flatMap(principal -> useCase.execute(mapper.toRequest(raw, principal.tenantId())));
```

- **Nunca** `@AuthenticationPrincipal`: el principal se obtiene en el interactor, no en el controller.
- **Nunca** leas el inquilino ni el sujeto del cuerpo o de la query. Salen del token.
- No reimplementes `ReactiveSecurityContextHolder`: para eso está `SecurityContext`.

---

## Spring Modulith — las fronteras son código

Cada módulo declara lo que puede usar en su `package-info.java`:

```java
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"commons", "tenants :: rule"})
package co.edu.uco.seguridad.pdp.{modulo};
```

- `"modulo"` → el paquete raíz del módulo (su API pública).
- `"modulo :: nombre"` → una interfaz nombrada (sub-paquete exportado explícitamente).
- `commons` y `shared` son `Type.OPEN`: cualquiera puede usarlos.

Fronteras declaradas hoy: `tenants → commons` · `applications → commons, tenants…` ·
`identity → commons, tenants, tenants :: rule` · `resources → commons, tenants, applications…`
(mira el `package-info.java` real antes de asumir).

> **Si `ModulithStructureTests` falla, no relajes `allowedDependencies` para que compile.**
> Cambiar una frontera entre módulos es una decisión de arquitectura y necesita su ADR en
> `security-platform-architecture`. Detente y repórtalo.

---

## StepVerifier

```java
// Valor único
StepVerifier.create(useCase.execute(request))
        .assertNext(res -> assertThat(res.id()).isEqualTo(expectedId))
        .verifyComplete();

// Mono vacío
StepVerifier.create(repository.findByEmail(unknown)).verifyComplete();

// Error por tipo
StepVerifier.create(useCase.execute(invalid))
        .expectError(DuplicateTenantException.class)
        .verify();

// Error comprobando también el código
StepVerifier.create(useCase.execute(invalid))
        .expectErrorSatisfies(e -> assertThat(e)
                .isInstanceOf(TenantNotActiveException.class)
                .extracting(ex -> ((DomainException) ex).code())
                .isEqualTo("TENANT_NOT_ACTIVE"))
        .verify();

// Flux: cantidad, u orden concreto
StepVerifier.create(repository.findAll()).expectNextCount(3).verifyComplete();
```

Los dobles de prueba son **fakes anónimos y lambdas**, no Mockito — ver `sb-testing`.
Los valores de tiempo e identificador se fijan en el test (`Instant.parse(...)`, un `UUID`
constante) para que el resultado sea determinista: por eso `TimeProvider` e `IdentifierGenerator`
se inyectan.

---

## Reglas invariantes

1. Nunca `block()`, `subscribe()` ni `Thread.sleep()` en el camino de una petición.
2. `map` que devuelve `Mono` es siempre un error: es `flatMap`.
3. `switchIfEmpty` con algo costoso va envuelto en `Mono.defer`.
4. `Mono.error` dentro de `switchIfEmpty` va con supplier.
5. Jackson es `tools.jackson`, nunca `com.fasterxml`.
6. Listeners con `@EventListener`, nunca `@ApplicationModuleListener`.
7. Una frontera de Modulith no se relaja para que compile.
