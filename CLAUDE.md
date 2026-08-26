# securityBaseline — PDP de la Plataforma Central de Seguridad

Servicio Spring Boot **reactivo** que implementa el contenedor **PDP** (Policy Decision Point)
del mapa C4 aceptado. Autentica vía Keycloak (patrón BFF), persiste en SurrealDB y organiza el
dominio con Spring Modulith.

> **Para trabajar una Historia de Usuario o Técnica, usa el flujo de agentes:**
> `@orquestador vamos con la HU-XXX` — ver [`.claude/README.md`](.claude/README.md).
> El contexto autoritativo del proyecto para los agentes está en `.claude/skills/pdp-context/SKILL.md`.

---

## Arrancar en local

```bash
docker compose up -d keycloak surrealdb        # Keycloak :9090 (admin/admin) · SurrealDB :8000 (root/root)
SPRING_PROFILES_ACTIVE=keycloak ./mvnw spring-boot:run
```

Sin Keycloak (JWT HMAC de desarrollo, útil para probar la API):

```bash
./mvnw spring-boot:run
```

El realm local se llama `security-baseline` y hay que crearlo a mano la primera vez —
ver [`keycloak/README.md`](keycloak/README.md).

## Comandos

```bash
./mvnw compile          # compilar
./mvnw test             # tests
./mvnw verify           # build completo + JaCoCo
./mvnw test -Dtest=ProvisionIdentityUseCaseImplTests    # un test puntual
```

**Maven siempre (`./mvnw`), nunca Gradle.** Java 25 requerido.

---

## Arquitectura en una pantalla

```
domain  ←  application  ←  infrastructure
```

- **`domain`** — Java puro. Cero Spring, cero Reactor, cero Jackson.
- **`application`** — conoce Reactor (`Mono`/`Flux`) pero **no** Spring. Sin anotaciones.
- **`infrastructure`** — única capa que conoce frameworks.

Verificado por `LayeredArchitectureTests` y `ModulithStructureTests` (ArchUnit). Si tu cambio
los rompe, el problema es el cambio, no el test.

### Módulos Modulith

| Módulo | Bounded context | Estado |
|---|---|---|
| `pdp/tenants` | BC-01 Tenants | ✅ |
| `pdp/applications` | BC-02 Aplicaciones | ✅ |
| `pdp/resources` | BC-03 Recursos | ✅ |
| `pdp/identity` | BC-06 Usuarios + BC-07 Identidad | 🟡 parcial |
| — | BC-04/05/08/09/10/11 | ⏳ pendientes |

Las fronteras se declaran en cada `package-info.java` con `@ApplicationModule(allowedDependencies=...)`.
**Cambiar una frontera es una decisión de arquitectura** — requiere ADR en el repo
`security-platform-architecture`, no un ajuste al vuelo para que compile.

### Flujo de una petición

```
Controller  →  Interactor  →  UseCase  →  Repository
  (HTTP)       (traduce)     (negocio)   (SurrealDB)
```

El interactor (ADR-016) existe para que el use case nunca reciba tipos de transporte.

---

## Convenciones que sorprenden

Este proyecto se aparta de varios patrones habituales de Spring. Antes de "corregir" algo que
parece raro, ten en cuenta:

| Convención | Por qué |
|---|---|
| **Entidades son `record` inmutables** | Sin setters. Factory con nombre de negocio (`provision`, `register`), transiciones con `withX(...)`. |
| **`AggregateRoot` es un wrapper, no una clase base** | `AggregateRoot.of(entidad, evento)`. Un campo de eventos dentro de un record rompería su igualdad estructural (ADR-017). |
| **Use cases sin `@Component`** | Todo se cablea explícitamente con `@Bean` en `{Modulo}Configuration` — la única clase Spring de cada módulo. |
| **Sin Lombok** | Constructores y `Objects.requireNonNull(x, RequiredArgumentMessages.X)` explícitos. |
| **`IdentifierGenerator` / `TimeProvider` inyectados** | Nada de `UUID.randomUUID()` ni `Instant.now()` en `application` — los tests deben ser deterministas. |
| **SurrealDB sin ORM** | SurrealQL literal con parámetros bind (`$param` + `Map.of(...)`). Sin JPA, sin Flyway. El esquema lo crea un `ApplicationRunner`. |
| **Eventos vía Spring, no RabbitMQ** | `DomainEventPublisher` retorna `Mono<Void>`. Listeners con `@EventListener`, no `@ApplicationModuleListener`. |
| **Jackson 3** | `tools.jackson.databind.*`. Importar `com.fasterxml.jackson.*` compila pero rompe en runtime. |
| **Controllers package-private** | `final class XController`, sin `public`. |
| **Tests terminan en `Tests`** | Plural. `StepVerifier` obligatorio para todo lo reactivo. |

### Prohibido

- `block()`, `blockFirst()`, `Thread.sleep()`, `subscribe()` dentro de una cadena reactiva
- `map(...)` que retorne un `Mono` (produce `Mono<Mono<T>>` — usa `flatMap`)
- `switchIfEmpty(algoCostoso)` sin envolver en `Mono.defer(...)`
- Concatenar valores de usuario en SurrealQL
- Imports con wildcard
- `@MockBean` en tests (Spring Boot 4 usa `@MockitoBean`)

---

## Seguridad

Dos perfiles mutuamente excluyentes:

| Perfil | Clase | Validación |
|---|---|---|
| default | `SecurityConfiguration` | HMAC con `pdp.security.jwt.secret` |
| `keycloak` | `KeycloakSecurityConfiguration` | RS256 vía JWKS + `oauth2Login` (BFF) |

`JwtSecurityProperties` impone un XOR: exactamente uno de `secret`/`jwkSetUri`. Si configuras
ambos o ninguno, la app no arranca.

El principal se obtiene con `SecurityContext.currentPrincipal()`. Tenant y sujeto salen
**siempre del token**, nunca del body ni del query string.

**La autorización hoy es binaria** (autenticado / no autenticado). No existe `hasAuthority` ni
roles de cliente. La autorización granular la resolverán los BC-04/05/08/09/10 — si una historia
la necesita, es una decisión de arquitectura pendiente, no algo a improvisar.

---

## Documentación de arquitectura

Vive en el repo hermano [`security-platform-architecture`](https://github.com/Seguridad-UCO/security-platform-architecture):
ADRs, event storming, modelo de dominio anémico/enriquecido, diagramas C4, drivers arquitectónicos.

Clónalo como hermano de este repo para que los agentes puedan consultarlo:

```
Desktop/Semillero/
├── securityBaseline/                  ← aquí
├── securityBaseline-fr/
├── securityBaseline-infra/
└── security-platform-architecture/
```

ADRs más citadas: **009** (Modulith) · **015/019** (SurrealDB) · **016** (interactors) ·
**017** (eventos de dominio) · **018** (JWT reactivo) · **020** (Keycloak) · **022** (auth frontend).
