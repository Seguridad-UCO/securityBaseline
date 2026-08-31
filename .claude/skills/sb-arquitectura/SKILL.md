---
name: sb-arquitectura
description: Arquitectura real de securityBaseline (PDP) — modulith de slices verticales, Clean Architecture reactiva, contratos base, dónde vive cada tipo de archivo y qué dependencias están permitidas. Cargar antes de planificar, implementar, testear o validar cualquier historia. El slice de referencia es siempre tenants.
---

# Skill: sb-arquitectura

Fuente de verdad para agentes. **Derivada del código, no de `docs/`.** La documentación describe la
intención del proyecto y ya se saneó, pero envejece con cada refactorización: para convención manda
el código. `.claude/tools/drift.ps1` verifica que ni `docs/` ni estas skills afirmen lo que el
código no sostiene — **córrelo cuando cambies una convención aquí**.

**Regla de esta skill:** ningún ejemplo se pega como bloque de código. Cada fila apunta al archivo
real de `tenants` — el slice más pequeño y completo, y el patrón a copiar. Ábrelo con `Read` cuando
necesites el detalle exacto.

Para nomenclatura, excepciones, mensajes, DTOs y estilo, ver `sb-estandares`. Para pruebas, `sb-testing`.

---

## Los dos árboles de primer nivel

| Paquete | Qué es | Spring permitido |
|---|---|---|
| `co.edu.uco.seguridad.shared` | Capacidad **técnica** transversal: web, seguridad, persistencia SurrealDB, eventos, observabilidad, contratos base | Sí |
| `co.edu.uco.seguridad.pdp` | El **negocio**: modulith de Spring Modulith con 4 slices + `commons` | Solo en `infrastructure` |

`ModulithStructureTests` verifica el mapa de dependencias entre módulos de `pdp`.
`LayeredArchitectureTests` verifica las tres reglas de capa. **Ambas corren en `./mvnw verify`.**

---

## Slices de negocio (`pdp`)

| Slice | Responsabilidad |
|---|---|
| `tenants` | Inquilinos del PDP. **Slice de referencia** — el más pequeño y completo |
| `applications` | Aplicaciones protegidas registradas por un inquilino. El más completo: tiene `rule/validator/` y eventos |
| `resources` | Recursos protegidos y sus concesiones. Tiene `domain/event/` |
| `identity` | Provisión de usuarios y asignación de inquilino |
| `commons` | Vocabulario de negocio **compartido entre slices**: `TenantId`, `ApplicationId`, `ResourceId`, `ApplicationName`, `PageWindow`, `ResultPage`, `AggregateRoot`, y la jerarquía de excepciones. Java puro |

Un slice nuevo se crea con las tres capas completas. No se añaden clases sueltas a `commons` salvo
que **dos o más slices** las usen: un tipo con un solo consumidor vive en su slice.

---

## Dirección de dependencias (no negociable — la verifica el build)

```
infrastructure  ──►  application  ──►  domain
                                  ──►  commons
```

- `domain` no depende de `application` ni de `infrastructure`. Ni de Spring. Ni de Reactor.
- `application` no depende de `infrastructure`. **Ni de Spring** (ver "Cableado explícito").
- `commons` no depende de nada del proyecto salvo `shared.message`.

Las tres primeras reglas están en `src/test/java/co/edu/uco/seguridad/LayeredArchitectureTests.java`.
Un `import` que las viole rompe el build, no la revisión de código.

---

## Árbol real de un slice — `tenants`

Rutas relativas a `src/main/java/co/edu/uco/seguridad/pdp/tenants/`.

### domain — Java puro, sin frameworks

**Cinco categorías, cada una en su carpeta.** No hay archivos sueltos en la raíz de `domain/`
salvo el agregado — todo lo demás se diferencia por lo que ES, no por dónde quedó cómodo:

| Ruta | Qué es | Categoría |
|---|---|---|
| `domain/Tenant.java` | Entidad como `record` con factoría con nombre (`register`) y comportamiento (`isActive`) | **el agregado** — único archivo que queda en la raíz |
| `domain/model/TenantName.java` | Value object: `record` con validación en el constructor compacto | **VO** — un atributo validado |
| `domain/model/TenantStatus.java` | Enum con comportamiento (`allowsRegistration()`), no un enum anémico | **VO** |
| `domain/rule/model/TenantExistence.java` | Entrada ya resuelta de una regla: el dato que la consulta devolvió (`record(tenantId, registered)`) | **hecho de regla** — vive junto a la regla que alimenta, no junto a los VOs |
| `domain/rule/TenantMustExistRule.java` | Interfaz vacía que extiende `OperationWithoutResult<T>` | **contrato de regla** |
| `domain/rule/impl/TenantMustExistRuleImpl.java` | La regla real: **pura, síncrona, sin puertos** | **decisión** |
| `domain/exception/InvalidTenantNameException.java` | Excepción de VO, extiende `InvalidValueException` | **lo que se dice cuando algo falla** |
| `domain/exception/TenantNotFoundException.java` | Excepción de regla de negocio, junto a la regla que la lanza | **lo que se dice cuando algo falla** |
| `domain/message/TenantsMessages.java` | Catálogo de mensajes del slice, en español | **las palabras exactas** |

**La historia que cuenta cada regla, en una carpeta:** abre `rule/TenantMustExistRule.java` (el
contrato), su `rule/model/TenantExistence.java` (lo que recibe, ya resuelto) y su
`rule/impl/TenantMustExistRuleImpl.java` (la decisión, que lanza `exception/TenantNotFoundException`,
cuyo texto sale de `message/TenantsMessages.java`). Las cuatro piezas de una misma regla están a un
`Read` de distancia entre sí — no hay que saltar a otro slice ni adivinar en qué carpeta general
quedó el `record` de entrada.

**No hay "objetos de acción" en este proyecto — y no se inventa una carpeta para ellos.** El
proyecto de referencia (`arquisoft-backend`) tiene `{Action}{Entity}Domain`: un objeto que agrupa
lo que una escritura necesita cuando no coincide 1:1 con el agregado. Aquí ningún caso de uso
necesita más que el agregado y lo que su regla resuelve — si algún día una escritura sí necesita
agrupar varias cosas, ese objeto va en la raíz de `domain/{slice}/`, junto al agregado (mismo
nivel, nunca dentro de `model/`), porque no es un atributo: es el bulto que una acción concreta
necesita. Hoy esa carpeta no existe porque no hay nada que meter en ella — no se crea vacía.

### application — Reactor sí, Spring no

| Ruta | Qué es |
|---|---|
| `application/usecase/CreateTenantUseCase.java` | **Interfaz vacía** que extiende un contrato base. Ese es todo su cuerpo |
| `application/usecase/impl/CreateTenantUseCaseImpl.java` | `final class`, constructor con `Objects.requireNonNull`, orquesta reglas y puertos |
| `application/rule/validator/TenantMustBeActiveValidator.java` | Interfaz vacía: el validador que **sí** hace E/S |
| `application/rule/validator/impl/TenantMustBeActiveValidatorImpl.java` | Consulta el puerto, arma el `record`, invoca la regla pura |
| `application/secondaryport/repository/TenantRepository.java` | Puerto de salida: firmas `Mono`/`Flux`, habla en tipos de **dominio** |
| `application/primaryport/request/CreateTenantRequest.java` | DTO de entrada: `record` con **value objects**, no Strings |
| `application/primaryport/response/TenantResponse.java` | DTO de salida del núcleo: `record` con value objects |

### infrastructure — aquí sí vive Spring

| Ruta | Qué es |
|---|---|
| `infrastructure/adapter/primary/web/controller/TenantController.java` | **package-private**, `final`. Recibe, obtiene el contexto, delega al interactor, envuelve |
| `infrastructure/adapter/primary/web/dto/request/raw/CreateTenantRawRequest.java` | `record` de **Strings desnudos**. Sin anotaciones |
| `infrastructure/adapter/primary/web/dto/response/TenantWebResponse.java` | `record` de Strings planos. Nunca expone value objects |
| `infrastructure/adapter/primary/web/interactor/CreateTenantInteractor.java` | Interfaz vacía que extiende un contrato base |
| `infrastructure/adapter/primary/web/interactor/impl/CreateTenantInteractorImpl.java` | Mapea raw → request, llama al use case, mapea response → web |
| `infrastructure/adapter/primary/web/mapper/CreateTenantRequestMapper.java` | `final class` con constructor privado y estáticos. Usa `RequestFieldParser` |
| `infrastructure/adapter/secondary/persistence/entity/TenantEntity.java` | `record` de Strings — la forma de la fila, no del dominio |
| `infrastructure/adapter/secondary/persistence/mapper/TenantPersistenceMapper.java` | Traduce `Entity` → dominio. Los VOs validan aquí |
| `infrastructure/adapter/secondary/persistence/repository/SurrealTenantRepository.java` | Implementa el puerto con `SurrealDbClient`. Su `toDomain(JsonNode)` construye la `Entity` y delega en el mapper |
| `infrastructure/adapter/secondary/persistence/schema/TenantSchema.java` | Constante del nombre de tabla. **Nunca un literal suelto en la consulta** |
| `infrastructure/config/TenantsConfiguration.java` | **Todo el cableado del slice**, con `@Bean` explícito |
| `infrastructure/properties/TenantCatalogProperties.java` | `@ConfigurationProperties` del slice |

---

## Los contratos base (`shared/contract`) — todo caso de uso extiende uno

Un `UseCase`, una `Rule` y un `Interactor` **no declaran métodos propios**: son interfaces vacías que
extienden uno de estos seis. Eso hace que sean funcionales y que un test pueda pasarlos como lambda.

| Contrato | Firma | Cuándo |
|---|---|---|
| `ReactiveOperation<I, O>` | `Mono<O> execute(I)` | Caso general: entra algo, sale algo |
| `ReactiveOperationWithoutInput<O>` | `Mono<O> execute()` | Consulta sin parámetros (listar) |
| `ReactiveOperationWithoutResult<I>` | `Mono<Void> execute(I)` | Regla que solo valida, o comando sin retorno |
| `ReactiveStreamOperation<I, O>` | `Flux<O> execute(I)` | Flujo de muchos elementos |
| `Operation<I, O>` | `O execute(I)` | **Síncrono, deliberado**: regla sin I/O |
| `OperationWithoutResult<I>` | `void execute(I)` | Síncrono: regla pura que solo lanza |

> **Toda regla es síncrona.** Una regla no consulta nada: recibe el dato ya resuelto y decide.
> Por eso todas extienden `OperationWithoutResult<T>` y viven en `domain/{slice}/rule/`. Quien
> hace la E/S es el validador (`application/{slice}/rule/validator/`), que sí es reactivo.
>
> Si una regla necesita `Mono`, es que hace dos cosas: pártela. Ver
> `TenantMustBeActiveValidatorImpl` (consulta) frente a `TenantMustExistRuleImpl` y
> `TenantStatusMustBeActiveRuleImpl` (deciden).

---

## Cableado explícito: el núcleo no conoce Spring

**No hay `@Component`, `@Service` ni `@Autowired` en `domain` ni en `application`.** Las
implementaciones son `final class` con constructor, y `{Slice}Configuration` las instancia con `new`
dentro de métodos `@Bean`.

Ver `tenants/infrastructure/config/TenantsConfiguration.java`: declara repositorio, las tres reglas,
los dos casos de uso y los dos interactores, en ese orden.

Consecuencias que hay que respetar:
- Una clase nueva de `application` **no se descubre sola**: si no la añades al `Configuration`, no existe.
- Cada dependencia del constructor se valida con `Objects.requireNonNull(x, RequiredArgumentMessages.X)`.
- Un test unitario instancia la clase con `new`, sin contexto de Spring. Ese es el objetivo del diseño.

---

## El flujo de una petición, de fuera hacia dentro

```
HTTP  →  Controller            (package-private, obtiene RequestContext, delega)
      →  Interactor            (mapea raw → request, llama, mapea response → web)
      →  UseCase               (orquesta reglas + puertos; sin lógica de formato)
      →  Rule(s)               (una restricción cada una; lanzan su excepción)
      →  Repository (puerto)   (habla en tipos de dominio)
      →  SurrealRepository     (adaptador: consulta + Entity + su mapper)
```

Dos reglas que este flujo impone:

- **El controller nunca llama al use case.** Llama al interactor. Si un controller importa una clase
  de `application`, es un defecto.
- **El use case nunca ve Strings crudos ni tipos web.** Recibe el DTO con value objects ya construidos.

---

## Dónde vive cada tipo de archivo (tabla de decisión)

| Voy a crear… | Va en |
|---|---|
| **El agregado** (la entidad raíz del slice) | `{slice}/domain/{Entidad}.java` — único archivo que queda en la raíz |
| **Value object** (un atributo validado) | `{slice}/domain/model/` |
| **Enum de negocio** (VO cerrado a valores fijos) | `{slice}/domain/model/` |
| **Specification de consulta** (`{X}Criteria`, con `matches()`) | `{slice}/domain/` — junto al agregado, no en `model/`: no es un atributo, es cómo se busca |
| Excepción de value object | `{slice}/domain/exception/` |
| **Restricción de negocio** (pura, síncrona) | `{slice}/domain/rule/` + `/impl/` |
| **Entrada ya resuelta de una regla** (`{X}Existence`, `{X}Availability`…) | `{slice}/domain/rule/model/` — junto a la regla que la consume, no junto a los VOs |
| **Objeto de acción** (bulto que una escritura necesita, sin mapear 1:1 al agregado) | `{slice}/domain/` junto al agregado — **hoy no existe ninguno**; no se crea la carpeta sin un caso real |
| Caso de uso (interfaz + impl) | `{slice}/application/usecase/` + `/impl/` |
| **Validador**: consulta el puerto y aplica las reglas | `{slice}/application/rule/validator/` + `/impl/` |
| Puerto de salida | `{slice}/application/secondaryport/repository/` |
| DTO que entra al núcleo | `{slice}/application/primaryport/request/` |
| DTO que sale del núcleo | `{slice}/application/primaryport/response/` |
| Excepción de regla de negocio | `{slice}/domain/exception/` |
| Mensaje de usuario del slice | `{slice}/domain/message/{Slice}Messages.java` |
| Endpoint | `{slice}/infrastructure/adapter/primary/web/controller/` |
| DTO crudo de HTTP | `…/web/dto/request/raw/` |
| DTO de respuesta HTTP | `…/web/dto/response/` |
| Interactor | `…/web/interactor/` + `/impl/` |
| Mapper web | `…/web/mapper/` |
| Adaptador de persistencia | `…/secondary/persistence/repository/` |
| Fila de base de datos | `…/secondary/persistence/entity/` |
| Nombre de tabla | `…/secondary/persistence/schema/{X}Schema.java` |
| Cableado de beans | `{slice}/infrastructure/config/{Slice}Configuration.java` |
| Value object usado por **2+ slices** | `pdp/commons/model/` |
| Capacidad técnica sin negocio | `shared/…` |

---

## Qué ya existe en `shared` — no lo reimplementes

| Necesito… | Ya está en |
|---|---|
| Envolver la respuesta HTTP | `shared/web/ApiResponse.java` (`success(code, message, data, context)`) |
| Página de resultados HTTP | `shared/web/PageResponse.java` |
| Parsear un campo crudo a un VO | `shared/web/RequestFieldParser.java` |
| Traducir excepción → HTTP | `shared/web/exceptionhandler/ApiErrorHandler.java`. **Un slice nunca crea su propio handler** |
| Id de petición / correlación | `shared/web/CorrelationWebFilter.java` + `RequestContext` |
| Reloj | `shared/port/TimeProvider.java`. Nunca `Instant.now()` en línea |
| Generar identificadores | `shared/port/IdentifierGenerator.java`. Nunca `UUID.randomUUID()` en línea |
| Publicar un evento de dominio | `shared/event/DomainEventPublisher.java` |
| Contexto de log reactivo | `shared/observability/ReactiveLogContext.java` |
| Consultar SurrealDB | `shared/persistence/surrealdb/SurrealDbClient.java` |
| Principal autenticado | `shared/security/SecurityContext.java` + `PdpPrincipal` |

---

## Todos los slices se ven igual

Un slice no es «lo que hizo falta»: es la misma forma siempre, para que entrar en uno cualquiera no
obligue a reaprender nada. `.claude/tools/consistencia.ps1` lo verifica.

| Si el slice tiene… | Entonces tiene, sin excepción |
|---|---|
| adaptador de persistencia | `persistence/entity`, `persistence/mapper`, `persistence/repository`, `persistence/schema` |
| controller | `dto/request/raw`, `dto/response`, `interactor` + `impl`, `mapper` |
| un contrato (`UseCase`, `Rule`, `Interactor`, `Validator`) | su `Impl` en el subpaquete `impl/` |
| excepciones propias | su `{Slice}Messages` en `domain/message` |
| cualquier cosa | su `{Slice}Configuration` |

Dos consecuencias que se incumplían hasta el 2026-08-31 y ahora no:

- **El adaptador nunca construye el agregado desde el JSON.** Lee la fila en una `{X}Entity` y
  delega en su `{Slice}PersistenceMapper` (`TenantPersistenceMapper.toDomain(entity)`). Separar «leer la fila» de «reconstruir el
  agregado» deja el mapeo y su validación en un solo sitio.
- **El método que convierte fila → dominio se llama `toDomain`**, o `to{Tipo}` si el adaptador
  maneja más de un agregado. Nunca `toUser`, `toRow` ni un nombre inventado.

### Una regla que dos módulos necesitan la publica su dueño

Cuando un slice necesita decidir algo sobre otro —«esta aplicación existe y es de este inquilino»—
**no consulta el repositorio ajeno**: consume un `Validator` que el dueño publica como interfaz
nombrada de Modulith. Una sola implementación inyectada, no una comprobación copiada, para que la
decisión no pueda divergir.

Es el **validador** y no la regla lo que se publica, porque el consumidor no puede resolver el dato:
no tiene el repositorio ajeno, que es justamente el motivo por el que pide la decisión prestada.

Ejemplos vivos: `tenants` publica `TenantMustBeActiveValidator`; `applications` publica
`ApplicationMustExistForTenantValidator`. Ambos con `@NamedInterface("rule")` en el `package-info`
de `application/rule/validator`, y declarados en el `allowedDependencies` del consumidor.

---

## Reglas invariantes

1. `domain` y `application` **no importan `org.springframework`**. Ni una anotación.
2. Un `UseCase`, `Rule` o `Interactor` extiende un contrato de `shared/contract` y **no declara métodos propios**.
3. Toda implementación es `final class` y valida sus dependencias con `Objects.requireNonNull`.
4. Toda clase nueva de `application` se registra a mano en `{Slice}Configuration`.
5. El controller es **package-private** y llama al interactor, nunca al use case.
6. El `raw` request es un `record` de Strings sin anotaciones; la validación vive en el mapper y en los value objects.
7. La respuesta web nunca expone value objects: sale plana.
8. Un nombre de tabla vive en su `{X}Schema`, nunca como literal en la consulta.
9. Si dudas de dónde va algo, mira `tenants` — y si `tenants` no lo tiene, mira `applications`.
10. El adaptador de persistencia pasa por `Entity` + `TenantPersistenceMapper` o su equivalente, siempre.
11. Una decisión sobre otro módulo se consume como `Validator` publicado, nunca consultando su repositorio.
13. Una `Rule` **no consulta nada y no devuelve `Mono`**: recibe el dato resuelto y decide. Si necesita
    consultar, lo que falta es un validador que lo haga por ella.
12. Antes de cerrar, `consistencia.ps1` sale limpio.
