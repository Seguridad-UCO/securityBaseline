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
│   ├── port/primary/         dto (request/response) + interactor
│   ├── port/secondary/       AuditPort y repository/
│   ├── rule/ + rulesvalidator/
│   └── exception/
└── infrastructure/           config Spring, adapter/primary (web), adapter/secondary
```

Reactor aparece en aplicación e infraestructura porque ahí está la frontera de I/O. El dominio es
síncrono y sin anotaciones: sus invariantes no hacen I/O.

```text
web → interactor → use case → rules validator → rules → domain
                        │                          │
                        └──────► port/secondary ◄──┘
                                    │
                                 adapters
```

Mensajes de error compartidos viven en el módulo OPEN `co.edu.uco.seguridad.crosscutting`.

## Ubicación verificable

- Dominio puro: [`recursos/domain`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/domain) y
  [`commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons).
- Casos de uso: [`RegisterProtectedApplicationUseCase.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/RegisterProtectedApplicationUseCase.java).
- Puertos secundarios: [`recursos/application/port/secondary`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/secondary).
- Adaptadores: [`recursos/infrastructure`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure).
- Prueba de estructura: [`ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java).

## Evidencia y límite

`./mvnw verify` ejecuta la verificación de módulos y las pruebas. Las pruebas de dominio y de casos
de uso corren sin levantar Spring, que es la comprobación real de que el núcleo no depende del
framework. La sustitución de los dummies por SurrealDB no debe tocar `domain/` ni `application/`.
