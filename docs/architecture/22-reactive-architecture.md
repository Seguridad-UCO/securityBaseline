# 22. Decisión de arquitectura reactiva

[← Modelo](21-refined-model.md) · [↑ Arquitectura](README.md)

## Decisión arquitectónica

La línea base es reactiva: Spring WebFlux, Netty y `Mono` para operaciones de aplicación e infraestructura. Java 25 y Spring Boot 4.1.0 son el runtime configurado.

## Justificación

La arquitectura objetivo encadena I/O remoto hacia PDP, OPA, IdP, SurrealDB y auditoría. WebFlux evita ocupar un hilo mientras esos sistemas responden. Se descarta Spring MVC para no introducir dos modelos de concurrencia antes de construir el camino crítico.

## Implementación

Los casos de uso devuelven `Mono`; repositorio, auditoría y transacción también. No hay llamadas `block()` en producción. El dominio es deliberadamente síncrono porque sus invariantes no hacen I/O. Actuator expone salud y métricas; la observación del registro usa Micrometer.

## Ubicación verificable

- Dependencias: [`pom.xml`](../../pom.xml).
- Controlador: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web/ProtectedApplicationController.java).
- Flujo: [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/applications/application/service/ProtectedApplicationService.java).
- Prueba real Netty: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/applications/infrastructure/web/ProtectedApplicationHttpTests.java).

## Evidencia y límite

La prueba HTTP inicia Netty y valida el flujo no bloqueante. El entorno local actual solo tiene JDK 23, por lo que la comprobación se ejecutó temporalmente con `-Djava.version=23`; el POM permanece en 25 y debe verificarse con JDK 25 en CI/equipo objetivo.
