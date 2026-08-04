# 21. Modelo refinado

[← Adaptadores](20-clean-adapters.md) · [Siguiente: reactivo →](22-reactive-architecture.md)

## Decisión arquitectónica

`ProtectedApplication` es una raíz de agregado con un recurso inicial propio. Tenant, nombre, identificador y ruta de recurso son value objects; no son `String` sin semántica.

## Justificación

El primer sujeto de prueba debe conservar integridad desde su creación. Un modelo anémico permitiría registros sin tenant, nombre vacío o recurso relativo. Se descarta modelar tenant y recurso como agregados independientes: para E-1 no existe operación autónoma sobre ellos y sería complejidad prematura.

## Implementación

La fábrica `register` crea el agregado solo con los cuatro valores requeridos. Su constructor exige una lista inmutable de cardinalidad uno. `TenantId`, `ApplicationName` y `ResourceIdentifier` validan formato y longitud. `ProtectedResource` es entidad interna del agregado.

## Ubicación verificable

- [`ProtectedApplication.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ProtectedApplication.java)
- [`TenantId.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/TenantId.java)
- [`ApplicationName.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ApplicationName.java)
- [`ResourceIdentifier.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ResourceIdentifier.java)
- Prueba: [`ProtectedApplicationTests.java`](../../src/test/java/co/edu/uco/seguridad/applications/domain/ProtectedApplicationTests.java)

## Evidencia y límite

Las pruebas prueban cardinalidad y formatos. Estados, múltiples recursos, entornos y relaciones gráficas se agregan cuando los casos de uso lo justifiquen; no se inventan en esta línea base.
