# Convención de idioma

[← Transversales](README.md)

## Decisión

El código (paquetes, clases, métodos, variables) se escribe en inglés. El vocabulario del negocio
que ese código representa, los mensajes de usuario y toda la documentación se escriben en español.

## Justificación

El código base nació mezclando ambos: paquetes `pdp.applications`/`pdp.resources` en español junto a
clases como `RegisterApplicationUseCase` en inglés, con Javadoc en español en medio de identificadores
en inglés. Ninguna de las dos convenciones puras es gratis: todo en español rompe con el ecosistema
Java/Spring, que usa inglés en sus propias convenciones (`Repository`, `Configuration`, `save`,
`findBy`); todo en inglés obliga a traducir el vocabulario del negocio (tenant, aplicación protegida,
recurso, acción) cada vez que se documenta o se discute con el equipo, que trabaja en español.

Se decidió por la línea que ya seguía la mayoría del código nuevo: identificadores técnicos en inglés,
significado de negocio en español. Es la misma separación que existe entre `shared` (capacidad
técnica) y `pdp/commons` (vocabulario del negocio) — ver
[04. Capacidades transversales](04-cross-cutting-capabilities.md).

## Implementación

| Elemento                                                                 | Idioma             | Ejemplo                                                            |
|--------------------------------------------------------------------------|--------------------|--------------------------------------------------------------------|
| Paquetes                                                                 | inglés             | `pdp.applications`, `pdp.resources`, `pdp.tenants`, `pdp.identity` |
| Clases, métodos, variables                                               | inglés             | `RegisterApplicationUseCase`, `tenantId`                           |
| Javadoc y comentarios                                                    | español            | explican el porqué, igual que esta documentación                   |
| Mensajes de error y catálogos (`{Slice}Messages`, `ValueObjectMessages`) | español            | texto que ve el usuario final                                      |
| Nombres de tests                                                         | inglés descriptivo | `reports_a_missing_field_by_name`                                  |
| Documentación (`docs/`)                                                  | español            | este archivo                                                       |

Los paquetes que nacieron en español (`applications`, `resources`) ya se renombraron a `applications`
y `resources`. No queda ningún paquete en español.

## Ubicación verificable

- Slices de negocio: [`pdp/applications`](../../src/main/java/co/edu/uco/seguridad/pdp/applications),
  [`pdp/resources`](../../src/main/java/co/edu/uco/seguridad/pdp/resources),
  [`pdp/tenants`](../../src/main/java/co/edu/uco/seguridad/pdp/tenants),
  [`pdp/identity`](../../src/main/java/co/edu/uco/seguridad/pdp/identity).
- Vocabulario de negocio compartido: [`pdp/commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons).
- Capacidades técnicas: [`shared`](../../src/main/java/co/edu/uco/seguridad/shared).

## Evidencia y límite

Esta es una convención de estilo, no una regla verificada por `ModulithStructureTests` ni por ninguna
otra prueba: no hay forma automatizada de rechazar un nombre de paquete en el idioma equivocado. Su
cumplimiento depende de la revisión de código — y es uno de los cuatro juicios explícitos del agente
validador del harness (ver [`docs/ai-harness`](../ai-harness/README.md)).

El renombrado de paquetes sí dejó una consecuencia verificable: todos los enlaces de "ubicación
verificable" de esta documentación apuntaban a las rutas viejas. Esa deriva se detecta ahora con
`.claude/tools/drift.ps1`.
