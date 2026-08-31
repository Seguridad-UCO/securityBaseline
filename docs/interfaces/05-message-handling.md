# 05. Manejo de mensajes

[← Interfaces](README.md) · [Siguiente: parámetros →](06-parameter-handling.md)

> **Estado — 2026-08-31.** La prueba HTTP end-to-end `ProtectedApplicationHttpTests` que este
> documento cita como evidencia **no existe**. Lo que hoy cubre esta área son pruebas unitarias
> (`ApiErrorHandlerTests`, `SecurityWebFilterChainTests`, los tests de controller y de mapper).
> Reponerla es parte de la historia **HU-001**; ver [el harness](../ai-harness/README.md).

## Decisión arquitectónica

Los éxitos usan un envelope común con código estable; los fallos usan RFC 9457 `ProblemDetail` con
código estable y correlación.

## Justificación

Mensajes textuales por controlador son inconsistentes y frágiles para los clientes. Se descarta
devolver excepciones o textos técnicos al consumidor.

## Implementación

`ApiResponse` lleva `code`, `message`, `data`, timestamp y los dos identificadores de correlación.

`ApiErrorHandler` traduce una sola vez. No declara un handler por excepción concreta: se engancha a
las dos jerarquías base y usa el `code()` que cada excepción ya trae. Añadir una regla nueva no
requiere tocar el handler — si tuviera un `switch` por tipo, cada regla nueva sería una edición aquí
y tarde o temprano una omisión.

| Situación | HTTP | Código |
|---|---|---|
| Campo obligatorio ausente | 400 | `MISSING_REQUEST_FIELD` |
| Campo con formato o tipo inválido | 400 | `MALFORMED_REQUEST_FIELD` |
| Parámetros incompatibles entre sí | 400 | `CONFLICTING_REQUEST_PARAMETERS` |
| Value object rechazado fuera de HTTP | 400 | `INVALID_*` |
| Tenant inexistente / suspendido | 400 | `TENANT_NOT_FOUND` / `TENANT_NOT_ACTIVE` |
| Nombre reservado | 400 | `RESERVED_APPLICATION_NAME` |
| Recurso de otro tenant | 400 | `RESOURCE_TENANT_MISMATCH` |
| Aplicación o concesión ya registrada | 409 | `APPLICATION_ALREADY_EXISTS` / `PROTECTED_RESOURCE_ALREADY_EXISTS` |
| Cuerpo ilegible | 400 | `MALFORMED_REQUEST` |
| Fallo no previsto | 500 | `INTERNAL_ERROR` |

El 500 devuelve un detalle genérico; la causa real va al log. Nunca se envía una traza al cliente.

## Ubicación verificable

- [`ApiResponse.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/ApiResponse.java)
- [`ApiErrorHandler.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/exceptionhandler/ApiErrorHandler.java)
- Prueba: `ProtectedApplicationHttpTests` *(no implementado)*

## Evidencia y límite

La prueba HTTP verifica el código de cada situación de la tabla que es alcanzable por HTTP. La
taxonomía crece de forma centralizada: cada excepción nueva aporta su código y el handler no cambia.
