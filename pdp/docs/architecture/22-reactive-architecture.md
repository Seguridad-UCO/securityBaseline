# 22. Decisión de arquitectura reactiva

[← Modelo](21-refined-model.md) · [↑ Arquitectura](README.md)

## Decisión arquitectónica

La línea base es reactiva: Spring WebFlux, Netty y `Mono` en aplicación e infraestructura. Java 25 y
Spring Boot 4.1.0 son el runtime configurado.

## Justificación

La arquitectura objetivo encadena I/O remoto hacia PDP, OPA, IdP, SurrealDB y auditoría. WebFlux
evita ocupar un hilo mientras esos sistemas responden. Se descarta Spring MVC para no introducir dos
modelos de concurrencia antes de construir el camino crítico.

## Implementación

Casos de uso, interactores, repositorios, auditoría y reglas con repositorio devuelven `Mono`. Las
reglas sin repositorio son síncronas a propósito: envolver en `Mono` una decisión que no hace I/O
solo añade indirección. `SurrealDbClient` habla con SurrealDB por HTTP a través del `WebClient`
reactivo de Spring, así que la cadena entera —desde el controlador hasta la fila— no bloquea un hilo
en ningún punto (ADR-0004).

No hay `block()` en producción. Sí lo hay en algunas pruebas, donde bloquear es correcto porque el
hilo de test existe para esperar — con una excepción intencional: los inicializadores de esquema
(`SurrealTenantSchemaInitializer` y análogos) sí bloquean, porque son `ApplicationRunner` que corren
antes de que Netty acepte tráfico, no dentro de una petición.

Detalle que importa en la saga de registro
([
`RegisterApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java)):
cada compensación se encadena con `onErrorResume(error -> compensar().then(Mono.error(error)))`, no
con un `try/catch` imperativo — la compensación es ella misma un `Mono` reactivo, y solo se suscribe
si la cadena anterior emitió error, preservando el error original tras compensar.

## Ubicación verificable

- Dependencias: [`pom.xml`](../../pom.xml)
- Controlador: [
  `ApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/primary/web/controller/ApplicationController.java)
- Flujo: [
  `RegisterApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java)
- Cliente HTTP reactivo: [
  `SurrealDbClient.java`](../../src/main/java/co/edu/uco/seguridad/shared/persistence/surrealdb/SurrealDbClient.java)
- Prueba real sobre Netty: `ApplicationHttpTests`

## Evidencia y límite

Las pruebas de flujo usan `StepVerifier`, que verifica la secuencia de señales y no solo el valor
final. La prueba HTTP arranca Netty.

El POM exige Java 25 y el pipeline compila con JDK 25.
