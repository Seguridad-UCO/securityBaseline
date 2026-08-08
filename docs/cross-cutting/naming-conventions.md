# Convención de idioma

[← Transversales](README.md)

## Decisión

El código (paquetes, clases, métodos, variables) se escribe en inglés. El vocabulario del negocio
que ese código representa, los mensajes de usuario y toda la documentación se escriben en español.
Ningún paquete nuevo debe nombrarse en español (p. ej. `recursos`, `aplicaciones`); los existentes se
conservan porque ya son parte del contrato entre módulos verificado por Spring Modulith y renombrarlos
sin una razón funcional sería puro churn.

## Justificación

El código base nació mezclando ambos: paquetes `pdp.aplicaciones`/`pdp.recursos` en español junto a
clases como `RegisterProtectedApplicationUseCase` en inglés, con Javadoc en español en medio de
identificadores en inglés. Ninguna de las dos convenciones puras (todo en español, todo en inglés) es
gratis: todo en español rompe con el ecosistema Java/Spring, que usa inglés en sus propias
convenciones (`Repository`, `Configuration`, `save`, `findBy`); todo en inglés obliga a traducir el
vocabulario del negocio (tenant, aplicación protegida, recurso, acción) cada vez que se documenta o se
discute con el equipo, que trabaja en español.

Se decide por la línea que ya sigue la mayoría del código nuevo: identificadores técnicos en inglés,
significado de negocio en español. Es la misma separación que ya existe entre `shared` (capacidad
técnica) y `pdp/commons` (vocabulario del negocio) — ver
[04. Capacidades transversales](04-cross-cutting-capabilities.md).

## Implementación

| Elemento | Idioma | Ejemplo |
|---|---|---|
| Paquetes nuevos | inglés | `pdp.protectedresources` (no `pdp.recursosprotegidos`) |
| Paquetes existentes en español | se conservan | `pdp.aplicaciones`, `pdp.recursos`, `pdp.tenants` (mixto) |
| Clases, métodos, variables | inglés | `RegisterProtectedApplicationUseCase`, `tenantId` |
| Javadoc y comentarios | español | explican el porqué, igual que esta documentación |
| Mensajes de error y catálogos (`crosscutting.messages`) | español | texto que ve el usuario final |
| Nombres de tests | inglés descriptivo | `reports_a_missing_field_by_name` |
| Documentación (`docs/`) | español | este archivo |

No se abre una tarea de renombrado masivo de `aplicaciones`/`recursos`/`tenants` a inglés: el costo
(romper el historial de Git, invalidar los enlaces de esta documentación, reentrenar al equipo) supera
el beneficio de una convención más limpia para un contenedor que Spring Modulith ya verifica
estructuralmente sin importar el idioma del nombre de paquete.

## Ubicación verificable

- Paquetes en español ya establecidos: [`pdp/aplicaciones`](../../src/main/java/co/edu/uco/seguridad/pdp/aplicaciones),
  [`pdp/recursos`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos).
- Paquetes técnicos en inglés: [`shared`](../../src/main/java/co/edu/uco/seguridad/shared).

## Evidencia y límite

Esta es una convención de estilo, no una regla verificada por `ModulithStructureTests` ni por
ninguna otra prueba: no hay forma automatizada de rechazar un nombre de paquete en el idioma
equivocado. Su cumplimiento depende de la revisión de código.
