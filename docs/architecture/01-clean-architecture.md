# 01. Arquitectura base: Clean Architecture

[← Arquitectura](README.md) · [Siguiente: contratos →](02-service-contracts.md)

## Decisión arquitectónica

Se adopta Clean Architecture materializada como arquitectura hexagonal dentro del módulo `applications`. Las dependencias apuntan hacia el dominio: HTTP, Spring, Reactor, observabilidad y memoria no son dependencias del agregado.

## Justificación

La regla “una aplicación tiene tenant, nombre válido y un recurso inicial” es conocimiento del negocio. Si viviera en un controlador o repositorio, cambiar WebFlux por otro transporte o el dummy por SurrealDB obligaría a reescribirla. Se descarta el CRUD por capas técnicas (`controller/service/repository`) porque mezcla dirección de dependencias y hace que la base de datos condicione el dominio.

## Implementación

El núcleo contiene el agregado `ProtectedApplication` y value objects. La aplicación orquesta mediante puertos de entrada/salida. Los adaptadores WebFlux, auditoría y persistencia implementan esos puertos. Reactor se conserva en aplicación/adaptadores por ser frontera de I/O; el dominio sigue siendo Java puro.

```text
WebFlux → port/in → service → port/out → dummy
                     ↓
                   domain
```

## Ubicación verificable

- Agregado: [`ProtectedApplication.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ProtectedApplication.java).
- Casos de uso: [`application/port/in`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/in).
- Puertos de salida: [`application/port/out`](../../src/main/java/co/edu/uco/seguridad/applications/application/port/out).
- Adaptadores: [`applications/infrastructure`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure).
- Prueba de estructura: [`ModulithStructureTests.java`](../../src/test/java/co/edu/uco/seguridad/ModulithStructureTests.java).

## Evidencia y límite

`./mvnw test` ejecuta la verificación de módulos y las pruebas de dominio. El módulo es único porque la HU es mínima; su estructura permite extraer `tenants` y `resources` como módulos posteriores sin invertir dependencias.
