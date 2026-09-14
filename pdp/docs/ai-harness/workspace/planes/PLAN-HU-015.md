# PLAN: HU-015 — Administración del catálogo de aplicaciones

## Metadata

- **ID:** HU-015
- **Slice:** `applications` (sin cambios propios) + `assignments` (nueva orquestación) + `authorization` (nuevos endpoints gateados) — ver §0 para por qué se reparte en tres módulos
- **Tipo:** Mixto (dos endpoints nuevos que gatean escrituras existentes, uno nuevo que orquesta un alta compuesta, uno interno para el backfill)
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-015-administracion-aplicaciones`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HU-015.md` (decisiones ya cerradas con Sebastián), `PLAN-HU-009.md` (mecanismo y hallazgo 6), código real (`package-info.java` de los 9 slices, vía `Read`) — no se consultó `security-platform-architecture` porque esta historia no introduce vocabulario de dominio nuevo, solo cablea un mecanismo ya aceptado
- **Criterios de la línea base que toca:** 1, 2, 3, 9, 11, 12, 20, 21, 22 (estructurales + reglas + endpoint sin persistencia nueva). No toca 13/14/16-19 (no hay DTO crudo con VOs nuevos ni consulta paginada nueva) ni 7/4 (no hay tabla nueva; sí usa `IdentifierGenerator`/`TimeProvider` ya existentes, no en líneas nuevas)

## 0. Por qué esta historia se reparte en tres módulos, no uno

`sb-arquitectura` es clara en que un slice no debería tocar a otro sin ser su dueño. Pero el grafo de
dependencias real (verificado abriendo los 9 `package-info.java`, no asumido) es:

```
authorization  ──►  applications, resources, roles ::rule, assignments ::usecase
assignments    ──►  applications, roles, profiles
roles          ──►  applications, resources
resources      ──►  applications
```

`applications` está en la **base** de ese grafo: todo el mundo depende de ella, ella no depende de
nadie del negocio (solo `tenants`). Eso significa que **`applications` nunca puede ser quien
consuma** `PrincipalMustBeApplicationAdministratorValidator` (vive en `authorization`, arriba del
todo) ni `DefineRoleUseCase`/`AssignRoleUseCase` (viven en `roles`/`assignments`, también arriba) —
hacerlo crearía un ciclo que `ModulithStructureTests` (`ApplicationModules.verify()`) rechaza en el
build, no en la revisión de código.

**Resolución (acordada con Sebastián), mismo principio que HU-010** (*"vive en `resources`, no en
`applications`: es el módulo que ya tenía permiso de mirar al otro"*):

| Necesidad | Vive en | Por qué ese módulo y no otro |
|---|---|---|
| Gatear `RemoveApplicationUseCase`/`RotateApplicationCredentialUseCase` con el validador de administración | **`authorization`** | Ya depende de `applications`. Consume el validador (que ya es suyo) y delega en `applications :: usecase` — sin tocar ninguna frontera nueva salvo publicar `applications :: usecase` como dependencia permitida (ya lo consume `resources`; no es la primera vez que se expone) |
| Orquestar "registrar app + crear rol ADMIN + asignárselo al registrador" | **`assignments`** | Ya depende de `applications :: rule/dto/exception` **y** de `roles :: rule/dto/exception`. Es el único módulo del grafo que ya ve a los dos que esta orquestación necesita tocar |
| Backfill manual del primer administrador de una app ya existente | **`assignments`** | Misma orquestación que el punto anterior, menos el paso de registrar — mismo dueño |

**Consecuencia que hay que aceptar, documentada aquí para que no se lea como un descuido:**
`RegisterApplicationUseCaseImpl`, `RemoveApplicationUseCaseImpl` y `RotateApplicationCredentialUseCaseImpl`
(los tres en `applications`) **no cambian ni una línea** — siguen siendo simples y sin gate propio.
Lo que sí cambia es que los endpoints HTTP `POST /api/v1/applications` (registro) y
`POST /api/v1/applications/{id}/credential-rotations` (rotación) **se mudan de controlador y de
módulo**: el primero pasa a vivir en `assignments`, el segundo (y el `DELETE` nuevo) en
`authorization`. La URL pública no cambia para ningún cliente existente del panel — cambia solo
dónde vive el código que la atiende.

## 1. Resumen funcional

Solo quien administra una aplicación puede eliminarla o rotar su credencial. Registrar sigue abierto
a cualquier usuario autenticado del tenant, pero quien registra queda automáticamente como el primer
administrador (alta directa, mismo request). Las aplicaciones que ya existían antes de esta historia
no tienen administrador: un endpoint interno, protegido por el mismo canal mTLS que ya usa el PEP,
permite asignárselo a mano, una vez por aplicación. No cubre: administrador global, reservar el
nombre de rol `ADMIN`, ni auditar estas decisiones (todo ya fuera de alcance por HU-009).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Registrar una aplicación deja a su registrador como administrador | `RegisterApplicationWithFirstAdministratorUseCaseImpl.execute` registra la app, define el rol `ADMIN` en su alcance y se lo asigna al `userId` del principal, en ese orden, en el mismo request |
| 2 | Un no-administrador no puede eliminar ni rotar la credencial de una aplicación | `AdministerApplicationRemovalUseCaseImpl`/`AdministerApplicationCredentialRotationUseCaseImpl` terminan en `NotAuthorizedToAdministerException` (400) cuando `PrincipalMustBeApplicationAdministratorValidator` rechaza |
| 3 | Un administrador sí puede eliminar y rotar | Los mismos casos de uso, con el validador en `ALLOW`, delegan en `RemoveApplicationUseCase`/`RotateApplicationCredentialUseCase` de `applications` sin modificarlos |
| 4 | Una aplicación ya existente puede recibir su primer administrador a mano | `AssignApplicationAdministratorUseCaseImpl`, expuesto en `/internal/v1/applications/{applicationId}/administrators`, define (o reutiliza) el rol `ADMIN` de esa aplicación y lo asigna al `userId` recibido |
| 5 | Cero regresión | `RegisterApplicationUseCase`, `RemoveApplicationUseCase`, `RotateApplicationCredentialUseCase` (los tres en `applications`) no cambian de firma ni de comportamiento; sus pruebas existentes siguen en verde sin tocarlas |

## 3. Reglas de negocio

Ninguna regla pura nueva (ningún VO ni restricción de conjunto que decidir en Java — la decisión de
"¿administra?" ya la toma OPA desde HU-009). Las únicas piezas de negocio son composiciones de casos
de uso ya validados, más un rechazo que ya existe.

| # | Regla | Dónde vive (VO / Rule) | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| — | Un sujeto sin `ALLOW` de administración no elimina ni rota la credencial | Reutilizada: `PrincipalMustBeApplicationAdministratorValidatorImpl` (ya existe, HU-009) | `AuthorizeAdministrationUseCase` → `AdministrationDecisionPort` (OPA) | `NotAuthorizedToAdministerException` → 400 (ya definida, no se reabre) |

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguna nueva. `Application` (existente) no cambia. `Role`/`Assignment` (existentes) se reutilizan
tal cual — un "administrador de aplicación" sigue siendo, para el catálogo, un usuario con una
asignación activa a un rol de alcance `APPLICATION` de nombre `ADMIN` en esa aplicación (decisión de
HU-009, no se reabre).

### Value objects

Ninguno nuevo. `RoleName("ADMIN")` se construye con el VO ya existente — el valor `"ADMIN"` es un
literal porque debe coincidir exactamente con lo que
`security-policy-engine/policies/administration/role.rego` ya reconoce (no es una decisión de este
plan: es un contrato ya fijado). El implementador lo declara como constante privada en los dos
casos de uso que lo usan (`RegisterApplicationWithFirstAdministratorUseCaseImpl`,
`AssignApplicationAdministratorUseCaseImpl`) — no hace falta un tercer sitio compartido para dos
consumidores del mismo módulo.

### Enums

Ninguno nuevo.

## 5. Persistencia

No hay tabla nueva. **Un método nuevo en un puerto existente** (marcado [M] en la sección 7 y 8):
`RoleRepository` no tiene forma de encontrar un rol existente por nombre+alcance (solo
`existsByNameInScope`, que responde `Boolean`) — lo necesita `AssignApplicationAdministratorUseCaseImpl`
para el camino "la aplicación ya tenía un rol `ADMIN` creado a mano, reutilízalo" del backfill (ver
§ ambigüedades del borrador original: "cualquier tenant puede hoy crear un rol llamado `ADMIN` sin
intención administrativa" — `security-policy-engine/docs/policies/administration.md`).

- **Inicializador de esquema:** ninguno nuevo (no hay tabla nueva).

## 6. Endpoint

| Verbo | Ruta | Código de éxito | Cuerpo de entrada | Cuerpo de salida | Módulo dueño (nuevo) |
|---|---|---|---|---|---|
| POST | `/api/v1/applications` | 201 | `{name, description, baseUrl}` (igual que hoy) | Igual que hoy + rol/asignación creados de forma transparente | `assignments` (**se muda** desde `applications`) |
| DELETE | `/api/v1/applications/{applicationId}` | 200 | — | `ApiResponse<Void>` (mismo patrón que `DELETE /api/v1/roles/{roleId}/assignments/{assignmentId}`) | `authorization` (**nuevo**, no existía) |
| POST | `/api/v1/applications/{applicationId}/credential-rotations` | 201 | — | Igual que hoy | `authorization` (**se muda** desde `applications`) |
| POST | `/internal/v1/applications/{applicationId}/administrators` | 201 | `{userId}` | `ApiResponse<AssignmentResponse>` aplanado | `assignments` (**nuevo**) |

- **Autorización:**
  - Los tres primeros: requieren sesión BFF (igual que hoy); el inquilino sale del principal.
  - El backfill vive bajo `/internal/v1/**`: hereda automáticamente la cadena mTLS + evidencia JWT
    de `InternalSecurityConfiguration` (`securityMatcher("/internal/v1/**")`, ya existente desde
    HU-003) — **no requiere ninguna configuración de seguridad nueva**, solo registrar la ruta bajo
    ese prefijo. El tenant se resuelve del catálogo (`ApplicationOwnerLookupValidator`), nunca del
    cuerpo, igual que el resto del canal interno.
- **Errores esperados:** `NotAuthorizedToAdministerException` → 400 (remove/rotate); `ApplicationNotFoundException` → 404 (backfill, aplicación inexistente — regla ya existente); el resto, igual que sus respectivos casos de uso ya documentan.

## 7. SPEC — el contrato

### Cambios de frontera de Modulith (antes que cualquier clase — sin esto, nada de abajo compila)

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/package-info.java                         [M]
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase",   // NUEVO — ya lo consume "resources"; segundo consumidor, no el primero
        "resources", "resources :: rule", "resources :: dto", "resources :: model", "resources :: exception",
        "assignments :: usecase", "assignments :: dto",
        "roles :: rule"})
package co.edu.uco.seguridad.pdp.authorization;
```

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/assignments/package-info.java                            [M]
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "identity :: rule", "identity :: exception",
        "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model",   // NUEVOS
        "roles :: rule", "roles :: dto", "roles :: exception",
        "roles :: usecase",   // NUEVO
        "profiles :: rule", "profiles :: dto"})
package co.edu.uco.seguridad.pdp.assignments;
```

```java
// pdp/src/main/java/co/edu/uco/seguridad/pdp/roles/application/usecase/package-info.java              [N]
/**
 * Primer NamedInterface de este subpaquete: {@code assignments} necesita {@code DefineRoleUseCase}
 * para el alta compuesta de HU-015. {@code roles} ya tiene otros NamedInterfaces (rule, dto, model,
 * exception) con consumidores, así que esto no dispara la trampa de "primer NamedInterface del
 * módulo" — solo abre este subpaquete concreto.
 */
@org.springframework.modulith.NamedInterface("usecase")
package co.edu.uco.seguridad.pdp.roles.application.usecase;
```

### Contratos nuevos — módulo `assignments`

```java
// pdp/assignments/application/usecase/RegisterApplicationWithFirstAdministratorUseCase.java     [N]
public interface RegisterApplicationWithFirstAdministratorUseCase
        extends ReactiveOperation<RegisterApplicationWithFirstAdministratorRequest, ApplicationRegistrationResponse> {
}
```

```java
// pdp/assignments/application/usecase/AssignApplicationAdministratorUseCase.java                [N]
public interface AssignApplicationAdministratorUseCase
        extends ReactiveOperation<AssignApplicationAdministratorRequest, AssignmentResponse> {
}
```

### Contratos nuevos — módulo `authorization`

```java
// pdp/authorization/application/usecase/AdministerApplicationRemovalUseCase.java                 [N]
public interface AdministerApplicationRemovalUseCase
        extends ReactiveOperationWithoutResult<AdministrationRequest> {
    // AdministrationRequest ya existe (HU-009): trae tenantId, applicationId, subjectUserId, subject, subjectRoles
}
```

```java
// pdp/authorization/application/usecase/AdministerApplicationCredentialRotationUseCase.java      [N]
public interface AdministerApplicationCredentialRotationUseCase
        extends ReactiveOperation<AdministrationRequest, ApplicationRegistrationResponse> {
}
```

### Firmas de DTOs

```java
// pdp/assignments/application/primaryport/request/RegisterApplicationWithFirstAdministratorRequest.java  [N]
public record RegisterApplicationWithFirstAdministratorRequest(
        RegisterApplicationRequest application, UserId registrarUserId) { }
        // application es "applications :: dto" reutilizado tal cual; registrarUserId resuelto del principal

// pdp/assignments/application/primaryport/request/AssignApplicationAdministratorRequest.java     [N]
public record AssignApplicationAdministratorRequest(TenantId tenantId, ApplicationId applicationId, UserId userId) {
}   // invariantes: los tres requireNonNull, mismo patrón que AssignRoleRequest
```

### Firmas nuevas en puertos existentes

```java
// pdp/roles/application/secondaryport/repository/RoleRepository.java                             [M]
/** Vacío si no hay rol con ese nombre en ese alcance exacto — complemento de existsByNameInScope. */
Mono<Role> findByNameInScope(RoleName name, RoleScope scope);
```

### Interactores e infraestructura web nuevos (resumen — firmas exactas en el árbol §8)

- `assignments`: `ApplicationRegistrationController` (`POST /api/v1/applications`, [M] recibe la ruta, hoy
  el interactor existe pero sin controlador que lo active — ver §11 ambigüedad 3),
  `InternalApplicationAdministratorController` (`POST /internal/v1/applications/{applicationId}/administrators`,
  activo), cada uno con su raw request, interactor, mapper y response DTO propios de `assignments`
  (no se reutilizan los de `applications`: son infraestructura, nunca se comparten entre módulos).
- `authorization`: `ApplicationAdministrationController` (`DELETE .../{applicationId}` activo,
  `POST .../{applicationId}/credential-rotations` pendiente de activar — ver §11 ambigüedad 3), con
  su propio raw request/response/mapper/interactor.

## 8. Árbol de archivos

```
pdp/roles/
└── application/usecase/
    └── package-info.java                                                          [N]

pdp/assignments/
├── application/
│   ├── primaryport/request/
│   │   ├── RegisterApplicationWithFirstAdministratorRequest.java                  [N]
│   │   └── AssignApplicationAdministratorRequest.java                             [N]
│   └── usecase/
│       ├── RegisterApplicationWithFirstAdministratorUseCase.java                  [N]
│       ├── AssignApplicationAdministratorUseCase.java                             [N]
│       └── impl/
│           ├── RegisterApplicationWithFirstAdministratorUseCaseImpl.java          [N]
│           └── AssignApplicationAdministratorUseCaseImpl.java                     [N]
├── infrastructure/adapter/primary/web/
│   ├── controller/
│   │   └── InternalApplicationAdministratorController.java                       [N] -- activo
│   ├── dto/request/raw/
│   │   ├── RegisterApplicationWithFirstAdministratorRawRequest.java               [N]
│   │   └── AssignApplicationAdministratorRawRequest.java                          [N]
│   ├── dto/response/
│   │   └── ApplicationRegisteredWebResponse.java                                  [N]  -- misma forma que la de `applications`, dueño distinto
│   ├── interactor/
│   │   ├── RegisterApplicationWithFirstAdministratorInteractor.java               [N] -- listo, sin controller que lo active aun
│   │   ├── AssignApplicationAdministratorInteractor.java                          [N]
│   │   └── impl/
│   │       ├── RegisterApplicationWithFirstAdministratorInteractorImpl.java       [N]
│   │       └── AssignApplicationAdministratorInteractorImpl.java                  [N]
│   └── mapper/
│       ├── RegisterApplicationWithFirstAdministratorRequestMapper.java            [N]
│       └── ApplicationRegisteredResponseMapper.java                               [N]
├── package-info.java                                                             [M]  -- allowedDependencies (§7)
└── infrastructure/config/AssignmentsConfiguration.java                            [M]  -- nuevos @Bean

pdp/roles/
└── application/secondaryport/repository/RoleRepository.java                       [M]  -- findByNameInScope

pdp/roles/infrastructure/adapter/secondary/persistence/repository/SurrealRoleRepository.java  [M]
                                                                                    -- stub UnsupportedOperationException del método nuevo

pdp/authorization/
├── application/usecase/
│   ├── AdministerApplicationRemovalUseCase.java                                   [N]
│   ├── AdministerApplicationCredentialRotationUseCase.java                        [N]
│   └── impl/
│       ├── AdministerApplicationRemovalUseCaseImpl.java                           [N]
│       └── AdministerApplicationCredentialRotationUseCaseImpl.java                [N]
├── infrastructure/adapter/primary/web/
│   ├── controller/ApplicationAdministrationController.java                        [N] -- solo DELETE activo
│   ├── dto/request/raw/ApplicationAdministrationRawRequest.java                   [N]
│   ├── dto/response/AdministeredApplicationWebResponse.java                       [N]
│   ├── interactor/
│   │   ├── ApplicationRemovalInteractor.java                                      [N] -- activo
│   │   ├── ApplicationCredentialRotationInteractor.java                          [N] -- listo, sin controller que lo active aun
│   │   └── impl/ (ambas Impl)                                                     [N]
│   └── mapper/{ApplicationAdministrationRequestMapper,AdministeredApplicationResponseMapper}.java [N]
├── package-info.java                                                             [M]  -- allowedDependencies (§7)
└── infrastructure/config/AuthorizationConfiguration.java                          [M]  -- nuevos @Bean

pdp/applications/
├── infrastructure/adapter/primary/web/controller/ApplicationController.java       [M]  -- pierde register() y rotate(); solo queda list() -- PENDIENTE (ver §11.3)
├── infrastructure/adapter/primary/web/interactor/RegisterApplicationInteractor.java (+Impl)         [M-eliminar, obsoleto tras el traslado]
├── infrastructure/adapter/primary/web/interactor/RotateApplicationCredentialInteractor.java (+Impl) [M-eliminar, obsoleto tras el traslado]
└── infrastructure/config/ApplicationsConfiguration.java                          [M]  -- retira los beans de los dos interactores eliminados
```

> `RegisterApplicationUseCase`, `RemoveApplicationUseCase`, `RotateApplicationCredentialUseCase` y
> sus `Impl` (todos en `applications`) **no aparecen en este árbol**: no cambian una sola línea.

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `application` (`assignments`) | `RegisterApplicationWithFirstAdministratorUseCaseImplTests` | camino feliz (verifica las tres llamadas en orden vía fakes capturadores); propaga el error si `RegisterApplicationUseCase` falla (no llama a los otros dos) |
| `application` (`assignments`) | `AssignApplicationAdministratorUseCaseImplTests` | rol `ADMIN` no existe → lo crea y asigna; rol `ADMIN` ya existe → lo reutiliza y solo asigna; aplicación inexistente → `ApplicationNotFoundException` |
| `application` (`authorization`) | `AdministerApplicationRemovalUseCaseImplTests` | administrador → delega en `RemoveApplicationUseCase`; no administrador → `NotAuthorizedToAdministerException`, sin llamar a `RemoveApplicationUseCase` |
| `application` (`authorization`) | `AdministerApplicationCredentialRotationUseCaseImplTests` | mismos dos casos que arriba, afirmando también el `ApplicationRegistrationResponse` devuelto |
| `infrastructure` (`assignments`) | `RegisterApplicationWithFirstAdministratorRequestMapperTests`, `AssignApplicationAdministratorRequestMapperTests`(vía interactor) | campo ausente/mal formado/válido, patrón estándar |
| `infrastructure` (`assignments`) | `InternalApplicationAdministratorControllerTests` | delega al interactor, responde el código esperado |
| `infrastructure` (`authorization`) | `ApplicationAdministrationControllerTests` | `DELETE` → 200 sin cuerpo |
| `infrastructure` (`roles`) | Extiende `SurrealRepositoryIntegrationTests` o equivalente | `findByNameInScope`: encontrado / vacío (Testcontainers) |

Presupuesto total estimado: ~22-26 pruebas (historia grande por el reparto en tres módulos, no por
complejidad de cada pieza individual).

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado (con enmiendas §13, §14, §15) | 2026-09-13 |
| Contrato aprobado (gate 1) | ✅ Aprobado, tras las tres enmiendas | 2026-09-13 |
| Pruebas en rojo | ✅ SPEC materializada, rojo por `UnsupportedOperationException` | 2026-09-13 |
| Implementación en verde | ✅ Verde | 2026-09-13 |
| Validación | ✅ Los cuatro juicios en verde en la 2ª vuelta; único bloqueante (deriva doc↔código) corregido el mismo día — ver el addendum en REPORTE-HU-015.md | 2026-09-13 |
| Entrega (gate 2) | ✅ Fusionado a `develop` (PR #48) + fix de índice único y de cobertura sobre `develop` (commits `56891ca`, `ff4774f`) | 2026-09-13 |

## 11. Ambigüedades pendientes

1. **`PdpPrincipal.userId()` es `Optional` y solo se llena de forma garantizada bajo el perfil
   `keycloak`** (el camino `LocalUserPrincipal`, que es el único que corre en producción). Bajo el
   perfil `dev` (JWT HMAC sin BFF), puede llegar vacío. `RegisterApplicationWithFirstAdministratorInteractorImpl`
   necesita un `UserId` concreto para el alta del primer administrador — si llega vacío, el
   implementador debe decidir entre `.orElseThrow(...)` (falla explícita en `dev`, nunca en
   producción real) o degradar a "sin alta automática, se documenta en el log". Recomiendo la
   primera: es una limitación preexistente de un perfil de desarrollo, no algo que esta historia
   deba resolver. No bloquea el gate 1: es una decisión de implementación, no de contrato.
2. **Nombre del controlador HTTP para `assignments` y `authorization`.** Elegí nombres descriptivos
   (`ApplicationRegistrationController`, `ApplicationAdministrationController`) sin precedente
   exacto en el proyecto (es la primera vez que dos módulos comparten la propiedad del mismo path
   base `/api/v1/applications`). Si al implementar aparece una colisión de nombres de beans u otra
   fricción de Spring, es un detalle de implementación, no un cambio de contrato.
3. **Dos `@PostMapping` quedaron deliberadamente sin activar.** `RegisterApplicationWithFirstAdministratorInteractor`
   (en `assignments`) y `ApplicationCredentialRotationInteractor` (en `authorization`) están
   completos como esqueleto e inyectados en `{Slice}Configuration`, pero **no** los expone ningún
   controlador todavía: `ApplicationController` (en `applications`) sigue sirviendo
   `POST /api/v1/applications` y `POST /api/v1/applications/{id}/credential-rotations` sin gate. No
   materialicé la relocación completa porque activar el mapeo nuevo sin retirar el viejo produciría
   `AmbiguousMappingException` al arrancar el contexto de Spring (dos `@PostMapping` en la misma
   ruta) — no lo dejé llegar a probarse, lo evité desde el diseño. Retirar `register()`/`rotate()` de
   `ApplicationController` rompe `ApplicationControllerTests`/`ApplicationHttpTests`, que son
   `pdp/src/test` — tocarlos es lo único que este plan no puede hacer. **Es el primer paso literal
   del implementador**: en un solo commit, quitar esos dos métodos (+ sus interactores/beans en
   `applications`, ahora huérfanos) de `ApplicationController`/`ApplicationsConfiguration` y añadir
   los `@PostMapping` que faltan a `ApplicationRegistrationController`/`ApplicationAdministrationController`,
   ajustando esos dos archivos de test en el mismo cambio (exactamente como sb-testing describe para
   un cambio de puerto: "es trabajo del implementador, no del planificador").

## 14. Enmienda de contrato (gate 1, segunda vuelta) — resolver el `UserId` del llamador vía `identity`

Al intentar activar los endpoints (corrección del bloqueante del REPORTE-HU-015.md), `@3-implementador`
encontró que el diseño original — `principal.userId().orElseThrow(...)` en
`RegisterApplicationWithFirstAdministratorInteractorImpl` y `ApplicationAdministrationRequestMapper` —
**rompe las 7 clases `*HttpTests` del proyecto** (`ApplicationHttpTests`, `AssignmentHttpTests`,
`ProfileAssignmentHttpTests`, `AuthorizationHttpTests`, `ProfileHttpTests`,
`ApplicationWithInitialResourceHttpTests`, `RoleHttpTests`): todas se autentican con
`TestJwtSupport` (JWT crudo, perfil dev), que `SecurityContext.currentPrincipal()` resuelve como
`PdpPrincipal.from(jwt)` — **siempre** con `userId = Optional.empty()`. Solo
`LocalUserPrincipal` (que construye `ProvisionIdentityUseCaseImpl` tras un login real vía Keycloak)
trae el `userId` ya resuelto, y ninguna prueba HTTP pasa por ahí. La ambigüedad 1 del borrador original
decía "solo pasa fuera del perfil `keycloak`, nunca en producción real" — cierto para producción, pero
la ambigüedad no contempló que es **el único modo que cualquier prueba HTTP del proyecto ejercita**.

**Decisión (con Sebastián):** el `UserId` del llamador se resuelve vía `identity` cuando el principal no
lo trae ya resuelto, en vez de exigirlo del principal. Esto cambia dos firmas ya aprobadas en la
sección 7 — de ahí la segunda vuelta por el gate 1, en vez de que el implementador lo decidiera solo.

### Piezas nuevas

```java
// pdp/identity/application/secondaryport/repository/SecurityUserRepository.java              [M]
/** Vacío si no hay identidad externa con ese subject, sin importar el emisor. Complemento de
 *  findIdentity(issuer, subject) para cuando el principal no trae el issuer (JWT crudo de dev/pruebas,
 *  ver ambigüedad 1 y su enmienda). */
Mono<ExternalIdentity> findIdentityBySubject(String subject);
```

```java
// pdp/identity/application/rule/validator/SubjectUserIdLookupValidator.java                   [N]
public interface SubjectUserIdLookupValidator extends ReactiveOperation<String, UserId> {
}
// Impl: repository.findIdentityBySubject(subject).map(ExternalIdentity::userId)
```

`identity/application/rule/validator` ya es `@NamedInterface("rule")` con consumidores (`assignments`
la tiene permitida) — publicar aquí no dispara la trampa del primer NamedInterface. **`authorization`
no tenía ninguna dependencia hacia `identity` todavía**: se añade `"identity :: rule"` a su
`allowedDependencies` (frontera nueva, justificada aquí, no relajada para que compile).

### Firmas modificadas (ya aprobadas antes, cambian ahora)

```java
// pdp/assignments/infrastructure/adapter/primary/web/interactor/impl/
// RegisterApplicationWithFirstAdministratorInteractorImpl.java                                [M]
// Constructor gana: SubjectUserIdLookupValidator subjectUserIdLookup
// execute(): principal.userId() presente -> Mono.just(it); si no, subjectUserIdLookup.execute(principal.subject())
// El resto de la orquestación (mapper, use case) no cambia.
```

```java
// pdp/authorization/infrastructure/adapter/primary/web/mapper/
// ApplicationAdministrationRequestMapper.java                                                 [M]
// ANTES: toAdministrationRequest(ApplicationAdministrationRawRequest raw, PdpPrincipal principal)
//        — resolvía el UserId internamente con orElseThrow.
// AHORA: toAdministrationRequest(ApplicationAdministrationRawRequest raw, PdpPrincipal principal,
//        UserId resolvedUserId) — sigue síncrono; ya no decide de dónde sale el UserId, solo lo recibe.
```

```java
// ApplicationRemovalInteractorImpl.java / ApplicationCredentialRotationInteractorImpl.java     [M]
// Ambos ganan: SubjectUserIdLookupValidator subjectUserIdLookup
// execute(): resuelven el UserId igual que el interactor de registro, ANTES de llamar al mapper
//            (ya no síncrono de punta a punta: SecurityContext.currentPrincipal()
//             .flatMap(principal -> resolveUserId(principal).map(userId ->
//                 ApplicationAdministrationRequestMapper.toAdministrationRequest(raw, principal, userId)))
//             .flatMap(useCase::execute)...)
```

### Impacto en pruebas ya escritas (para `@2-tester-spec`, no para `@3-implementador`)

- **Fakes de `SecurityUserRepository`** — confirmado con `mvnw test-compile` tras materializar el
  método nuevo: exactamente 3 archivos, `UserMustExistValidatorImplTests`,
  `AssignTenantUseCaseImplTests`, `ProvisionIdentityUseCaseImplTests`. Necesitan
  `findIdentityBySubject` con `throw new UnsupportedOperationException();`.
- **`ApplicationAdministrationRequestMapperTests.fails_fast_when_the_principal_has_no_resolved_user_id`**
  ya no tiene sentido con la firma nueva (el mapper ya no decide, solo recibe) — se retira o se
  reescribe contra la firma nueva. Es un cambio de contrato aprobado aquí, no una "prueba discutible".
- Nuevo caso de prueba: `SubjectUserIdLookupValidatorImplTests` (encontrado / vacío).
- Sigue pendiente, sin relación con esta enmienda: el bloqueante original del REPORTE-HU-015.md
  (`ApplicationControllerTests`/`ApplicationHttpTests` por el traslado de `register()`/`rotate()`).

## 15. Enmienda de contrato (gate 1, tercera vuelta) — fixture de identidad para las pruebas HTTP

Al implementar `findIdentityBySubject` (§14), `@3-implementador` confirmó que la consulta funciona
—y que además había un bug real: la cadena resolvía vacía en silencio en vez de fallar, produciendo
un `200 OK` sin cuerpo en vez de un error; ya corregido con `switchIfEmpty(Mono.error(...))`— pero
**ninguna de las 7 clases `*HttpTests` del proyecto vincula jamás una identidad externa** para el
`subject` con el que firman su JWT de prueba (`grep -c '\.linkIdentity(' pdp/src/test/**/*HttpTests.java`
→ 0 en las 7). Activar el registro/rotación gateados las deja en rojo con
`IllegalStateException: No fue posible resolver el UserId del llamador` — no un caso límite, el 100 %
de sus llamadas, porque `SubjectUserIdLookupValidator` siempre resuelve vacío para un subject que
nadie vinculó.

**Decisión de diseño (por qué aquí y no en cada clase por separado):** el fixture vive en
`AbstractSurrealDbIntegrationTest` — la base que **ya** comparten las 7 clases y que **ya** expone el
contenedor `SURREALDB` (mismo patrón sin Spring que usa `SurrealRepositoryIntegrationTests` para
construir un `SurrealDbClient` a mano) — en vez de duplicar la construcción del repositorio en cada
`@BeforeEach`. Es un método nuevo, aditivo, que no cambia el comportamiento de ninguna prueba que no
lo llame — su alcance real es más ancho que HU-015 (lo hereda cualquier prueba HTTP futura que
necesite un llamador con `UserId` resoluble), pero nace de una necesidad de esta historia.

### Pieza nueva

```java
// pdp/src/test/java/co/edu/uco/seguridad/AbstractSurrealDbIntegrationTest.java                [M-test, aditivo]
/**
 * Vincula una identidad externa para que {@code SubjectUserIdLookupValidator} (HU-015 §14) pueda
 * resolver el {@code UserId} de un JWT crudo de prueba —el único principal que produce
 * {@code TestJwtSupport}, que nunca trae {@code userId} ya resuelto—. Sin Spring: construye su
 * propio {@code SurrealDbClient} contra el mismo contenedor, igual que
 * {@code SurrealRepositoryIntegrationTests}. Llamar una vez por prueba/clase, con un {@code subject}
 * único (la base no se limpia entre corridas).
 */
protected static UserId linkTestIdentity(TenantId tenant, String subject) {
    UserId userId = new UserId(UUID.randomUUID());
    SurrealDbClient client = /* mismo client que ya construye SurrealRepositoryIntegrationTests */;
    SecurityUserRepository repository = new SurrealSecurityUserRepository(client);
    repository.save(SecurityUser.provision(userId, tenant, new Email(subject.replaceAll("[^a-zA-Z0-9]", "") + "@test.local"),
                    "Test User", Instant.now()))
            .then(repository.linkIdentity(new ExternalIdentity(userId, TestJwtSupport.ISSUER, subject, "test")))
            .block();
    return userId;
}
```

### Consumidores a ajustar (las 7 clases, `@2-tester-spec`)

Cada `*HttpTests` llama `linkTestIdentity(tenant, subject)` en su `@BeforeEach`/fixture, con el mismo
`subject` que ya usa al firmar su token con `TestJwtSupport.signedToken(tenant, subject)` — no se
inventa un subject nuevo, se vincula el que cada clase ya usaba. Las 7:
`ApplicationHttpTests`, `AssignmentHttpTests`, `ProfileAssignmentHttpTests`, `AuthorizationHttpTests`,
`ProfileHttpTests`, `ApplicationWithInitialResourceHttpTests`, `RoleHttpTests`.

## 13. Corrección encontrada por `ModulithStructureTests` (FASE 3 de `@2-tester-spec`)

Al correr `verificar.ps1 -Rapido` con las pruebas ya escritas, `ModulithStructureTests` rechazó dos
dependencias que el plan original no vio (no se detectan compilando, solo verificando):

1. `AssignApplicationAdministratorUseCaseImpl` y `AssignmentsConfiguration` consultaban
   `RoleRepository` (secondaryport de `roles`) **directamente** — nunca exportado, y además una
   violación de `sb-arquitectura` regla invariante 11 ("una decisión sobre otro módulo se consume
   como validador publicado, nunca consultando su repositorio"). **Corregido**: nuevo
   `RoleNameInScopeQuery` (dto) + `RoleLookupByNameInScopeValidator` (+Impl) publicados en
   `roles :: rule` (ya permitido en `assignments`, no hizo falta tocar ningún `allowedDependencies`
   nuevo) — mismo patrón que `ApplicationOwnerLookupValidator`.
2. `AdministeredApplicationResponseMapper` (en `authorization`) leía `ApplicationBaseUrl`
   (`applications :: model`) sin que ese NamedInterface estuviera en el `allowedDependencies` de
   `authorization` — se me olvidó añadirlo en la FASE 5 original (sí lo añadí a `assignments`, no a
   `authorization`). **Corregido**: `"applications :: model"` añadido a
   `authorization/package-info.java`.

Ambas correcciones son plomería (nuevas dependencias permitidas y un validador publicado, cero
lógica de negocio) — no cambian ninguna decisión de las secciones 0-6. `ModulithStructureTests`
está en verde después de ambas.

## 12. Verificado al cerrar FASE 5

`RoleRepository.findByNameInScope` (nueva, §7) rompe la compilación de **8 clases de prueba** del
módulo `roles` que la doblan como clase anónima — exactamente el efecto documentado en `sb-testing`
("Al cambiar la firma de un puerto"), mismo patrón que `findTenantIdById` en HU-003 (6 archivos).
Es trabajo de `@2-tester-spec`/`@3-implementador`, no de este plan: cada fake necesita el método
nuevo con `throw new UnsupportedOperationException();`, salvo la prueba que lo ejercite de verdad.

Archivos afectados (confirmado con `mvnw test-compile`):
`DefineRoleRulesValidatorImplTests`, `GrantResourceRulesValidatorImplTests`,
`RoleMustExistForTenantValidatorImplTests`, `RoleNamesLookupValidatorImplTests`,
`RoleScopeMustCoverApplicationValidatorImplTests`, `DefineRoleUseCaseImplTests`,
`GrantResourceToRoleUseCaseImplTests`, `ListRolesUseCaseImplTests`.

**Actualización tras traer `develop` (2026-09-13, PR #47 de OPA-policies):** al actualizar esta rama
contra `develop` aparecieron **5 fallos adicionales de test-compile, preexistentes en `develop`
mismo, sin relación con HU-015**: `ProtectedResourceRepository.findIdByApplicationPathAndMethod`
(añadido por esa PR para que `AuthorizationContextResolverImpl` resuelva entitlements reales) rompe
5 fakes de `resources` (`ProtectedResourceMustExistValidatorTests`,
`ProtectedResourceOwnerLookupValidatorImplTests`, `RegisterProtectedResourceRulesValidatorTests`,
`RegisterProtectedResourceUseCaseImplTests` ×2) de la misma forma que `findByNameInScope` rompe los
de `roles`. Verificado que `mvnw compile` (sin tests) sigue en verde con o sin los cambios de
HU-015 — el rojo de `test-compile` en esta rama es la suma de dos efectos independientes: el de
HU-015 (`roles`, 8 archivos) y el que ya traía `develop` (`resources`, 5 archivos, ajeno a esta
historia). El resto de la SPEC de HU-015 (todas las piezas `[N]` de `assignments` y `authorization`,
más los cambios de `package-info.java`) compila limpio.
