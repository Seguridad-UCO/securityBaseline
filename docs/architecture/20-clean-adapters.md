# 20. Adaptadores limpios

[← SOLID](12-solid.md) · [Siguiente: modelo →](21-refined-model.md)

## Decisión arquitectónica

Los adaptadores conectan protocolos o tecnologías; no contienen decisiones de registro, unicidad,
cardinalidad ni consistencia de negocio.

## Justificación

Poner reglas en el controller produce endpoints inconsistentes; ponerlas en el dummy las pierde al
adoptar SurrealDB. Se descarta el controlador “grueso” que valida, consulta y persiste.

## Implementación

El controlador recibe, delega al mapper, llama al interactor y envuelve la respuesta. No conoce
value objects ni reglas. El mapper traduce formatos y delega el formato al value object en lugar de
repetir su expresión regular. El repositorio dummy ejecuta la especificación que recibe: `matches`
vive en el dominio, y el adaptador solo elige *cómo* recorrerla. La auditoría dummy registra
identificadores, nunca el payload.

Las decisiones que podrían haberse filtrado al adaptador y no lo hicieron:

| Decisión | Dónde vive realmente |
|---|---|
| Formato de un código de recurso | `ResourceCode` |
| Comparación de nombres sin distinguir mayúsculas | `ApplicationName.sameAs` |
| Qué significa que un filtro esté ausente | `ProtectedApplicationCriteria` |
| Límite máximo de una ventana | `PageWindow` |
| Unicidad de una concesión | `ProtectedResourceMustBeUniqueRule` |

## Ubicación verificable

- Web: [`recursos/infrastructure/adapter/primary/web`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web)
- Persistencia dummy: [`InMemoryProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/persistence/repository/InMemoryProtectedResourceRepository.java)
- Auditoría dummy: [`InMemoryAuditAdapter.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/secondary/audit/InMemoryAuditAdapter.java)

## Evidencia y límite

La prueba HTTP crea una aplicación sin conocer el dummy, y las pruebas de dominio rechazan un código
inválido sin WebFlux. El mapper traduce formatos, pero nunca toma decisiones de negocio: cuando una
traducción necesite un `if` sobre significado, ese `if` pertenece a una regla.

Cuando la auditoría dummy se sustituya por un listener de eventos de dominio
([ADR-0002](../governance/adr/adr-0002-domain-events-modulith-registry.md)), esta tabla gana una fila:
"a quién le importa que algo se registró" tampoco es una decisión del adaptador web ni del caso de
uso — la decide quien escucha el evento.
