# 14. DTOs seguros

[← Estrategia de entrada](13-input-strategy-dtos.md) · [↑ Interfaces](README.md)

## Decisión arquitectónica

La defensa en profundidad se mantiene, pero ya no descansa en Bean Validation. Hay tres barreras
independientes y cada una sirve para algo distinto:

| Barrera              | Dónde                        | Protege contra                         |
|----------------------|------------------------------|----------------------------------------|
| Contrato de petición | setters del DTO validado     | Entrada HTTP ausente o mal formada     |
| Invariante de valor  | constructor del value object | Cualquier llamador, venga o no de HTTP |
| Regla de negocio     | rules validator              | Datos bien formados pero no permitidos |

## Justificación

Bean Validation solo existe cuando la invocación viene de HTTP. Un caso de uso llamado desde un
listener, un job o un test no pasa por ella. Por eso la regla de formato vive en el value object:
así ningún camino de entrada puede saltársela.

Además, `spring-boot-starter-validation` **se retiró del `pom.xml`**. No basta con dejar de usar las
anotaciones: mientras la API esté en el classpath, alguien acabará añadiendo un `@NotBlank`. Sin la
dependencia, el compilador lo impide.

## Implementación

- Los DTO crudos son `record`: inmutables y sin defaults implícitos peligrosos.
- El DTO validado es mutable, pero su mutabilidad está acotada al mapper y cada mutación pasa por
  su validación. Leer un campo que nunca se asignó lanza `NullPointerException` con el nombre del
  campo: eso es un error de programación, no del cliente, y debe fallar ruidosamente.
- Los filtros opcionales se modelan con `Optional`, nunca con `null` como valor de negocio.
- Los DTO de respuesta son `record` planos de tipos primitivos. Serializar el modelo de lectura
  publicaría la forma de `TenantId`, `ResourceCode` y demás value objects, y renombrar un campo
  interno se convertiría en un cambio incompatible de la API sin que nadie lo note.

## Ubicación verificable

- [
  `resources/infrastructure/web/dto`](../../src/main/java/co/edu/uco/seguridad/pdp/applications/infrastructure/adapter/primary/web/dto)
- `ApplicationWebResponse.java`
- Ausencia de la dependencia: [`pom.xml`](../../pom.xml)
- [Reglas de dominio](../domain-and-data/03-business-rules-data-integrity.md)

## Evidencia y límite

Un `resourceCode` en mayúsculas es rechazado por el setter **y** por `ResourceCode`; la prueba de
dominio lo comprueba sin WebFlux y la prueba HTTP con él. Eso demuestra que ninguna capa es la única
barrera.
