---
name: sb-estandares
description: Estándares de código de securityBaseline — idioma, value objects, jerarquía de excepciones y su mapeo HTTP, catálogos de mensajes, DTOs en dos niveles, reglas reactivas, cableado e inmutabilidad. Cargar junto con sb-arquitectura antes de implementar, testear o validar.
---

# Skill: sb-estandares

Complementa a `sb-arquitectura` (esa cubre capas y ubicación; esta cubre reglas de código
transversales). **Derivada del código real**, no de `docs/`. Cada regla apunta a un archivo abierto
con `Read` en vez de a un snippet: un snippet envejece en silencio, una ruta rota falla ruidosamente.

---

## Idioma

| Elemento | Idioma |
|---|---|
| Paquetes, clases, métodos, variables | **inglés** |
| Nombres de test | **inglés**, snake_case descriptivo (`rejects_a_null_value`) |
| Javadoc y comentarios | **español** — explican el porqué |
| Mensajes que ve el usuario (`*Messages`) | **español** |
| Documentación en `docs/` | **español** |

Ningún paquete nuevo se nombra en español. Los existentes (`pdp`, `commons`) se conservan.
**Ninguna prueba verifica esto** — es responsabilidad de la revisión, y por eso es uno de los cuatro
juicios del validador.

---

## Value objects

Todo VO es un `record` con validación en el **constructor compacto**, que normaliza y luego rechaza.
Ver `pdp/commons/TenantId.java` — el patrón exacto:

1. `null` → lanza con `ValueObjectMessages.VALUE_REQUIRED`
2. `value = value.trim()` (reasignación en el constructor compacto)
3. vacío tras el trim → lanza con `VALUE_REQUIRED`
4. formato/longitud → lanza con la constante anidada de su tipo

Reglas:
- **Un VO nunca acepta un valor inválido.** No hay estado intermedio ni `isValid()`.
- Cada VO tiene **su propia excepción**, que extiende `InvalidValueException`.
- El mensaje sale del catálogo, nunca de un literal en línea.
- Un VO usado por dos o más slices vive en `pdp/commons/`; con un solo consumidor, en su slice
  (`tenants/domain/TenantName.java`).

---

## Entidades y enums

- La entidad es un `record` con **factoría con nombre** (`Tenant.register(...)`) en vez de un
  constructor público desnudo, y con comportamiento (`isActive()`). Ver `tenants/domain/Tenant.java`.
- Los enums llevan comportamiento: `TenantStatus.allowsRegistration()`. Un enum que solo enumera y
  obliga a un `switch` en el use case está en el lugar equivocado.
- El constructor compacto valida sus componentes con
  `Objects.requireNonNull(x, RequiredArgumentMessages.X)`.

---

## Jerarquía de excepciones y su mapeo HTTP

Base en `pdp/commons/exception/`. Toda excepción de negocio lleva `(code, message)` y expone `code()`.

| Clase | Extiende | HTTP | Para |
|---|---|---|---|
| `DomainException` | `RuntimeException` | — | Raíz abstracta. Nunca se lanza directa |
| `InvalidValueException` | `DomainException` | **400** | Valor mal formado (value objects) |
| `BusinessRuleViolationException` | `DomainException` | **400** | Restricción de negocio violada |
| `ConflictBusinessRuleException` | `BusinessRuleViolationException` | **409** | Colisión de estado: duplicado |
| `RequestContractException` | `RuntimeException` (en `shared/web/exception/`) | **400** | El contrato HTTP se rompió; lleva `field()` |

Concretas de `shared/web/exception/`: `MissingRequestFieldException`,
`MalformedRequestFieldException`, `ConflictingRequestParametersException`.

Reglas:
- **Nunca `throw new RuntimeException(...)`** ni `IllegalArgumentException` en código de negocio.
- El `code` es una constante en mayúsculas con guion bajo: `TENANT_NOT_FOUND`, `INVALID_TENANT_ID`.
- Una excepción de VO va en `{slice}/domain/exception/`; una de regla, en `{slice}/application/exception/`.
- **Un slice nunca declara su propio `@RestControllerAdvice`.** El único handler es
  `shared/web/exceptionhandler/ApiErrorHandler.java`, que responde `ProblemDetail` y engancha por
  jerarquía base, no por clase concreta. Añadir una excepción nueva bajo una base existente **no**
  requiere tocarlo.
- El handler de `Exception` devuelve 500 sin datos técnicos. Nunca se filtra una traza al cliente.

---

## Catálogos de mensajes — cuatro, no los confundas

| Catálogo | Ruta | Para |
|---|---|---|
| `RequiredArgumentMessages` | `shared/message/` | Mensajes de `Objects.requireNonNull` — argumentos obligatorios |
| `ValueObjectMessages` | `pdp/commons/message/` | Razones de invalidez de VOs. Clases anidadas por VO: `ValueObjectMessages.TenantName.LENGTH` |
| `{Slice}Messages` | `{slice}/application/message/` | Mensajes de reglas de negocio del slice. Métodos estáticos que interpolan |
| `WebContractMessages` | `shared/web/message/` | Mensajes del contrato HTTP |

Todos son `final class` con constructor privado. **Cero literales de mensaje fuera de estos cuatro.**

---

## DTOs: dos niveles, tres barreras

No hay Bean Validation en el proyecto — `spring-boot-starter-validation` está retirado a propósito.
La validación vive en el tipo, no en una anotación.

```
CreateTenantRawRequest      record de Strings desnudos, sin anotaciones   (infrastructure)
        │  CreateTenantRequestMapper + RequestFieldParser.parse(...)      ← barrera 1: campo presente
        ▼                                                                 ← barrera 2: VO válido
CreateTenantRequest         record de value objects + requireNonNull      (application)  ← barrera 3
        │  use case
        ▼
TenantResponse              record de value objects                       (application)
        │  TenantResponseMapper
        ▼
TenantWebResponse           record de Strings planos                      (infrastructure)
```

Reglas:
- Todo campo del raw request es `String` (o `String` opcional en query). **Nunca un tipo rico ni un enum.**
- El mapper usa `RequestFieldParser.parse(campo, valor, VO::new)`, que convierte una
  `InvalidValueException` en `MalformedRequestFieldException` con el **nombre del campo**. Nunca
  construyas el VO a mano en el mapper: perderías el nombre del campo en el error.
- Para enteros, `RequestFieldParser.parseInt`. Para opcionales, `RequestFieldParser.optional`.
- **La respuesta web nunca expone value objects ni enums de dominio**: sale plana
  (`tenant.status().name()`).
- Los mappers son `final class` con constructor privado y métodos estáticos.

---

## Reglas de negocio (`Rule`)

- **Una regla, una restricción.** Si el nombre lleva "y", son dos reglas.
- Nombre en indicativo: `TenantCodeMustBeUniqueRule`, `TenantMustBeActiveRule`.
- Interfaz vacía en `rule/` extendiendo un contrato de `shared/contract`; implementación en `rule/impl/`.
- **Con I/O → reactiva. Sin I/O → síncrona.** Ver `TenantStatusMustBeActiveRule` (síncrona,
  `OperationWithoutResult<Tenant>`) frente a `TenantMustBeActiveRule` (reactiva, consulta el repo).
- Una regla que solo rechaza devuelve `Mono<Void>` y usa
  `.filter(...).flatMap(x -> Mono.error(new XException(...)))`. Ver `TenantCodeMustBeUniqueRuleImpl`.
- Una regla que además trae el dato devuelve el DTO y usa
  `.switchIfEmpty(Mono.error(() -> new XNotFoundException(...)))`. Ver `TenantMustBeActiveRuleImpl`.
- **Nunca `if/throw` suelto dentro del use case.** Si hay una condición de negocio, es una regla.
- Cuando un caso de uso necesita coordinar varias reglas, va un `rule/validator/` (ver `applications`).

---

## Estilo reactivo

- `Mono<T>` para uno o ninguno, `Flux<T>` para muchos, `Mono<Void>` para "solo valida" o "sin retorno".
- `Mono.error(() -> new X(...))` con **supplier** dentro de `switchIfEmpty`: sin el supplier la
  excepción se construye siempre, incluso en el camino feliz.
- `Mono.fromSupplier(...)` para diferir la construcción de la entidad hasta que las reglas pasaron.
- **No hay `block()` en `src/main`**, salvo en un `ApplicationRunner` de arranque
  (`SurrealTenantSchemaInitializer`), donde es correcto porque no está en el camino de una petición.
- Las reglas sin I/O son síncronas a propósito: envolverlas en `Mono` es ruido.

---

## Cableado e inmutabilidad

- **Cero anotaciones de Spring en `domain` y `application`.** El cableado vive en
  `{slice}/infrastructure/config/{Slice}Configuration.java`, con `@Bean` explícito.
- Toda implementación es `final class`; todo campo es `private final`.
- Todo argumento de constructor se valida con `Objects.requireNonNull(x, RequiredArgumentMessages.X)`.
  Si falta la constante, se añade a `RequiredArgumentMessages` — no se pone un literal.
- Los `record` que reciben colecciones copian: `content = List.copyOf(content)` (ver `PageResponse`).
- Una clase nueva de `application` que no se registre en su `Configuration` **no existe en runtime**.

---

## Controllers

- **package-private** y `final`. No `public class`.
- `@RestController` + `@RequestMapping("/api/v1/{recurso}")`.
- Recibe `ServerWebExchange`, obtiene `RequestContext` con `CorrelationWebFilter.context(exchange)`.
- Devuelve `Mono<ResponseEntity<ApiResponse<T>>>` envolviendo con
  `ApiResponse.success(code, WebContractMessages.xxx(), data, context)`.
- **Delega al interactor**, nunca al use case. No construye DTOs de aplicación ni decide reglas.
- El código de éxito es una constante en mayúsculas: `TENANT_CREATED`, `TENANTS_LISTED`.

---

## Persistencia (SurrealDB)

- El adaptador implementa el puerto y traduce entre `Entity` (Strings) y dominio con su
  `{X}PersistenceMapper`.
- El nombre de tabla vive en `{X}Schema.TABLE`; la consulta lo interpola con `.formatted(...)`.
- Los valores **siempre** van como parámetros (`Map.of("id", ...)`), nunca concatenados.
- El esquema se asegura en un `ApplicationRunner` (`Surreal{X}SchemaInitializer`).
- `SurrealRecordId.idPart(...)` extrae el id de un `record id` de Surreal.

---

## Seguridad y observabilidad

- El inquilino sale del principal autenticado (`shared/security/SecurityContext`, `PdpPrincipal`),
  **nunca del body ni de un query param**.
- Nada de `Instant.now()` ni `UUID.randomUUID()` en línea: van por `TimeProvider` e
  `IdentifierGenerator` (`shared/port/`).
- El contexto de log reactivo se aplica con `.transform(...)` usando `ReactiveLogContext`.
  No se crea `Observation` a mano.
- Nunca se registran tokens, contraseñas ni el cuerpo completo de una petición.

---

## Reglas invariantes

1. Código en inglés; mensajes de usuario y Javadoc en español.
2. Un VO nunca existe en estado inválido, y su mensaje sale del catálogo.
3. Nunca `RuntimeException`/`IllegalArgumentException` en negocio: siempre una hija de `DomainException`.
4. Un slice nunca crea su propio manejador de excepciones HTTP.
5. Cero literales de mensaje fuera de los cuatro catálogos.
6. El raw request es Strings desnudos; el mapper usa `RequestFieldParser`.
7. La respuesta web sale plana.
8. Una regla = una restricción. Sin I/O ⇒ síncrona.
9. Nunca `if/throw` de negocio dentro del use case.
10. Cero Spring en `domain` y `application`; el cableado es explícito en `{Slice}Configuration`.
11. Nada de `block()` en el camino de una petición.
