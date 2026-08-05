# 03. Reglas de negocio e integridad de datos

## Decisión arquitectónica

La integridad se protege en value objects, agregado y caso de uso; la base de datos solo refuerza, no sustituye, esas reglas.

## Justificación

Un `NOT NULL` no valida formato, cardinalidad ni unicidad funcional por tenant. Confiar solo en persistencia impediría pruebas puras y permitiría inconsistencias al cambiar adaptador.

## Implementación

`TenantId`, `ApplicationName` y `ResourceIdentifier` rechazan valores inválidos. `ProtectedApplication` exige exactamente un recurso. El servicio consulta existencia por `(tenant,name)` y emite excepción de negocio antes de guardar.

## Ubicación verificable

- [`domain`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ProtectedApplicationService.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- Pruebas: [`ProtectedApplicationTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp) y [`ProtectedApplicationServiceTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

Las pruebas demuestran formato, recurso y duplicado. En SurrealDB se añadirá índice/constraint equivalente como segunda barrera, sin retirar las reglas del dominio.
