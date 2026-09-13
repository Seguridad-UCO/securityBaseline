# PLAN: HU-010 — Registrar una aplicación con su recurso inicial, con compensación explícita

## Metadata

- **ID:** HU-010
- **Slice:** `resources` (existente)
- **Tipo:** Escritura
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-010-saga-registro-aplicacion-recurso-inicial`
- **Fuentes:**
  - `pdp/docs/ai-harness/workspace/HU-010.md` (borrador original)
  - `pdp/docs/criteria-compliance-matrix.md` (criterio 10, único no cumplido; confirma que
    `ReactiveTransactionPort`/`SnapshotReactiveTransactionAdapter` existieron y se retiraron —
    `RemoveApplicationUseCase` quedó de esa época, sin nadie que lo invoque)
  - Decisiones tomadas interactivamente con Sebastián en esta sesión (ver §0)
  - Código real leído antes de planificar: `RemoveApplicationUseCase.java` (su propio Javadoc ya
    decía literalmente "se invoca cuando el registro de un recurso protegido en `resources` falla
    después de que la aplicación ya fue creada" — confirma la dirección de esta historia),
    `RegisterApplicationUseCase`/`RegisterApplicationRequest`/`ApplicationRegistrationResponse`
    (applications, reutilizables sin cambio), `RegisterProtectedResourceUseCase`/`RegisterProtectedResourceRequest`/
    `RegisteredProtectedResourceResponse` (resources, reutilizables sin cambio),
    `resources/package-info.java` (ya depende de `applications :: rule`, `:: dto`, `:: exception` —
    falta solo `:: usecase`, que `applications/application/usecase/package-info.java` **ya publica**
    y nadie consume todavía), `AuthorizeUseCaseImpl.recordAudit` (patrón a espejar para "el fallo de
    un paso secundario se loguea y no cambia el resultado ya decidido")
- **Criterios de la línea base que toca:** 1, 2, 9, 10, 11, 12, 13, 14, 21, 22

## 0. Hallazgos y decisiones tomadas antes de planificar

El borrador de `HU-010.md` decía "cablear la saga de compensación" como si el orquestador ya
existiera y solo faltara conectarle la compensación. **No es así**: se verificó contra el código
que `POST /api/v1/applications` y `POST /api/v1/applications/{id}/resources` son dos llamadas HTTP
completamente independientes, en slices distintos, sin ningún orquestador entre ellas —
`RemoveApplicationUseCase` es un resto de una versión anterior del proyecto (la matriz de
criterios lo confirma: hubo un `ReactiveTransactionPort` que se retiró en un refactor previo).
Cerrar el criterio 10 exige **construir por primera vez** el flujo combinado, no conectar uno
existente. Esto se resolvió con tres decisiones:

| # | Pregunta | Decisión |
|---|---|---|
| 1 | ¿Forma del endpoint nuevo? | Un tercer endpoint, aparte de los dos que ya existen: `POST /api/v1/applications/with-initial-resource`. `POST /api/v1/applications` y `POST /api/v1/applications/{id}/resources` quedan intactos — el frontend (`securityBaseline-fr`) y las pruebas E2E de HU-001/012/013/014 no se tocan |
| 2 | ¿El recurso inicial es obligatorio? | Sí — es lo único que le da sentido al criterio 2 ("si el registro del recurso falla, la aplicación se elimina"): si fuera opcional, la mitad de las veces no habría nada que compensar |
| 3 | ¿Cómo se registra una compensación que también falla? | Log estructurado (`LOG.error`), sin puerto ni tabla nuevos — mismo patrón que `AuthorizeUseCaseImpl.recordAudit`: "un fallo al auditar se registra por log, nunca cambia la decisión ya calculada". El cliente sigue viendo el error original del registro del recurso, no el de la compensación |

### Hallazgo — el nuevo endpoint vive en `resources`, no en `applications`

La dirección de dependencia Modulith ya establecida es `resources → applications` (ver
`resources/package-info.java`: depende de `applications :: rule/:: dto/:: exception`), nunca al
revés. Un orquestador que necesita llamar tanto a `RegisterApplicationUseCase`/
`RemoveApplicationUseCase` (applications) como a `RegisterProtectedResourceUseCase` (resources)
solo puede vivir del lado que ya tiene permiso de mirar al otro — es decir, en `resources`. La
URL (`/api/v1/applications/with-initial-resource`) no tiene que coincidir con el módulo que aloja
el controller; ya es así hoy entre `ApplicationController` (`applications`) y
`ProtectedResourceController` (`resources`), que comparten el prefijo `/api/v1/applications` desde
dos módulos distintos.

### Hallazgo — la frontera Modulith que falta ya existe del lado que la publica (parcialmente)

`applications/application/usecase/package-info.java` ya tiene `@NamedInterface("usecase")` — nadie
la consume todavía. No hace falta declarar esa frontera (la trampa de "primer `@NamedInterface`"
no aplica: `applications` ya tiene varias). Solo hace falta que `resources` agregue
`"applications :: usecase"` a su propio `allowedDependencies`.

**Pero falta una segunda.** `RegisterApplicationRequest` (ya en `applications :: dto`, importable)
tiene un campo `ApplicationBaseUrl` — un value object de `applications/domain/model/`, que **no**
tiene `package-info.java` y por tanto no está publicado. El mapper de `resources` necesita construir
un `ApplicationBaseUrl` para armar el `RegisterApplicationRequest` que reenvía a
`RegisterApplicationUseCase`, así que necesita importar el tipo, no solo el DTO que lo contiene.
Mismo patrón que ya existe: `resources/domain/model/package-info.java` ya publica
`@NamedInterface("model")` para sus propios VOs (`ResourcePath`, `HttpVerb`) — `applications` gana
el mismo tipo de frontera para los suyos.

### Hallazgo — sin caso de rechazo de negocio real y reproducible tras crear la aplicación

`ProtectedResourceMustBeUniqueRule` es la única regla de `RegisterProtectedResourceUseCase`, y
está por `(applicationId, path, method)` — como el `applicationId` de este flujo siempre es nuevo,
esa regla nunca puede rechazar el primer recurso de una aplicación recién creada. **No existe hoy
un rechazo de negocio genuino y reproducible para el segundo paso.** Consecuencia para la sección 9:
la compensación (y la doble falla) se prueban con un `RegisterProtectedResourceUseCase` falso que
falla a propósito (poison pill), no forzando una regla real — probarlo con infraestructura real
sería forzar la capa equivocada (`sb-testing`: "si probar algo exige un workaround de framework, la
lógica está en la capa equivocada"). La prueba E2E cubre el camino feliz completo contra SurrealDB
real; la compensación se prueba a nivel de caso de uso.

### Hallazgo — la respuesta web no puede anidar el DTO web de `applications`

Un primer diseño de `ApplicationWithInitialResourceWebResponse` anidaba
`ApplicationRegisteredWebResponse` (el DTO web ya existente de `applications`). Se descartó: esa
clase vive en `applications/infrastructure/.../dto/response/`, y la capa `infrastructure` de un
módulo **nunca** está publicada para que otro módulo la importe (solo `application` lo está, vía
`@NamedInterface`) — habría sido infraestructura de un módulo importando infraestructura de otro,
sin precedente en el proyecto. La respuesta queda plana con los once campos primitivos
directamente, construidos por el mapper de `resources` a partir de `ApplicationRegistrationResponse`
(capa `application`, sí publicada) — sin cambiar el nivel de "plana" que ya exige `sb-estandares`.

### Observación — no se toca (mensaje de éxito preexistente ajeno a esta historia)

`WebContractMessages.successApplicationRegistered()` ya dice literalmente "Aplicación protegida y
recurso inicial registrados" — pero hoy lo reutilizan genéricamente **cuatro** controllers sin
relación (`ApplicationController`, `ProtectedResourceController`, `TenantController`,
`UserController`), ninguno de los cuales registra realmente "una aplicación y su recurso inicial".
Es deriva preexistente, de mayor alcance que esta historia (tocaría los cuatro). Esta historia
**no lo reutiliza** — declara su propio mensaje nuevo — y no lo corrige; queda anotado por si se
quiere una historia de limpieza aparte.

## 1. Resumen funcional

Nuevo endpoint `POST /api/v1/applications/with-initial-resource`: registra una aplicación y, en la
misma operación, su primer recurso protegido. Si el registro de la aplicación falla, nada se creó
(camino ya existente, sin cambios). Si el registro del recurso falla después de que la aplicación
ya se guardó, la aplicación se elimina (compensación explícita, `RemoveApplicationUseCase`) y el
error del recurso llega al cliente. Si la propia compensación falla, se registra por log — el
cliente nunca ve un 500 silencioso ni un estado a medias sin explicación.

**No cubre:** cambiar `POST /api/v1/applications` ni `POST /api/v1/applications/{id}/resources`
(quedan exactamente como están); ninguna forma de transacción distribuida o puerto genérico
(`TransactionPort`) — la compensación es una llamada explícita, visible en el propio caso de uso;
migrar aplicaciones ya registradas sin recursos (no es un defecto que esta historia deba corregir,
son datos legítimos de antes de que este flujo existiera).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Camino feliz | `POST .../with-initial-resource` responde 201 con la aplicación (credencial en claro incluida, HU-012) y el recurso, ambos persistidos |
| 2 | Compensación | Si `RegisterProtectedResourceUseCase` falla, la aplicación recién creada se elimina (`RemoveApplicationUseCase`) y el error del recurso llega al cliente con su código HTTP habitual (400/409) |
| 3 | Compensación que también falla | Si además falla `RemoveApplicationUseCase`, se registra por log (`LOG.error`, con el `applicationId` huérfano) — el cliente sigue viendo el error original del recurso, nunca un 500 sin explicación ni silencio |
| 4 | Sin transacción mágica | Ningún `TransactionPort`/puerto genérico nuevo: la compensación es una llamada explícita dentro del propio caso de uso, leíble de arriba a abajo |
| 5 | Los dos endpoints existentes no cambian | `POST /api/v1/applications` y `POST /api/v1/applications/{id}/resources` siguen respondiendo exactamente igual que hoy (evidencia: la suite completa existente sigue en verde) |

## 3. Reglas de negocio

Ninguna regla pura nueva. Es orquestación de dos casos de uso ya validados por sus propias reglas
(`RegisterApplicationRulesValidator`, `RegisterProtectedResourceRulesValidator`) — este caso de uso
nuevo no decide nada de negocio, solo encadena y compensa.

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| — | Si el segundo paso falla, el primero se deshace | `RegisterApplicationWithInitialResourceUseCaseImpl` (orquestación, no decisión) | `RemoveApplicationUseCase` (ya existente) | El error propagado sigue siendo el del segundo paso — su propio mapeo HTTP no cambia |

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguna nueva. `Application` y `ProtectedResource` se reutilizan tal cual.

### Value objects

Ninguno nuevo.

## 5. Persistencia

No toca el esquema. No hay tabla ni consulta nueva — el caso de uso nuevo delega enteramente en
los repositorios que `RegisterApplicationUseCase`, `RegisterProtectedResourceUseCase` y
`RemoveApplicationUseCase` ya usan.

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida |
|---|---|---|---|---|
| POST | `/api/v1/applications/with-initial-resource` | 201 | nombre/descripción/URL base de la aplicación + ruta/verbo del recurso inicial | Aplicación registrada (con credencial en claro, HU-012) + recurso registrado |

- **Autorización:** BFF — requiere token, el inquilino sale del principal.
- **Errores esperados:** cualquier excepción de `RegisterApplicationRulesValidator` (antes del
  primer paso, nada se creó) o de `RegisterProtectedResourceRulesValidator`/`ApplicationMustExistForTenantValidator`
  (después del primer paso, se compensa) — mismo mapeo HTTP que ya tienen hoy en sus endpoints
  originales, sin excepciones nuevas.

## 7. SPEC — el contrato

### `resources/application` — nuevos `[N]`

```java
// pdp/resources/application/primaryport/request/RegisterApplicationWithInitialResourceRequest.java
public record RegisterApplicationWithInitialResourceRequest(
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        co.edu.uco.seguridad.pdp.commons.model.ApplicationName name,
        String description,
        co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl baseUrl,
        co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath resourcePath,
        co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb resourceMethod) { }

// pdp/resources/application/primaryport/response/ApplicationWithInitialResourceRegistrationResponse.java
public record ApplicationWithInitialResourceRegistrationResponse(
        co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse application,
        co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse resource) { }

// pdp/resources/application/usecase/RegisterApplicationWithInitialResourceUseCase.java
public interface RegisterApplicationWithInitialResourceUseCase
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<RegisterApplicationWithInitialResourceRequest,
                ApplicationWithInitialResourceRegistrationResponse> { }
```

### `resources/infrastructure` — nuevos `[N]`

```java
// pdp/resources/infrastructure/adapter/primary/web/dto/request/raw/RegisterApplicationWithInitialResourceRawRequest.java
public record RegisterApplicationWithInitialResourceRawRequest(
        String name, String description, String baseUrl, String resourcePath, String resourceMethod) { }

// pdp/resources/infrastructure/adapter/primary/web/dto/response/ApplicationWithInitialResourceWebResponse.java
// Plano, sin anidar el DTO web de `applications`: esa clase vive en la capa de infraestructura de
// `applications`, y esa capa no está publicada fuera de su propio módulo (solo `application` lo
// está) — anidarla habría sido infraestructura de un módulo importando infraestructura de otro.
public record ApplicationWithInitialResourceWebResponse(String applicationId, String tenantId,
        String applicationName, String description, String baseUrl, String credential,
        java.time.Instant applicationRegisteredAt, String resourceId, String resourcePath, String resourceMethod,
        java.time.Instant resourceRegisteredAt) { }

// pdp/resources/infrastructure/adapter/primary/web/interactor/RegisterApplicationWithInitialResourceInteractor.java
public interface RegisterApplicationWithInitialResourceInteractor
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<RegisterApplicationWithInitialResourceRawRequest,
                ApplicationWithInitialResourceWebResponse> { }
```

Implementación (`RegisterApplicationWithInitialResourceUseCaseImpl`,
`RegisterApplicationWithInitialResourceInteractorImpl`,
`RegisterApplicationWithInitialResourceRequestMapper.toRequest(raw, tenantId)`,
`ApplicationWithInitialResourceResponseMapper.toResponse(...)`, `ApplicationWithInitialResourceController`):
mismo patrón que `RegisterProtectedResourceInteractorImpl`/`RegisterProtectedResourceRequestMapper`/
`ProtectedResourceController` — un interactor que resuelve el principal, mapea, ejecuta y proyecta;
un mapper que usa `RequestFieldParser` para cada campo; un controller `package-private` con
`@RequestMapping("/api/v1/applications")` propio (en `resources`, junto a `ProtectedResourceController`,
no dentro de él — las rutas no colisionan: uno es `POST /api/v1/applications/{id}/resources`, el otro
`POST /api/v1/applications/with-initial-resource`).

### Firmas modificadas `[M]` — el tester las aplica, el planificador ya cableó donde era autocontenido

```java
// applications/domain/model/package-info.java — [N], nuevo, declara la frontera
@org.springframework.modulith.NamedInterface("model")
package co.edu.uco.seguridad.pdp.applications.domain.model;

// resources/package-info.java — +2 entradas en allowedDependencies, aditivo
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "tenants", "tenants :: dto", "tenants :: rule",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model"})
package co.edu.uco.seguridad.pdp.resources;
```

Ninguna rompe consumidores existentes (son ampliaciones de permiso, no cambios de firma) — el
planificador las aplica directamente en la FASE 5, no quedan para el tester.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/
├── shared/message/RequiredArgumentMessages.java                                       [M] -- +5 constantes
├── shared/web/message/WebContractMessages.java                                        [M] -- +1 método, aditivo
├── pdp/applications/domain/model/package-info.java                                    [N] -- @NamedInterface("model")
└── pdp/resources/
    ├── package-info.java                                          [M] -- +"applications :: usecase", +"applications :: model"
    ├── application/
    │   ├── primaryport/request/RegisterApplicationWithInitialResourceRequest.java     [N]
    │   ├── primaryport/response/ApplicationWithInitialResourceRegistrationResponse.java [N]
    │   ├── usecase/RegisterApplicationWithInitialResourceUseCase.java                  [N]
    │   └── usecase/impl/RegisterApplicationWithInitialResourceUseCaseImpl.java         [N]
    └── infrastructure/
        ├── adapter/primary/web/controller/ApplicationWithInitialResourceController.java [N]
        ├── adapter/primary/web/dto/request/raw/RegisterApplicationWithInitialResourceRawRequest.java [N]
        ├── adapter/primary/web/dto/response/ApplicationWithInitialResourceWebResponse.java [N]
        ├── adapter/primary/web/interactor/RegisterApplicationWithInitialResourceInteractor.java [N]
        ├── adapter/primary/web/interactor/impl/RegisterApplicationWithInitialResourceInteractorImpl.java [N]
        ├── adapter/primary/web/mapper/RegisterApplicationWithInitialResourceRequestMapper.java [N]
        ├── adapter/primary/web/mapper/ApplicationWithInitialResourceResponseMapper.java [N]
        └── config/ResourcesConfiguration.java                                          [M] -- +2 beans (el planificador
                                                                                              los cablea: clases enteramente
                                                                                              nuevas, dependen solo de beans
                                                                                              ya existentes — mismo criterio
                                                                                              que HU-013/HU-014/HU-009)
```

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `resources` application | `RegisterApplicationWithInitialResourceUseCaseImplTests` (nueva) | Camino feliz: ambos pasos completan, la respuesta combina `ApplicationRegistrationResponse` (con credencial) y `RegisteredProtectedResourceResponse`; el registro de aplicación falla (regla de `applications`) → ni el recurso ni la compensación se intentan (poison pill), el error de aplicación llega intacto; el registro del recurso falla (poison pill) → `RemoveApplicationUseCase` se invoca con el `ApplicationId` correcto (capturado) y el error **del recurso** (no uno nuevo) llega al cliente; el registro del recurso **y** la compensación fallan (ambos poison pill) → el cliente sigue recibiendo el error del recurso, no el de la compensación, y no se propaga silenciosamente (verificado por el propio `StepVerifier`, no por el log) |
| `resources` infrastructure | `RegisterApplicationWithInitialResourceRequestMapperTests` (nueva) | Cada uno de los 5 campos crudos: ausente → `MissingRequestFieldException`; mal formado → `MalformedRequestFieldException`; válido → value objects correctos y `tenantId` del principal |
| `resources` infrastructure | `ApplicationWithInitialResourceControllerTests` (nueva) | El controller delega al interactor y responde 201 con el cuerpo combinado |
| `resources` infrastructure (E2E) | `ApplicationWithInitialResourceHttpTests` (nueva) | Camino feliz completo contra SurrealDB real: `POST .../with-initial-resource` → 201, `$.data.credential` existe, `$.data.resourcePath` coincide con lo enviado; una consulta posterior (`GET /api/v1/applications/{id}/resources`) confirma que el recurso quedó persistido de verdad. La compensación no se prueba aquí — no hay un rechazo de negocio real y reproducible tras crear la aplicación (ver Hallazgos) |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-13 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ✅ Aprobada (2ª corrida, tras corregir el bloqueante de documentación) | 2026-09-13 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes

Ninguna — las tres decisiones que el borrador necesitaba ya se tomaron (ver §0). Queda registrada
como observación (no bloqueante) la deriva preexistente de `successApplicationRegistered()`
reutilizado por cuatro controllers sin relación entre sí.
