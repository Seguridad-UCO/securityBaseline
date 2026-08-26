---
name: pdp-context
description: >-
  Contexto autoritativo del proyecto securityBaseline (PDP reactivo) para subagentes.
  Cargar SIEMPRE antes de planificar, implementar, testear o validar cualquier Historia de
  Usuario o Técnica. Contiene el stack verificado, la arquitectura hexagonal + DDD sobre
  Spring Modulith reactivo (WebFlux), el patrón de entidades como records inmutables, el
  AggregateRoot-como-wrapper, la capa de interactors (ADR-016), la persistencia SurrealDB
  sin ORM, los contratos ReactiveOperation y las plantillas de código canónicas extraídas
  del código real. Esta skill es la ÚNICA fuente de verdad del estado del proyecto — no
  leer README.md ni docs/ del repositorio de código.
---

# Skill: pdp-context — Contexto Autoritativo del PDP

## Propósito

Esta skill es el **único documento** que los subagentes deben consultar para entender el
estado del proyecto `securityBaseline`. Reemplaza cualquier lectura de:

- `README.md`, `docs/` del repositorio de código — documentación para humanos, puede estar desactualizada.
- `keycloak/README.md` — guía de setup local, no contexto de arquitectura.

**Regla dura:** si hay contradicción entre esta skill y cualquier otro documento del
repositorio, **gana esta skill**. Si hay contradicción entre esta skill y una ADR del repo
`security-platform-architecture`, **detente y reporta al usuario** — puede indicar que la
skill quedó desactualizada frente a una decisión nueva.

---

## Protocolo de Carga

Los subagentes cargan esta skill **al inicio de su flujo**, antes de hacer preguntas o generar código.

Tras cargarla, el subagente dispone de:
- Stack verificado (sección "Stack Verificado")
- Arquitectura hexagonal + Modulith reactiva (sección "Arquitectura")
- Los 11 bounded contexts de dominio y su estado real de implementación
- El modelo de entidades inmutables y `AggregateRoot` (sección "Modelo de Dominio")
- Plantillas canónicas de código (sección "Plantillas de Código")
- Reglas de test y anti-patrones (sección "Testing")

---

## Stack Verificado

Valores tomados de `pom.xml` real. **No inventar versiones.**

| Componente | Versión | Notas |
|---|---|---|
| Java | **25** | El POM lo exige. `--release 25`. |
| Spring Boot | **4.1.0** | Framework 7. Jackson **3** (`tools.jackson.databind`), no Jackson 2. |
| Build | **Maven** (wrapper) | `./mvnw` — **nunca** Gradle, nunca `mvn` global. |
| Spring WebFlux | (BOM) | **Todo el proyecto es reactivo.** No hay `spring-boot-starter-web`. |
| Spring Modulith | starter-core (BOM 2.1.0) | Módulos con `@ApplicationModule(allowedDependencies=...)`. |
| Spring Security | resource-server + oauth2-client | Dos perfiles: HMAC (default) y Keycloak (BFF). |
| SurrealDB | `surrealdb/surrealdb:latest` | **Sin driver oficial ni ORM.** Cliente propio sobre `WebClient` → `/sql`. |
| Keycloak | `quay.io/keycloak/keycloak:26.7.2` | Local en `:9090`, realm `security-baseline`. |
| Micrometer Tracing | bridge-otel | `X-Request-Id` / `X-Correlation-Id`. |
| JUnit 5 + Reactor Test | (BOM) | `StepVerifier` obligatorio para flujos reactivos. |
| ArchUnit | junit5 | `LayeredArchitectureTests` + `ModulithStructureTests` verifican las fronteras. |
| Testcontainers | boot-testcontainers | `AbstractSurrealDbIntegrationTest`. |
| JaCoCo | plugin | Reporte de cobertura. |

### Consecuencias directas del stack

1. **Reactivo de punta a punta** → todo retorna `Mono<T>` o `Flux<T>`. **Prohibido** `block()`, `.toFuture().get()`, `Thread.sleep` o cualquier llamada bloqueante dentro de la cadena.
2. **Jackson 3** → los imports son `tools.jackson.databind.*`, **no** `com.fasterxml.jackson.*`.
3. **Sin JPA, sin Hibernate, sin Flyway** → la persistencia es SurrealQL literal contra `SurrealDbClient`. Los esquemas se inicializan con un `ApplicationRunner` por módulo.
4. **Sin Lombok** → constructores y getters explícitos. Los records ya dan getters.
5. **Sin RabbitMQ / Kafka** → los eventos de dominio se publican con `DomainEventPublisher` sobre eventos de Spring (ADR-017).
6. **Maven** → `./mvnw`, nunca `./gradlew`.

---

## Arquitectura

### Dirección de dependencias (no negociable)

```
domain  ←  application  ←  infrastructure
```

- `domain`: **Java puro**. CERO imports de Spring, Reactor, Jackson, o cualquier framework. Solo `java.*` y `commons` del propio proyecto.
- `application`: importa `domain` + `reactor.core.publisher` (Mono/Flux) + `shared.contract`. **Sin anotaciones de Spring.**
- `infrastructure`: depende de ambas + frameworks completos. Es la única capa que conoce Spring, WebFlux, SurrealDB y Keycloak.

> **Diferencia clave frente a proyectos DDD "clásicos":** aquí `application` **sí** conoce
> Reactor (`Mono`/`Flux`), porque la asincronía es parte del contrato de la operación, no un
> detalle de infraestructura. Lo que `application` **no** conoce es Spring.

Verificado automáticamente por `LayeredArchitectureTests` (ArchUnit). Si tu código rompe esto, el test falla.

### Bounded Contexts

Los 11 contextos de dominio vienen del repo `security-platform-architecture`
(`docs/02-domain/03-bounded-contexts.md`). **Solo 4 están implementados como módulos Modulith.**

| ID | Contexto de dominio | Módulo Modulith | Estado |
|---|---|---|---|
| BC-01 | Tenants | `pdp/tenants` | ✅ Implementado |
| BC-02 | Aplicaciones | `pdp/applications` | ✅ Implementado |
| BC-03 | Recursos | `pdp/resources` | ✅ Implementado |
| BC-04 | Roles | — | ⏳ Pendiente |
| BC-05 | Perfiles | — | ⏳ Pendiente |
| BC-06 | Usuarios | `pdp/identity` (parcial) | 🟡 Parcial |
| BC-07 | Identidad y autenticación | `pdp/identity` (parcial) | 🟡 Parcial |
| BC-08 | Asignaciones | — | ⏳ Pendiente |
| BC-09 | Políticas de acceso | — | ⏳ Pendiente |
| BC-10 | Autorización | — | ⏳ Pendiente |
| BC-11 | Auditoría de seguridad | `pdp/resources/...secondary/audit` (stub in-memory) | 🟡 Stub |

> **BC-06 y BC-07 conviven en el módulo `identity`.** No los separes sin una ADR que lo decida.

### Fronteras Modulith declaradas (reales)

```java
// pdp/tenants/package-info.java
@ApplicationModule(allowedDependencies = "commons")

// pdp/applications/package-info.java
@ApplicationModule(allowedDependencies = {"commons", "tenants", "tenants :: dto", "tenants :: rule"})

// pdp/resources/package-info.java
@ApplicationModule(allowedDependencies = {"commons",
        "tenants", "tenants :: dto", "tenants :: rule",
        "applications", "applications :: repository", "applications :: exception"})

// pdp/identity/package-info.java
@ApplicationModule(allowedDependencies = {"commons", "tenants", "tenants :: rule"})
```

**Regla dura:** si tu HU necesita que un módulo dependa de otro que no está en su
`allowedDependencies`, **NO agregues la dependencia por tu cuenta** — repórtalo como
ambigüedad. Cambiar una frontera Modulith es una decisión de arquitectura, no de implementación.
`ModulithStructureTests` fallará si la violas.

### Módulos transversales

| Paquete | Contenido |
|---|---|
| `shared/contract` | `Operation`, `OperationWithoutResult`, `ReactiveOperation`, `ReactiveOperationWithoutInput`, `ReactiveOperationWithoutResult`, `ReactiveStreamOperation` |
| `shared/event` | `DomainEvent` (interface), `DomainEventPublisher` (puerto), `SpringDomainEventPublisher` (adaptador) |
| `shared/port` | `IdentifierGenerator`, `TimeProvider` |
| `shared/persistence/surrealdb` | `SurrealDbClient`, `SurrealDbException`, `SurrealDbProperties`, `SurrealRecordId` |
| `shared/web` | `ApiResponse`, `PageResponse`, `RequestContext`, `CorrelationWebFilter`, `RequestFieldParser`, `exceptionhandler/`, `session/` |
| `shared/security` | `PdpPrincipal`, `LocalUserPrincipal`, `SecurityContext`, `JwtSecurityProperties`, `CorsProperties`, `KeycloakSessionProperties` |
| `shared/config` | `SecurityConfiguration` (perfil default), `KeycloakSecurityConfiguration` (perfil `keycloak`) |
| `shared/auth` | Servicios OIDC/BFF: `OidcAuthenticationSuccessHandler`, `KeycloakOidcSessionService`, `OidcAuthorizationFlowService`, … |
| `pdp/commons` | `AggregateRoot`, `TenantId`, `ApplicationId`, `ApplicationName`, `ResourceId`, `PageWindow`, `ResultPage`, `exception/` |

> `pdp/commons` es vocabulario **del dominio PDP** (value objects compartidos, jerarquía de
> excepciones). `shared/*` es **contrato técnico** (Reactor, web, persistencia, seguridad).
> No mezclar: un `TenantId` va en `commons`; un `ReactiveOperation` va en `shared`.

### Estructura estándar de un módulo

```
pdp/{modulo}/
├── package-info.java                 # @ApplicationModule(allowedDependencies = {...})
├── domain/                           # Java puro
│   ├── {Entidad}.java                # record inmutable
│   ├── {ValueObject}.java            # record con validación en constructor compacto
│   ├── event/{Entidad}{Accion}Event.java   # implements DomainEvent
│   └── exception/{X}Exception.java   # extends DomainException (de commons)
├── application/
│   ├── port/
│   │   ├── primary/dto/request/{Accion}{Entidad}Request.java    # record
│   │   ├── primary/dto/response/{Entidad}Response.java          # record
│   │   └── secondary/repository/{Entidad}Repository.java        # interface, Mono/Flux
│   ├── rule/{Regla}Rule.java         # interface extends ReactiveOperation<I,O>
│   ├── rule/impl/{Regla}RuleImpl.java
│   ├── usecase/{Accion}{Entidad}UseCase.java        # interface extends ReactiveOperation<Req,Res>
│   ├── usecase/impl/{Accion}{Entidad}UseCaseImpl.java  # final class, sin anotaciones
│   └── exception/{X}NotFoundException.java
└── infrastructure/
    ├── adapter/
    │   ├── primary/web/
    │   │   ├── controller/{Entidad}Controller.java              # @RestController, package-private
    │   │   ├── dto/request/raw/{Accion}RawRequest.java          # record crudo (strings)
    │   │   ├── dto/request/raw/{Accion}BodyRequest.java         # record del body JSON
    │   │   ├── dto/response/{Entidad}WebResponse.java           # record
    │   │   ├── interactor/{Accion}Interactor.java               # interface (ADR-016)
    │   │   ├── interactor/impl/{Accion}InteractorImpl.java      # final class
    │   │   └── mapper/{X}Mapper.java                            # static, sin estado
    │   └── secondary/persistence/
    │       ├── repository/Surreal{Entidad}Repository.java       # implements el puerto
    │       └── schema/{Modulo}Schema.java                       # constantes de tabla
    │       └── schema/Surreal{Modulo}SchemaInitializer.java     # ApplicationRunner
    ├── config/{Modulo}Configuration.java   # ← ÚNICA clase con @Configuration del módulo
    └── properties/{Modulo}Properties.java  # @ConfigurationProperties
```

**Regla dura:** `{Modulo}Configuration` es la **única** clase del módulo consciente de Spring
(aparte del `@RestController`). Todos los use cases, interactors, reglas y repositorios se
cablean ahí con `@Bean` explícito. **Prohibido** `@Component`, `@Service`, `@Repository`,
`@Autowired` o `@RequiredArgsConstructor` en `application/` o en las clases `*Impl`.

---

## Modelo de Dominio

### Entidades = records inmutables (NO clases con setters)

Este proyecto **no** usa el patrón "entidad mutable con constructor privado + `build()`/`rebuild()`".
Las entidades son `record` inmutables con validación en el constructor compacto:

```java
public record SecurityUser(UserId id, TenantId tenantId, Email email, String name,
        Instant createdAt, Instant lastLoginAt) {

    public SecurityUser {
        Objects.requireNonNull(id, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(email, RequiredArgumentMessages.USER_EMAIL);
        Objects.requireNonNull(createdAt, RequiredArgumentMessages.REGISTERED_AT);
        Objects.requireNonNull(lastLoginAt, RequiredArgumentMessages.REGISTERED_AT);
        name = name == null ? "" : name.trim();       // normalización en el constructor
    }

    /** Factory de creación — nombre del negocio, no "build". */
    public static SecurityUser provision(UserId id, TenantId tenantId, Email email,
            String name, Instant now) {
        return new SecurityUser(id, tenantId, email, name, now, now);
    }

    /** Transición de estado = nueva instancia (wither). NUNCA un setter. */
    public SecurityUser withLogin(String name, Instant now) {
        return new SecurityUser(id, tenantId, email, name, createdAt, now);
    }
}
```

**Reglas:**
- Validación de invariantes → constructor compacto, con `Objects.requireNonNull(x, RequiredArgumentMessages.X)`.
- Normalización (trim, lowercase) → también en el constructor compacto.
- Creación → `static` factory con nombre del negocio (`provision`, `register`, `open`…), **no** `build`.
- Cambio de estado → método `withX(...)` que retorna una **nueva** instancia.
- **No hay `rebuild()`**: reconstruir desde persistencia es simplemente invocar el constructor canónico desde el adaptador.

### Value Objects

`record` de un solo componente, con validación:

```java
public record Email(String value) {
    public Email {
        Objects.requireNonNull(value, RequiredArgumentMessages.USER_EMAIL);
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException(value);
        }
    }
}
```

VOs compartidos ya existentes en `pdp/commons`: `TenantId`, `ApplicationId`, `ApplicationName`,
`ResourceId`, `PageWindow`, `ResultPage`. **Reutilízalos, no los dupliques por módulo.**

### AggregateRoot = wrapper, NO clase base

```java
public record AggregateRoot<T, E extends DomainEvent>(T entity, List<E> domainEvents) {
    public static <T, E extends DomainEvent> AggregateRoot<T, E> of(T entity, E event) { ... }
}
```

**No se extiende.** Como las entidades son records inmutables, un campo interno de eventos
rompería su igualdad estructural (ADR-017). Por eso el agregado **empareja** entidad + eventos:

```java
// En el use case, cuando la operación produce un evento:
AggregateRoot<Application, ApplicationRegisteredEvent> aggregate =
        AggregateRoot.of(application, new ApplicationRegisteredEvent(application.id(), time.now()));

return repository.save(aggregate.entity())
        .flatMap(saved -> Flux.fromIterable(aggregate.domainEvents())
                .flatMap(publisher::publish)
                .then(Mono.just(saved)));
```

**No hay `getUnPublishedEvents()` / `clearUnPublishedEvents()`** — ese es el patrón del otro
proyecto. Aquí el agregado es inmutable y de un solo uso.

### Eventos de dominio

```java
public interface DomainEvent {
    Instant occurredOn();
}
```

Un evento concreto es un `record` que lo implementa, en `domain/event/`:

```java
public record ApplicationRegisteredEvent(ApplicationId applicationId, TenantId tenantId,
        Instant occurredOn) implements DomainEvent {
    public ApplicationRegisteredEvent {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.OCCURRED_ON);
    }
}
```

Publicación vía el puerto (**nunca** `ApplicationEventPublisher` de Spring directo desde el use case):

```java
public interface DomainEventPublisher {
    Mono<Void> publish(DomainEvent event);
}
```

**¿Cuándo emitir un evento?**
- **Sí** — otro módulo Modulith necesita reaccionar al hecho, o hay un caso de auditoría (BC-11).
- **No** — CRUD interno sin consumidores. En ese caso el use case **no** inyecta `DomainEventPublisher` y no se crea la clase de evento.

> Spring Modulith **no** trae backend de Event Publication Registry configurado en este
> proyecto. Los listeners usan `@EventListener`, **no** `@ApplicationModuleListener`
> (ver el comentario en `InMemoryAuditAdapter`). No cambies esto sin ADR.

### Excepciones

Jerarquía en `pdp/commons/exception/`:

```
DomainException (abstract, tiene code())
├── BusinessRuleViolationException
├── ConflictBusinessRuleException
├── InvalidValueException
├── InvalidIdentifierException
├── InvalidTenantIdException
├── InvalidApplicationNameException
└── InvalidPageWindowException
```

- Toda excepción de dominio **extiende** una de estas, nunca `RuntimeException` directo.
- El dominio **nunca conoce HTTP**: publica un `code()` estable que el handler de
  `shared/web/exceptionhandler/` traduce una sola vez a la respuesta HTTP.
- Excepciones de "no encontrado" a nivel de caso de uso van en `{modulo}/application/exception/`.

### Puertos de determinismo

**Prohibido** `UUID.randomUUID()` e `Instant.now()` dentro de `application/`. Se inyectan:

```java
public interface IdentifierGenerator { UUID next(); }
public interface TimeProvider { Instant now(); }
```

Razón: los tests deben ser deterministas. Un use case que llame directo a `UUID.randomUUID()`
es un defecto, no una preferencia de estilo.

---

## Contratos de Operación

Todo puerto primario (use case, regla, interactor) extiende uno de estos:

| Contrato | Firma | Uso |
|---|---|---|
| `ReactiveOperation<I, O>` | `Mono<O> execute(I input)` | El caso normal. |
| `ReactiveOperationWithoutInput<O>` | `Mono<O> execute()` | Listados sin filtro, consultas de contexto. |
| `ReactiveOperationWithoutResult<I>` | `Mono<Void> execute(I input)` | Comandos sin retorno. |
| `ReactiveStreamOperation<I, O>` | `Flux<O> execute(I input)` | Streaming real (raro). |
| `Operation<I, O>` / `OperationWithoutResult<I>` | síncronos | Solo mappers/validaciones puras. **No** para I/O. |

```java
public interface ProvisionIdentityUseCase
        extends ReactiveOperation<ProvisionIdentityRequest, LocalUserPrincipal> { }
```

La interfaz del use case queda **vacía** — solo declara los tipos. La implementación va en `usecase/impl/`.

### Reglas de negocio publicadas (`application/rule/`)

Una regla reutilizable entre módulos también es un `ReactiveOperation`:

```java
public interface TenantMustBeActiveRule extends ReactiveOperation<TenantId, TenantResponse> { }
```

Se exportan vía Modulith como `"tenants :: rule"` para que otros módulos las consuman sin
tocar el dominio ajeno. Si tu HU necesita validar algo que otro módulo ya sabe validar,
**consume su regla** — no dupliques la lógica.

---

## Persistencia — SurrealDB sin ORM

No hay JPA, ni repositorios derivados, ni migraciones Flyway. El adaptador escribe SurrealQL:

```java
public final class SurrealSecurityUserRepository implements SecurityUserRepository {

    private final SurrealDbClient client;

    public SurrealSecurityUserRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<SecurityUser> findByEmail(Email email) {
        return client.execute(
                        "SELECT * FROM %s WHERE email = $email LIMIT 1;".formatted(IdentitySchema.USER_TABLE),
                        Map.of("email", email.value()))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toUser(rows.get(0))));
    }
}
```

**Reglas de persistencia:**
- Nombres de tabla → constantes en `{Modulo}Schema` (`IdentitySchema.USER_TABLE`), nunca literales dispersos.
- Parámetros → **siempre** bind con `Map.of(...)` y `$param`. **Nunca** concatenar valores de usuario en el SQL.
- El nombre de la tabla sí se interpola con `.formatted(...)` porque viene de una constante del código, no del usuario.
- Mapeo `JsonNode` → dominio en métodos `private static` del propio adaptador (`toUser`, `toIdentity`).
- El esquema se crea con un `ApplicationRunner` (`Surreal{Modulo}SchemaInitializer`), registrado como `@Bean` en la configuración del módulo.
- Jackson **3**: el import es `tools.jackson.databind.JsonNode`.
- No hay transacciones multi-tabla — SurrealDB por HTTP no las soporta para este caso. Si una operación toca varias tablas, usa el patrón **saga con compensación** (ADR-019).

Config local (`application.properties`): `pdp.persistence.surrealdb.url=http://localhost:8000`,
namespace `pdp`, database `pdp`, usuario/clave `root`/`root`.

---

## Capa Web e Interactors (ADR-016)

El flujo de una petición es:

```
Controller  →  Interactor  →  UseCase  →  Repository
   (HTTP)      (traduce)     (negocio)    (SurrealDB)
```

**El interactor existe para que el use case no reciba tipos de transporte.** Traduce el
`RawRequest` (strings crudos del HTTP) al DTO de aplicación, y la respuesta de aplicación al
`WebResponse`.

```java
public final class ListUsersInteractorImpl implements ListUsersInteractor {

    private final ListUsersUseCase useCase;

    public ListUsersInteractorImpl(ListUsersUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<List<UserWebResponse>> execute() {
        return useCase.execute().map(UserResponseMapper::toResponseList);
    }
}
```

Controller — **package-private**, sin lógica, con `ApiResponse` y `RequestContext`:

```java
@RestController
@RequestMapping("/api/v1/users")
final class UserController {

    private final ListUsersInteractor listInteractor;

    UserController(ListUsersInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<UserWebResponse>>>> list(ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute()
                .map(users -> ResponseEntity.ok(ApiResponse.success("USERS_LISTED",
                        WebContractMessages.successCatalogQueried(), users, context)));
    }
}
```

**Reglas web:**
- El controller es `final` y **package-private** (sin `public`). No se testea vía Spring context completo salvo `@WebFluxTest`.
- Toda respuesta va envuelta en `ApiResponse.success(codigo, mensaje, datos, context)`.
- El `RequestContext` se obtiene con `CorrelationWebFilter.context(exchange)`, nunca se construye a mano.
- El código de resultado (`"USERS_LISTED"`) es una constante estable de contrato, en SCREAMING_SNAKE_CASE.
- Los mensajes salen de `WebContractMessages`, no se escriben inline.
- Los mappers son clases con métodos `static` puros, sin estado ni inyección.

---

## Seguridad

Dos configuraciones **mutuamente excluyentes** por perfil (ver `shared/config/`):

| Perfil | Clase | Validación de token |
|---|---|---|
| `!keycloak` (default) | `SecurityConfiguration` | HMAC con `pdp.security.jwt.secret` |
| `keycloak` | `KeycloakSecurityConfiguration` | RS256 vía JWKS + `oauth2Login` (BFF) |

- `JwtSecurityProperties` impone un **XOR** en el constructor: exactamente uno de `secret` / `jwkSetUri`. Si ambos o ninguno → la app no arranca.
- Claims obligatorios validados en el decoder: `sub`, `tenant`, `jti`, `aud` (debe contener `pdp.security.jwt.audience`), más `iss` y timestamps.
- El principal se obtiene con `SecurityContext.currentPrincipal()` → `Mono<PdpPrincipal>`. **Nunca** con `@AuthenticationPrincipal` (los use cases reciben un solo parámetro de negocio).
- Tenant y sujeto salen **siempre del token**, nunca del body ni del query string.
- **Hoy la autorización es binaria** (autenticado / no autenticado). No existe `hasRole` ni `hasAuthority` en el proyecto. Si una HU requiere autorización granular, **repórtalo como decisión de arquitectura pendiente** — es precisamente lo que los BC-04/05/08/09/10 van a resolver.
- CSRF solo se exige cuando hay cookie `SECURITY_BASELINE_SESSION` (patrón BFF). Cookie: `HttpOnly`, `SameSite=Lax`, `Secure` por propiedad.

---

## Testing

### Convenciones

- Los archivos de test terminan en `Tests` (plural), no `Test`. Ej: `ProvisionIdentityUseCaseImplTests`.
- Ubicación espejo del código: `src/test/java/<mismo paquete>/`.
- Patrón AAA (Arrange–Act–Assert) explícito.
- **`StepVerifier` es obligatorio** para todo lo que retorne `Mono`/`Flux`:

```java
StepVerifier.create(useCase.execute(request))
        .assertNext(principal -> assertThat(principal.tenantId()).isEqualTo(expectedTenant))
        .verifyComplete();
```

- **Prohibido `block()` en tests.** Si necesitas el valor, usa `StepVerifier` o `.as(StepVerifier::create)`.
- Dobles de prueba: implementaciones **fake** del puerto (clases pequeñas en el propio test) o Mockito. Preferir fakes para repositorios — son más legibles que 6 `when(...)` encadenados.
- `IdentifierGenerator` y `TimeProvider` se inyectan como fakes fijos (`() -> UUID.fromString("...")`, `() -> Instant.parse("...")`) para que el test sea determinista.

### Qué testear por capa

| Capa | Qué se testea | Qué NO |
|---|---|---|
| `domain` | Invariantes del constructor compacto, normalización, factories, withers, VOs. | Getters de records (los genera el compilador). |
| `application` | Flujo del use case: ramas de decisión, reglas invocadas, evento publicado o no. | Que Reactor funcione. Que el repositorio real funcione. |
| `infrastructure` | Mappers (entrada y salida), controller vía `@WebFluxTest`, adaptador Surreal vía Testcontainers. | Serialización de Jackson campo por campo. |

### Anti-patrones (no los generes)

- Un test por getter de un record.
- Testear que `Objects.requireNonNull` lanza NPE en cada campo cuando ya hay un test representativo del constructor.
- Mockear `SurrealDbClient` para verificar el string SQL exacto — testea el comportamiento (qué retorna), no la sintaxis literal.
- Tests de "ciclo de eventos" en HUs de solo consulta — las consultas no emiten eventos.

### Presupuesto orientativo

| Tamaño de HU | Tests esperados |
|---|---|
| Pequeña (1 endpoint, 1 entidad) | 12 – 20 |
| Mediana (2-3 endpoints) | 20 – 40 |
| Grande (4+ endpoints o flujo con reglas cruzadas) | 40 – 65 |
| Más de 65 | revisar — casi siempre es sobre-testeo |

### Tests de arquitectura (ya existen, no los toques sin razón)

- `LayeredArchitectureTests` — verifica la dirección de dependencias entre capas.
- `ModulithStructureTests` — verifica las fronteras `@ApplicationModule`.
- `AbstractSurrealDbIntegrationTest` — base con Testcontainers para adaptadores reales.

---

## Comandos del proyecto

```bash
./mvnw compile                      # compilar
./mvnw test                         # tests
./mvnw verify                       # build completo + jacoco
./mvnw spring-boot:run              # arrancar (perfil default, HMAC)
SPRING_PROFILES_ACTIVE=keycloak ./mvnw spring-boot:run   # arrancar contra Keycloak local
./mvnw test -Dtest=ProvisionIdentityUseCaseImplTests     # un test puntual
```

Infra local: `docker compose up -d keycloak surrealdb` desde la raíz del repo.
Keycloak `:9090` (admin/admin, realm `security-baseline`), SurrealDB `:8000` (root/root).

---

## Nomenclatura

- **Bilingüe deliberado:** el vocabulario del dominio en el idioma de la ADR que lo definió; los sufijos técnicos siempre en inglés (`UseCase`, `Repository`, `Interactor`, `Mapper`, `Controller`, `Request`, `Response`, `Event`, `Rule`).
- Clases de implementación: sufijo `Impl` (`ProvisionIdentityUseCaseImpl`).
- Tests: sufijo `Tests`.
- Códigos de respuesta y de error: `SCREAMING_SNAKE_CASE` estable (`USERS_LISTED`, `TENANT_NOT_ACTIVE`).
- Mensajes de argumento requerido: constantes en `RequiredArgumentMessages`, nunca strings inline.
- Imports **explícitos**, nunca wildcard.

---

## Reglas Invariantes (resumen ejecutable)

1. **Todo es reactivo.** Nada de `block()`, `Thread.sleep`, ni llamadas bloqueantes en la cadena.
2. **`domain` es Java puro.** Cero Spring, cero Reactor, cero Jackson.
3. **`application` conoce Reactor pero no Spring.** Sin anotaciones.
4. **Entidades = records inmutables.** Factories con nombre de negocio, withers para transiciones, validación en constructor compacto.
5. **`AggregateRoot` se usa como wrapper, no se extiende.**
6. **Sin Lombok, sin `@Component` en use cases.** Todo se cablea en `{Modulo}Configuration` con `@Bean`.
7. **`IdentifierGenerator` y `TimeProvider` siempre inyectados.** Nunca `UUID.randomUUID()` ni `Instant.now()` en `application`.
8. **SurrealQL con parámetros bind.** Tablas desde `{Modulo}Schema`. Jackson 3.
9. **Fronteras Modulith son intocables** sin ADR. Si tu HU las necesita cambiar → reporta ambigüedad.
10. **Controller package-private, respuesta con `ApiResponse` + `RequestContext`.**
11. **Interactor obligatorio** entre controller y use case (ADR-016).
12. **Tests con `StepVerifier`**, nombres terminados en `Tests`, fakes deterministas.
13. **Maven (`./mvnw`)**, Java 25, nunca Gradle.
14. **Autorización granular no existe todavía** — si la HU la pide, es decisión de arquitectura.
15. **Reutiliza los VO de `pdp/commons`** (`TenantId`, `ApplicationId`, `ResourceId`, `PageWindow`, `ResultPage`) en vez de crear duplicados.
