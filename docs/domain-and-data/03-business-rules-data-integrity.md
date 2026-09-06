# 03. Reglas de negocio e integridad de datos

[← Dominio y datos](README.md) · [Siguiente: transacciones →](10-transactions.md)

## Decisión arquitectónica

La integridad se protege en tres niveles con responsabilidades distintas: value objects (formato),
entidades (partes obligatorias) y **rules** explícitas (política). La base de datos refuerza, nunca
sustituye, esas reglas.

## Justificación

Un `NOT NULL` no valida formato, ni cardinalidad, ni unicidad funcional por tenant. Confiar solo en
la persistencia impide probar el negocio sin base de datos y permite inconsistencias al cambiar de
adaptador.

Las reglas se sacaron del caso de uso porque una condición de negocio enterrada en un `if` dentro de
una orquestación no se puede probar por separado, no se puede reutilizar y no se puede nombrar en
una conversación con el negocio.

## Implementación

**Toda regla vive en `domain/{slice}/rule/` y es una función pura**: recibe el dato ya resuelto en
un `record` y o no dice nada, o lanza su excepción. Ninguna consulta nada, ninguna devuelve `Mono`.

| Regla | Entrada ya resuelta | Excepción |
|---|---|---|
| `ApplicationNameMustNotBeReservedRule` | `ApplicationName` | `ReservedApplicationNameException` |
| `ApplicationNameMustBeUniqueForTenantRule` | `ApplicationNameAvailability` | `DuplicateApplicationException` |
| `ApplicationMustExistForTenantRule` | `ApplicationExistence` | `ApplicationNotFoundException` |
| `TenantMustExistRule` | `TenantExistence` | `TenantNotFoundException` |
| `TenantStatusMustBeActiveRule` | `TenantActivation` | `TenantNotActiveException` |
| `TenantCodeMustBeUniqueRule` | `TenantCodeAvailability` | `DuplicateTenantException` |
| `UserMustExistRule` | `UserExistence` | `UserNotFoundException` |
| `ProtectedResourceMustBeUniqueRule` | `ProtectedResourceAvailability` | `DuplicateProtectedResourceException` |

**Quien consulta es el validador**, en `application/{slice}/rule/validator/`: resuelve contra el
puerto lo que cada regla necesita saber, construye el `record` y deja decidir a la regla. Ejecuta
primero lo que no necesita E/S, de modo que una petición inválida se rechaza sin tocar el
almacenamiento. Cuando hay una sola regla y nadie más la consume, el propio caso de uso hace ese
papel sin validador de por medio (`CreateTenantUseCaseImpl`, `AssignTenantUseCaseImpl`).

El puerto responde exactamente lo que la regla necesita, no el agregado entero:
`ApplicationRepository.existsByTenantAndId` devuelve un booleano y `TenantRepository.findStatusById`
devuelve un enum. Preguntar «¿existe?» cargando la fila completa era traer de más.

Dos validadores se publican como interfaz nombrada de Modulith y los consumen otros módulos:
`TenantMustBeActiveValidator` (lo usan `applications` e `identity`) y
`ApplicationMustExistForTenantValidator` (lo usa `resources`). Es una única implementación
inyectada, no una comprobación copiada, de modo que la decisión no puede divergir entre módulos.

`resources` **no** vuelve a validar el tenant al registrar: `applications` ya lo hace durante el
registro de la aplicación. Repetirlo sería una segunda decisión sobre lo mismo y una consulta de más.

## Ubicación verificable

- [`applications/domain/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/domain/rule)
- [`tenants/domain/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/domain/rule)
- [`resources/domain/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/domain/rule)
- [`TenantMustBeActiveValidator.java`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/application/rule/validator/TenantMustBeActiveValidator.java)
- Pruebas de las reglas (sin Reactor ni dobles): [`ApplicationRuleTests`](../../src/test/java/co/edu/uco/seguridad/pdp/applications/domain/rule/ApplicationRuleTests.java),
  [`TenantRuleTests`](../../src/test/java/co/edu/uco/seguridad/pdp/tenants/domain/rule/TenantRuleTests.java),
  [`ProtectedResourceMustBeUniqueRuleTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/domain/rule/ProtectedResourceMustBeUniqueRuleTests.java)
- Pruebas de los validadores (ahí sí hay repositorio falso): [`TenantMustBeActiveValidatorTests`](../../src/test/java/co/edu/uco/seguridad/pdp/tenants/application/rule/validator/TenantMustBeActiveValidatorTests.java)

## Evidencia y límite

Cada regla se prueba aislada, con un stub por escenario. En SurrealDB se añadirá el índice único
equivalente como segunda barrera, sin retirar las reglas del núcleo: el índice protege los datos,
la regla explica el motivo.
