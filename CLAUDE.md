# securityBaseline — PDP de la Plataforma Central de Seguridad

Servicio Spring Boot **reactivo** que implementa el contenedor **PDP** (Policy Decision Point) del
mapa C4 aceptado. Autentica vía Keycloak (patrón BFF), persiste en SurrealDB y organiza el dominio
con Spring Modulith. Este repositorio también aloja al **PEP** ([`pep/`](pep/)) y a **OPA**
([`security-policy-engine/`](security-policy-engine/)) — ver [`docs/PLATAFORMA.md`](docs/PLATAFORMA.md)
para cómo encajan los tres.

> **Para trabajar una historia, usa el flujo de agentes** — ver [`.claude/README.md`](.claude/README.md).
> El diseño del harness y su porqué están en [`docs/ai-harness/README.md`](docs/ai-harness/README.md).
>
> **Cómo debe trabajar un agente** —qué no toca sin permiso, cómo consultar el grafo sin quemar
> contexto, qué validar antes de cerrar— está en [`AGENTS.md`](AGENTS.md), común a Claude Code y
> Codex. No se repite aquí para que no puedan divergir.

---

## Arrancar en local

```bash
docker compose up -d surrealdb keycloak
SPRING_PROFILES_ACTIVE=keycloak ./mvnw spring-boot:run
```

Sin Keycloak (JWT HMAC de desarrollo, útil para probar la API a mano):

```bash
./mvnw spring-boot:run
```

El realm local es `security-baseline` — ver [`keycloak/README.md`](keycloak/README.md).
Perfiles disponibles: `dev`, `keycloak`, `prod`.

## Comandos

**Prefiere las herramientas del harness**: devuelven un resumen en vez de cientos de líneas de log,
y resuelven solo el JDK que exige el POM.

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1            # verify
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/verificar.ps1 -Rapido    # test
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/mapa.ps1                 # PROJECT-MAP
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1                # deriva doc↔código
```

Maven directo, si hace falta:

```bash
./mvnw compile · ./mvnw test · ./mvnw verify
./mvnw test -Dtest=CreateTenantUseCaseImplTests
```

**Maven siempre (`./mvnw`), nunca Gradle. Java 25 requerido** — si `JAVA_HOME` apunta a otra
versión, Maven falla con `release version 25 not supported` aunque el código esté bien.
`verificar.ps1` lo resuelve por su cuenta. `verify` además requiere **Docker** (Testcontainers).

---

## Arquitectura en una pantalla

```
domain  ←  application  ←  infrastructure
```

- **`domain`** — Java puro. Cero Spring, cero Reactor, cero Jackson.
- **`application`** — conoce Reactor (`Mono`/`Flux`) pero **no** Spring. Sin anotaciones.
- **`infrastructure`** — única capa que conoce frameworks.

Verificado por `LayeredArchitectureTests` y `ModulithStructureTests`. Si tu cambio los rompe, el
problema es el cambio, no la prueba.

### Módulos Modulith

| Módulo | Bounded context | Estado |
|---|---|---|
| `pdp/tenants` | BC-01 Tenants | ✅ |
| `pdp/applications` | BC-02 Aplicaciones | ✅ |
| `pdp/resources` | BC-03 Recursos | ✅ |
| `pdp/identity` | BC-06 Usuarios + BC-07 Identidad | 🟡 parcial |
| — | BC-04/05/08/09/10/11 | ⏳ pendientes |

`pdp/commons` es el vocabulario de negocio compartido; `shared` son las capacidades técnicas.
Ambos son `Type.OPEN`.

Las fronteras se declaran en cada `package-info.java` con `@ApplicationModule(allowedDependencies=…)`.
**Cambiar una frontera es una decisión de arquitectura** — requiere ADR en
`security-platform-architecture`, no un ajuste al vuelo para que compile.

### Flujo de una petición

```
Controller  →  Interactor  →  UseCase  →  Rule(s)  →  Repository
  (HTTP)       (traduce)     (negocio)              (SurrealDB)
```

El interactor (ADR-016) existe para que el use case nunca reciba tipos de transporte.

---

## Convenciones que sorprenden

Este proyecto se aparta de varios patrones habituales de Spring. Antes de «corregir» algo que
parece raro:

| Convención | Por qué |
|---|---|
| **Entidades son `record` inmutables** | Sin setters. Factoría con nombre de negocio (`register`, `provision`); transiciones con `withX(...)` |
| **`AggregateRoot` es un wrapper, no una clase base** | `AggregateRoot.of(entidad, evento)`. Un campo de eventos dentro de un record rompería su igualdad estructural (ADR-017) |
| **Use cases sin `@Component`** | Todo se cablea con `@Bean` en `{Modulo}Configuration`, la única clase Spring de cada módulo. Una clase que no registres ahí **no existe en runtime** |
| **Un `UseCase` es una interfaz vacía** | Extiende un contrato de `shared/contract` y no declara métodos propios |
| **Sin Lombok** | Constructores y `Objects.requireNonNull(x, RequiredArgumentMessages.X)` explícitos |
| **Sin Bean Validation** | `spring-boot-starter-validation` está retirado. La validación vive en el VO y en el mapper |
| **`IdentifierGenerator` / `TimeProvider` inyectados** | Nada de `UUID.randomUUID()` ni `Instant.now()` en `application`: los tests deben ser deterministas |
| **SurrealDB sin ORM** | SurrealQL literal con parámetros (`$param` + `Map.of(...)`). Sin JPA, sin Flyway. El esquema lo crea un `ApplicationRunner` |
| **Eventos vía Spring, no RabbitMQ** | `DomainEventPublisher` devuelve `Mono<Void>`. Listeners con `@EventListener`, no `@ApplicationModuleListener` |
| **Jackson 3** | `tools.jackson.databind.*`. Importar `com.fasterxml.jackson.*` compila pero rompe en runtime |
| **Controllers package-private** | `final class XController`, sin `public` |
| **Sin Mockito** | Los dobles son lambdas y clases anónimas: los contratos son interfaces funcionales |
| **Tests terminan en `Tests`** | En plural. `StepVerifier` obligatorio para todo lo reactivo |
| **Código en inglés, mensajes en español** | Javadoc y textos de usuario en español; identificadores en inglés |

### Prohibido

- `block()`, `blockFirst()`, `Thread.sleep()`, `subscribe()` dentro de una cadena reactiva
  (única excepción: un `ApplicationRunner` de arranque)
- `map(...)` que devuelva un `Mono` — produce `Mono<Mono<T>>`; usa `flatMap`
- `switchIfEmpty(algoCostoso)` sin envolver en `Mono.defer(...)`
- Concatenar valores de usuario en SurrealQL
- `if/throw` de negocio dentro de un use case — eso es una `Rule`
- Que un slice declare su propio `@RestControllerAdvice`
- Leer el inquilino del cuerpo o de la query — sale del principal
- Imports con wildcard
- `@MockBean` en tests (Spring Boot 4 usa `@MockitoBean`)

---

## Estado de la línea base

**22 de los 23 criterios se cumplen.** HU-001 cerró los criterios 16 a 19 (consulta por
specification, puerto dinámico y ventana de paginación). Queda abierto **solo el criterio 10**:
`RemoveApplicationUseCase` existe como compensación y ningún caso de uso la invoca — no hay saga
cableada. Ver [`docs/criteria-compliance-matrix.md`](docs/criteria-compliance-matrix.md).

No des por cumplido un criterio porque una tabla lo diga: `drift.ps1` verifica que la
documentación no afirme lo que el código no sostiene — y desde ahora también vigila este archivo.

## Documentación

- Arquitectura y criterios de este repo: [`docs/`](docs/README.md)
- ADRs, dominio y contratos: repositorio hermano `security-platform-architecture`
- Event storming y modelos: repositorio hermano `artefactos-referencia`
- Cómo consultarlos: skill `sb-fuentes`
