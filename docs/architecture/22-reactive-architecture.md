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

Casos de uso, interactores, repositorios, auditoría, transacción y reglas con repositorio devuelven
`Mono`. Las reglas sin repositorio son síncronas a propósito: envolver en `Mono` una decisión que no
hace I/O solo añade indirección.

No hay `block()` en producción. Sí lo hay en algunas pruebas, donde bloquear es correcto porque el
hilo de test existe para esperar.

Detalle que importa: `SnapshotReactiveTransactionAdapter` recibe un `Supplier<Mono<T>>` y no un
`Mono<T>` ya construido, y toma la copia dentro de `Mono.defer`. Así el snapshot se toma en el
momento de la suscripción y no cuando se ensambla la cadena — con un `Mono` ya armado, el rollback
restauraría un estado equivocado.

## Ubicación verificable

- Dependencias: [`pom.xml`](../../pom.xml)
- Controlador: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/controller/ProtectedApplicationController.java)
- Flujo: [`RegisterProtectedApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImpl.java)
- Transacción: [`SnapshotReactiveTransactionAdapter.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/transaction/SnapshotReactiveTransactionAdapter.java)
- Prueba real sobre Netty: [`ProtectedApplicationHttpTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/ProtectedApplicationHttpTests.java)

## Evidencia y límite

Las pruebas de flujo usan `StepVerifier`, que verifica la secuencia de señales y no solo el valor
final. La prueba HTTP arranca Netty.

El POM exige Java 25 y el pipeline compila con JDK 25.
