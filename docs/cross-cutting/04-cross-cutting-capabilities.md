# 04. Capacidades transversales

[← Transversales](README.md) · [Siguiente: observabilidad →](08-logging-instrumentation.md)

## Decisión arquitectónica

Mensajes, errores, correlación, logging, métricas, auditoría, transaccionalidad, reloj, generación
de identificadores y configuración son componentes compartidos, no reglas de ningún módulo.

## Justificación

Duplicarlos por historia hace que cada módulo responda y registre distinto. Se descarta incrustar
infraestructura transversal en el dominio o en el controlador.

## Implementación

| Paquete | Contiene | Por qué ahí |
|---|---|---|
| `shared/web` | envelope, paginación HTTP, correlación, excepciones de contrato | Frontera HTTP, común a todo adaptador web |
| `shared/observability` | puente Reactor Context → MDC | Aplica a cualquier flujo reactivo |
| `shared/rule` | los tres contratos de regla | Vocabulario común de reglas, Java puro |
| `shared/port` | `TimeProvider`, `IdentifierGenerator`, `ReactiveTransactionPort` | Capacidades que todo caso de uso puede necesitar |
| `shared/event` | `DomainEvent`, `DomainEventPublisher` (+ `SpringDomainEventPublisher`) | Publicar un hecho de negocio sin que quien lo produce conozca a quien escucha (ADR-0002) |
| `shared/config` | implementaciones por defecto de esos puertos | Único lugar donde se decide el reloj real |
| `pdp/commons` | value objects y excepciones base del PDP | Lenguaje del dominio compartido entre módulos |

La separación entre `shared` y `pdp/commons` no es cosmética: `pdp/commons` es vocabulario del
negocio (`TenantId`, `ApplicationName`), `shared` es capacidad técnica. Mantener `pdp/commons` libre
de Reactor es lo que permite afirmar que el dominio es Java puro.

`TimeProvider` e `IdentifierGenerator` existen porque `Instant.now()` y `UUID.randomUUID()` dentro
de un caso de uso lo vuelven imposible de probar de forma determinista. Con los puertos, la prueba
fija el instante y el identificador.

La auditoría es un puerto para que la aplicación pueda solicitarla sin saber dónde se almacena.

## Ubicación verificable

- [`shared`](../../src/main/java/co/edu/uco/seguridad/shared)
- [`pdp/commons`](../../src/main/java/co/edu/uco/seguridad/pdp/commons)
- [`SharedPortsConfiguration.java`](../../src/main/java/co/edu/uco/seguridad/shared/config/SharedPortsConfiguration.java)
- [`application.properties`](../../src/main/resources/application.properties) y perfiles `dev`, `qa`, `prod`

## Evidencia y límite

`RegisterProtectedApplicationUseCaseImplTests` fija el instante con un `TimeProvider` de prueba y
verifica que el `registeredAt` resultante es exactamente ese; sin el puerto esa aserción sería
imposible. La política de retención y el backend OTLP se definirán al habilitar auditoría
productiva.
