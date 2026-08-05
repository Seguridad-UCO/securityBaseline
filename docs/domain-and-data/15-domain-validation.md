# 15. Validación del dominio

## Decisión arquitectónica

Se combinan value objects/aggregate validators para invariantes y una Specification para criterios de consulta reutilizables.

## Justificación

Las reglas de creación deben ejecutarse siempre; los filtros deben poder combinarse sin duplicar métodos. Se descarta poner validación exclusiva en Bean Validation, porque no existe cuando se invoca el caso de uso desde otro adaptador.

## Implementación

Los constructores de VOs y agregado validan creación. `ProtectedApplicationCriteria.matches` representa una specification compuesta por tenant, nombre y recurso; cada condición ausente equivale a “no restringir”.

## Ubicación verificable

- [`ProtectedApplicationCriteria.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ProtectedApplication.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ProtectedApplicationTests.java`](../../src/test/java/co/edu/uco/seguridad/pdp)

## Evidencia y límite

El patrón soporta añadir `and/or/not` si aparecen filtros complejos. Para E-1, tres criterios opcionales evitan sobreingeniería.
