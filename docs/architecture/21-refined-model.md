# 21. Modelo refinado

[← Adaptadores](20-clean-adapters.md) · [Siguiente: reactivo →](22-reactive-architecture.md)

## Decisión arquitectónica

El modelo se reparte entre los módulos del mapa PDP en lugar de vivir en un único agregado
`ProtectedApplication`. `Tenant`, `Application` y `ProtectedResource` son entidades de su módulo;
`TenantId`, `ApplicationId`, `ResourceId`, `ApplicationName`, `ResourceCode`, `ActionCode` y
`PageWindow` son value objects. Ningún concepto del dominio es un `String` sin semántica.

> **Nota de reconciliación.** Versiones anteriores de este documento describían un agregado único
> `ProtectedApplication` con `ResourceIdentifier`. Ese diseño nunca se implementó y fue reemplazado
> por la separación en módulos de [`pdp-modulith-alignment.md`](pdp-modulith-alignment.md), que es
> la decisión vigente y la que verifica `ModulithStructureTests`. Este documento se corrigió para
> describir el código real.

## Justificación

El primer sujeto de prueba debe conservar integridad desde su creación. Un modelo anémico permitiría
registros sin tenant, con nombre vacío o con un código de recurso que el PDP no podría resolver. La
separación en módulos evita además que `resources` alcance el almacenamiento de `applications`.

## Implementación

- **Cuándo `record`:** cuando el concepto *es* exactamente sus valores y no tiene estado oculto.
  Aplica a todas las entidades y value objects del dominio.
- **Cuándo constructor compacto:** siempre. Es el único camino de entrada, de modo que una instancia
  que existe ya cumple sus invariantes y nada aguas abajo tiene que volver a comprobar `null`.
- **Cuándo factoría con nombre:** cuando la creación tiene un significado propio —
  `Application.register`, `ProtectedResource.register`, `PageWindow.ofPage` / `ofRange`.
- **Cuándo comportamiento en la entidad:** cuando la pregunta se responde con los datos que ya
  tiene — `belongsTo`, `isSameGrantAs`, `sameAs`, `contains`.
- **Cuándo NO un value object:** los identificadores de correlación siguen siendo `String` en
  `RequestContext`; son metadatos de transporte y no tienen invariante de negocio.

Ninguna clase de dominio tiene setters, Lombok ni anotaciones de framework. La mutabilidad existe en
exactamente dos lugares, ambos deliberados y ambos fuera del dominio: las entidades de persistencia
(porque los drivers lo exigen) y los DTO validados (porque la validación por setter es la
estrategia elegida en la frontera).

## Ubicación verificable

- [`commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons)
- [`ProtectedResource.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/domain/ProtectedResource.java)
- [`Application.java`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/domain/Application.java)
- [`Tenant.java`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants/domain/Tenant.java)
- Pruebas: [`ValueObjectTests`](../../src/test/java/co/edu/uco/seguridad/pdp/commons/model/ValueObjectTests.java) y
  [`ResourcePathTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/domain/model/ResourcePathTests.java)

## Evidencia y límite

Las pruebas cubren formatos, longitudes límite, comparación sin distinguir mayúsculas y ausencia de
partes obligatorias. Estados, múltiples recursos por aplicación y relaciones de grafo se agregarán
cuando un caso de uso los justifique; no se inventan en esta línea base.
