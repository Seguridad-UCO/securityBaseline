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

Las reglas se separan por si necesitan o no infraestructura, porque esa diferencia determina su
firma, su costo y el orden en que conviene ejecutarlas:

| Regla | Repositorio | Excepción |
|---|---|---|
| `ApplicationNameMustNotBeReservedRule` | no | `ReservedApplicationNameException` |
| *(la pertenencia del recurso al tenant de la aplicación se comprueba hoy dentro del propio caso de uso, con `ApplicationRepository.findByTenantAndId`, no como una `Rule` propia)* | sí | `ApplicationNotFoundException` |
| `TenantMustBeActiveRule` | sí | `TenantNotFoundException` / `TenantNotActiveException` |
| `ApplicationNameMustBeUniqueForTenantRule` | sí | `DuplicateApplicationException` |
| `ProtectedResourceMustBeUniqueRule` | sí | `DuplicateProtectedResourceException` |

Cada validator ejecuta primero las reglas sin repositorio: una petición inválida se rechaza sin
tocar el almacenamiento.

`TenantMustBeActiveRule` la publica el módulo `tenants` y la consumen `applications` y `resources`.
Es una única implementación inyectada, no una comprobación copiada, de modo que la decisión no puede
divergir entre módulos.

`resources` **no** vuelve a validar el tenant al registrar: `applications` ya lo hace con esa misma
regla durante el registro de la aplicación. Repetirla sería una segunda decisión sobre lo mismo y
una consulta de más.

## Ubicación verificable

- [`applications/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/application/rule)
- [`resources/application/rule`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/rule)
- [`TenantMustBeActiveRule.java`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/application/rule/TenantMustBeActiveRule.java)
- Pruebas: [`ApplicationRegistrationRuleTests`](../../src/test/java/co/edu/uco/seguridad/pdp/applications/application/rule/ApplicationRegistrationRuleTests.java),
  [`ProtectedResourceMustBeUniqueRuleImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/application/rule/impl/ProtectedResourceMustBeUniqueRuleImplTests.java),
  [`TenantMustBeActiveRuleImplTests`](../../src/test/java/co/edu/uco/seguridad/pdp/tenants/application/rule/TenantMustBeActiveRuleImplTests.java)

## Evidencia y límite

Cada regla se prueba aislada, con un stub por escenario. En SurrealDB se añadirá el índice único
equivalente como segunda barrera, sin retirar las reglas del núcleo: el índice protege los datos,
la regla explica el motivo.
