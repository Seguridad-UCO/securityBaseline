# 14. DTOs seguros

## Decisión arquitectónica

Los DTOs son inmutables y declaran restricciones de presencia, tamaño y patrón; el dominio vuelve a validar lo crítico.

## Justificación

Un DTO mutable/nulo facilita estados parciales. Solo Bean Validation no protege llamadas que no provienen de HTTP; por ello se usa defensa en profundidad.

## Implementación

El request es `record`, usa `@NotBlank`, `@Size` y `@Pattern`; sus campos no tienen defaults implícitos peligrosos. El mapper crea VOs no nulos. Para filtros opcionales se usa `Optional`, no `null` como valor de negocio.

## Ubicación verificable

- [`RegisterProtectedApplicationRequest.java`](../../src/main/java/co/edu/uco/seguridad/applications/infrastructure/web/RegisterProtectedApplicationRequest.java)
- [`ProtectedApplicationCriteria.java`](../../src/main/java/co/edu/uco/seguridad/applications/domain/ProtectedApplicationCriteria.java)
- [Reglas de dominio](../domain-and-data/03-business-rules-data-integrity.md).

## Evidencia y límite

Un recurso sin `/` es rechazado por el DTO y también por `ResourceIdentifier`. Esto prueba que ninguna capa única es la única barrera.
