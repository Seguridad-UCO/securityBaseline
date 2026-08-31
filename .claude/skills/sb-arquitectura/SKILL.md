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
| `applications` | Aplicaciones protegidas registradas por un inquilino. El más completo: tiene `rulesvalidator/` y eventos |
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

| Ruta | Qué es |
|---|---|
| `domain/Tenant.java` | Entidad como `record` con factoría con nombre (`register`) y comportamiento (`isActive`) |
| `domain/TenantName.java` | Value object: `record` con validación en el constructor compacto |
| `domain/TenantStatus.java` | Enum con comportamiento (`allowsRegistration()`), no un enum anémico |
| `domain/exception/InvalidTenantNameException.java` | Excepción de VO, extiende `InvalidValueException` |

### application — Reactor sí, Spring no

| Ruta | Qué es |
|---|---|
| `application/usecase/CreateTenantUseCase.java` | **Interfaz vacía** que extiende un contrato base. Ese es todo su cuerpo |
| `application/usecase/impl/CreateTenantUseCaseImpl.java` | `final class`, constructor con `Objects.requireNonNull`, orquesta reglas y puertos |
| `application/rule/TenantCodeMustBeUniqueRule.java` | Interfaz vacía que extiende un contrato base |
| `application/rule/impl/TenantCodeMustBeUniqueRuleImpl.java` | La regla real |
| `application/port/secondary/repository/TenantRepository.java` | Puerto de salida: firmas `Mono`/`Flux`, habla en tipos de **dominio** |
| `application/port/primary/dto/request/CreateTenantRequest.java` | DTO de entrada: `record` con **value objects**, no Strings |
| `application/port/primary/dto/response/TenantResponse.java` | DTO de salida del núcleo: `record` con value objects |
| `application/exception/TenantNotFoundException.java` | Excepción de regla de negocio |
| `application/message/TenantsMessages.java` | Catálogo de mensajes del slice, en español |

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
| `infrastructure/adapter/secondary/persistence/repository/SurrealTenantRepository.java` | Implementa el puerto con `SurrealDbClient` |
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

> **Una regla sin entrada/salida es síncrona a propósito.** Ver
> `tenants/application/rule/TenantStatusMustBeActiveRule.java` (síncrona, solo mira el estado en
> memoria) frente a `TenantMustBeActiveRule.java` (reactiva, consulta el repositorio).
> Envolver en `Mono` una regla que no hace I/O es ruido.

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
      →  SurrealRepository     (adaptador: consulta + Entity + PersistenceMapper)
```

Dos reglas que este flujo impone:

- **El controller nunca llama al use case.** Llama al interactor. Si un controller importa una clase
  de `application`, es un defecto.
- **El use case nunca ve Strings crudos ni tipos web.** Recibe el DTO con value objects ya construidos.

---

## Dónde vive cada tipo de archivo (tabla de decisión)

| Voy a crear… | Va en |
|---|---|
| Entidad o value object | `{slice}/domain/` |
| Enum de negocio | `{slice}/domain/` |
| Excepción de value object | `{slice}/domain/exception/` |
| Caso de uso (interfaz + impl) | `{slice}/application/usecase/` + `/impl/` |
| Restricción de negocio | `{slice}/application/rule/` + `/impl/` |
| Coordinador de varias reglas | `{slice}/application/rulesvalidator/` |
| Puerto de salida | `{slice}/application/port/secondary/repository/` |
| DTO que entra al núcleo | `{slice}/application/port/primary/dto/request/` |
| DTO que sale del núcleo | `{slice}/application/port/primary/dto/response/` |
| Excepción de regla de negocio | `{slice}/application/exception/` |
| Mensaje de usuario del slice | `{slice}/application/message/{Slice}Messages.java` |
| Endpoint | `{slice}/infrastructure/adapter/primary/web/controller/` |
| DTO crudo de HTTP | `…/web/dto/request/raw/` |
| DTO de respuesta HTTP | `…/web/dto/response/` |
| Interactor | `…/web/interactor/` + `/impl/` |
| Mapper web | `…/web/mapper/` |
| Adaptador de persistencia | `…/secondary/persistence/repository/` |
| Fila de base de datos | `…/secondary/persistence/entity/` |
| Nombre de tabla | `…/secondary/persistence/schema/{X}Schema.java` |
| Cableado de beans | `{slice}/infrastructure/config/{Slice}Configuration.java` |
| Value object usado por **2+ slices** | `pdp/commons/` |
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
