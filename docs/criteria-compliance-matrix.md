# Matriz de cumplimiento de los 23 criterios

[← Índice principal](README.md) · [Enfoque general](baseline-criteria-overview.md)

Auditoría realizada el 2026-08-07 sobre el commit `6a31033` y resultado tras la refactorización.

## Hallazgo transversal

La documentación describía una arquitectura considerablemente más completa que el código. Ocho
criterios estaban **documentados pero no implementados**: sus páginas enlazaban a clases que nunca
existieron (`ProtectedApplicationCriteria`, `PageWindow`, `ApplicationPage`,
`SearchProtectedApplicationsUseCase`, `ApplicationResponseMapper`, `ApplicationRepository`,
`ReactiveTransactionPort`, `SnapshotReactiveTransactionAdapter`, `InMemoryAuditAdapter`,
`DomainException`, `TimeProvider`, `ApplicationIdGenerator`, `ResourceIdentifier`).

Además, todos los enlaces de “Ubicación verificable” apuntaban al directorio del paquete y no a
archivos, lo que ocultaba que los destinos no existían. La documentación afirmaba “ocho pruebas”
cuando había tres.

Ese fue el problema principal: no era un desvío de detalle, era que la evidencia no correspondía al
código.

## Matriz

| # | Criterio | Estado inicial | Problema encontrado | Cambio realizado | Estado final |
|---|---|---|---|---|---|
| 1 | Clean Architecture | Parcial | Capas presentes pero sin `port/out` explícito; puertos declarados como interfaces anidadas dentro de los servicios | Puertos extraídos a `application/port/out`; `domain`/`application`/`infrastructure` en los tres módulos | Cumple |
| 2 | Contratos de servicios | Parcial | Solo existía el caso de uso de registro; `SearchProtectedApplicationsUseCase`, documentado, no existía | Añadido el caso de uso de consulta; convención de firmas (`Mono<T>`, `Mono<Void>`, `Mono<ResultPage<T>>`) documentada y aplicada | Cumple |
| 3 | Reglas e integridad | Parcial | Las reglas eran `if` y ternarios dentro de los servicios; no eran probables por separado | 5 rules con interfaz e implementación, separadas por uso de repositorio, coordinadas por rules validators | Cumple |
| 4 | Capacidades transversales | Parcial | Faltaban reloj, generador de identificadores y transacción; `Instant.now()` y `UUID.randomUUID()` en línea | `shared/port` con `TimeProvider`, `IdentifierGenerator`, `ReactiveTransactionPort`; `shared/rule`; `shared/config` | Cumple |
| 5 | Manejo de mensajes | Parcial | Envelope correcto, pero el handler mapeaba tipos concretos y no cubría fallos no previstos | Handler enganchado a las jerarquías base usando el `code()` de cada excepción; añadido 500 sin datos técnicos | Cumple |
| 6 | Manejo de parámetros | No cumplía | No existían parámetros de consulta; el body dependía de `@Valid` | Query completa con filtros y ventana, todos `String` y `required=false`; validación explícita en el mapper | Cumple |
| 7 | Adaptadores dummy | Parcial | El adaptador de auditoría era un lambda `Mono.empty()`; no había adaptador de transacción | `InMemoryAuditAdapter` real, `SnapshotReactiveTransactionAdapter`, repositorios sobre entidades de persistencia | Cumple |
| 8 | Logging e instrumentación | No cumplía | `ReactiveLogContext` existía pero **no se usaba en ningún sitio**: era código muerto | Aplicado con `.transform(...)` en los cuatro casos de uso; documentado que no se crea `Observation` manual | Cumple |
| 9 | Excepciones | Parcial | Dos excepciones sueltas sin jerarquía; el handler traducía `IllegalArgumentException` genérica | Jerarquía `DomainException` → `InvalidValueException` / `BusinessRuleViolationException`, más `RequestContractException`; 15 excepciones específicas | Cumple |
| 10 | Transacciones | No cumplía | No existía puerto ni adaptador; solo compensación manual encadenada | Se retiraron `ReactiveTransactionPort` y el adaptador de snapshot. `RemoveApplicationUseCase` quedó como operación compensatoria | **No cumple** — ningún caso de uso invoca la compensación: no hay saga cableada |
| 11 | Interacción entre capas | Parcial | No había interactor; el controlador construía el comando | Interactores con interfaz e implementación; el controlador solo recibe, mapea, delega y envuelve | Cumple |
| 12 | SOLID | Parcial | ISP y DIP débiles: puertos anidados en las implementaciones; servicios con varias responsabilidades | Contratos mínimos y separados; reglas como beans sustituibles; lógica en implementaciones | Cumple |
| 13 | DTOs | Parcial | Un solo DTO con anotaciones Jakarta; el controlador devolvía el modelo de lectura del núcleo | Estrategia en dos niveles: raw `String` → mapper → DTO validado con setters; DTO de respuesta propio | Cumple |
| 14 | DTOs seguros | Parcial | Dependía de Bean Validation; el response exponía value objects | `spring-boot-starter-validation` retirado del POM; tres barreras independientes; respuestas planas | Cumple |
| 15 | Validación de dominio | Parcial | Invariantes en VOs sí; la specification documentada no existía | `ProtectedApplicationCriteria` con `matches`; cada VO lanza su excepción específica | Cumple |
| 16 | Repositorios dinámicos | No cumplía | Los stores tenían métodos concretos; no había `findBy(criteria, window)` | Los cuatro puertos siguen exponiendo métodos concretos (`findAllByTenant`, `findAllByApplication`) | **No cumple** — no existe `findBy(criteria, window)` en ningún puerto |
| 17 | Consultas dinámicas | No cumplía | No existía ninguna consulta | `RequestFieldParser.optional` existe, pero ninguna specification lo consume | **No cumple** — no existe la specification de filtros |
| 18 | Paginación | No cumplía | `PageWindow` y `ApplicationPage` no existían | `PageWindow` (1..100, `ofPage`/`ofRange`), `ResultPage` y `PageResponse` existen y están probados | **Parcial** — son código muerto: ningún caso de uso, puerto ni endpoint los usa |
| 19 | Rangos | No cumplía | No existía | `offset`/`limit` convergentes en `PageWindow`; combinaciones ambiguas rechazadas | **Parcial** — la lógica existe y está probada, pero ningún endpoint la expone |
| 20 | Adaptadores limpios | Parcial | El controlador construía el comando y devolvía el tipo del núcleo | Controlador delgado; mapper delega el formato al VO; dummies sin decisiones | Cumple |
| 21 | Modelo refinado | Parcial | Records anémicos sin factorías ni comportamiento; doc describía un agregado inexistente | Factorías con nombre, comportamiento en las entidades, criterio explícito record/clase/VO; doc reconciliada | Cumple |
| 22 | Arquitectura reactiva | Cumplía | Sin hallazgos de fondo; faltaba documentar por qué la transacción usa `Supplier` + `defer` | Documentado; reglas sin I/O deliberadamente síncronas | Cumple |
| 23 | Arquitectura antes del negocio | Parcial | 3 pruebas frente a las “ocho” documentadas; sin pipeline por ambiente ni manejo de secretos | 109 pruebas, 92,7 % de cobertura, pipeline de tres ambientes con Quality Gate, Key Vault provisionado | Cumple |

## Resumen

| Estado | Criterios |
|---|---|
| **Cumple** | 18 |
| **Parcial** | 2 — el 18 y el 19 |
| **No cumple** | 3 — el 10, el 16 y el 17 |

**Estado final: 18 de 23.** La auditoría de agosto dejó los 23 en verde, pero la refactorización
posterior desconectó la búsqueda con criterios y la saga de compensación sin actualizar esta matriz.
Se corrige aquí en vez de mantener el número.

> **Revisión — 2026-08-31.** Al construir el harness de IA se volvió a comprobar la matriz contra el
> código, y reapareció exactamente el hallazgo transversal de agosto: la evidencia no correspondía al
> código. Concretamente:
>
> - `ProtectedApplicationCriteria`, `SearchProtectedApplicationsUseCase` y sus mappers y pruebas
>   **nunca llegaron a existir**; los criterios 16 y 17 dependían de ellos.
> - `PageWindow`, `ResultPage` y `PageResponse` existen y están probados, pero **ningún caso de uso,
>   puerto ni endpoint los usa**: son código muerto, así que 18 y 19 quedan en parcial.
> - `RemoveApplicationUseCase` está documentada como operación compensatoria, pero **nadie la
>   invoca**: no hay saga cableada, así que el 10 no se cumple.
> - `SnapshotReactiveTransactionAdapter` y `ReactiveTransactionPort` se retiraron en el Stage 4 y no
>   se sustituyeron por nada.
> - De las quince pruebas que la guía de evidencia mapeaba a criterios, **nueve no existían**.
>
> Cerrar 10 y 16-19 —y reponer la prueba HTTP end-to-end— es el alcance de la historia **HU-001**,
> que además es la primera que se ejecuta por el flujo agéntico. La comprobación es ahora ejecutable:
> `.claude/tools/drift.ps1`.

## Lo que sigue sin estar hecho, y se dice aquí

- La auditoría real (fuera de memoria) sigue sin implementarse: `InMemoryAuditAdapter` sigue
  registrando solo identificadores como listener de `ProtectedResourceRegistered`
  ([ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md)).
  No estaba en el alcance de las cuatro etapas.
- La auditoría por eventos usa entrega síncrona (`ApplicationEventPublisher`), no el Event
  Publication Registry de Modulith: aunque desde el Stage 4 ya hay persistencia real (SurrealDB),
  Modulith 2.1 no trae un backend de registry para SurrealDB (solo JPA/JDBC/MongoDB/Neo4j) — ver la
  actualización en la nota de implementación de
  [ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md#nota-de-implementación).
- El Quality Gate contra SonarQube real requiere la service connection y la extensión en la
  organización.

## Evolución posterior

Decisiones y etapas: [gobierno](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/README.md) y
[hoja de ruta](plans/2026-08-07-architecture-and-roadmap.md).

| Etapa | Cambio | Criterios tocados |
|---|---|---|
| 0 | ADRs, C4, convención de idioma | 23 |
| 1 | DTO de registro único; puerto `SnapshotCapable` | 1, 11, 13, 20 |
| 2 | Eventos de dominio; auditoría por listener | 1, 4, 7, 10 |
| — | Interactores por operación; mapeo en interactor; `TenantStatusMustBeActiveRule` | 11, 12, 20 |
| 3 | Seguridad real (JWT reactivo); `tenantId` retirado del cuerpo/query, el tenant obligatorio y tomado del principal | 4, 6, 9, 13, 14 |
| 4 | Persistencia real (SurrealDB por HTTP, sin driver Java); retiro de `ReactiveTransactionPort`/`SnapshotCapable`; la saga de compensación quedó **sin cablear** (ver revisión de 2026-08-31); Testcontainers | 4, 7, 10, 16 |

Las cuatro etapas del plan de arquitectura están implementadas.
La verificación del proyecto es `./mvnw verify`, y **exige un JDK 25** (el POM lo fija). Si el
`JAVA_HOME` del entorno apunta a otra versión, Maven falla con `release version 25 not supported`
aunque el código esté bien: `.claude/tools/verificar.ps1` selecciona el JDK correcto por su cuenta.
Desde el Stage 4, `verify` también requiere Docker (Testcontainers provisiona SurrealDB
automáticamente; no hace falta `docker compose up` manual).
