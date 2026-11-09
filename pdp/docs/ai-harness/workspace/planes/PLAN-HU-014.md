# PLAN: HU-014 — Rotación de la credencial de una aplicación

## Metadata

- **ID:** HU-014
- **Slice:** `applications` (existente)
- **Tipo:** Escritura
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-014-rotacion-credencial-aplicacion`
- **Fuentes:**
    - `pdp/docs/ai-harness/workspace/HU-014.md` (historia dictada por Sebastián, dependiente de
      HU-012/HU-013 — ambas ya en `develop`)
    - Código real leído antes de planificar: `Application.java`, `ApplicationRepository.java`,
      `SurrealApplicationRepository.java` (confirma que `save(Application)` usa `CREATE`, no
      `UPSERT` — no se puede reutilizar para actualizar una fila existente, ver Hallazgo 2),
      `ApplicationController.java`, `ApplicationMustExistForTenantRule`/`Impl`/`ApplicationExistence`
      (regla ya existente, reutilizable tal cual), `ApplicationRegistrationResponse`/
      `ApplicationRegisteredWebResponse`/`ApplicationResponseMapper.toRegisteredResponse` (de HU-012,
      reutilizables sin cambio), `RevokeAssignmentInteractor`/`Impl`/`RawRequest` (patrón de "solo un
      id del path, sin cuerpo" a espejar)
- **Criterios de la línea base que toca:** 1, 2, 4, 9, 11, 12, 13, 15, 21, 22

## 0. Hallazgos antes de planificar

### Hallazgo 1 — ninguna decisión de diseño quedó pendiente

La propia `HU-014.md` ya resolvió las cuatro preguntas de su tabla de decisiones:

1. **¿Quién puede rotar?** Mientras HU-009 no exista, el mismo criterio que ya usa el resto de
   `applications` — cualquier usuario autenticado del tenant. No hay `Rule` de autorización nueva.
2. **¿Invalidación inmediata?** Sí — sobrescribir el hash es, por sí solo, invalidación inmediata:
   no hay "credencial vieja" que conservar en ningún lado.
3. **¿Se audita?** HU-013 ya decidió que la validación de credenciales queda fuera del alcance de
   `AccessAuditRepository` (es para decisiones de autorización de usuario, no autenticación de
   aplicación). Por la misma razón, la rotación tampoco se audita aquí.
4. **¿Cuántas rotaciones se conservan?** Ninguna — solo importa el hash vigente.

### Hallazgo 2 — `save(Application)` no sirve para rotar: hace falta una actualización angosta

`SurrealApplicationRepository.save` ejecuta `CREATE type::record(...)`, que en SurrealDB **falla**
si el `id` ya existe — está pensado solo para el registro inicial (HU-012), nunca se ha reusado para
actualizar una fila. Cambiar `save` a `UPSERT` afectaría el camino ya probado de HU-012 sin
necesidad. La solución, siguiendo el mismo criterio que ya trajo `findCredentialHashById` en
HU-013 ("el puerto responde lo mínimo que la regla necesita"): un método nuevo y angosto,
`updateCredentialHash(ApplicationId, ApplicationCredentialHash)`, que solo toca esa columna.

### Hallazgo 3 — falta un `findByIdForTenant` que devuelva el agregado completo

Ningún método del puerto hoy devuelve una `Application` completa por id — `existsByTenantAndId`
responde `boolean`, `findTenantIdById` solo el tenant, `findCredentialHashById` solo el hash. Rotar
necesita el agregado completo (nombre, descripción, `baseUrl`, `registeredAt`) para poder construir
la respuesta con la misma forma que el registro (HU-012), así que se añade
`findByIdForTenant(TenantId, ApplicationId): Mono<Application>` — mismo nombre y forma que ya usan
`ProfileRepository`/`RoleRepository` para el mismo propósito en sus propios slices.

### Hallazgo 4 — ninguna `Rule` nueva; se reutiliza `ApplicationMustExistForTenantRule` tal cual

La pregunta "¿existe esta aplicación para este inquilino?" ya la resuelve
`ApplicationMustExistForTenantRule` + `ApplicationExistence` (usadas hoy por
`ApplicationMustExistForTenantValidator`, publicado a `resources`). El caso de uso de rotación
invoca la regla directamente sobre el resultado de `findByIdForTenant` — mismo patrón que
`AddRoleToProfileRulesValidatorImpl` en HU-011 — sin crear una validación nueva.

### Hallazgo 5 — cero DTOs de respuesta nuevos: se reutilizan los de HU-012

`ApplicationRegistrationResponse` (núcleo) y `ApplicationRegisteredWebResponse` +
`ApplicationResponseMapper.toRegisteredResponse` (web) ya expresan exactamente "la proyección de la
aplicación más un secreto en texto plano que se muestra una sola vez" — la misma forma que necesita
la respuesta de rotar. No se declara ningún tipo nuevo para la salida.

## 1. Resumen funcional

Un nuevo endpoint (`POST /api/v1/applications/{applicationId}/credential-rotations`) genera un
secreto nuevo para una aplicación ya registrada, lo hashea y sobrescribe el hash guardado —
invalidando el anterior de inmediato, sin período de gracia. La respuesta trae el secreto nuevo en
texto plano una sola vez, igual que el registro (HU-012). La identidad de la aplicación
(`ApplicationId`, nombre, `baseUrl`, `tenantId`, `registeredAt`) no cambia.

**No cubre:** período de gracia entre credencial vieja y nueva (se descarta explícitamente — HU-014.md
ya lo resuelve), notificar a la aplicación integrada de la rotación, ni auditar la operación (ver
Hallazgo 1).

## 2. Criterios de aceptación

| # | Criterio                                            | Resultado esperado                                                                                                           |
|---|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| 1 | Rotar genera una nueva credencial                   | `POST .../credential-rotations` responde 201 con un secreto nuevo en texto plano                                             |
| 2 | La identidad no cambia                              | `ApplicationId`, nombre, `baseUrl`, `tenantId`, `registeredAt` de la respuesta son los mismos que antes de rotar             |
| 3 | La credencial anterior deja de validar de inmediato | Tras rotar, `findCredentialHashById` devuelve el hash **nuevo** — el viejo ya no está en ningún lado                         |
| 4 | Aplicación inexistente se rechaza                   | Rotar una aplicación que no existe (o no es del tenant) responde `ApplicationNotFoundException` (ya existente) → 400, no 500 |

## 3. Reglas de negocio

Ninguna regla nueva — ver Hallazgo 4. Se reutiliza `ApplicationMustExistForTenantRule` tal cual,
invocada directamente por el caso de uso (sin validador nuevo, mismo criterio que
"con una sola regla y ningún otro consumidor, el propio use case hace de validador").

## 4. Modelo de dominio afectado

### Entidad / agregado

- **`Application`** (existente): gana la transición `withCredentialHash(ApplicationCredentialHash)`
  — mismo patrón que `Profile.withRole(...)`: no cambia identidad ni el resto de los campos, solo
  reemplaza el hash.

### Value objects

Ninguno nuevo — `ApplicationCredentialHash` (HU-012) y el secreto en claro (`String`, mismo criterio
que `ApplicationRegistrationResponse.credential`) ya existen.

## 5. Persistencia

No se toca el esquema (`application` sigue `SCHEMALESS`). Dos consultas nuevas en el puerto
existente:

- `Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId)` — vacío si
  no existe o no pertenece al tenant.
- `Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash)`
  — `UPDATE` angosto, solo esa columna.

## 6. Endpoint

| Verbo | Ruta                                                        | Código de éxito | Cuerpo de entrada | Cuerpo de salida                                                                                |
|-------|-------------------------------------------------------------|-----------------|-------------------|-------------------------------------------------------------------------------------------------|
| POST  | `/api/v1/applications/{applicationId}/credential-rotations` | 201             | (sin cuerpo)      | `ApplicationRegisteredWebResponse` — misma forma que el registro (HU-012), con el secreto nuevo |

- **Autorización:** BFF — requiere token, el inquilino sale del principal (Hallazgo 1, punto 1). No
  es el canal interno de HU-013: esto lo dispara un administrador autenticado, no el PEP.
- **Errores esperados:** `ApplicationNotFoundException` (ya existente) → 400.

## 7. SPEC — el contrato

### `applications/application` — nuevos `[N]`

```java
// pdp/applications/application/primaryport/request/RotateApplicationCredentialRequest.java
public record RotateApplicationCredentialRequest(
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId) { }

// pdp/applications/application/usecase/RotateApplicationCredentialUseCase.java
public interface RotateApplicationCredentialUseCase
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<RotateApplicationCredentialRequest,
                co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse> { }
```

### Firmas modificadas `[M]` — el tester las aplica, el planificador no las toca

```java
// applications/domain/Application.java — nuevo método, aditivo
public Application withCredentialHash(ApplicationCredentialHash credentialHash) { ... }

// applications/application/secondaryport/repository/ApplicationRepository.java — 2 métodos nuevos, aditivos
Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId);
Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash);

// applications/infrastructure/adapter/primary/web/controller/ApplicationController.java
ApplicationController(RegisterApplicationInteractor registerInteractor, ListApplicationsInteractor listInteractor,
        RotateApplicationCredentialInteractor rotateInteractor)
// nuevo método:
@PostMapping("/{applicationId}/credential-rotations")
Mono<ResponseEntity<ApiResponse<ApplicationRegisteredWebResponse>>> rotate(@PathVariable String applicationId,
        ServerWebExchange exchange)
```

### `applications/infrastructure` — nuevos `[N]`

```java
// pdp/applications/infrastructure/adapter/primary/web/dto/request/raw/RotateApplicationCredentialRawRequest.java
public record RotateApplicationCredentialRawRequest(String applicationId) { }

// pdp/applications/infrastructure/adapter/primary/web/interactor/RotateApplicationCredentialInteractor.java
public interface RotateApplicationCredentialInteractor
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<RotateApplicationCredentialRawRequest,
                co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse> { }
```

Implementación (`RotateApplicationCredentialUseCaseImpl`, `RotateApplicationCredentialInteractorImpl`,
`RotateApplicationCredentialRequestMapper.toRequest(raw, tenantId)`): mismas firmas que sus espejos
en `RevokeAssignmentInteractor`/`Impl`/`RequestMapper` (un id del path + tenant del principal, sin
cuerpo) y `RegisterApplicationUseCaseImpl` (genera secreto, hashea, arma la respuesta con el
secreto en claro) — no se repiten letra por letra.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/
├── shared/message/RequiredArgumentMessages.java                              [M] -- +2 constantes (ver §11)
└── pdp/applications/
    ├── domain/Application.java                                              [M] -- +withCredentialHash(...); NO lo toca el planificador
    ├── application/
    │   ├── primaryport/request/RotateApplicationCredentialRequest.java      [N]
    │   ├── secondaryport/repository/ApplicationRepository.java              [M] -- +2 métodos; NO lo toca el planificador
    │   ├── usecase/RotateApplicationCredentialUseCase.java                  [N]
    │   └── usecase/impl/RotateApplicationCredentialUseCaseImpl.java         [N]
    └── infrastructure/
        ├── adapter/primary/web/controller/ApplicationController.java        [M] -- +endpoint rotate; NO lo toca el planificador
        ├── adapter/primary/web/dto/request/raw/RotateApplicationCredentialRawRequest.java [N]
        ├── adapter/primary/web/interactor/RotateApplicationCredentialInteractor.java [N]
        ├── adapter/primary/web/interactor/impl/RotateApplicationCredentialInteractorImpl.java [N]
        ├── adapter/primary/web/mapper/RotateApplicationCredentialRequestMapper.java [N]
        ├── adapter/secondary/persistence/repository/SurrealApplicationRepository.java [M] -- +2 métodos; NO lo toca el planificador
        ├── config/ApplicationsConfiguration.java                            [M] -- +2 beans (el planificador SÍ los cablea:
        │                                                                          clases enteramente nuevas, dependen solo de
        │                                                                          beans ya existentes — mismo criterio que HU-013)
        └── shared/web/message/WebContractMessages.java                      [M] -- +1 método, aditivo (el planificador lo agrega)
```

## 9. Casos de prueba esperados

| Capa                                | Clase de prueba                                         | Casos                                                                                                                                                                                                                                                                                                                                          |
|-------------------------------------|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `applications` domain               | `ApplicationTests` (existente, +1 caso)                 | `withCredentialHash` conserva identidad, `baseUrl`, `registeredAt`, y solo cambia `credentialHash`                                                                                                                                                                                                                                             |
| `applications` application          | `RotateApplicationCredentialUseCaseImplTests` (nueva)   | rotar una aplicación existente devuelve el secreto nuevo en claro y persiste su hash (nunca el secreto) vía `updateCredentialHash`; rotar una aplicación inexistente lanza `ApplicationNotFoundException` **sin** llegar a generar ni hashear un secreto (poison-pill en `SecretGenerator`/`CredentialHasher`, mismo patrón que HU-012/HU-013) |
| `applications` infrastructure       | `RotateApplicationCredentialRequestMapperTests` (nueva) | `applicationId` ausente → `MissingRequestFieldException`; mal formado → `MalformedRequestFieldException`; válido → `ApplicationId` correcto, `tenantId` del principal                                                                                                                                                                          |
| `applications` infrastructure       | `ApplicationControllerTests` (existente, +1 caso)       | `rotate` usa el `applicationId` de la ruta y responde 201 con el cuerpo del interactor                                                                                                                                                                                                                                                         |
| `applications` infrastructure (E2E) | `ApplicationHttpTests` (existente, +1 caso)             | registra una aplicación, rota su credencial → 201 con un `credential` distinto al original; una validación posterior (si se ejercita directamente el caso de uso de HU-013 en la prueba, no por HTTP) confirma que el secreto viejo ya no sirve y el nuevo sí                                                                                  |

## 10. Trazabilidad

| Fase                       | Estado      | Fecha      |
|----------------------------|-------------|------------|
| Plan                       | ✅ Generado  | 2026-09-13 |
| Contrato aprobado (gate 1) | ⏳ Pendiente |            |
| Pruebas en rojo            | ⏳ Pendiente |            |
| Implementación en verde    | ⏳ Pendiente |            |
| Validación                 | ✅ Aprobada  | 2026-09-13 |
| Entrega (gate 2)           | ⏳ Pendiente |            |

## 11. Ambigüedades pendientes

Ninguna — las cuatro decisiones que `HU-014.md` marcaba como necesarias ya venían resueltas en su
propio texto (ver Hallazgo 1), y el resto se resuelve por precedente directo de código ya existente.

Lista de constantes nuevas de `RequiredArgumentMessages`: `ROTATE_APPLICATION_CREDENTIAL_USE_CASE`,
`ROTATE_APPLICATION_CREDENTIAL_INTERACTOR`.
