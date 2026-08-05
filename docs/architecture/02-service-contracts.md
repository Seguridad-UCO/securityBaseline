# 02. Contratos de servicios

[← Clean Architecture](01-clean-architecture.md) · [Siguiente: interacción →](11-layer-interaction.md)

## Decisión arquitectónica

Los contratos de negocio se declaran como puertos de entrada y no como controladores ni servicios Spring: registrar y buscar aplicaciones protegidas.

## Justificación

El consumidor de negocio debe conocer operación, entrada y salida, pero no HTTP, anotaciones o persistencia. Se descarta exponer directamente un `Repository` al controlador porque ese patrón permite que la interfaz de usuario eluda reglas y transacciones.

## Implementación

`RegisterProtectedApplicationUseCase` recibe un `RegisterProtectedApplicationCommand` tipado y devuelve `Mono<ProtectedApplication>`. `SearchProtectedApplicationsUseCase` recibe criterios y ventana de resultados. Los puertos de salida expresan colaboración: repositorio, auditoría, transacción, reloj y generador de ID. `ProtectedApplicationService` implementa los casos de uso y depende de esas interfaces.

## Ubicación verificable

- [`RegisterProtectedApplicationUseCase.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`SearchProtectedApplicationsUseCase.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- Contrato HTTP traducido, no sustituido: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp)

## Evidencia y límite

Las pruebas de servicio instancian el caso de uso con dummies, sin levantar Spring. Esto demuestra independencia del framework. Los contratos futuros de PEP/PDP se agregan como nuevos puertos, no dentro de este CRUD.
