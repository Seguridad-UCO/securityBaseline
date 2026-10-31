# PLAN: HU-036 — Quién puede crear y editar roles y perfiles de alcance TENANT

## Metadata

- **ID:** HU-036
- **Slice:** `authorization` (validador nuevo + cableado). Consume `assignments` por sus interfaces nombradas ya declaradas; no toca `roles`, `profiles` ni `assignments`.
- **Tipo:** Escritura (gate de las escrituras de catálogo de alcance `TENANT`, individuales y masivas)
- **Fecha:** 2026-10-30
- **Rama:** `fix/HU-036-gate-catalogo-alcance-tenant` (apilada sobre HU-035)
- **Fuentes:** `workspace/HU-036.md` (incluye la sección «Decisiones tomadas», tratadas como CERRADAS); `workspace/PLAN-HU-035.md` §11 y `reportes/REPORTE-HU-035.md` («Límites conocidos»); `PLAN-HU-016.md` y `PLAN-HU-019.md` (decisión «TENANT sin gate, cero regresión» que esta historia revierte); ADR-023 (modelo de administración, fail-closed), ADR-024 (administrador global, diferido), ADR-025 (zero trust), ADR-027 (MFA step-up), ADR-014 (matriz HTTP); código leído: `AdministerRoleDefinitionUseCaseImpl`, `AdministerProfileDefinitionUseCaseImpl`, `AdministerResourceGrantUseCaseImpl`, `AdministerProfileRoleAdditionUseCaseImpl`, sus cuatro interactores y requests, `RoleAdministrationController`, `ProfileAdministrationController`, `CatalogMaintenanceController`, `BulkResourceController`, `BulkCatalogImportService`, `PrincipalMustBeApplicationAdministratorValidator` (+ `Impl` y `MfaAwareApplicationAdministratorValidator`), `AuthorizeAdministrationUseCaseImpl`, `ResolveUserApplicationsUseCase(Impl)`, `ReadCurrentUserApplicationsUseCaseImpl`, `AdministrationEvent`/`AdministrationOperation`, `ApiErrorHandler`, `AuthorizationConfiguration`, `authorization/package-info.java`; front: `securityBaseline-vue/src/api/client.ts`, `security-ui/src/security-api.ts`, `securityBaseline-fr/src/api.js`. Sin event storming propio: es una política transversal de ADR-023/024, no un comando de un BC.
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 5, 9, 11, 12, 20, 21, 22 y 23. (6, 13, 14 y 15 no: no hay parámetros ni VOs nuevos; 7 y 16–19 no: no hay persistencia ni consulta paginada nueva.)

## 1. Resumen funcional

Hoy un rol o perfil de alcance `TENANT` —visible para todas las aplicaciones del inquilino— se crea, se edita y se borra sin ningún gate: el use case recibe `administration` vacío y delega directo. Esta historia lo cierra con **un único validador reutilizable** (`PrincipalMustAdministerAtLeastOneApplicationValidator`) que evalúa «el principal administra al menos una aplicación del inquilino» reutilizando `ResolveUserApplicationsUseCase` y el gate por aplicación ya existente (con su MFA), y lo cablea en **todas** las vías que escriben catálogo `TENANT`: definición individual de rol y de perfil, concesión/revocación de recursos a un rol, alta/baja de roles de un perfil, `PATCH`/`DELETE` de roles y perfiles, y la carga masiva (filas `TENANT`).

Es un actor **intermedio** (ADR-036): el administrador global de ADR-024 sigue siendo el estado final y se activará en su propia historia sustituyendo la implementación de este validador, no su contrato.
**No cubre:** roles/perfiles de alcance `APPLICATION` (ya gateados), el modelo de asignaciones, el administrador global, auditoría de la carga masiva ni de `PATCH`/`DELETE` (ver §6.4), migración de datos (no hay), ni cambios en repos hermanos (ADR-036 se entrega como borrador en §12; el front se revisó, ver §6.6).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| C1 | Principal que no administra ninguna aplicación del inquilino crea un **rol** `TENANT` (`POST /api/v1/roles`, `scope=TENANT`) | `403` `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG`; no se llama a `DefineRoleUseCase` (no se sondean duplicados) |
| C2 | Ídem con un **perfil** `TENANT` (`POST /api/v1/profiles`) | `403` igual |
| C3 | Ídem con las demás vías de edición: conceder/revocar recurso a rol `TENANT`, agregar/quitar rol de perfil `TENANT`, `PATCH`/`DELETE` de rol y perfil `TENANT` | `403` igual; la escritura no ocurre |
| C4 | Carga masiva (`POST /resources/bulk/catalog/{mode}` y `/validate`) con filas `TENANT` y principal sin actor | `200` con `ready=false`; error **por fila** `NOT_AUTHORIZED` (campo `Alcance*`); nada se guarda en `commit` |
| C5 | Principal que administra ≥ 1 aplicación del inquilino, con MFA válido | La operación procede igual que hoy (`201`/`200`); en masiva, `ready=true` |
| C6 | Principal que administra una aplicación pero sin evidencia de MFA | `403` `MFA_REQUIRED` (individual); `NOT_AUTHORIZED` por fila (masiva, colapsado — paridad HU-035) |
| C7 | Fail-closed | Error del gate de una aplicación, `DENY`, `INDETERMINATE`, principal sin aplicaciones o fallo al resolverlas ⇒ no autoriza. Nunca un `500` por esto |
| C8 | Evaluación | Aplicaciones del principal en orden estable (id ascendente); consulta secuencial; corte al **primer permiso** |
| C9 | Aislamiento por inquilino | Solo cuentan las aplicaciones del inquilino del principal (`ResolveUserApplicationsRequest(userId, principal.tenantId())`); `AdministrationRequest` lleva ese mismo inquilino |
| C10 | El gate precede a cualquier otra comprobación | Individual: antes de `Define*UseCase`; masiva: antes de `DUPLICATE_*` (no se puede sondear qué existe) |
| C11 | Auditoría | `ROLE_DEFINED`/`PROFILE_DEFINED`/`RESOURCE_GRANTED|REVOKED`/`PROFILE_ROLE_ADDED|REMOVED` también en la rama `TENANT`, con el `applicationId` que concedió el permiso (`ALLOWED`) o la primera aplicación evaluada (`DENIED`); sin evento si no hay aplicación a la que atribuir (ver §6.3) |
| C12 | Roles y perfiles `TENANT` **existentes** | Siguen resolviéndose en autorización y en «mis aplicaciones» (el cambio solo afecta a **definir/editar**; sin migración) |
| C13 | Sin regresión en alcance `APPLICATION` | Mismo gate, misma auditoría, mismos códigos que hoy |
| C14 | Arquitectura | `LayeredArchitectureTests`, `ModulithStructureTests` (sin tocar `allowedDependencies`), `consistencia.ps1` y `drift.ps1` en verde |
| C15 | `verificar.ps1` completo en verde | Ver §10 (en esta herramienta solo se pudo validar compilación y la parte sin sockets) |

## 3. Reglas de negocio

No hay `Rule` de dominio nueva: la restricción de fondo («administrar una aplicación») ya la decide OPA a través del gate existente (ADR-023: nada de «ADMIN» en Java). Lo nuevo es una **composición** de decisiones ya tomadas por ese gate, que necesita E/S (resolver las aplicaciones del sujeto), así que vive como validador reactivo en `application/rule/validator`.

| # | Regla | Dónde vive | Puerto / dato que trae | Resultado |
|---|---|---|---|---|
| R1 | Quien crea/edita catálogo `TENANT` debe administrar ≥ 1 aplicación del inquilino | `PrincipalMustAdministerAtLeastOneApplicationValidator` (**nuevo**, publicado en `authorization :: rule`) | `ResolveUserApplicationsUseCase` (assignments) + `PrincipalMustBeApplicationAdministratorValidator` (gate ya decorado con MFA) | Emite el `ApplicationId` que concedió el permiso; si ninguna: `MfaEvidenceRequiredException` (403) o `NotAuthorizedToAdministerTenantCatalogException` (403) |
| R2 | Quien crea/edita catálogo `APPLICATION` debe administrar esa aplicación | `PrincipalMustBeApplicationAdministratorValidator` (existente) | gate OPA | `NotAuthorizedToAdministerException` / `MfaEvidenceRequiredException` (403) — sin cambios |

> Los casos de uso **no** llevan `if/throw`: el único bifurcador es el `switch` exhaustivo sobre `CatalogAdministration` (tipo sellado), que es despacho estructural, no regla de negocio.

### 3.1 Algoritmo de R1 (contrato del implementador; las pruebas se escriben contra esto)

Entrada: `TenantAdministrationRequest(tenantId, subjectUserId, subject, authenticationContext)`.

1. `userApplications.execute(new ResolveUserApplicationsRequest(subjectUserId, tenantId))` → claves de `assignmentsByApplication()`. **Orden:** ascendente por `applicationId.value().toString()` (el mapa devuelto no tiene orden, y un orden estable hace el comportamiento y las pruebas deterministas).
2. Por cada aplicación, **secuencialmente** (`concatMap`, nunca en paralelo) y con corte al primer permiso: `applicationAdministrator.execute(new AdministrationRequest(tenantId, application, subjectUserId, subject, Set.of(), authenticationContext))`.
   - Completa vacío ⇒ **permiso**: se emite esa `ApplicationId` y se deja de consultar.
   - `MfaEvidenceRequiredException` ⇒ no autoriza; se recuerda **la primera** y se continúa.
   - Cualquier otro error (`NotAuthorizedToAdministerException`, red, `RuntimeException`) ⇒ no autoriza; se continúa. (El gate ya traduce `DENY`/`INDETERMINATE` a `NotAuthorizedToAdministerException`; un error de transporte llega aquí como lo que sea y cuenta como «no autoriza».)
3. Si ninguna autorizó:
   - alguna exigió MFA ⇒ se propaga **esa misma** `MfaEvidenceRequiredException` (código `MFA_REQUIRED`);
   - si no ⇒ `NotAuthorizedToAdministerTenantCatalogException(tenantId, primeraAplicaciónEvaluada)` (`Optional.empty()` si el sujeto no tenía aplicaciones).
4. Si `ResolveUserApplicationsUseCase` falla ⇒ `NotAuthorizedToAdministerTenantCatalogException(tenantId, Optional.empty())` (fail-closed: nunca un `500` ni un permiso por defecto).
5. El estado (la primera MFA, la primera aplicación evaluada) es **local a cada ejecución** (`Mono.defer`), nunca un campo del validador: el bean es singleton.
6. No se aprovecha que el MFA es de sesión para cortar antes: el validador no asume cómo decide el gate por dentro (si el MFA pasara a ser por aplicación, cortar sería un error). Coste conocido: hasta N consultas a OPA para un sujeto sin permiso (ver §11, R3).

## 4. Modelo de dominio afectado

Sin entidades, VOs ni enums nuevos. `RoleScope`/`RoleScopeLevel` no cambian (el alcance `GLOBAL` sigue rechazado por los mappers con `400`, y por la masiva con `INVALID_VALUE`).

Tipos nuevos de `application` (DTOs de entrada) y de `domain/exception`:

| Tipo | Capa | Descripción |
|---|---|---|
| `TenantAdministrationRequest` | application/primaryport/request | Quién pregunta, sin aplicación: `(TenantId, UserId subjectUserId, String subject, AuthenticationContextEvidence)` |
| `CatalogAdministration` | application/primaryport/request | `sealed interface` con `OfApplication(AdministrationRequest)` y `OfTenant(TenantAdministrationRequest)`. **Reemplaza** a `Optional<AdministrationRequest>` en las cuatro requests de §7: se elimina el estado «sin gate» |
| `NotAuthorizedToAdministerTenantCatalogException` | domain/exception | `BusinessRuleViolationException`, código `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG`; lleva `Optional<ApplicationId> evaluatedApplication` para la auditoría |

## 5. Persistencia

No aplica. Sin tablas, índices ni consultas nuevas en puertos. `AdministrationEvent` no cambia (su `applicationId` sigue siendo obligatorio; por eso la atribución de §6.3).

## 6. Endpoints — contrato resultante

Ninguna ruta nueva ni cambia ningún verbo. Cambia **quién puede** invocar las existentes cuando el objetivo es `TENANT`. El inquilino sale siempre del principal.

### 6.1 Vías encontradas (inventario completo)

Búsqueda: `DefineRoleUseCase`, `UpdateRoleUseCase`, `RemoveRoleUseCase`, `DefineProfileUseCase`, `UpdateProfileUseCase`, `RemoveProfileUseCase`, `AddRoleToProfileUseCase`, `RemoveRoleFromProfileUseCase`, `GrantResourceToRoleUseCase`, `RevokeResourceFromRoleUseCase`, `saveAllAtomically`, `RoleScope.ofTenant(`, y los controladores de `roles`, `profiles` y `authorization`.

| # | Vía | Entrada HTTP | Camino | Gate hoy (TENANT) | Gate tras HU-036 |
|---|---|---|---|---|---|
| V1 | Crear rol | `POST /api/v1/roles` | `RoleAdministrationController.define` → `AdministerRoleDefinitionInteractorImpl` → `AdministerRoleDefinitionUseCaseImpl` | ninguno | R1 |
| V2 | Crear perfil | `POST /api/v1/profiles` | `ProfileAdministrationController.define` → `AdministerProfileDefinitionInteractorImpl` → `AdministerProfileDefinitionUseCaseImpl` | ninguno | R1 |
| V3 | Conceder/revocar recurso a rol (edita el rol) | `POST /roles/{id}/resources`, `POST /roles/{id}/resources/batch`, `PUT /roles/{id}/resources`, `DELETE /roles/{id}/resources/{resourceId}` | `RoleAdministrationController` → `AdministerResourceGrantInteractorImpl` → `AdministerResourceGrantUseCaseImpl` | ninguno (PLAN-HU-016 §2 caso 6) | R1 |
| V4 | Agregar/quitar rol de un perfil (edita el perfil) | `POST /profiles/{id}/roles`, `DELETE /profiles/{id}/roles/{roleId}` | `ProfileAdministrationController` → `AdministerProfileRoleAdditionInteractorImpl` → `AdministerProfileRoleAdditionUseCaseImpl` | ninguno (PLAN-HU-019 caso 6) | R1 |
| V5 | Renombrar / borrar rol y perfil | `PATCH`+`DELETE /roles/{id}`, `PATCH`+`DELETE /profiles/{id}` | `CatalogMaintenanceController` (llama a `UpdateRoleUseCase`/`RemoveRoleUseCase`/`UpdateProfileUseCase`/`RemoveProfileUseCase` tras `gateOptional`) | `gateOptional(vacío)` ⇒ ninguno | R1 |
| V6 | Carga masiva | `POST /resources/bulk/catalog/{mode}/validate`, `POST /resources/bulk/catalog/{mode}`, `POST .../error-report` (este último solo valida) | `BulkResourceController` → `BulkCatalogImportService.validate/commit` | ninguno para filas `TENANT` (HU-035 §11.1) | R1, memoizado: ≤ 1 consulta por llamada |

**Verificado que NO son vías:** `RoleController` y `ProfileController` (módulos `roles`/`profiles`) solo exponen `GET`; `AssignApplicationAdministratorUseCaseImpl` y `RegisterApplicationWithFirstAdministratorUseCaseImpl` definen el rol `ADMIN` de alcance `APPLICATION`; `RoleScope.ofTenant(` solo aparece en los mappers de persistencia, los mappers/interactores de definición y la masiva; no hay semilla ni inicializador que cree roles; `BulkResourceController.catalogTemplate` solo lee. Las asignaciones de rol/perfil (`AssignRole*`, `AssignProfile*`) ya se gatean por la aplicación destino y quedan fuera (alcance de la historia).

> **Ampliación de alcance sobre el enunciado literal («crear/editar»)** — decisión del planificador (§11, P1): V3–V5 son edición de un rol/perfil `TENANT` y comparten primitiva con V1/V2. Dejarlas abiertas mantiene la puerta lateral que esta historia existe para cerrar (HU-035 §11.1 ya señalaba la concesión de recursos). Si en el Gate 1 se prefiere una historia más estrecha, V3–V5 son separables sin tocar el validador: basta no cablearlas.

### 6.2 Errores HTTP

| Situación | Excepción | Código | HTTP |
|---|---|---|---|
| Ninguna aplicación del inquilino administrada / sin aplicaciones / fallo al resolver | `NotAuthorizedToAdministerTenantCatalogException` | `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` | **403** (**[M]** `ApiErrorHandler`: añadir el código a la lista de 403; hoy cualquier otro código de `BusinessRuleViolationException` caería a 400) |
| Administra pero falta MFA | `MfaEvidenceRequiredException` (existente) | `MFA_REQUIRED` | 403 (ADR-027; ya mapeado) |
| Alcance `APPLICATION` sin permiso (sin cambios) | `NotAuthorizedToAdministerException` | `NOT_AUTHORIZED_TO_ADMINISTER` | 403 |
| `UserId` del llamador no resoluble en los interactores | `IllegalStateException` (existente) | — | 500 (patrón existente, no se toca) |
| `UserId` no resoluble en `CatalogMaintenanceController` | ver §6.5 | — | 403 (hoy: gate omitido) |
| Fila `TENANT` de la masiva sin permiso | error de fila `NOT_AUTHORIZED`, campo `Alcance*` | — | 200 con `ready=false` |

Nunca se afirma un `500` en pruebas por esta historia.

### 6.3 Auditoría (decisión 5 del responsable técnico; forma exacta decidida aquí)

`AdministrationEvent` exige `applicationId` no nulo; no se cambia. Regla:

| Resultado del gate TENANT | Evento | `applicationId` |
|---|---|---|
| Permitió y la operación terminó bien | `ALLOWED` de la operación (`ROLE_DEFINED`, `PROFILE_DEFINED`, `RESOURCE_GRANTED`/`RESOURCE_REVOKED`, `PROFILE_ROLE_ADDED`/`PROFILE_ROLE_REMOVED`) | la aplicación **que concedió el permiso** |
| `NotAuthorizedToAdministerTenantCatalogException` con `evaluatedApplication` presente | `DENIED` de la operación | la **primera aplicación evaluada** (id ascendente) |
| `NotAuthorizedToAdministerTenantCatalogException` con `evaluatedApplication` vacío (sin aplicaciones, o fallo al resolverlas) | **ninguno** (no hay aplicación a la que atribuirlo) | — |
| `MfaEvidenceRequiredException` | ninguno (paridad con el flujo por aplicación, que tampoco audita MFA) | — |
| La operación falla después de permitir (p. ej. duplicado) | ninguno (paridad: hoy solo se audita éxito o `NotAuthorized…`) | — |

Semántica a documentar para quien lea el log: en las filas `TENANT`, `applicationId` significa «aplicación contra la que se evaluó el permiso», **no** «aplicación afectada» (la operación afecta a todo el inquilino). Auditar con `ids.next()`/`time.now()` inyectados (criterio 4); un fallo al escribir la auditoría no rompe la operación (patrón existente `onErrorResume`). `PATCH`/`DELETE` (V5) y la masiva (V6) **no** auditan: no hay `AdministrationOperation` para ellos ni la tenían por la vía de aplicación (paridad; límite conocido de HU-035).

### 6.4 Masiva (V6): semántica exacta

Misma que la fila `APPLICATION` de HU-035 (§6.1 de su plan), con estos puntos:

1. Orden por fila: (a) celdas obligatorias → (b) nombre y alcance → (c) coherencia alcance/aplicación (`TENANT` con `Aplicación` ⇒ `INVALID_VALUE`, **sin** consultar el gate) → (d) **gate** (`TENANT`: validador R1; `APPLICATION`: búsqueda de la aplicación + gate por aplicación) → (e) `DUPLICATE_IN_FILE` → (f) `DUPLICATE_PERSISTED`.
2. **Una sola consulta de R1 por llamada** a `validate`/`commit`, perezosa (cero consultas si no hay filas `TENANT` aceptables) y memoizada junto con el resto del estado de la llamada (`Batch`): el resultado —permiso o denegación— se reutiliza para las demás filas `TENANT` de ese archivo. Nada se comparte entre llamadas.
3. Cualquier fallo del validador (`NotAuthorized…`, `MfaEvidenceRequiredException`, error de red) ⇒ la fila `TENANT` produce `NOT_AUTHORIZED` (fail-closed). `MFA_REQUIRED` se colapsa en `NOT_AUTHORIZED`, igual que en las filas `APPLICATION` y en recursos.
4. Código/campo/mensaje: `NOT_AUTHORIZED`, campo `Alcance*`, descripción «No está autorizado para definir roles ni perfiles de alcance TENANT: debe administrar al menos una aplicación del tenant». Constante privada de `BulkCatalogImportService`, como las demás (límite conocido de HU-035: aún no hay catálogo `{Slice}Messages` para este servicio).
5. Las hojas de relación no invocan el gate: un rol/perfil `TENANT` denegado no entra a `accepted`, así que sus relaciones salen `ROLE_NOT_FOUND`/`PROFILE_NOT_FOUND` (consistente con HU-035 §6.1.7).
6. Contadores y `ready` no cambian de contrato: la fila denegada cuenta como inválida; `commit` no guarda nada si algún error.
7. `AdministrationRequest` no se usa aquí: el validador recibe `TenantAdministrationRequest(principal.tenantId(), userId, principal.subject(), principal.authenticationContext())`.

### 6.5 `CatalogMaintenanceController` (V5): además, cierra un fail-open propio

`gate(p, app)` hace `user(p).flatMap(u -> administrator.execute(...))`. Si `user(p)` completa **vacío** (principal sin `UserId` y sin identidad externa vinculada), `gate` completa vacío y el `.then(roles.execute(...))` de la cadena **se ejecuta igual**: el gate por aplicación se omite. Es un fail-open previo a esta historia que afecta a `PATCH`/`DELETE` de aplicación, recurso, rol y perfil. Como la historia reescribe exactamente esas líneas para V5, el contrato exige:

- `user(p)` pasa a **fallar cerrado**: sin `UserId` resoluble ⇒ error (para el gate TENANT: `NotAuthorizedToAdministerTenantCatalogException(p.tenantId(), Optional.empty())`; para el gate por aplicación: `NotAuthorizedToAdministerException(p.tenantId(), app)`), ambos 403. No `IllegalStateException`/500: en este controlador no se afirmaba 500 y 403 es lo coherente con «no autoriza».
- `gateOptional(p, app)` se renombra a `gateCatalog` y, con `app` vacío (alcance `TENANT`), invoca R1 en vez de completar vacío.
- La comprobación de existencia del rol/perfil (`RoleApplicationLookupValidator`/`ProfileApplicationLookupValidator`, que da 404) sigue **antes** del gate, como hoy.
- Sin auditoría (§6.3).

### 6.6 Front (decisión 6): comprobado, sin cambio en este repositorio

`securityBaseline-vue/src/api/client.ts` muestra `payload.detail` y, solo si falta, un texto genérico de 403; `securityBaseline-fr/src/api.js` muestra `detail`; `security-ui/src/security-api.ts` **ignora `detail` en los 403** y muestra siempre «No tiene permisos para administrar la seguridad de esta aplicación.». Ninguno de los tres trata `MFA_REQUIRED` de forma especial (no aparece la cadena). Conclusión: el `403` y el nuevo código ya se muestran como «sin permisos»; no hay pantalla nueva. **Pendiente en `security-ui` (repositorio hermano, fuera de esta rama):** el texto «de esta aplicación» es impreciso para el alcance tenant y podría distinguir `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` y `MFA_REQUIRED` por `error.code`. Se anota, no se toca.

## 7. SPEC — el contrato

Rutas desde `pdp/src/main/java/co/edu/uco/seguridad/`. `[N]` = materializado por el planificador como esqueleto compilable. `[M]` = firma existente que cambia; **la materializa el implementador en un solo paso con sus consumidores y pruebas** (las firmas de aquí son contrato, no se renegocian).

### [N] Piezas nuevas

```java
// pdp/authorization/application/primaryport/request/TenantAdministrationRequest.java
public record TenantAdministrationRequest(TenantId tenantId, UserId subjectUserId, String subject,
                                          AuthenticationContextEvidence authenticationContext) { }
// invariantes (constructor compacto, requireNonNull con RequiredArgumentMessages): los cuatro componentes no nulos.
// Constantes ya creadas: TENANT_ID, USER_ID, SUBJECT, AUTHENTICATION_CONTEXT_EVIDENCE existen; la del propio record es TENANT_ADMINISTRATION_REQUEST.

// pdp/authorization/application/primaryport/request/CatalogAdministration.java
public sealed interface CatalogAdministration {
    record OfApplication(AdministrationRequest administration) implements CatalogAdministration { }  // no nulo
    record OfTenant(TenantAdministrationRequest actor) implements CatalogAdministration { }          // no nulo
}

// pdp/authorization/application/rule/validator/PrincipalMustAdministerAtLeastOneApplicationValidator.java
public interface PrincipalMustAdministerAtLeastOneApplicationValidator
        extends ReactiveOperation<TenantAdministrationRequest, ApplicationId> { }
// NO extiende PrincipalMustBeApplicationAdministratorValidator (dos beans de tipos distintos: sin ambigüedad de inyección).

// pdp/authorization/application/rule/validator/impl/PrincipalMustAdministerAtLeastOneApplicationValidatorImpl.java
public final class PrincipalMustAdministerAtLeastOneApplicationValidatorImpl
        implements PrincipalMustAdministerAtLeastOneApplicationValidator {
    public PrincipalMustAdministerAtLeastOneApplicationValidatorImpl(
            ResolveUserApplicationsUseCase userApplications,
            PrincipalMustBeApplicationAdministratorValidator applicationAdministrator);
    public Mono<ApplicationId> execute(TenantAdministrationRequest input);   // algoritmo: §3.1
}

// pdp/authorization/domain/exception/NotAuthorizedToAdministerTenantCatalogException.java
public final class NotAuthorizedToAdministerTenantCatalogException extends BusinessRuleViolationException {
    public NotAuthorizedToAdministerTenantCatalogException(TenantId tenantId, Optional<ApplicationId> evaluatedApplication);
    public Optional<ApplicationId> evaluatedApplication();
}
// code = "NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG"; message = AuthorizationMessages.notAuthorizedToAdministerTenantCatalog()
```

Aditivos ya hechos (no rompen a nadie): `AuthorizationMessages.notAuthorizedToAdministerTenantCatalog()` y las constantes `PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR`, `RESOLVE_USER_APPLICATIONS_USE_CASE`, `TENANT_ADMINISTRATION_REQUEST`, `CATALOG_ADMINISTRATION` en `RequiredArgumentMessages`. Bean `principalMustAdministerAtLeastOneApplicationValidator` ya registrado en `AuthorizationConfiguration` (consume el bean `PrincipalMustBeApplicationAdministratorValidator` ya decorado con MFA, **nunca** `...ValidatorImpl` desnudo).

### [M] Requests de los cuatro casos de uso (`pdp/authorization/application/primaryport/request/`)

El componente `administration` cambia de `Optional<AdministrationRequest>` a `CatalogAdministration` (no nulo, constante `CATALOG_ADMINISTRATION`). Los constructores de compatibilidad de dos/tres argumentos se conservan con el mismo reemplazo de tipo.

```java
public record AdministerRoleDefinitionRequest(CatalogAdministration administration, DefineRoleRequest role)
public record AdministerProfileDefinitionRequest(CatalogAdministration administration, DefineProfileRequest profile)
public record AdministerResourceGrantRequest(CatalogAdministration administration, GrantResourceRequest grant, boolean revocation)
        // + AdministerResourceGrantRequest(CatalogAdministration administration, GrantResourceRequest grant)
public record AdministerProfileRoleAdditionRequest(CatalogAdministration administration, AddRoleToProfileRequest addition, boolean removal)
        // + AdministerProfileRoleAdditionRequest(CatalogAdministration administration, AddRoleToProfileRequest addition)
```

### [M] Casos de uso (`pdp/authorization/application/usecase/impl/`)

Cada constructor gana `PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication` **justo después de `mustBeAdministrator`** (`requireNonNull` con `PRINCIPAL_MUST_ADMINISTER_AT_LEAST_ONE_APPLICATION_VALIDATOR`):

```java
AdministerRoleDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
        PrincipalMustAdministerAtLeastOneApplicationValidator mustAdministerSomeApplication,
        DefineRoleUseCase defineRole, AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time)
AdministerProfileDefinitionUseCaseImpl(…mustBeAdministrator, …mustAdministerSomeApplication,
        DefineProfileUseCase defineProfile, AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time)
AdministerResourceGrantUseCaseImpl(…mustBeAdministrator, …mustAdministerSomeApplication,
        GrantResourceToRoleUseCase grantResource, RevokeResourceFromRoleUseCase revokeResource,
        AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time)
        // y el constructor de compatibilidad sin revokeResource, con el mismo parámetro nuevo en la misma posición
AdministerProfileRoleAdditionUseCaseImpl(…mustBeAdministrator, …mustAdministerSomeApplication,
        AddRoleToProfileUseCase addRoleToProfile, RemoveRoleFromProfileUseCase removeRoleFromProfile,
        AdministrationAuditRepository audit, IdentifierGenerator identifiers, TimeProvider time)
        // y el de compatibilidad sin removeRoleFromProfile
```

Semántica de `execute(input)` (idéntica en los cuatro; `Define*`/`Grant*`/`Revoke*`/`Add*`/`Remove*` es la operación propia):

- `switch (input.administration())` exhaustivo:
  - `OfApplication(a)`: **exactamente el comportamiento actual** (gate por aplicación → operación; auditoría `ALLOWED`/`DENIED` solo ante `NotAuthorizedToAdministerException`).
  - `OfTenant(actor)`: `mustAdministerSomeApplication.execute(actor)` → con la `ApplicationId` emitida `app`: operación → `ALLOWED` auditado con contexto `new AdministrationRequest(actor.tenantId(), app, actor.subjectUserId(), actor.subject(), Set.of(), actor.authenticationContext())`. La operación solo se suscribe **después** del permiso (`Mono.defer`).
  - Ante `NotAuthorizedToAdministerTenantCatalogException e`: si `e.evaluatedApplication()` está presente ⇒ auditar `DENIED` atribuido a esa aplicación; luego `Mono.error(e)`. Si está vacía ⇒ solo `Mono.error(e)`. `MfaEvidenceRequiredException` se propaga sin auditar.
  - El `onErrorResume` de `DENIED` envuelve **solo el gate** (un `NotAuthorizedToAdministerTenantCatalogException` nunca nace de la operación, pero el alcance de la captura no debe ampliarse).
- La operación auditada es `ROLE_DEFINED` / `PROFILE_DEFINED` / `RESOURCE_GRANTED|RESOURCE_REVOKED` (`input.revocation()`) / `PROFILE_ROLE_ADDED|PROFILE_ROLE_REMOVED` (`input.removal()`), como hoy.

### [M] Interactores (`pdp/authorization/infrastructure/adapter/primary/web/interactor/impl/`)

Firmas públicas **sin cambio**. Cambia lo que construyen:

- `AdministerRoleDefinitionInteractorImpl`, `AdministerProfileDefinitionInteractorImpl`: alcance `APPLICATION` ⇒ `new CatalogAdministration.OfApplication(new AdministrationRequest(…))` (igual que hoy); alcance `TENANT` ⇒ `new CatalogAdministration.OfTenant(new TenantAdministrationRequest(principal.tenantId(), userId, principal.subject(), principal.authenticationContext()))`. Sin rama que deje la request sin gate.
- `AdministerResourceGrantInteractorImpl`, `AdministerProfileRoleAdditionInteractorImpl`: `…ApplicationLookup` devuelve `Optional<ApplicationId>`; presente ⇒ `OfApplication`, vacío ⇒ `OfTenant` (con los mismos cuatro datos del principal y el `UserId` ya resuelto).

### [M] `BulkCatalogImportService` (`…/primary/web/service/`)

```java
public BulkCatalogImportService(BulkCatalogWorkbookService books, RoleRepository roles, ProfileRepository profiles,
        ProtectedResourceRepository resources, ApplicationNameForTenantLookupValidator applications,
        PrincipalMustBeApplicationAdministratorValidator administrators,
        PrincipalMustAdministerAtLeastOneApplicationValidator tenantAdministrators,   // NUEVO, tras `administrators`
        IdentifierGenerator ids, TimeProvider time)
// validate/commit/Result: firmas sin cambio. Semántica de las filas TENANT: §6.4.
```

### [M] `CatalogMaintenanceController` (`…/primary/web/controller/`)

El constructor (package-private) gana `PrincipalMustAdministerAtLeastOneApplicationValidator tenantAdministrator` **justo después de `administrator`**. `gateOptional` → `gateCatalog`; `user(p)` falla cerrado. Semántica: §6.5. Rutas, verbos y códigos de éxito sin cambio.

### [M] `ApiErrorHandler` (`shared/web/exceptionhandler/`)

Añadir `"NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG"` a los códigos que responden `403`. Sin handler nuevo.

### [M] `AuthorizationConfiguration` (`…/authorization/infrastructure/config/`)

Los beans de los cuatro casos de uso y de `BulkCatalogImportService` reciben el validador nuevo (ya hay un bean de ese tipo). Sin bean nuevo adicional. `RoleAdministrationController`/`ProfileAdministrationController` no cambian.

### Modulith (sin ADR de frontera)

No se declara ninguna dependencia nueva: el validador consume `assignments :: usecase` y `assignments :: dto`, que ya están en `allowedDependencies` de `authorization` (los usa `ReadCurrentUserApplicationsUseCaseImpl`). `ModulithStructureTests` quedó verde con los esqueletos. **No se relaja `allowedDependencies`.** El validador nuevo queda publicado por `authorization :: rule` (el `package-info` de `application/rule/validator` ya es `@NamedInterface("rule")`), por si una historia futura (p. ej. otro slice que escriba catálogo) lo necesita.

## 8. Árbol de archivos

Rutas desde `pdp/src/main/java/co/edu/uco/seguridad/`.

```
pdp/authorization/
├── domain/
│   ├── exception/NotAuthorizedToAdministerTenantCatalogException.java                       [N]
│   └── message/AuthorizationMessages.java                                                    [N-aditivo] notAuthorizedToAdministerTenantCatalog()
├── application/
│   ├── primaryport/request/
│   │   ├── TenantAdministrationRequest.java                                                  [N]
│   │   ├── CatalogAdministration.java                                                        [N]
│   │   ├── AdministerRoleDefinitionRequest.java                                              [M]
│   │   ├── AdministerProfileDefinitionRequest.java                                           [M]
│   │   ├── AdministerResourceGrantRequest.java                                               [M]
│   │   └── AdministerProfileRoleAdditionRequest.java                                         [M]
│   ├── rule/validator/
│   │   ├── PrincipalMustAdministerAtLeastOneApplicationValidator.java                        [N]
│   │   └── impl/PrincipalMustAdministerAtLeastOneApplicationValidatorImpl.java               [N] esqueleto (UnsupportedOperationException)
│   └── usecase/impl/
│       ├── AdministerRoleDefinitionUseCaseImpl.java                                          [M]
│       ├── AdministerProfileDefinitionUseCaseImpl.java                                       [M]
│       ├── AdministerResourceGrantUseCaseImpl.java                                           [M]
│       └── AdministerProfileRoleAdditionUseCaseImpl.java                                     [M]
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── interactor/impl/
    │   │   ├── AdministerRoleDefinitionInteractorImpl.java                                   [M]
    │   │   ├── AdministerProfileDefinitionInteractorImpl.java                                [M]
    │   │   ├── AdministerResourceGrantInteractorImpl.java                                    [M]
    │   │   └── AdministerProfileRoleAdditionInteractorImpl.java                              [M]
    │   ├── controller/CatalogMaintenanceController.java                                      [M] gate TENANT + user(p) fail-closed
    │   └── service/BulkCatalogImportService.java                                             [M] gate TENANT memoizado
    └── config/AuthorizationConfiguration.java                                                [N] bean del validador (ya) + [M] cuatro beans de use case y el de la masiva
shared/
├── message/RequiredArgumentMessages.java                                                     [N-aditivo] 4 constantes
└── web/exceptionhandler/ApiErrorHandler.java                                                 [M] 403 para el código nuevo
```

`authorization/package-info.java` no cambia. Documentación a actualizar por el implementador: `pdp/docs/ai-harness/CHECKPOINT.md` (fila HU-036), el `REPORTE-HU-036.md` (lo escribe el validador) y la corrección de la nota «sin gate» de PLAN-HU-035 §11.1 solo si el responsable lo pide (es histórico). La matriz `criteria-compliance-matrix.md` no se toca (ningún criterio cambia de estado).

## 9. Casos de prueba esperados

Sin Mockito; fakes anónimos/lambdas, `StepVerifier`, sin Spring, AssertJ. Principal de prueba como en `BulkResourceImportServiceTests`. Los dobles de `AdministrationAuditRepository` capturan en una `List<AdministrationEvent>`; `IdentifierGenerator`/`TimeProvider` fijos.

### 9.1 Matriz por criterio

| Criterio | Qué se demuestra | Clase de prueba (capa) | Casos |
|---|---|---|---|
| C5, C8 | Primer permiso gana; corte | `PrincipalMustAdministerAtLeastOneApplicationValidatorImplTests` (application, **nueva**) | `emits_the_only_application_when_the_gate_permits_it`; `stops_at_the_first_permit_and_does_not_consult_the_rest`; `consults_applications_in_ascending_id_order_regardless_of_map_order`; `skips_an_application_it_does_not_administer_and_permits_the_next` |
| C7 | Fail-closed | ídem | `denies_when_the_subject_has_no_applications_and_never_calls_the_gate` (excepción con `evaluatedApplication` vacío); `denies_when_every_application_is_rejected` (con la primera aplicación evaluada); `a_gate_error_on_one_application_does_not_authorize_it_and_the_next_is_still_tried`; `denies_when_every_gate_call_errors` (nunca propaga el error crudo); `denies_when_resolving_the_applications_fails` (vacío); `does_nothing_until_subscribed` |
| C6 | MFA | ídem | `propagates_the_first_mfa_requirement_when_no_application_permits`; `mfa_requirement_wins_over_plain_denials`; `a_later_permit_wins_over_an_earlier_mfa_requirement`; `mfa_requirement_is_not_audited_by_the_validator` (no tiene auditoría) |
| C9 | Inquilino | ídem | `resolves_applications_with_the_actors_user_and_tenant` (captura `ResolveUserApplicationsRequest`); `builds_the_application_request_with_tenant_user_subject_authentication_and_no_roles` (captura `AdministrationRequest`) |
| C12/invariantes | Datos | `TenantAdministrationRequestTests`, `CatalogAdministrationTests` (application, **nuevas**) | cada componente nulo ⇒ NPE con su mensaje; `OfApplication`/`OfTenant` rechazan `null`; el `switch` sobre el tipo sellado es exhaustivo (compila sin `default`) |
| Excepción | Código y mensaje | `NotAuthorizedToAdministerTenantCatalogExceptionTests` (domain, **nueva**) | código `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG`; mensaje del catálogo; `evaluatedApplication()` presente/vacío |
| C1, C10, C11 | Rol `TENANT` | `AdministerRoleDefinitionUseCaseImplTests` (application, **[M]**) | `tenant_actor_that_administers_an_application_defines_the_role_and_audits_allowed_under_that_application`; `tenant_gate_denial_never_reaches_define_role`; `tenant_denial_audits_denied_under_the_first_evaluated_application`; `tenant_denial_without_an_evaluated_application_audits_nothing`; `tenant_mfa_requirement_propagates_without_audit`; `a_failing_audit_does_not_break_the_tenant_definition`; `tenant_definition_that_fails_after_the_gate_is_not_audited`; `the_tenant_gate_is_not_invoked_for_an_application_role` y `the_application_gate_is_not_invoked_for_a_tenant_role`. Los casos `APPLICATION` existentes se adaptan a `OfApplication` sin cambiar su aserción (C13). **Se elimina** el caso actual «tenant role skips the gate» (su contrario es la regla nueva) |
| C2, C10, C11 | Perfil `TENANT` | `AdministerProfileDefinitionUseCaseImplTests` (**[M]**) | los mismos que arriba con `PROFILE_DEFINED` |
| C3, C11 | Concesión/revocación | `AdministerResourceGrantUseCaseImplTests` (**[M]**) | `tenant_grant_is_gated_and_audited_as_resource_granted`; `tenant_revocation_is_gated_and_audited_as_resource_revoked`; `tenant_denial_never_reaches_grant_or_revoke`; MFA y atribución como arriba |
| C3, C11 | Agregar/quitar rol de perfil | `AdministerProfileRoleAdditionUseCaseImplTests` (**[M]**) | `PROFILE_ROLE_ADDED`/`PROFILE_ROLE_REMOVED` con gate TENANT; denegación no llega a las operaciones |
| C1–C3, C9 | Cableado de la request | `AdministerRoleDefinitionInteractorImplTests`, `AdministerProfileDefinitionInteractorImplTests`, `AdministerResourceGrantInteractorImplTests`, `AdministerProfileRoleAdditionInteractorImplTests` (infrastructure, **[M]**) | alcance `TENANT` ⇒ `OfTenant` con `tenantId`, `userId`, `subject` y `authenticationContext` del principal; `APPLICATION` ⇒ `OfApplication`; lookup vacío ⇒ `OfTenant`; **nunca** una request sin gate (no existe el tipo) |
| C3 (V5) | `PATCH`/`DELETE` | `CatalogMaintenanceControllerTests` (infrastructure, **nueva**; controlador construido a mano, `MockServerWebExchange`) | `renaming_a_tenant_role_requires_the_tenant_gate` y su denegación (la operación **no** se invoca, ⇒ `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG`); ídem `deleteRole`, `updateProfile`, `deleteProfile`; `an_application_role_uses_only_the_application_gate`; `a_missing_role_is_404_before_any_gate`; `an_unresolvable_user_id_denies_instead_of_skipping_the_gate` (tenant y aplicación; cubre el fail-open de §6.5, también para `updateApplication`/`updateResource`/`deleteResource`) |
| C4, C6, C7, C10 (V6) | Masiva | `BulkCatalogImportServiceTests` (infrastructure, **[M]**: el constructor gana el validador; las llamadas existentes no cambian) | `tenant_roles_are_accepted_when_the_actor_administers_an_application`; `tenant_roles_are_rejected_per_row_when_the_actor_has_no_permission` (`NOT_AUTHORIZED`, campo `Alcance*`, `ready=false`); ídem modo `profiles`; `commit_saves_nothing_when_a_tenant_row_is_rejected`; `a_failing_or_mfa_blocked_tenant_gate_is_not_authorized_per_row` (fail-closed y colapso de MFA); `the_tenant_gate_is_consulted_once_per_call_for_many_tenant_rows`; `the_tenant_gate_is_not_consulted_without_tenant_rows`; `the_tenant_gate_precedes_duplicate_checks` (denegado + nombre persistido ⇒ `NOT_AUTHORIZED` y `existsByNameInScope` sin llamar); `a_tenant_row_with_an_application_value_is_invalid_value_without_consulting_the_gate`; `mixed_batch_rejects_only_the_tenant_rows_and_saves_nothing`; `relations_of_a_rejected_tenant_role_are_reported_as_not_found`; `the_tenant_validator_receives_tenant_user_subject_and_authentication_of_the_principal`; `an_unsupported_mode_never_consults_the_tenant_gate`; contadores `valid + invalid == total` con filas `TENANT` rechazadas |
| C1–C3 | Traducción HTTP | `ApiErrorHandlerTests` (shared, **[M]**) | `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` ⇒ 403 con `ProblemDetail` (código y mensaje); `MFA_REQUIRED` y `NOT_AUTHORIZED_TO_ADMINISTER` siguen en 403; un código desconocido sigue en 400 |
| C12 | TENANT existentes se resuelven | **No se escribe prueba nueva**: la ruta de autorización no se toca. Quedan como red de seguridad `ResolveUserApplicationsUseCaseImplTests`, `AuthorizationContextResolverImplTests` y los de `ReadCurrentUserApplications*` (todos ya incluyen roles/perfiles `TENANT`) y deben seguir en verde | — |
| C13 | Sin regresión `APPLICATION` | los mismos tests `[M]` de arriba (rama `OfApplication`) + `ApplicationSecurityHttpTests`/`...HttpTests` existentes | aserciones intactas |
| C14 | Arquitectura | `LayeredArchitectureTests`, `ModulithStructureTests`, `consistencia.ps1`, `drift.ps1` | verdes; **sin** cambios de `allowedDependencies` |

> `AuthorizationConfiguration` no se prueba (cableado, excluido de cobertura). La resolución del bean `PrincipalMustBeApplicationAdministratorValidator` (el decorado con MFA) la cubren `PdpApplicationTests` y las pruebas HTTP con contexto, que requieren sockets.

### 9.2 Presupuesto

Validador ≈ 15; excepción/requests ≈ 6; cuatro casos de uso ≈ 24 (6 por cada uno); cuatro interactores ≈ 8; controlador de mantenimiento ≈ 9; masiva ≈ 13; handler ≈ 2. Total orientativo **75–85** casos, de los cuales ~25 son ajustes de pruebas existentes. Es más que el presupuesto habitual (10–15) porque la historia gatea seis vías con la misma primitiva; la duplicación es entre casos de uso (cada uno tiene su propia auditoría), no dentro de uno.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-10-30 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

**Estado de la verificación del planificador (honesto):** `verificar.ps1 -Compilar` (main + test) **VERDE** con los esqueletos. `verificar.ps1 -Rapido` en esta herramienta: 978 pruebas, 84 con fallo/error, **todas** en clases que necesitan sockets locales (`Unable to establish loopback connection`: fixtures de OPA, Redis, Keycloak simulado, `@SpringBootTest`/Testcontainers: `ApplicationHttpTests`, `AssignmentHttpTests`, `ProfileAssignmentHttpTests`, `ApplicationSecurityHttpTests`, `AuthorizationHttpTests`, `InternalSecurityChainIntegrationTests`, `OpaAdministrationDecisionAdapterTests`, `OpaPolicyDecisionAdapterTests`, `ProfileHttpTests`, `RoleHttpTests`, `ApplicationWithInitialResourceHttpTests`, `InMemoryAuditAdapterTests`, `RedisDistributedCachePortTests`, `RedisTokenRevocationAdapterTests`, `BffSessionTokenRenewalIntegrationTests`, `CorsConfigurationTests`, `SecurityWebFilterChainTests`, `SessionLogoutBearerIntegrationTests`, `SurrealRepositoryIntegrationTests`, `PdpApplicationTests`). Ninguna prueba unitaria de dominio/aplicación/infraestructura falla; `LayeredArchitectureTests` (3) y `ModulithStructureTests` (1) **pasan**. Hay que repetir `verificar.ps1` completo en una máquina con loopback antes del Gate 2.

## 11. Ambigüedades resueltas, decisiones y riesgos

**Sin ambigüedades pendientes que bloqueen el contrato.** Las decisiones 1–6 del responsable técnico se aplicaron tal cual. Lo siguiente lo resolvió el planificador con el criterio más seguro (fail-closed) y es revisable en el Gate 1.

### Decisiones del planificador

| # | Decisión | Por qué |
|---|---|---|
| P1 | El gate TENANT cubre **V1–V6** (crear, conceder/revocar recursos, agregar/quitar roles de perfil, renombrar/borrar, masiva), no solo «crear». | Editar un rol/perfil `TENANT` cambia el catálogo de todo el inquilino igual que crearlo; HU-035 ya señalaba la concesión de recursos como parte del hueco. Dejar V3–V5 abiertas es la puerta lateral. Separable si el responsable lo pide (§6.1). |
| P2 | `CatalogAdministration` (tipo sellado) reemplaza a `Optional<AdministrationRequest>`. | Con el `Optional`, «vacío = sin gate» era un estado representable y fue la causa del hueco. Con el tipo sellado, el compilador obliga a decir qué gate se exige; un interactor que olvide el gate no compila. Alternativa descartada: añadir un segundo `Optional<TenantAdministrationRequest>` junto al existente (permite combinaciones inválidas: ninguno o ambos). |
| P3 | El validador emite el `ApplicationId` que autorizó. | Es lo que la auditoría necesita (decisión 5) sin repetir la consulta. |
| P4 | Código nuevo `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` (403) en vez de reutilizar `NOT_AUTHORIZED_TO_ADMINISTER`. | El mensaje y el código existentes hablan de «la aplicación X»; aquí no hay ninguna. Un código propio permite al front distinguirlo. Coste: una cadena más en `ApiErrorHandler`. El `NOT_AUTHORIZED` de la HU es el código **por fila** de la masiva, que sí se usa tal cual. |
| P5 | `MFA_REQUIRED` se reutiliza propagando la **misma** `MfaEvidenceRequiredException` de la primera aplicación que lo exigió. | No se inventa otra excepción de MFA; el sujeto ya administra esa aplicación, no se le revela nada nuevo. |
| P6 | Orden de evaluación: id ascendente, secuencial. | `ResolveUserApplicationsUseCase` devuelve un mapa sin orden; hace deterministas el corte, la auditoría `DENIED` y las pruebas. |
| P7 | Fallo al resolver aplicaciones o error de una consulta del gate ⇒ no autoriza (no 500). | Decisión 2 («error ⇒ no autoriza») y ADR-023.6 / ADR-025. |
| P8 | Atribución de `DENIED` a la primera aplicación evaluada; sin evento si no hay ninguna. | Decisión 5 delegó la forma exacta. Documentado en §6.3 que `applicationId` significa «contra la que se evaluó», no «afectada». |
| P9 | Masiva: `MFA_REQUIRED` colapsa en `NOT_AUTHORIZED` por fila; el campo es `Alcance*`. | Paridad con HU-035 (recursos y filas `APPLICATION`); el campo `Aplicación` estaría vacío en una fila `TENANT`. |
| P10 | `CatalogMaintenanceController.user(p)` pasa a fallar cerrado (§6.5). | Es un fail-open real (el `.then(...)` corre aunque el gate no se ejecute) en las mismas líneas que se tocan. No se corrigen los `500` de los interactores (patrón existente). |
| P11 | ADR-036 se entrega como borrador (§12), no se escribe en `security-platform-architecture`. | Ese repositorio es de solo lectura para el flujo de agentes (`sb-fuentes`). La numeración 036 es la de la decisión del responsable; ADR-035 ya está reservado por HU-032 (CHECKPOINT) y no existe aún en el repositorio hermano. |
| P12 | No hay migración ni prueba nueva para C12. | La ruta de lectura/autorización no se toca (decisión 3). |

### Riesgos

- **R1 — Cambio de comportamiento observable (es el objetivo).** Tras desplegar, quien hoy crea roles/perfiles `TENANT` sin administrar ninguna aplicación recibirá 403. Verificar con el responsable que no hay un flujo de arranque (p. ej. un script de alta de inquilino) que dependa de ello: no se encontró ninguno en `pdp/`; no se revisaron pipelines externos.
- **R2 — Interdependencia con la dependencia circular de arranque:** un inquilino sin ninguna aplicación (o cuyo único administrador aún no existe) **no podrá** crear catálogo `TENANT` hasta registrar una aplicación (el registro asigna al registrador como primer administrador, HU-015). Es la consecuencia buscada del actor intermedio; la sustituye ADR-024.
- **R3 — Coste:** un sujeto sin permiso paga 2 consultas de asignaciones + hasta N consultas de rol activo + N decisiones OPA (N = aplicaciones donde tiene alguna asignación). Las concesiones en lote (`POST/PUT /roles/{id}/resources…`) repiten R1 **por recurso** porque cada uno pasa por el interactor; hoy ya repiten el gate por aplicación. Si se vuelve un problema, se memoiza por petición en una historia propia; no se optimiza aquí (decisión 4 pide misma semántica que la individual).
- **R4 — Aplicaciones obsoletas:** tener una asignación no autoriza por sí sola (decide el gate); R1 cuenta aplicaciones donde el sujeto tiene asignación activa; si una aplicación se elimina dejando asignaciones huérfanas, solo cuenta si el gate (OPA) concede sobre ella. No se validó que `RemoveApplication` borre las asignaciones; la comprobación de dependencias (`ApplicationDeletionDependencyValidator`) sugiere que no quedan huérfanas.
- **R5 — Actor demasiado amplio por diseño:** administrar *cualquier* aplicación habilita definir catálogo de *todo* el inquilino (y conceder a un rol `TENANT` recursos de aplicaciones que no administra). Es lo decidido (decisión 1) y lo que ADR-024 estrecha. Endurecimiento posible y compatible: exigir, en V3, administrar la aplicación del **recurso** concedido.
- **R6 — Auditoría parcial para atacantes sin aplicaciones** (§6.3): un principal sin asignaciones no deja evento `DENIED`. Mejora futura: `AdministrationEvent` con `applicationId` opcional.
- **R7 — Modulith:** sin cambio de fronteras; si el implementador necesitara una dependencia nueva, se detiene y se reporta (no se relaja `allowedDependencies`).
- **R8 — Pruebas existentes que cambian de significado:** los casos «rol/perfil/concesión `TENANT` sin gate → éxito» de `Administer*UseCaseImplTests` afirman exactamente lo contrario de lo que ahora se exige; deben reescribirse, no «arreglarse» (el implementador y el tester lo registran en el reporte).

## 12. Borrador de ADR-036 (para copiar a `security-platform-architecture/docs/01-governance/adr/`)

**ADR-036: Actor de las escrituras de catálogo de alcance tenant**

| Campo | Valor |
|---|---|
| Estado | Proposed (pasa a Accepted al aprobar el Gate 1 de HU-036) |
| Fecha | 2026-10-30 |
| Decisores | Responsable técnico |
| Relacionado | ADR-023 (modelo de administración), ADR-024 (administrador global, diferido), ADR-025 (zero trust), ADR-027 (MFA step-up); `securityBaseline` HU-016, HU-019, HU-035, HU-036 |

**Contexto.** ADR-023 gatea la administración **por aplicación**; HU-016/HU-019 dejaron sin gate los roles y perfiles de alcance `TENANT` («cero regresión») porque no había actor al que atribuirlos, y HU-035 trasladó el hueco a la carga masiva. Cualquier principal autenticado puede definir permisos que afectan a todas las aplicaciones de su inquilino. ADR-024 evalúa el administrador global de plataforma pero difiere su construcción (toca `Assignment` y cuatro slices).

**Decisión.** Mientras no exista el administrador global, el actor de toda escritura de catálogo `TENANT` (crear, editar, borrar roles y perfiles, conceder recursos, componer perfiles; individual y masiva) es **un principal que administra al menos una aplicación de su inquilino**, decidido por el mismo mecanismo de ADR-023 (`AuthorizeAdministrationUseCase`/OPA, nunca una comparación de nombre de rol en Java) y con la evidencia de MFA de ADR-027. Se evalúa sobre las aplicaciones donde el sujeto tiene asignaciones activas, en orden estable y con corte al primer permiso. Es **fail-closed**: error, `DENY` o `INDETERMINATE` no autoriza; si alguna aplicación exigió MFA y ninguna autorizó, `MFA_REQUIRED` (403); si no, `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` (403). Se audita en la rama `TENANT` con la aplicación que concedió el permiso.

**Consecuencias.** (+) Cierra el hueco sin construir el administrador global ni tocar `Assignment`; (+) un rol/perfil `TENANT` solo surte efecto cuando una asignación lo usa, y esa asignación ya está gateada por la aplicación destino. (−) El actor es más amplio que el ideal: administrar cualquier aplicación habilita catálogo de todo el inquilino. (−) Un inquilino sin aplicaciones no puede definir catálogo `TENANT`. **Evolución:** ADR-024 sustituye la implementación de `PrincipalMustAdministerAtLeastOneApplicationValidator` por una que reconozca al administrador global; el contrato y los puntos de uso no cambian.
