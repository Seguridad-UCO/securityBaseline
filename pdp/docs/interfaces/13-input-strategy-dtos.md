# 13. Estrategia para recibir información

[← Parámetros](06-parameter-handling.md) · [Siguiente: DTOs seguros →](14-secure-dtos.md)


## Decisión arquitectónica

Toda entrada empieza como un **Raw Request** (`record`, todos los campos `String`, sin anotaciones de
validación) y termina como un **App Request** tipado
(`application.port.primary.dto.request`) antes de tocar el caso de uso. Cuántos pasos hay entre esos
dos puntos depende de si hace falta **ensamblar** algo a partir de los campos sueltos:

```text
Authorization: Bearer <jwt>  →  SecurityContext.currentPrincipal()  →  TenantId
                                                                            │
JSON / query string                                                       │
 ↓                                                                        │
Raw Request DTO      record, todos los campos String, sin anotaciones     │
 ↓                    de validación, y sin tenantId (ADR-0003)            │
 ├─ Registro: el mapper convierte cada campo directo a su value object ◄──┤
 │   ↓                                                                    │
 │  App Request       tipado, inmutable (application.port.primary.dto.request)
 │                                                                        │
 └─ Búsqueda: el mapper pasa por un Web Request mutable, porque          │
     construir el criterio de búsqueda a partir de campos sueltos SÍ ◄────┘
     es una transformación real
     ↓
    Web Request DTO   mutable, campos tipados como value objects (infraestructura)
     ↓
    App Request       tipado, inmutable — el criterio ya está ensamblado como Specification
 ↓
Use Case → Domain
```

El tenant no es un campo del payload: desde ADR-0003 es el interactor quien lo lee del principal
autenticado (`SecurityContext.currentPrincipal()`) y se lo entrega al mapper como parámetro aparte,
para el registro y para la búsqueda por igual. Un `Raw Request` con `tenantId` habría dejado a un
cliente pedir operar sobre un tenant que no es el suyo con solo cambiar un campo del cuerpo.

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
evolución de arquitectura ambos tenían un Web Request intermedio). El registro tiene tres campos que
se convierten uno a uno en su value object — `applicationName`, `resourceCode`, `action` — más el
tenant, que entra por su cuenta desde el principal autenticado. Un Web Request ahí solo repetía esos
campos con el mismo tipo, para que un mapper después los volviera a copiar sin transformar nada. Se
eliminó: el mapper de registro ahora construye el App Request directamente, campo por campo, y
conserva la misma precisión de error por campo porque sigue usando `RequestFieldParser.parse` para
cada uno. La búsqueda sí necesita el paso intermedio, porque `nameContains`/`resourceContains` y la
ventana de paginación se **consolidan**, junto con el tenant del principal, en un objeto
`ApplicationCriteria` (una Specification) — eso es una transformación real, no una copia.

## Implementación

### Registro — un solo mapper, sin DTO intermedio; el tenant llega aparte

```java
public static RegisterProtectedApplicationRequest toRequest(
        RegisterApplicationRawRequest raw, TenantId tenantId) {
    return new RegisterProtectedApplicationRequest(
            tenantId,
            RequestFieldParser.parse("applicationName", raw.applicationName(), ApplicationName::new),
            RequestFieldParser.parse("resourceCode", raw.resourceCode(), ResourceCode::new),
            RequestFieldParser.parse("action", raw.action(), ActionCode::new));
}
```

El interactor es quien llama a este mapper, y es quien resuelve `tenantId` antes de llamarlo:

```java
SecurityContext.currentPrincipal()
        .map(principal -> RegisterApplicationRequestMapper.toRequest(raw, principal.tenantId()))
        .flatMap(useCase::execute)
        // ...
```

`RequestFieldParser.parse` delega el formato al value object y solo añade **qué campo** lo traía —
exactamente la misma garantía que daban los setters del Web Request que se eliminó, pero sin una clase
mutable que solo iba a vivir para volver a copiarse en un record. `tenantId` no pasa por
`RequestFieldParser` porque no es texto sin analizar: para cuando el interactor lo lee, el
`ReactiveJwtDecoder` ya verificó la firma del token y `PdpPrincipal.from` ya construyó el
`TenantId` a partir del claim (ver [ADR-018](https://github.com/Seguridad-UCO/security-platform-architecture/blob/main/docs/01-governance/adr/ADR-018-jwt-reactive-security-implementation.md)).

### Búsqueda — el Web Request sigue siendo mutable a propósito

```java
public void setResourceCode(String value) {
    this.resourceCode = RequestFieldParser.parse("resourceCode", value, ResourceCode::new);
}
```

No es un `record` a propósito. El constructor canónico de un record validaría todo de una vez, solo
podría reportar el primer problema y no dejaría un lugar con nombre donde colgar la regla de cada
campo — y aquí, a diferencia del registro, después hay una transformación (armar el criterio) que
necesita los valores ya construidos (los filtros opcionales y la ventana) antes de decidir cómo
combinarlos con el tenant que trae el interactor.

Pasado el mapper de búsqueda, el adaptador proyecta al **App Request** ensamblando
`ApplicationCriteria` a partir de los campos ya tipados. El App Request y el Web Request
pueden compartir nombre simple; viven en paquetes distintos y los mappers usan nombres completamente
cualificados cuando hace falta.

## Ubicación verificable

- Crudo: [`RegisterApplicationWithFirstAdministratorRawRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp/assignments/infrastructure/adapter/primary/web/dto/request/raw/RegisterApplicationWithFirstAdministratorRawRequest.java)
  (HU-015: el registro se trasladó de `applications` a `assignments`, que orquesta también el alta
  del primer administrador — ver `pdp/docs/ai-harness/workspace/planes/PLAN-HU-015.md` §0),
  `SearchProtectedApplicationsRawRequest.java`
- Web Request (solo búsqueda): `SearchProtectedApplicationsRequest.java`
- [`RequestFieldParser.java`](../../src/main/java/co/edu/uco/seguridad/shared/web/RequestFieldParser.java)
  — utilidad compartida del adaptador web; el interactor invoca los mappers que la usan
- Tenant: [`PdpPrincipal.java`](../../src/main/java/co/edu/uco/seguridad/shared/security/PdpPrincipal.java),
  [`SecurityContext.java`](../../src/main/java/co/edu/uco/seguridad/shared/security/SecurityContext.java)
- App Request: [`application/primaryport/request`](../../src/main/java/co/edu/uco/seguridad/pdp/resources/application/primaryport/request)
- Mappers: [`RegisterApplicationWithFirstAdministratorRequestMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp/assignments/infrastructure/adapter/primary/web/mapper/RegisterApplicationWithFirstAdministratorRequestMapper.java),
  `ListApplicationsRequestMapper.java`
- Interactores: `RegisterApplicationWithFirstAdministratorInteractorImpl.java`,
  `SearchProtectedApplicationsInteractorImpl.java`
- Pruebas: [`RegisterApplicationWithFirstAdministratorRequestMapperTests`](../../src/test/java/co/edu/uco/seguridad/pdp/assignments/infrastructure/adapter/primary/web/mapper/RegisterApplicationWithFirstAdministratorRequestMapperTests.java),
  `ListApplicationsRequestMapperTests`

## Evidencia y límite

Las pruebas de ambos mappers cubren dato válido, campo ausente, campo en blanco, formato incorrecto,
tipo incorrecto y valor fuera de rango, y verifican en cada caso el **nombre del campo** reportado. La
prueba HTTP confirma que el mismo error llega al cliente como `MISSING_REQUEST_FIELD` o
`MALFORMED_REQUEST_FIELD` con la propiedad `field`.

Límite: este esquema reporta el primer campo que falla, no la lista completa. Acumular todos los
errores es posible dentro de la misma estructura — cada campo ya se valida por separado en ambos
mappers — y se hará cuando un consumidor lo pida.
