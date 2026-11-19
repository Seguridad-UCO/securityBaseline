# PLAN: Gatear la administración de roles — solo quien administra la aplicación define roles y concede recursos

## Metadata

- **ID:** HU-016
- **Slice:** `roles` (pierde dos escrituras) y `authorization` (las recibe, orquestadas) — mismo patrón de HU-015 con
  `applications`
- **Tipo:** Escritura
- **Fecha:** 2026-09-14
- **Rama sugerida:** `feature/HU-016-gatear-roles-administracion`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-016.md` (dictada), `ADR-023-application-administration-model.md` (
  `security-platform-architecture`), código real: `roles/*`, `authorization/*` (HU-009, HU-015 como precedente de
  cableado)
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 21, 22 (estructurales + reglas) — sin 13/14/20 nuevos (no
  hay DTO nuevo de HTTP más allá de mover los existentes), sin 5/6 nuevos (las rutas y sus códigos de éxito no cambian)

## 1. Resumen funcional

`DefineRoleUseCase` y `GrantResourceToRoleUseCase` (slice `roles`) hoy están abiertos a cualquier
usuario autenticado del tenant. Esta historia exige, **solo para roles de alcance `APPLICATION`**,
que quien define el rol o concede el recurso sea administrador de esa aplicación — mecanismo de
HU-009 (`PrincipalMustBeApplicationAdministratorValidator` + `AuthorizeAdministrationUseCase`),
gateado vía OPA, nunca un `if` de rol en Java. Roles de alcance `TENANT` (el único otro alcance que
este canal admite; `GLOBAL` ya se rechaza en el mapper desde HU-004) **no** se gatean: sin
administrador de tenant/global todavía (ADR-024), gatearlos cerraría el único camino que existe hoy
para crear roles de ese alcance. No cubre `resources` (HU-017), `assignments` (HU-018), `profiles`
(HU-019), autoservicio de administradores (HU-020) ni auditoría administrativa (HU-021).

## 2. Criterios de aceptación

| # | Criterio                                                                                     | Resultado esperado                                                                                               |
|---|----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| 1 | Definir un rol `APPLICATION` como administrador de esa aplicación                            | `201`, rol creado, igual que hoy                                                                                 |
| 2 | Definir un rol `APPLICATION` **sin** ser administrador de esa aplicación                     | `400 NOT_AUTHORIZED_TO_ADMINISTER`, el rol **no** se crea                                                        |
| 3 | Definir un rol `TENANT` sin ser administrador de ninguna aplicación                          | `201`, sin gate — cero regresión                                                                                 |
| 4 | Conceder un recurso a un rol `APPLICATION` como administrador de la aplicación dueña del rol | `200`, recurso concedido, igual que hoy                                                                          |
| 5 | Conceder un recurso a un rol `APPLICATION` **sin** ser administrador de esa aplicación       | `400 NOT_AUTHORIZED_TO_ADMINISTER`, el recurso **no** se concede                                                 |
| 6 | Conceder un recurso a un rol `TENANT`                                                        | `200`, sin gate — cero regresión                                                                                 |
| 7 | `GET /api/v1/roles` (listar)                                                                 | Sin cambios — no gateado, sigue en `roles`                                                                       |
| 8 | Rol inexistente en `POST /{roleId}/resources`                                                | `404`-equivalente del dominio (`RoleNotFoundException`), igual que hoy — el lookup de aplicación no lo enmascara |
| 9 | Suite completa                                                                               | `verificar.ps1` en verde                                                                                         |

## 3. Reglas de negocio

Ninguna regla de dominio **nueva**: se reutiliza el mecanismo completo de HU-009
(`AuthorizeAdministrationUseCase` → `AdministrationDecisionPort` → OPA, fail-closed a
`INDETERMINATE`) a través de `PrincipalMustBeApplicationAdministratorValidator`, ya implementado y
probado. Esta historia solo **cablea** ese validador en dos puntos nuevos.

| #  | Regla                                                                                            | Dónde vive                                                                                                                                        | Puerto que trae el dato                                                                     | Excepción → HTTP                                       |
|----|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|--------------------------------------------------------|
| R1 | Quien define un rol `APPLICATION` debe administrar esa aplicación                                | Reutiliza `PrincipalMustBeApplicationAdministratorValidator` (ya existe)                                                                          | `AuthorizeAdministrationUseCase` (ya existe)                                                | `NotAuthorizedToAdministerException` → 400 (ya existe) |
| R2 | Quien concede un recurso a un rol `APPLICATION` debe administrar la aplicación **dueña del rol** | Ídem R1                                                                                                                                           | Ídem, más `RoleApplicationLookupValidator` **[N]** para resolver el `applicationId` del rol | Ídem                                                   |
| R3 | Un rol `TENANT` no se gatea                                                                      | Decisión de alcance de esta historia, no una `Rule`: el interactor no construye `AdministrationRequest` cuando `scope.applicationId()` está vacío | —                                                                                           | —                                                      |

> **Por qué R3 no es una `Rule`:** no rechaza nada — decide si hay algo que gatear. Es control de
> flujo del interactor, no una restricción de negocio con excepción propia.

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguno nuevo. `Role` (existente) no cambia.

### Value objects

Ninguno nuevo. Se reutilizan `RoleScope`, `ApplicationId`, `TenantId`, `UserId` (todos existentes).

### DTOs nuevos (primaryport de `authorization`)

| DTO                                                                                                          | Nuevo o existente | Invariantes                                                                                                                                                            | Vive en                                          |
|--------------------------------------------------------------------------------------------------------------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| `AdministerRoleDefinitionRequest(Optional<AdministrationRequest> administration, DefineRoleRequest role)`    | Nuevo             | Ambos componentes `requireNonNull` (el `Optional` en sí, no su contenido) — mismo patrón que `RoleScope` (Javadoc: "Optional como componente", precedente ya aceptado) | `authorization/application/primaryport/request/` |
| `AdministerResourceGrantRequest(Optional<AdministrationRequest> administration, GrantResourceRequest grant)` | Nuevo             | Ídem                                                                                                                                                                   | `authorization/application/primaryport/request/` |

`Optional<AdministrationRequest>` vacío significa "esta escritura no requiere administración" (rol
`TENANT`) — nunca "no se pudo resolver": si el rol no existe, `RoleApplicationLookupValidator`
lanza `RoleNotFoundException` antes de llegar a construir el DTO (ver §7).

## 5. Persistencia

Sin cambios. No hay tabla, campo ni consulta nueva — `RoleApplicationLookupValidator` reutiliza
`RoleRepository.findByIdForTenant`, que ya existe.

## 6. Endpoint

| Verbo  | Ruta                               | Código de éxito    | Cuerpo de entrada                               | Cuerpo de salida                                                                          |
|--------|------------------------------------|--------------------|-------------------------------------------------|-------------------------------------------------------------------------------------------|
| `POST` | `/api/v1/roles`                    | `201` (sin cambio) | `DefineRoleRawRequest` (sin cambio de forma)    | `RoleAdministrationWebResponse` (misma forma que `RoleWebResponse`, nueva clase — ver §7) |
| `POST` | `/api/v1/roles/{roleId}/resources` | `200` (sin cambio) | `GrantResourceRawRequest` (sin cambio de forma) | `RoleAdministrationWebResponse`                                                           |
| `GET`  | `/api/v1/roles`                    | `200` (sin cambio) | —                                               | — (permanece en `roles`, sin tocar)                                                       |

**Las dos rutas de escritura se mueven físicamente de `RoleController` (`roles`) a un nuevo
`RoleAdministrationController` (`authorization`).** No es un cambio de contrato HTTP — mismas rutas,
mismos verbos, mismos códigos, mismo cuerpo de entrada y salida — es un cambio de **qué módulo aloja
el adaptador primario**, exactamente el precedente de HU-015 con `ApplicationAdministrationController`
(PLAN-HU-015.md §0: "vive en `authorization`, no en `applications` — es el módulo que ya depende de
ella"). Aquí aplica el mismo razonamiento: `authorization` ya depende de `roles` (`roles :: rule`);
`roles` **no puede** depender de `authorization` sin crear un ciclo (`ModulithStructureTests` lo
rechazaría). Gatear una escritura de `roles` con un validador de `authorization` solo es posible si
el punto de entrada vive en el módulo que ya mira hacia `roles`.

- **Autorización:** requiere token. El inquilino sale del principal (sin cambio). La aplicación
  contra la que se gatea sale del propio `RoleScope` de la petición (`DefineRole`) o del rol ya
  existente (`GrantResourceToRole`) — nunca del cuerpo ni de la query.
- **Errores esperados:** `NotAuthorizedToAdministerException` → 400 (ya existe, ver §3);
  `RoleNotFoundException` → 400 (ya existe, sin cambio); el resto de rechazos existentes de
  `DefineRoleRulesValidator`/`GrantResourceRulesValidator` no cambian.

## 7. SPEC — el contrato

### Contratos nuevos

```java
// pdp/roles/application/rule/validator/RoleApplicationLookupValidator.java
/**
 * Resuelve a qué aplicación pertenece un rol, si a alguna — vacío para alcance TENANT (el único
 * otro alcance que este canal admite hoy). Rechaza con RoleNotFoundException si el rol no existe
 * para ese inquilino, mismo criterio que RoleMustExistForTenantValidator (HU-016).
 */
public interface RoleApplicationLookupValidator
        extends ReactiveOperation<RoleOwnershipQuery, Optional<ApplicationId>> {
}
```

```java
// pdp/authorization/application/usecase/AdministerRoleDefinitionUseCase.java
public interface AdministerRoleDefinitionUseCase
        extends ReactiveOperation<AdministerRoleDefinitionRequest, RoleResponse> {
}
```

```java
// pdp/authorization/application/usecase/AdministerResourceGrantUseCase.java
public interface AdministerResourceGrantUseCase
        extends ReactiveOperation<AdministerResourceGrantRequest, RoleResponse> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/AdministerRoleDefinitionInteractor.java
public interface AdministerRoleDefinitionInteractor
        extends ReactiveOperation<DefineRoleRawRequest, RoleAdministrationWebResponse> {
}
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/interactor/AdministerResourceGrantInteractor.java
public interface AdministerResourceGrantInteractor
        extends ReactiveOperation<GrantResourceRawRequest, RoleAdministrationWebResponse> {
}
```

> `DefineRoleRawRequest` y `GrantResourceRawRequest` **se mueven** de
> `roles/infrastructure/adapter/primary/web/dto/request/raw/` a la misma ruta relativa dentro de
> `authorization` — misma forma exacta (son `record` de Strings desnudos), solo cambia el paquete.

### Firmas de DTOs

```java
// pdp/authorization/application/primaryport/request/AdministerRoleDefinitionRequest.java
public record AdministerRoleDefinitionRequest(Optional<AdministrationRequest> administration, DefineRoleRequest role) { }
// invariantes: ambos componentes requireNonNull (el Optional en sí; su contenido puede ser empty)
```

```java
// pdp/authorization/application/primaryport/request/AdministerResourceGrantRequest.java
public record AdministerResourceGrantRequest(Optional<AdministrationRequest> administration, GrantResourceRequest grant) { }
// invariantes: idénticas a AdministerRoleDefinitionRequest
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/response/RoleAdministrationWebResponse.java
public record RoleAdministrationWebResponse(String id, String name, String scope, String tenantId,
        String applicationId, List<String> resourceIds, String registeredAt) { }
// misma forma exacta que roles/.../RoleWebResponse — nombre distinto porque vive en otro módulo,
// no porque el contrato HTTP cambie (mismo criterio de AdministeredApplicationWebResponse en HU-015,
// que tampoco reutilizó ApplicationWebResponse)
```

### Firmas nuevas en `RequiredArgumentMessages`

```java
// shared/message/RequiredArgumentMessages.java
public static final String ROLE_APPLICATION_LOOKUP_VALIDATOR = "se requiere el validador de aplicación del rol";
public static final String ADMINISTER_ROLE_DEFINITION_USE_CASE = "se requiere el caso de uso de administración de definición de rol";
public static final String ADMINISTER_RESOURCE_GRANT_USE_CASE = "se requiere el caso de uso de administración de concesión de recurso";
public static final String ADMINISTRATION = "se requiere la solicitud de administración (puede estar vacía)";
public static final String DEFINE_ROLE_REQUEST = "se requiere la solicitud de definición de rol";
public static final String GRANT_RESOURCE_REQUEST = "se requiere la solicitud de concesión de recurso";
```

### Firmas nuevas en `AuthorizationMessages` (mueven desde `RolesMessages`)

```java
// pdp/authorization/domain/message/AuthorizationMessages.java — [M], se agregan estos dos métodos
public static String globalScopeNotAdministrableYet() { ... }          // texto idéntico al de RolesMessages
public static String applicationIdNotApplicableForTenantScope() { ... } // texto idéntico al de RolesMessages
```

`RolesMessages` **[M]** pierde estos dos métodos (sin otro consumidor: solo los usaba
`DefineRoleRequestMapper`, que se mueve). El implementador comprueba que ningún `pdp/src/test`
distinto de `DefineRoleRequestMapperTests` los referencia antes de borrarlos.

## 8. Árbol de archivos

> `[N]` nuevo · `[M]` modificado · `[D]` eliminado (movido a otra ruta, ver flecha).

```
pdp/roles/
├── application/
│   └── rule/
│       └── validator/
│           ├── RoleApplicationLookupValidator.java                         [N]
│           └── impl/
│               └── RoleApplicationLookupValidatorImpl.java                 [N]
├── domain/
│   └── message/
│       └── RolesMessages.java                                              [M] quita 2 métodos
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/RoleController.java                                  [M] quita define()/grantResource()
    │   ├── dto/request/raw/DefineRoleRawRequest.java                       [D] → authorization (misma forma)
    │   ├── dto/request/raw/GrantResourceRawRequest.java                    [D] → authorization (misma forma)
    │   ├── mapper/DefineRoleRequestMapper.java                             [D] → authorization (ajusta catálogo de mensajes)
    │   ├── mapper/GrantResourceRequestMapper.java                          [D] → authorization (sin cambios de lógica)
    │   ├── interactor/DefineRoleInteractor.java                            [D] → authorization (renombrado)
    │   ├── interactor/impl/DefineRoleInteractorImpl.java                   [D] → authorization (renombrado, lógica nueva)
    │   ├── interactor/GrantResourceToRoleInteractor.java                   [D] → authorization (renombrado)
    │   └── interactor/impl/GrantResourceToRoleInteractorImpl.java          [D] → authorization (renombrado, lógica nueva)
    └── config/RolesConfiguration.java                                      [M] quita 2 beans de interactor, agrega 1 bean de validador

pdp/authorization/
├── application/
│   ├── primaryport/request/
│   │   ├── AdministerRoleDefinitionRequest.java                            [N]
│   │   └── AdministerResourceGrantRequest.java                             [N]
│   └── usecase/
│       ├── AdministerRoleDefinitionUseCase.java                            [N]
│       ├── AdministerResourceGrantUseCase.java                             [N]
│       └── impl/
│           ├── AdministerRoleDefinitionUseCaseImpl.java                    [N]
│           └── AdministerResourceGrantUseCaseImpl.java                     [N]
├── domain/
│   └── message/AuthorizationMessages.java                                  [M] agrega 2 métodos
├── infrastructure/
│   ├── adapter/primary/web/
│   │   ├── controller/RoleAdministrationController.java                    [N] rutas movidas de RoleController
│   │   ├── dto/request/raw/DefineRoleRawRequest.java                       [N] ← roles (misma forma)
│   │   ├── dto/request/raw/GrantResourceRawRequest.java                    [N] ← roles (misma forma)
│   │   ├── dto/response/RoleAdministrationWebResponse.java                 [N]
│   │   ├── mapper/DefineRoleRequestMapper.java                             [N] ← roles (usa AuthorizationMessages)
│   │   ├── mapper/GrantResourceRequestMapper.java                          [N] ← roles (sin cambios)
│   │   ├── mapper/RoleAdministrationResponseMapper.java                    [N]
│   │   ├── interactor/AdministerRoleDefinitionInteractor.java              [N] ← DefineRoleInteractor
│   │   ├── interactor/impl/AdministerRoleDefinitionInteractorImpl.java     [N]
│   │   ├── interactor/AdministerResourceGrantInteractor.java               [N] ← GrantResourceToRoleInteractor
│   │   └── interactor/impl/AdministerResourceGrantInteractorImpl.java      [N]
│   └── config/AuthorizationConfiguration.java                              [M] agrega 6 beans (2 use cases, 2 interactors) + registra el controller
└── package-info.java                                                      [M] agrega "roles :: usecase", "roles :: model", "roles :: dto"

shared/message/RequiredArgumentMessages.java                                [M] agrega 6 constantes (§7)
```

**Trampa de Modulith (ver protocolo del planificador):** `roles` ya tiene NamedInterfaces con
consumidores (`rule`, `dto`, `model`, `usecase`, `exception` — todos ganados en HU-011/HU-015), así
que agregar `"roles :: usecase"`, `"roles :: model"` y `"roles :: dto"` a `authorization` **no**
dispara la trampa de "primer NamedInterface del módulo": son subpaquetes que ya existen como
interfaz nombrada, solo se declara un consumidor nuevo. Verificar igual compilando — si
`ModulithStructureTests` marca algo como "ya estaba permitido", es la señal de que sí hace falta un
`@NamedInterface` nuevo en algún subpaquete que hoy no lo tiene.

## 9. Casos de prueba esperados

> Los tests de `DefineRoleRequestMapperTests` y `GrantResourceRequestMapperTests` (hoy en
> `roles/infrastructure/.../mapper/`) se **mueven** a la ruta equivalente en `authorization` — es
> tocar `pdp/src/test`, trabajo del tester/implementador, no del planificador. Igual para cualquier
> test de `RoleControllerTests` que hoy cubra `define`/`grantResource`.

| Capa                               | Clase de prueba                               | Casos                                                                                                                                                                                                                                                                    |
|------------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application` (`roles`)            | `RoleApplicationLookupValidatorImplTests`     | rol `APPLICATION` existente → `Optional` con el id correcto; rol `TENANT` existente → `Optional.empty()`; rol inexistente → `RoleNotFoundException`                                                                                                                      |
| `application` (`authorization`)    | `AdministerRoleDefinitionUseCaseImplTests`    | administración presente + decisión `ALLOW` → delega y devuelve `RoleResponse`; administración presente + rechazo → `NotAuthorizedToAdministerException`, `DefineRoleUseCase` **no** se invoca; administración vacía (`TENANT`) → delega directo, sin llamar al validador |
| `application` (`authorization`)    | `AdministerResourceGrantUseCaseImplTests`     | mismos tres casos que arriba, sobre `GrantResourceToRoleUseCase`                                                                                                                                                                                                         |
| `infrastructure` (`authorization`) | `AdministerRoleDefinitionInteractorImplTests` | rol `APPLICATION` → construye `Optional` presente con el `applicationId` del propio `RoleScope` de la petición; rol `TENANT` → `Optional.empty()`, sin tocar `RoleApplicationLookupValidator`                                                                            |
| `infrastructure` (`authorization`) | `AdministerResourceGrantInteractorImplTests`  | resuelve el `applicationId` vía `RoleApplicationLookupValidator`; propaga `RoleNotFoundException` si el rol no existe                                                                                                                                                    |
| `infrastructure` (`authorization`) | `DefineRoleRequestMapperTests` (movido)       | mismos casos que hoy, con `AuthorizationMessages` en vez de `RolesMessages`                                                                                                                                                                                              |
| `infrastructure` (`authorization`) | `GrantResourceRequestMapperTests` (movido)    | sin cambios de caso                                                                                                                                                                                                                                                      |
| `infrastructure` (`authorization`) | `RoleAdministrationControllerTests`           | delega a cada interactor y responde el código esperado (201/200); no decide reglas                                                                                                                                                                                       |
| `infrastructure` (`roles`)         | `RoleControllerTests` (ajustado)              | ya no prueba `define`/`grantResource` (movidos); sigue probando `list`                                                                                                                                                                                                   |

Presupuesto total estimado: **14–17 pruebas**, dentro del rango orientativo de `sb-testing` para una
historia con dos escrituras y una validación de propiedad nueva.

## 10. Trazabilidad

| Fase                       | Estado                                         | Fecha      |
|----------------------------|------------------------------------------------|------------|
| Plan                       | ✅ Generado                                     | 2026-09-14 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                     | 2026-09-14 |
| Pruebas en rojo            | ✅ Confirmado                                   | 2026-09-14 |
| Implementación en verde    | ✅ Verde (668 pruebas)                          | 2026-09-14 |
| Validación                 | ✅ APROBADO — ver `REPORTE-HU-016.md`           | 2026-09-14 |
| Entrega (gate 2)           | ⏳ Pendiente de confirmación para commit y push |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. Dos preguntas que HU-016.md dejaba abiertas se resolvieron por
investigación de código, documentadas arriba:

1. **¿De cuál `applicationId` se exige administración al conceder un recurso?** Del **rol**
   (`RoleApplicationLookupValidator`), no del recurso — confirmado: `GrantResourceRulesValidatorImpl`
   ya resuelve el rol primero y usa su `scope()` para la cobertura (R5); esta historia solo adelanta
   esa misma resolución antes del gate.
2. **¿Migración para roles/concesiones ya creados por un no-administrador?** Ninguna: la regla aplica
   hacia adelante (quién puede *escribir* desde ahora), no reevalúa lo ya persistido — mismo criterio
   que HU-015 aplicó a aplicaciones ya registradas.
3. **¿Reutiliza `NotAuthorizedToAdministerException` tal cual?** Sí, sin `reasonCode` nuevo — mismo
   precedente de HU-015 (un solo tipo de excepción para las dos operaciones que gateó).
