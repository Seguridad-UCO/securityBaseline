# 01. Arquitectura base: Clean Architecture

[← Arquitectura](README.md) · [Siguiente: contratos →](02-service-contracts.md)

## Decisión arquitectónica

Clean Architecture materializada como arquitectura hexagonal dentro de cada módulo del contenedor
PDP. Las dependencias apuntan hacia el dominio: HTTP, Spring, Reactor, observabilidad y memoria no
son dependencias de las entidades ni de los value objects.

## Justificación

“Una aplicación pertenece a un tenant activo, tiene nombre único y expone recursos con acciones” es
conocimiento del negocio. Si viviera en un controlador o en un repositorio, cambiar WebFlux por otro
transporte o el dummy por SurrealDB obligaría a reescribirla. Se descarta el CRUD por capas técnicas
(`controller/service/repository`) porque mezcla la dirección de las dependencias y hace que la base
de datos condicione el dominio.

## Implementación

Cada módulo repite la misma estructura interna:

```text
<módulo>/                     API publicada del módulo (ModuleApi cuando aplica)
├── domain/                   Java puro: entidades, value objects, especificaciones
├── application/
│   ├── usecase/ (+ impl)     casos de uso
│   ├── primaryport/         dto (request/response) + interactor
│   ├── secondaryport/       repository/ y los puertos que cada módulo necesite
│   ├── rule/ + rule/validator/
│   └── exception/
└── infrastructure/           config Spring, adapter/primary (web), adapter/secondary
```

Reactor aparece en aplicación e infraestructura porque ahí está la frontera de I/O. El dominio es
síncrono y sin anotaciones: sus invariantes no hacen I/O.

```text
web → interactor → use case → rules validator → rules → domain
                        │                          │
                        └──────► secondaryport ◄──┘
                                    │
                                 adapters
```

Mensajes de error compartidos viven en el módulo OPEN `co.edu.uco.seguridad.crosscutting`.

Tres decisiones registradas como ADR amplían esta estructura sin romper la regla de dependencias:
eventos de dominio publicados por los
agregados ([ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md)),
un adaptador primario de seguridad delante del
interactor ([ADR-018](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-018-jwt-reactive-security-implementation.md))
y adaptadores secundarios reales sobre
SurrealDB ([ADR-019](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-019-surrealdb-implementation.md)).
Las tres ya están implementadas; la estructura descrita arriba refleja el estado actual del código,
no un objetivo pendiente.

## Ubicación verificable

- Dominio puro: [`resources/domain`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/domain) y
  [`commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons).
- Casos de uso: [
  `RegisterApplicationUseCase.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/RegisterApplicationUseCase.java).
- Puertos secundarios: [
  `resources/application/secondaryport`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/secondaryport).
- Adaptadores: [`resources/infrastructure`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure).
- Prueba de estructura: [
  `ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java).

## Evidencia y límite

`./mvnw verify` ejecuta la verificación de módulos y las pruebas. Las pruebas de dominio y de casos
de uso corren sin levantar Spring, que es la comprobación real de que el núcleo no depende del
framework. La sustitución de los dummies por SurrealDB no debe tocar `domain/` ni `application/`.
