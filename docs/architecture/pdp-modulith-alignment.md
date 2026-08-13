# Estructura ejecutable del PDP (Spring Modulith)

[← Arquitectura](README.md)

## Estructura implementada

La línea base representa el **contenedor PDP**:

```text
co.edu.uco.seguridad
├── shared/            capacidades transversales (módulo OPEN)
│   ├── contract/      ReactiveOperation · OperationWithoutResult · …
│   ├── port/          TimeProvider · IdentifierGenerator
│   ├── event/         DomainEvent · DomainEventPublisher
│   ├── security/      PdpPrincipal · SecurityContext · handlers 401/403 (ADR-0003)
│   ├── web/           ApiResponse · PageResponse · RequestFieldParser · correlación
│   ├── observability/ ReactiveLogContext
│   └── config/        puertos transversales + EventPublisherConfiguration + SecurityConfiguration
└── pdp/
    ├── commons/       shared kernel: TenantId, ApplicationId, ResourceId, ApplicationName,
    │                  PageWindow, ResultPage, AggregateRoot y excepciones base
    ├── tenants/       FindTenantInteractor · TenantMustBeActiveRule · TenantStatusMustBeActiveRule
    ├── aplicaciones/  RegisterApplicationInteractor · RemoveApplicationInteractor
    └── recursos/      Register/SearchProtectedApplication(s)Interactor; orquesta E-1
```

Dependencias Modulith: `aplicaciones → commons, tenants` y
`recursos → commons, tenants, aplicaciones`. Los interactors y DTOs se publican como Named Interfaces;
`domain`, `application` e `infrastructure` internos quedan verificados por Modulith.

`shared` está fuera de `pdp` a propósito: capacidades técnicas, no vocabulario del PDP. El
vocabulario de negocio vive en `pdp/commons` (Java puro, sin Reactor).

Eventos de dominio ([ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md)):
`Application` / `ProtectedResource` registran hechos vía `registerWithEvent`;
`DomainEventPublisher` usa `ApplicationEventPublisher` (sin Event Publication Registry: aunque ya hay
persistencia real desde el Stage 4, Modulith no trae un backend de registry para SurrealDB — ver la
actualización en la nota de implementación de ADR-017). Seguridad reactiva
([ADR-018](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-018-jwt-reactive-security-implementation.md)) y persistencia real
([ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md)) ya están implementadas:
`SecurityWebFilterChain` exige JWT en toda ruta salvo `/actuator/health,info`, los interactores de
`recursos` leen el tenant del principal autenticado en vez de aceptarlo en el cuerpo o la query, y
los tres repositorios secundarios hablan con SurrealDB por HTTP en vez de guardar en memoria.

## Contratos publicados por módulo

| Módulo | Publica | No publica |
|---|---|---|
| `tenants` | `FindTenantInteractor`, `TenantMustBeActiveRule`, DTOs, excepciones | `Tenant`, repositorio, adaptadores |
| `aplicaciones` | `RegisterApplicationInteractor`, `RemoveApplicationInteractor`, DTOs, excepciones | `Application`, repositorio, reglas internas |
| `recursos` | Interactores HTTP, DTOs de catálogo | dominio, reglas, puertos secundarios, adaptadores |

`FindTenantInteractor` responde *qué* es un tenant; `TenantMustBeActiveRule` decide *si* puede
operar (carga + `TenantStatusMustBeActiveRule`). No se mezclan consulta y decisión en un solo método.

## Flujo E-1

`SecurityWebFilterChain` valida el JWT antes de que la petición llegue al controlador. El
controlador HTTP entrega el JSON crudo al interactor. El interactor lee el tenant del principal
autenticado (`SecurityContext.currentPrincipal()`), mapea a DTO tipado, ejecuta el caso de uso y
proyecta la respuesta HTTP. El caso de uso registra la aplicación con `RegisterApplicationInteractor`
(que aplica sus reglas, incluida la del tenant, y publica `ApplicationRegistered`) y luego valida y
persiste el recurso, publicando `ProtectedResourceRegistered`. `InMemoryAuditAdapter` escucha ese
evento. Si el recurso falla, se compensa con `RemoveApplicationInteractor`.

## Evidencia

- Arranque: [`PdpApplication.java`](../../src/main/java/co/edu/uco/seguridad/pdp/PdpApplication.java)
- Gate: [`ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java)
- Flujo HTTP: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/ProtectedApplicationHttpTests.java)

`./mvnw verify` comprueba compilación, contexto Spring, dependencias Modulith, tests y cobertura.
