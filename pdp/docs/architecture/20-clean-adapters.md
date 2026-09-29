# 20. Adaptadores limpios

[← SOLID](12-solid.md) · [Siguiente: modelo →](21-refined-model.md)

## Decisión arquitectónica

Los adaptadores conectan protocolos o tecnologías; no contienen decisiones de registro, unicidad,
cardinalidad ni consistencia de negocio.

## Justificación

Poner reglas en el controller produce endpoints inconsistentes; ponerlas en el adaptador de
persistencia las habría perdido al pasar del dummy en memoria a SurrealDB (ADR-0004). Se descarta el
controlador “grueso” que valida, consulta y persiste.

## Implementación

El controlador recibe, delega al mapper, llama al interactor y envuelve la respuesta. No conoce
value objects ni reglas. El mapper traduce formatos y delega el formato al value object en lugar de
repetir su expresión regular. El repositorio SurrealDB traduce la especificación que recibe a un
`WHERE` de SurrealQL: `matches` vive en el dominio, y el adaptador solo elige *cómo* ejecutarla contra
el motor real. La auditoría (aún dummy) registra identificadores, nunca el payload.

Las decisiones que podrían haberse filtrado al adaptador y no lo hicieron:

| Decisión                                         | Dónde vive realmente                |
|--------------------------------------------------|-------------------------------------|
| Formato de un código de recurso                  | `ResourceCode`                      |
| Comparación de nombres sin distinguir mayúsculas | `ApplicationName.sameAs`            |
| Qué significa que un filtro esté ausente         | `ApplicationCriteria`               |
| Límite máximo de una ventana                     | `PageWindow`                        |
| Unicidad de una concesión                        | `ProtectedResourceMustBeUniqueRule` |

## Ubicación verificable

- Web: [
  `resources/infrastructure/adapter/primary/web`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/primary/web)
- Persistencia real: [
  `SurrealProtectedResourceRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/persistence/repository/SurrealProtectedResourceRepository.java)
- Auditoría dummy: [
  `InMemoryAuditAdapter.java`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/infrastructure/adapter/secondary/audit/InMemoryAuditAdapter.java)

## Evidencia y límite

La prueba HTTP crea una aplicación sin conocer qué motor la persiste (Testcontainers provee una
SurrealDB real), y las pruebas de dominio rechazan un código inválido sin WebFlux. El mapper traduce
formatos, pero nunca toma decisiones de negocio: cuando una traducción necesite un `if` sobre
significado, ese `if` pertenece a una regla.

Desde el Stage
2 ([ADR-017](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-017-domain-events-application-event-publisher.md)), "
a
quién le importa que algo se registró" tampoco es una decisión del adaptador web ni del caso de uso:
la decide quien escucha `ProtectedResourceRegistered` como listener (`InMemoryAuditAdapter`, aún
dummy en su contenido — solo identificadores, nunca el payload).
