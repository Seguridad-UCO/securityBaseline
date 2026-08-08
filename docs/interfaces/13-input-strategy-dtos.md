# 13. Estrategia para recibir información

[← Parámetros](06-parameter-handling.md) · [Siguiente: DTOs seguros →](14-secure-dtos.md)

## Decisión arquitectónica

Toda entrada empieza como un **Raw Request** (`record`, todos los campos `String`, sin anotaciones de
validación) y termina como un **App Request** tipado
(`application.port.primary.dto.request`) antes de tocar el caso de uso. Cuántos pasos hay entre esos
dos puntos depende de si hace falta **ensamblar** algo a partir de los campos sueltos:

```text
JSON
 ↓
Raw Request DTO      record, todos los campos String, sin anotaciones de validación
 ↓
 ├─ Registro: el mapper convierte cada campo directo a su value object — nada que ensamblar
 │   ↓
 │  App Request       tipado, inmutable (application.port.primary.dto.request)
 │
 └─ Búsqueda: el mapper pasa por un Web Request mutable, porque construir el criterio de
     búsqueda a partir de campos sueltos SÍ es una transformación real
     ↓
    Web Request DTO   mutable, campos tipados como value objects (infraestructura)
     ↓
    App Request       tipado, inmutable — el criterio ya está ensamblado como Specification
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

**El registro y la búsqueda dejaron de compartir la misma cantidad de pasos** (hasta el Stage 1 de la
evolución de arquitectura ambos tenían un Web Request intermedio). El registro tiene cuatro campos que
se convierten uno a uno en su value object — `tenantId`, `applicationName`, `resourceCode`, `action` —
y el App Request los recibe con el mismo nombre y tipo. Un Web Request ahí solo repetía esos cuatro
campos con el mismo tipo, para que un mapper después los volviera a copiar sin transformar nada. Se
eliminó: el mapper de registro ahora construye el App Request directamente, campo por campo, y
conserva la misma precisión de error por campo porque sigue usando `RequestFieldParser.parse` para cada
uno. La búsqueda sí necesita el paso intermedio, porque `tenantId`/`nameContains`/`resourceContains` y
la ventana de paginación se **consolidan** en un objeto `ProtectedApplicationCriteria` (una
Specification) — eso es una transformación real, no una copia.

## Implementación

### Registro — un solo mapper, sin DTO intermedio

```java
public static RegisterProtectedApplicationRequest toRequest(RegisterProtectedApplicationRawRequest raw) {
    return new RegisterProtectedApplicationRequest(
            RequestFieldParser.parse("tenantId", raw.tenantId(), TenantId::new),
            RequestFieldParser.parse("applicationName", raw.applicationName(), ApplicationName::new),
            RequestFieldParser.parse("resourceCode", raw.resourceCode(), ResourceCode::new),
            RequestFieldParser.parse("action", raw.action(), ActionCode::new));
}
```

`RequestFieldParser.parse` delega el formato al value object y solo añade **qué campo** lo traía —
exactamente la misma garantía que daban los setters del Web Request que se eliminó, pero sin una clase
mutable que solo iba a vivir para volver a copiarse en un record.

### Búsqueda — el Web Request sigue siendo mutable a propósito

```java
public void setResourceCode(String value) {
    this.resourceCode = RequestFieldParser.parse("resourceCode", value, ResourceCode::new);
}
```

No es un `record` a propósito. El constructor canónico de un record validaría todo de una vez, solo
podría reportar el primer problema y no dejaría un lugar con nombre donde colgar la regla de cada
campo — y aquí, a diferencia del registro, después hay una transformación (armar el criterio) que
necesita los cuatro valores ya construidos antes de decidir cómo combinarlos.

Pasado el mapper de búsqueda, el adaptador proyecta al **App Request** ensamblando
`ProtectedApplicationCriteria` a partir de los campos ya tipados. El App Request y el Web Request
pueden compartir nombre simple; viven en paquetes distintos y los mappers usan nombres completamente
cualificados cuando hace falta.

## Ubicación verificable

- Crudo: [`RegisterProtectedApplicationRawRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/raw/RegisterProtectedApplicationRawRequest.java),
  [`SearchProtectedApplicationsRawRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/raw/SearchProtectedApplicationsRawRequest.java)
- Web Request (solo búsqueda): [`SearchProtectedApplicationsRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/dto/request/SearchProtectedApplicationsRequest.java)
- [`RequestFieldParser.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/RequestFieldParser.java)
  — utilidad compartida del adaptador web; el interactor invoca los mappers que la usan
- App Request: [`application/port/primary/dto/request`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/application/port/primary/dto/request)
- Mappers: [`RegisterProtectedApplicationRequestMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/RegisterProtectedApplicationRequestMapper.java),
  [`SearchProtectedApplicationsRequestMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/SearchProtectedApplicationsRequestMapper.java)
- Pruebas: [`RegisterProtectedApplicationRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/RegisterProtectedApplicationRequestMapperTests.java),
  [`SearchProtectedApplicationsRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/recursos/infrastructure/adapter/primary/web/mapper/SearchProtectedApplicationsRequestMapperTests.java)

## Evidencia y límite

Las pruebas de ambos mappers cubren dato válido, campo ausente, campo en blanco, formato incorrecto,
tipo incorrecto y valor fuera de rango, y verifican en cada caso el **nombre del campo** reportado. La
prueba HTTP confirma que el mismo error llega al cliente como `MISSING_REQUEST_FIELD` o
`MALFORMED_REQUEST_FIELD` con la propiedad `field`.

Límite: este esquema reporta el primer campo que falla, no la lista completa. Acumular todos los
errores es posible dentro de la misma estructura — cada campo ya se valida por separado en ambos
mappers — y se hará cuando un consumidor lo pida.
