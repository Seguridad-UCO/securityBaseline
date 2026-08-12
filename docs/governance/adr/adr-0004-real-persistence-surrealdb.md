# ADR-0004: Persistencia real con SurrealDB detrás de los puertos existentes

[← Gobierno](../README.md)

## Estado

**Implementada.** Ver [Nota de implementación](#nota-de-implementación) para las decisiones concretas
que no estaban fijadas cuando se aceptó esta ADR — la más importante es que no se usó un driver
SurrealDB, porque no existe uno viable en Java.

## Contexto

Los repositorios actuales (`InMemoryTenantRepository`, `InMemoryApplicationRepository`,
`InMemoryProtectedResourceRepository`) son dummies en memoria (ver
[07. Adaptadores dummy](../infrastructure/07-dummy-adapters.md)). El compromiso del proyecto es
SurrealDB como motor de persistencia detrás de los mismos puertos secundarios.

El adaptador de transacción actual (`SnapshotReactiveTransactionAdapter`) copia el mapa en memoria
antes del trabajo y lo restaura si falla. En una base real la transacción la da el motor.

## Decisión

Se reemplazarán los repositorios dummy por adaptadores reales sobre SurrealDB, implementando
**los mismos puertos** (`ProtectedResourceRepository`, `ApplicationRepository`, `TenantRepository`):
puerto en términos de dominio → adaptador → driver → *entity mapper*. La transacción real vía
`ReactiveTransactionPort` reemplazará a `SnapshotReactiveTransactionAdapter` y al puerto
`SnapshotCapable` (solo útil para el dummy).

> Como se documenta abajo, "driver" terminó siendo un cliente HTTP propio sobre `WebClient`, y
> `ReactiveTransactionPort`/`SnapshotCapable` se retiraron en vez de reemplazarse — ver
> [Nota de implementación](#nota-de-implementación).

## Justificación

1. **Cumple el compromiso documentado** del proyecto con SurrealDB.
2. **`domain/` y `application/` no cambian.** Criterio de aceptación: sustituir el adaptador
   secundario no debe tocar las capas internas.
3. **Secretos ya reservados.** El scaffolding de Azure Key Vault reserva
   `pdp-datasource-password` por ambiente (ver [`infra/README.md`](../../infra/README.md)).
4. **Puertos en dominio.** Los casos de uso siguen hablando de agregados, no de entidades de
   persistencia.

## Alternativas consideradas

- **PostgreSQL + R2DBC.** Más familiar en stacks Spring reactivos, pero contradice el compromiso
  con SurrealDB; solo se reconsideraría ante una limitación técnica real del motor.
- **JPA/Hibernate.** No es reactivo; incompatible con WebFlux/Reactor.
- **Mantener los dummies indefinidamente.** Descartado: el alcance incluye persistencia real.

## Consecuencias

- Cliente HTTP propio (`SurrealDbClient`, sobre `WebClient`) en vez de un driver de terceros, y
  esquema definido/asegurado en el arranque por cada módulo con datos.
- Paquete `infrastructure/adapter/secondary/persistence/{repository,schema}/` por módulo con datos,
  más `shared/persistence/surrealdb/` para el cliente compartido.
- Retiro de `ReactiveTransactionPort` y `SnapshotCapable` (no solo del dummy): la orquestación
  cross-módulo pasa a saga con compensación explícita por paso — ver
  [Nota de implementación](#nota-de-implementación).
- Las configuraciones de módulo cambian la implementación cableada, no la forma de los puertos.
- Pruebas con Testcontainers + SurrealDB (contenedor único compartido por la JVM de prueba, sin
  módulo oficial de Testcontainers para SurrealDB — `GenericContainer` + espera HTTP sobre
  `/health`).
- Consumo real de secretos de Key Vault por ambiente (`PDP_DATASOURCE_URL`,
  `PDP_DATASOURCE_USERNAME`, `PDP_DATASOURCE_PASSWORD`), sin valor de respaldo en `qa`/`prod`.

## Nota de implementación

Decisiones que no estaban fijadas en el texto original de esta ADR y se tomaron durante la etapa 4:

- **No existe un driver Java viable para SurrealDB.** El artefacto oficial `com.surrealdb:surrealdb`
  pesa ~212 MB comprimido (~600 MB sin comprimir) porque empaqueta binarios nativos embebidos para
  12 plataformas; `com.surrealdb:surrealdb-driver` está abandonado en la versión 0.1.0 desde 2023; el
  driver comunitario `dev.bitbite:surrealdb-java` es demasiado inmaduro para depender de él en
  producción. Se descartó cada una de las tres opciones y se optó por hablar directamente con el
  *endpoint* HTTP `/sql` de SurrealDB usando el `WebClient` reactivo que el proyecto ya usa,
  enviando SurrealQL crudo con parámetros ligados por *query string* (`?nombre=valor` → `$nombre` en
  la consulta).
- **`ReactiveTransactionPort` y `SnapshotCapable` se retiraron, no se reemplazaron.** Las
  transacciones `BEGIN/COMMIT` de SurrealDB solo cubren un lote de SurrealQL dentro de **una misma
  petición HTTP**; no pueden envolver el trabajo real del caso de uso de registro, que orquesta dos
  módulos (`aplicaciones` y `recursos`) y publica eventos de dominio entre medio. Envolver eso en un
  puerto de transacción genérico habría sido una abstracción que prometía más de lo que el motor
  puede dar. `RegisterProtectedApplicationUseCaseImpl` pasó a un patrón **saga con compensación
  explícita por paso**: si falla el registro del recurso protegido después de guardar la aplicación,
  se compensa borrando la aplicación (`removeApplicationInteractor.execute(...)`); si falla la
  publicación del evento después de guardar el recurso, se compensa borrando el recurso
  (`resources.deleteById(...)`).
- **SurrealQL real difiere de lo documentado en foros/ejemplos antiguos.** `type::thing(...)` no
  existe en SurrealDB 3.2.4; el error de parseo sugiere explícitamente `type::record('tabla', $id)`,
  que es lo que se usó. Los parámetros ligados por HTTP siempre llegan como *string*, así que
  `LIMIT`/`START` necesitan el cast `<int>$parametro` y las fechas `<datetime>$parametro`.
  `SELECT count() FROM ... GROUP ALL` siempre devuelve exactamente una fila (`{"count":N}`), incluso
  sin coincidencias, lo que simplifica el conteo de páginas.
- **La duplicidad de recurso protegido se detecta a nivel de aplicación, no de base de datos.** Hay
  un índice único (`protected_resource_grant` sobre `applicationId, resourceCode, action`) como
  respaldo ante condiciones de carrera, pero es un respaldo defensivo, no el mecanismo principal: las
  violaciones de índice único de SurrealDB llegan como `kind:"Internal"`, un error genérico no
  distinguible de otros fallos internos. El mecanismo principal sigue siendo
  `ApplicationNameMustBeUniqueForTenantRule` a nivel de reglas de aplicación, que sí produce una
  excepción de dominio específica y mapeable a un código HTTP concreto.
- **Los tests con contexto de Spring completo ahora dependen de una SurrealDB real** porque cada
  `Configuration` de módulo registra un `ApplicationRunner` que define el esquema en el arranque. Se
  resolvió con `AbstractSurrealDbIntegrationTest`: un contenedor Testcontainers único, compartido por
  toda la JVM de prueba, con las propiedades de conexión inyectadas vía `@DynamicPropertySource` — así
  `./mvnw verify` sigue siendo autocontenido sin necesitar un `docker compose up` manual.
- **Jackson 3, no Jackson 2** (mismo hallazgo que en [ADR-0003](adr-0003-real-security-reactive-jwt.md)):
  `SurrealDbClient` parsea la respuesta de SurrealDB con `tools.jackson.databind.ObjectMapper`, y
  `JsonNode.asString()`/`asString(valorPorDefecto)` reemplazan a `asText()`.
