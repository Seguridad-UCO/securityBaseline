# 16. Estrategia de repositorios

## Decisión arquitectónica

El repositorio expone búsqueda por objeto criterio y ventana, no métodos específicos como `findByName`.

## Justificación

El catálogo crecerá en atributos y combinaciones. Multiplicar métodos convierte cada filtro en cambio de API e infraestructura.

## Implementación

`findBy(ProtectedApplicationCriteria, PageWindow)` delega la semántica de criterio al dominio y deja al adaptador elegir cómo traducirla. El dummy filtra una colección; SurrealDB lo traducirá a SurrealQL/índices.

## Ubicación verificable

- [`ProtectedApplicationRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`InMemoryProtectedApplicationRepository.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- HTTP de consumo: [`ProtectedApplicationController.java`](../../src/main/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

El contrato ya cubre igualdad y contiene. Operadores relacionales y `IN/NOT IN` se agregan como nuevos campos/operadores del criterio cuando el modelo los necesite.
