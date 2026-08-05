# 11. Interacción entre capas

[← Contratos](02-service-contracts.md) · [Siguiente: SOLID →](12-solid.md)

## Decisión arquitectónica

Solo se permite el flujo adaptador de entrada → caso de uso → dominio/puertos → adaptador de salida. El controlador no consulta memoria ni valida reglas de negocio; el dominio no conoce HTTP.

## Justificación

Esta dirección evita ciclos y hace visible dónde vive cada responsabilidad. Se descarta permitir llamadas horizontales entre controller, repositorio y auditoría porque dificulta cambiar infraestructura y probar inconsistencias.

## Implementación

En un `POST`, el controller valida forma, mapea el DTO a comando y llama al puerto. El servicio verifica unicidad, construye el agregado, persiste y audita dentro de la transacción. El adaptador dummy realiza solo almacenamiento. La respuesta vuelve por mapper y envelope común.

## Ubicación verificable

- Entrada: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp).
- Orquestación: [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/pdp).
- Salidas: [`ProtectedApplicationRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp) y [`AuditPort.java`](../../src/main/java/co/edu/uco/seguridad/pdp).
- Verificación: [`ProtectedApplicationHttpTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

La prueba HTTP recorre el flujo completo y la prueba Modulith detecta dependencias de módulo ilegales. Los límites internos de cada capa se complementan con revisión y pruebas de dominio; el siguiente paso de CI puede añadir ArchUnit específico por capa.
