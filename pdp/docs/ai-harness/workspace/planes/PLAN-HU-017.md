# PLAN: Gatear el registro de recursos protegidos — solo quien administra la aplicación amplía su catálogo

## Metadata

- **ID:** HU-017
- **Slice:** `resources` (pierde una escritura) y `authorization` (la recibe, orquestada) — mismo patrón de
  HU-015/HU-016
- **Tipo:** Escritura
- **Fecha:** 2026-09-14
- **Rama sugerida:** `feature/HU-017-gatear-registro-recursos`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-017.md` (dictada), `ADR-023` (`security-platform-architecture`), código
  real: `resources/*`, `authorization/*`, precedente directo de HU-016 (mismo mecanismo, slice distinto)
- **Criterios de la línea base que toca:** 1, 2, 9, 11, 12, 21, 22 (estructurales) — sin 3 nuevo (ninguna `Rule` nueva,
  mismo argumento que HU-016 §3)

## 1. Resumen funcional

`RegisterProtectedResourceUseCase` (agregar un recurso protegido a una aplicación ya registrada) hoy
está abierto a cualquier usuario autenticado del tenant. Esta historia exige que quien registra el
recurso sea administrador de la aplicación dueña — mismo mecanismo de HU-009/HU-016
(`PrincipalMustBeApplicationAdministratorValidator`, gateado vía OPA). A diferencia de HU-016, **no
hay caso ungated**: un recurso protegido siempre pertenece a una aplicación
(`RegisterProtectedResourceRequest.applicationId` es obligatorio, sin alcance TENANT/GLOBAL
equivalente), así que el gate aplica siempre, sin `Optional`. `RegisterApplicationWithInitialResourceUseCase`
(HU-010) sigue sin gatearse — mismo argumento que HU-015 aplicó a `RegisterApplicationUseCase`: no
hay administrador antes de que la aplicación exista.

## 2. Criterios de aceptación

| # | Criterio                                                      | Resultado esperado                                            |
|---|---------------------------------------------------------------|---------------------------------------------------------------|
| 1 | Registrar un recurso como administrador de la aplicación      | `201`, recurso creado, igual que hoy                          |
| 2 | Registrar un recurso sin ser administrador de esa aplicación  | `400 NOT_AUTHORIZED_TO_ADMINISTER`, el recurso **no** se crea |
| 3 | `POST /api/v1/applications/with-initial-resource` (HU-010)    | Sin cambios — sigue sin gate, cero regresión                  |
| 4 | `GET /api/v1/applications/{applicationId}/resources` (listar) | Sin cambios — no gateado, sigue en `resources`                |
| 5 | Suite completa                                                | `verificar.ps1` en verde                                      |

## 3. Reglas de negocio

Ninguna regla nueva: reutiliza el mecanismo completo de HU-009 (`AuthorizeAdministrationUseCase` →
`AdministrationDecisionPort` → OPA, fail-closed) a través de `PrincipalMustBeApplicationAdministratorValidator`,
ya implementado. Esta historia solo cablea ese validador en un punto nuevo.

| #  | Regla                                                          | Dónde vive                                                               | Excepción → HTTP                                       |
|----|----------------------------------------------------------------|--------------------------------------------------------------------------|--------------------------------------------------------|
| R1 | Quien registra un recurso debe administrar la aplicación dueña | Reutiliza `PrincipalMustBeApplicationAdministratorValidator` (ya existe) | `NotAuthorizedToAdministerException` → 400 (ya existe) |

> A diferencia de HU-016 (R3), aquí no hace falta una decisión de "¿hay algo que gatear?": el
> `applicationId` siempre está presente en `RegisterProtectedResourceRequest`, así que el gate
> siempre se evalúa. Sin `Optional<AdministrationRequest>`.

## 4. Modelo de dominio afectado

Ninguna entidad, value object ni enum nuevo. Se reutilizan `RegisterProtectedResourceRequest`,
`RegisteredProtectedResourceResponse`, `ApplicationId`, `TenantId`, `UserId` (todos existentes).

### DTO nuevo (primaryport de `authorization`)

| DTO                                                                                                                      | Nuevo o existente | Invariantes                                                                 | Vive en                                          |
|--------------------------------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------------------------------------------|--------------------------------------------------|
| `AdministerResourceRegistrationRequest(AdministrationRequest administration, RegisterProtectedResourceRequest resource)` | Nuevo             | Ambos componentes `requireNonNull` — sin `Optional`, a diferencia de HU-016 | `authorization/application/primaryport/request/` |

## 5. Persistencia

Sin cambios. No hay tabla, campo ni consulta nueva.

## 6. Endpoint

| Verbo  | Ruta                                             | Código de éxito    | Cuerpo de entrada                                            | Cuerpo de salida                                                                                                                                                                       |
|--------|--------------------------------------------------|--------------------|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST` | `/api/v1/applications/{applicationId}/resources` | `201` (sin cambio) | `RegisterProtectedResourceBodyRequest` (sin cambio de forma) | `AdministeredResourceWebResponse` (misma forma que `ProtectedResourceWebResponse`, nueva clase — mismo criterio de nombre distinto que `AdministeredApplicationWebResponse` en HU-015) |
| `GET`  | `/api/v1/applications/{applicationId}/resources` | `200` (sin cambio) | —                                                            | — (permanece en `resources`, sin tocar)                                                                                                                                                |

**La escritura se mueve físicamente de `ProtectedResourceController` (`resources`) a un nuevo
`ResourceAdministrationController` (`authorization`)** — mismas rutas, mismos verbos, mismo código,
mismo cuerpo. Mismo razonamiento exacto que HU-015/HU-016: `authorization` ya depende de `resources`
(`resources :: rule`, `:: dto`, `:: model`, `:: exception`); `resources` no puede depender de
`authorization` sin ciclo.

- **Autorización:** requiere token. El inquilino sale del principal. La aplicación contra la que se
  gatea sale directamente de `RegisterProtectedResourceRequest.applicationId()` — ya viene en la
  petición (raw request), sin necesitar ninguna consulta previa (a diferencia de HU-016
  `GrantResourceToRole`, que sí necesitaba resolver el `applicationId` del rol).
- **Errores esperados:** `NotAuthorizedToAdministerException` → 400 (ya existe); el resto de rechazos
  de `RegisterProtectedResourceRulesValidator`/`ApplicationMustExistForTenantValidator` no cambian
  (siguen dentro de `RegisterProtectedResourceUseCaseImpl`, sin tocar).

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/authorization/application/usecase/AdministerResourceRegistrationUseCase.java
public interface AdministerResourceRegistrationUseCase
        extends ReactiveOperation<AdministerResourceRegistrationRequest, RegisteredProtectedResourceResponse> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/AdministerResourceRegistrationInteractor.java
public interface AdministerResourceRegistrationInteractor
        extends ReactiveOperation<RegisterProtectedResourceRawRequest, AdministeredResourceWebResponse> {
}
```

> `RegisterProtectedResourceRawRequest` y `RegisterProtectedResourceBodyRequest` **se mueven** de
> `resources/infrastructure/.../dto/request/raw/` a la misma ruta relativa dentro de `authorization`
> — misma forma exacta (records de Strings desnudos), solo cambia el paquete.

### Firmas de DTOs

```java
// pdp/authorization/application/primaryport/request/AdministerResourceRegistrationRequest.java
public record AdministerResourceRegistrationRequest(AdministrationRequest administration,
        RegisterProtectedResourceRequest resource) { }
// invariantes: ambos componentes requireNonNull
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/response/AdministeredResourceWebResponse.java
public record AdministeredResourceWebResponse(String id, String applicationId, String tenantId, String path,
        String method, java.time.Instant registeredAt) { }
// misma forma exacta que resources/.../ProtectedResourceWebResponse
```

### Firmas nuevas en `RequiredArgumentMessages`

```java
public static final String ADMINISTER_RESOURCE_REGISTRATION_USE_CASE =
        "se requiere el caso de uso de administración de registro de recurso";
public static final String REGISTER_PROTECTED_RESOURCE_REQUEST =
        "se requiere la solicitud de registro de recurso protegido";
public static final String ADMINISTRATION_REQUEST = "se requiere la solicitud de administración";
```

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado · `[D]` eliminado (movido a otra ruta).

```
pdp/resources/
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/ProtectedResourceController.java                    [M] quita register()
    │   ├── dto/request/raw/RegisterProtectedResourceRawRequest.java       [D] → authorization (misma forma)
    │   ├── dto/request/raw/RegisterProtectedResourceBodyRequest.java      [D] → authorization (misma forma)
    │   ├── mapper/RegisterProtectedResourceRequestMapper.java             [D] → authorization (sin cambios de lógica — no usa ResourcesMessages)
    │   ├── interactor/RegisterProtectedResourceInteractor.java            [D] → authorization (renombrado)
    │   └── interactor/impl/RegisterProtectedResourceInteractorImpl.java   [D] → authorization (renombrado, lógica nueva)
    └── config/ResourcesConfiguration.java                                 [M] quita 1 bean de interactor

pdp/authorization/
├── application/
│   ├── primaryport/request/AdministerResourceRegistrationRequest.java    [N]
│   └── usecase/
│       ├── AdministerResourceRegistrationUseCase.java                    [N]
│       └── impl/AdministerResourceRegistrationUseCaseImpl.java           [N]
├── infrastructure/
│   ├── adapter/primary/web/
│   │   ├── controller/ResourceAdministrationController.java              [N] ruta movida de ProtectedResourceController
│   │   ├── dto/request/raw/RegisterProtectedResourceRawRequest.java      [N] ← resources (misma forma)
│   │   ├── dto/request/raw/RegisterProtectedResourceBodyRequest.java     [N] ← resources (misma forma)
│   │   ├── dto/response/AdministeredResourceWebResponse.java             [N]
│   │   ├── mapper/RegisterProtectedResourceRequestMapper.java            [N] ← resources (idéntico)
│   │   ├── mapper/AdministeredResourceResponseMapper.java                [N]
│   │   ├── interactor/AdministerResourceRegistrationInteractor.java      [N] ← RegisterProtectedResourceInteractor
│   │   └── interactor/impl/AdministerResourceRegistrationInteractorImpl.java [N]
│   └── config/AuthorizationConfiguration.java                            [M] agrega 2 beans (use case, interactor)
└── package-info.java                                                     [M] agrega "resources :: usecase"

pdp/resources/application/usecase/package-info.java                       [N] @NamedInterface("usecase") — primer consumidor externo de este subpaquete (resources :: rule/:: dto/:: model/:: exception ya lo son desde HU-004/HU-010, así que esto no dispara la trampa de "primer NamedInterface del módulo")
pdp/resources/application/primaryport/response/package-info.java          [N] @NamedInterface("dto") — omisión real encontrada al compilar: solo `primaryport/request` estaba publicado; `RegisteredProtectedResourceResponse` (en `response`) nunca había cruzado a otro módulo hasta esta historia

shared/message/RequiredArgumentMessages.java                              [M] agrega 3 constantes (§7)
```

**Nota operativa (secuenciación, igual que HU-016):** por las mismas razones de colisión de rutas,
`ResourceAdministrationController` no se crea con el `@PostMapping` real activo hasta que, en el
mismo paso, se retire `register()` de `ProtectedResourceController`. El planificador solo materializa
hasta donde no colisiona (beans de use case/interactor, sin el controller registrando la ruta); el
resto es `[M]` de un solo paso para el implementador/tester, exactamente como HU-016 lo resolvió.

## 9. Casos de prueba esperados

> `RegisterProtectedResourceRequestMapperTests` (si existe en `resources`) se mueve a `authorization`
> — trabajo de `pdp/src/test`, del tester/implementador, no del planificador.

| Capa                               | Clase de prueba                                                                   | Casos                                                                                                                                                                                        |
|------------------------------------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application` (`authorization`)    | `AdministerResourceRegistrationUseCaseImplTests`                                  | administrador → delega y devuelve la respuesta; no administrador → `NotAuthorizedToAdministerException`, `RegisterProtectedResourceUseCase` **no** se invoca                                 |
| `infrastructure` (`authorization`) | `AdministerResourceRegistrationInteractorImplTests`                               | construye `AdministrationRequest` con el `applicationId` de la propia petición (sin consulta adicional); resuelve `UserId` vía `SubjectUserIdLookupValidator` cuando el principal no lo trae |
| `infrastructure` (`authorization`) | `RegisterProtectedResourceRequestMapperTests` (movido, si existía)                | mismos casos que antes                                                                                                                                                                       |
| `infrastructure` (`authorization`) | `ResourceAdministrationControllerTests`                                           | delega al interactor y responde `201`; no decide reglas                                                                                                                                      |
| `infrastructure` (`resources`)     | `ProtectedResourceControllerTests` (ajustado, si existía con casos de `register`) | ya no prueba `register` (movido); sigue probando `list` si aplica                                                                                                                            |

Presupuesto estimado: **8–11 pruebas** — menor que HU-016 porque no hace falta el validador de
resolución adicional (`RoleApplicationLookupValidator` no tiene equivalente aquí: el `applicationId`
ya viene en la petición).

## 10. Trazabilidad

| Fase                       | Estado                                         | Fecha      |
|----------------------------|------------------------------------------------|------------|
| Plan                       | ✅ Generado                                     | 2026-09-14 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                     | 2026-09-14 |
| Pruebas en rojo            | ✅ Confirmado                                   | 2026-09-14 |
| Implementación en verde    | ✅ Verde (671 pruebas)                          | 2026-09-14 |
| Validación                 | ✅ APROBADO — ver `REPORTE-HU-017.md`           | 2026-09-14 |
| Entrega (gate 2)           | ⏳ Pendiente de confirmación para commit y push |            |

## 11. Ambigüedades pendientes

Ninguna. Las dos preguntas que HU-017.md dejaba abiertas se resolvieron por investigación de código:

1. **¿El recurso puede pertenecer a una aplicación distinta de la que se administra?** No aplica la
   pregunta tal como estaba planteada: `RegisterProtectedResourceRequest.applicationId()` **es** la
   aplicación del recurso a registrar — no hay una segunda aplicación de por medio (a diferencia de
   HU-016, donde el rol y el recurso concedido podían, en teoría, ser de aplicaciones distintas). Se
   gatea directamente contra ese `applicationId`, leído del propio raw request.
2. **¿Reutiliza `NotAuthorizedToAdministerException` tal cual?** Sí, sin cambios — mismo precedente de
   HU-015/HU-016.
