# Eventos RabbitMQ de asignaciones

[← Interfaces](README.md) · [← Índice PDP](../README.md)

Este documento define cómo una aplicación recibe cambios de sus asignaciones de roles y perfiles.
Los eventos son **notificaciones de sincronización**, no la fuente de decisión de seguridad: el PDP
continúa siendo la fuente autoritativa para autorizar.

## Qué se publica

El PDP genera un evento cuando un usuario recibe o pierde directamente un rol, o cuando recibe o
pierde un perfil. Un perfil produce **un único evento consolidado** que enumera los roles
materializados o retirados; no genera además un evento por cada rol del perfil.

| `eventType` | Significado |
|---|---|
| `ROLE_ASSIGNED` | Se otorgó un rol de forma directa. |
| `ROLE_REVOKED` | Se revocó un rol otorgado de forma directa. |
| `PROFILE_ASSIGNED` | Se otorgó un perfil y sus roles efectivos. |
| `PROFILE_REVOKED` | Se revocó un perfil y sus roles efectivos. |

Los UUID son referencias autoritativas. Los nombres son una fotografía informativa tomada al crear
el evento; un cambio posterior de nombre no reescribe un evento pendiente.

## Contrato `AssignmentChangedEventV1`

El contenido se serializa como JSON en camelCase y siempre declara `schemaVersion: 1`.

```json
{
  "eventId": "c4f1c58a-a686-4d11-9d33-d4206ea9aed2",
  "schemaVersion": 1,
  "eventType": "ROLE_ASSIGNED",
  "occurredOn": "2026-11-06T16:30:00Z",
  "tenantId": "universidad-uco",
  "applicationId": "8a5e3c77-1b5c-41dc-89ca-b9a6c8191f89",
  "applicationName": "Gestión académica",
  "userId": "f446c3e4-ebbd-42a7-b341-dab1e1667954",
  "correlationId": null,
  "assignmentId": "d80cca50-8a17-482b-9de6-e1d349f92c86",
  "roleId": "1fce53e3-39e0-40a5-9eb1-d7c870c2eb8d",
  "roleName": "ACADEMIC_EDITOR",
  "profileAssignmentId": null,
  "profileId": null,
  "profileName": null,
  "roles": []
}
```

Para perfiles, los campos directos de rol son `null` y `roles` contiene los roles involucrados:

```json
{
  "eventId": "d6b4e5b0-1844-4b66-a7d4-12c1279e27b4",
  "schemaVersion": 1,
  "eventType": "PROFILE_REVOKED",
  "occurredOn": "2026-11-06T16:31:00Z",
  "tenantId": "universidad-uco",
  "applicationId": "8a5e3c77-1b5c-41dc-89ca-b9a6c8191f89",
  "applicationName": "Gestión académica",
  "userId": "f446c3e4-ebbd-42a7-b341-dab1e1667954",
  "correlationId": null,
  "assignmentId": null,
  "roleId": null,
  "roleName": null,
  "profileAssignmentId": "7fdc9482-35d8-4905-b4d6-5c130a14327e",
  "profileId": "a9690bd1-56b0-4c86-98f3-fc513684b4a9",
  "profileName": "Coordinación académica",
  "roles": [
    {
      "assignmentId": "caab6c86-7019-41e9-ac99-fb15b1818ad8",
      "roleId": "1fce53e3-39e0-40a5-9eb1-d7c870c2eb8d",
      "roleName": "ACADEMIC_EDITOR"
    }
  ]
}
```

Los consumidores deben ignorar campos desconocidos para permitir versiones posteriores. Un
`schemaVersion` no soportado debe dejar el mensaje sin confirmar y generar una alerta.

## Topología RabbitMQ

| Recurso | Valor |
|---|---|
| Exchange | `security.assignment.events` |
| Tipo | `direct`, durable |
| Routing key | `assignment.changed.<applicationId>` |
| Cola de una aplicación | `security.assignment.<applicationId>` |
| Durabilidad de la cola | durable |

Antes de habilitar el consumidor se debe crear su cola y enlazarla exclusivamente a la routing key
de su propio `applicationId`. RabbitMQ no conserva retroactivamente mensajes publicados antes de
que exista una cola enlazada.

Ejemplo local (sustituya `APP_ID` por el UUID real):

```bash
docker run -d --name pdp-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:4-management
docker exec pdp-rabbit rabbitmqadmin declare exchange name=security.assignment.events type=direct durable=true
docker exec pdp-rabbit rabbitmqadmin declare queue name=security.assignment.APP_ID durable=true
docker exec pdp-rabbit rabbitmqadmin declare binding source=security.assignment.events destination_type=queue destination=security.assignment.APP_ID routing_key=assignment.changed.APP_ID
```

La consola local queda en `http://localhost:15672` con usuario y contraseña `guest`.

## Publicación y sus dos ACK

El flujo no espera al equipo consumidor:

```text
PDP guarda evento pendiente en outbox
        │
        ▼
PDP ── publica ──► RabbitMQ ── ACK técnico ──► PDP marca outbox "published"
                          │
                          ▼
               cola durable de la aplicación
                          │
                          ▼
                  consumidor procesa y hace ACK
```

El primer ACK es un *publisher confirm*: RabbitMQ confirma que aceptó la publicación. El PDP espera
ese ACK antes de marcar el evento publicado. Si recibe NACK, ocurre timeout, se pierde la conexión
o el mensaje es devuelto por no tener una ruta/cola, conserva la outbox pendiente y reintenta con
backoff.

El segundo ACK es independiente: lo emite el consumidor a RabbitMQ cuando termina su trabajo. El
PDP no lo ve, no lo espera y no cambia la asignación por su resultado.

La configuración relevante es:

```properties
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.template.mandatory=true
pdp.assignment-events.exchange=security.assignment.events
pdp.assignment-events.confirm-timeout=PT5S
```

## Cómo implementar un consumidor seguro

La entrega es **al menos una vez**. Es normal recibir el mismo `eventId` más de una vez, por ejemplo
si la aplicación procesó el mensaje pero cayó antes de enviar su ACK.

Cada consumidor debe mantener una tabla local con `event_id` único. La operación de negocio y el
registro de `event_id` se realizan en la misma transacción local. Solo después se hace ACK al
mensaje.

Pseudocódigo:

```text
al recibir mensaje (manual ACK):
  validar schemaVersion y applicationId
  iniciar transacción local
    si processed_event ya contiene eventId:
      confirmar transacción
      ACK RabbitMQ
      terminar
    aplicar el cambio informativo local
    insertar eventId en processed_event
  confirmar transacción local
  ACK RabbitMQ

si falla antes del ACK:
  NACK/requeue o dejar expirar la conexión
```

No use `autoAck`: RabbitMQ eliminaría el mensaje antes de que la aplicación demuestre que pudo
persistir su resultado. Si el evento no corresponde a la aplicación de la cola, debe rechazarse y
alertarse: esa condición indica una topología incorrecta.

## Prueba de extremo a extremo

1. Inicie RabbitMQ y aprovisione exchange, cola y binding como se indicó arriba.
2. Inicie el PDP apuntando a RabbitMQ:

   ```bash
   SPRING_RABBITMQ_HOST=localhost SPRING_RABBITMQ_PORT=5672 ./mvnw -f pdp/pom.xml spring-boot:run
   ```

3. Genere una asignación o revocación mediante el endpoint administrativo habitual del PDP para la
   aplicación cuyo UUID usó en el binding.
4. Confirme que el dispatcher recibió el publisher confirm: la fila correspondiente de
   `assignment_outbox` tendrá `publishedOn` distinto de `NONE`.
5. Consulte sin consumir el mensaje para probar que RabbitMQ lo retuvo:

   ```bash
   docker exec pdp-rabbit rabbitmqadmin get queue=security.assignment.APP_ID requeue=true count=10
   ```

6. Arranque el consumidor con ACK manual. Tras procesar, la métrica de mensajes de esa cola debe
   bajar. Reenvíe intencionalmente el mismo JSON o fuerce una caída antes del ACK para comprobar
   que `eventId` hace el proceso idempotente.

Para probar un broker caído, detenga el contenedor antes de crear una asignación. La operación del
PDP debe responder normalmente y la outbox conservar el evento pendiente. Al iniciar RabbitMQ y
restaurar el binding, el dispatcher debe publicarlo en el siguiente ciclo.

## Límites y responsabilidades

- El PDP confirma aceptación por RabbitMQ, no éxito funcional en otra aplicación.
- RabbitMQ retiene mensajes solo después de que una cola durable esté enlazada.
- Cada equipo consumidor es responsable de su cola, disponibilidad, reintentos internos,
  idempotencia y alertas.
- La asignación de seguridad no se revierte porque una aplicación consumidora esté apagada.
- Estado técnico actual: la escritura de la asignación y de la outbox se ejecuta de forma
  consecutiva. La siguiente mejora pendiente es agrupar ambas sentencias en una transacción
  SurrealQL para eliminar la ventana excepcional en la que la asignación podría quedar guardada si
  fallara la escritura de la outbox.

