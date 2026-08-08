# 13. Estrategia para recibir información

[← Parámetros](06-parameter-handling.md) · [Siguiente: DTOs seguros →](14-secure-dtos.md)

## Decisión arquitectónica

La entrada atraviesa **dos niveles de DTO web** y luego un **App Request** tipado antes de tocar el caso de uso:

```text
JSON
 ↓
Raw Request DTO      record, todos los campos String, sin anotaciones de validación
 ↓
Mapper               invoca los setters; aquí ocurre la validación
 ↓
Web Request DTO      campos tipados como value objects (infraestructura)
 ↓
App Request          tipado, inmutable (application.port.primary.dto.request)
 ↓
Use Case → Domain
```

## Justificación

Con anotaciones de Jakarta Validation sobre el DTO que recibe el JSON, un valor inválido lo rechaza
el framework **antes** de que el código de la aplicación lo vea. El resultado es una respuesta de
error que no controlamos, que no lleva nuestro código estable ni los identificadores de correlación,
y que no puede distinguir “falta el campo” de “el campo tiene mal formato”.

El DTO crudo elimina ese problema por construcción: si todos los campos son `String` y ninguno tiene
restricciones, el binding **siempre** tiene éxito. A partir de ahí, cada rechazo es una decisión
nuestra, con nuestro código de error y nuestro nombre de campo.

Se descartó recibir entidades del dominio directamente desde JSON: la forma pública quedaría atada
al modelo interno y no habría manera de versionar la API.

## Implementación

El nivel uno es un `record` inerte. No promete nada sobre su contenido, así que nadie puede
consumirlo directamente: el mapper es el único camino hacia adelante.

El nivel dos es una clase mutable con setters, y es el único lugar del proyecto donde eso es
deliberado. Cada setter es el paso de validación de su campo: primero presencia, después el value
object.

```java
public void setResourceCode(String value) {
    this.resourceCode = RequestFieldParser.parse("resourceCode", value, ResourceCode::new);
}
```

No es un `record` a propósito. El constructor canónico de un record validaría todo de una vez, solo
podría reportar el primer problema y no dejaría un lugar con nombre donde colgar la regla de cada
campo.

`RequestFieldParser.parse` delega el formato al value object y solo añade **qué campo** lo traía. Si
repitiera aquí la expresión regular, la frontera tendría una segunda definición de “código válido”
que podría divergir de la del dominio.

Pasado el mapper, el adaptador proyecta al **App Request** (`application.port.primary.dto.request`).
Ningún `String` sin tipar viaja hacia adentro. El App Request y el Web Request pueden compartir
nombre simple; viven en paquetes distintos y los mappers usan nombres completamente cualificados
cuando hace falta.

## Ubicación verificable

- [`RegisterProtectedApplicationRawRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/raw/RegisterProtectedApplicationRawRequest.java)
- [`RegisterProtectedApplicationRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/RegisterProtectedApplicationRequest.java) (web)
- [`RequestFieldParser.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/RequestFieldParser.java)
- App Request: [`application/port/primary/dto/request`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/primary/dto/request)
- [`RegisterProtectedApplicationRequestMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/RegisterProtectedApplicationRequestMapper.java)
- Pruebas: [`RegisterProtectedApplicationRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/RegisterProtectedApplicationRequestMapperTests.java)

## Evidencia y límite

Las pruebas del mapper cubren dato válido, campo ausente, campo en blanco, formato incorrecto, tipo
incorrecto y valor fuera de rango, y verifican en cada caso el **nombre del campo** reportado. La
prueba HTTP confirma que el mismo error llega al cliente como `MISSING_REQUEST_FIELD` o
`MALFORMED_REQUEST_FIELD` con la propiedad `field`.

Límite: este esquema reporta el primer campo que falla, no la lista completa. Acumular todos los
errores es posible dentro de la misma estructura — los setters ya están separados por campo — y se
hará cuando un consumidor lo pida.
