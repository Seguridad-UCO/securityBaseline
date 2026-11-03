# 15. Validación del dominio

[← Transacciones](10-transactions.md) · [Siguiente: repositorios →](16-repository-strategy.md)

## Decisión arquitectónica

Dos mecanismos distintos para dos problemas distintos: **invariantes** en constructores de value
objects y entidades, y **specification** para los criterios de consulta.

## Justificación

Las reglas de creación deben ejecutarse siempre, vengan de donde vengan. Los filtros deben poder
combinarse sin duplicar métodos. Se descarta poner la validación de formato exclusivamente en Bean
Validation, porque no existe cuando el caso de uso se invoca desde otro adaptador.

## Implementación

**Invariantes.** El constructor compacto es el único camino de entrada. Una instancia que existe ya
es válida, y por eso nada aguas abajo vuelve a comprobar `null` ni formato. Cada value object lanza
su propia excepción (`InvalidResourceCodeException`, `InvalidPageWindowException`, …) en vez de un
`IllegalArgumentException` genérico, para que el traductor HTTP pueda dar un código estable sin
inspeccionar mensajes.

**Specification.** `ApplicationCriteria.matches` compone tenant, nombre y recurso. Cada
condición ausente equivale a “no restringir”. La semántica vive en el dominio, no en el adaptador:
es lo que garantiza que el dummy y un futuro adaptador SurrealDB no puedan discrepar sobre qué
significa `contains`.

Un fragmento en blanco se normaliza a ausente, para que `?nameContains=` no se interprete como
“nombres que contienen la cadena vacía”.

## Ubicación verificable

- `ApplicationCriteria.java`
- [`commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons) (value objects e invariantes)
- Pruebas: [
  `ResourcePathTests`](../../src/test/java/co/edu/uco/seguridad/pdp/resources/domain/model/ResourcePathTests.java)

## Evidencia y límite

Las pruebas cubren cada filtro por separado, la combinación conjuntiva de los tres, el criterio sin
filtros y el fragmento en blanco. El patrón soporta `and/or/not` si aparecen filtros complejos; para
E-1 tres criterios opcionales evitan sobreingeniería.
