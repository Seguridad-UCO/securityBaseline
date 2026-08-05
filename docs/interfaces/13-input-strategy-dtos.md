# 13. Estrategia para recibir información

## Decisión arquitectónica

La frontera HTTP usa DTOs `record`; el interior usa command y value objects.

## Justificación

El DTO evita que la forma pública sea el modelo del dominio y permite versionar la API. Se descarta recibir entidades del dominio directamente desde JSON.

## Implementación

`RegisterProtectedApplicationRequest` recibe tres campos. `ProtectedApplicationMapper` crea command y VOs. La respuesta es un DTO independiente, por lo que el agregado no se serializa ni expone detalles internos.

## Ubicación verificable

- [`RegisterProtectedApplicationRequest.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`ProtectedApplicationMapper.java`](../../src/main/java/co/edu/uco/seguridad/pdp)
- [`RegisterProtectedApplicationCommand.java`](../../src/main/java/co/edu/uco/seguridad/pdp).

## Evidencia y límite

La prueba HTTP usa JSON y comprueba un response DTO. Campos futuros no alteran el constructor del agregado sin decisión de dominio.
