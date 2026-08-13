# 11. Interacción entre capas

[← Contratos](02-service-contracts.md) · [Siguiente: SOLID →](12-solid.md)

## Decisión arquitectónica

Solo se permite el flujo adaptador de entrada → interactor → caso de uso → rules validator → rules →
dominio/puertos → adaptador de salida. El controlador no consulta memoria ni decide reglas; el
dominio no conoce HTTP.

## Justificación

Esta dirección evita ciclos y hace visible dónde vive cada responsabilidad. Se descartan las
llamadas horizontales entre controller, repositorio y auditoría porque dificultan cambiar la
infraestructura y ocultan las inconsistencias.

## Implementación

```text
HTTP → Controller (arma RawRequest)
            ↓
       Interactor.execute(raw)
            ├─ mapper → DTO tipado
            ├─ Use Case
            │     ├─ Rules Validator → Rules
            │     └─ Domain · port/secondary → adapters
            └─ proyección → respuesta HTTP
            ↓
       Controller → ApiResponse
```

El controlador solo recibe, ejecuta el interactor y envuelve. El interactor mapea; el caso de uso
orquesta; el rules validator compone reglas; cada regla decide una cosa. Ver
[ADR-016](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-016-interactor-layer.md).

## Ubicación verificable

- Entrada: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/controller/ProtectedApplicationController.java)
- Interactores: [`application/port/primary/interactor`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/primary/interactor)
- Orquestación: [`RegisterProtectedApplicationUseCaseImpl.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/usecase/impl/RegisterProtectedApplicationUseCaseImpl.java)
- Reglas: [`recursos/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/rule)
- Salidas: [`recursos/application/port/secondary`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/secondary)

## Evidencia y límite

[`ProtectedApplicationHttpTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/ProtectedApplicationHttpTests.java)
recorre el flujo completo y `ModulithStructureTests` detecta dependencias de módulo ilegales. Los
límites internos de cada capa se sostienen con revisión y pruebas; añadir ArchUnit por capa es el
siguiente refuerzo posible.
