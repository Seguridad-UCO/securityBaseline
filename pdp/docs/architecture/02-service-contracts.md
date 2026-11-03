# 02. Contratos de servicios

[← Clean Architecture](01-clean-architecture.md) · [Siguiente: interacción →](11-layer-interaction.md)

## Decisión arquitectónica

Los contratos de negocio se declaran como puertos de entrada, no como controladores ni como
servicios Spring. La convención de firmas es explícita y uniforme:

| Forma               | Contrato                | Cuándo                                                    |
|---------------------|-------------------------|-----------------------------------------------------------|
| Con retorno         | `Mono<T>`               | La operación produce un valor que el llamador necesita    |
| Sin retorno         | `Mono<Void>`            | La operación solo debe completarse                        |
| Colección acotada   | `Mono<ResultPage<T>>`   | La consulta devuelve filas más total y ventana            |
| Regla de negocio    | `void execute(I)`       | **Siempre síncrona**: recibe el dato ya resuelto y decide |
| Validador de reglas | `Mono<Void> execute(I)` | Hace la E/S que las reglas necesitan y las aplica         |

No se usa `Flux` en los puertos de consulta: un flujo de filas perdería el total y la ventana, que
son parte de la respuesta.

## Justificación

El consumidor de negocio debe conocer operación, entrada y salida, pero no HTTP, anotaciones ni
persistencia. Se descarta exponer un `Repository` al controlador porque ese patrón permite que la
interfaz de usuario eluda reglas y transacciones.

## Implementación

`RegisterApplicationUseCase` recibe un comando completamente tipado y devuelve
`Mono<ProtectedApplicationCatalogEntry>`. `ListApplicationsUseCase` recibe criterios y
ventana y devuelve `Mono<ResultPage<...>>`. Los puertos de salida expresan colaboración:
repositorio, auditoría, transacción, reloj y generador de identificadores, cada uno con su propia
interfaz pequeña.

## Ubicación verificable

- [
  `RegisterApplicationUseCase.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/RegisterApplicationUseCase.java)
- `ListApplicationsUseCase.java`
- Contratos de regla: [`shared/rule`](../../src/main/java/co/edu/uco/seguridad/shared/contract)
- Puertos transversales: [`shared/port`](../../src/main/java/co/edu/uco/seguridad/shared/port)
- Implementaciones: [`resources/application`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application)

## Evidencia y límite

[
`RegisterApplicationUseCaseImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImplTests.java)
instancia el caso de uso con dummies y sin Spring. Los contratos futuros de PEP/PDP se agregarán
como nuevos puertos, no como métodos dentro de estos.
