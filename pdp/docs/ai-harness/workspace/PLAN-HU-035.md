# PLAN: HU-035 — Endurecer la carga masiva de roles y perfiles

## Metadata

- **ID:** HU-035
- **Slice:** `authorization` (adaptador primario web; no toca dominio ni `application`)
- **Tipo:** Escritura (confirmación) y consulta (plantilla / validación)
- **Fecha:** 2026-10-30
- **Rama sugerida:** `feature/HU-035-endurecer-carga-masiva-catalogo`
- **Fuentes:** `HU-035.md`; ADR-023 (gate por aplicación, sin rol global, fail-closed), ADR-025 (zero trust, fail-closed), ADR-014 (matriz HTTP: 400 contrato, 403 autorización); código: `BulkCatalogImportService`, `BulkCatalogWorkbookService`, `BulkResourceImportService`, `BulkResourceWorkbookService`, `BulkResourceController`, `RoleAdministrationController`, `AdministerRoleDefinitionUseCaseImpl` / `AdministerProfileDefinitionUseCaseImpl` (qué se gatea hoy por la vía individual), `MfaAwareApplicationAdministratorValidator`, `ApiErrorHandler`, `AuthorizationConfiguration`; pruebas `BulkCatalogImportServiceTests`, `BulkCatalogWorkbookServiceTests`, `CatalogWorkbooks`, `BulkResourceImportServiceTests`; front: `securityBaseline-vue`, `security-ui`, `securityBaseline-fr` (grep).
- **Criterios de la línea base que toca:** 1, 2, 5, 9, 11, 12, 20, 21, 22 (estructurales y de endpoint); 3 (el gate se consume como `Validator` publicado, no se copia); 6 (el `mode` entra como `String` y se rechaza como `MalformedRequestFieldException`); 4 (ids y reloj siguen inyectados).

## 1. Resumen funcional

Cierra seis defectos de la carga masiva de catálogo (`/api/v1/resources/bulk/catalog/**`): (1) falta el gate de administrador por fila, (2) contadores negativos, (3) `mode` inválido → 500 en la plantilla, (4) `mode` sin validar en validación/confirmación/reporte, (5) sin cabeceras, sin tope de tamaño/filas, sin códigos `REQUIRED`, (6) menores (archivo vacío en inglés, celda numérica descartada, campo equivocado del nombre inválido).
**No cubre:** la plantilla/endpoints de recursos, la UI, auditoría de la carga masiva (`AdministrationAuditRepository`), el gate de los roles/perfiles de alcance `TENANT` (ver §11), ni cambios en `ApiErrorHandler`.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Principal que no administra la aplicación de la fila | Error de esa fila `NOT_AUTHORIZED` (campo `Aplicación`); no se persiste nada del lote (`ready=false`) |
| 2 | Aplicación de otro tenant o inexistente | Mismo error que inexistente: `APPLICATION_NOT_FOUND`, mensaje neutro, **sin llamar al gate** |
| 3 | Fallo del gate (DENY, INDETERMINATE, MFA, cualquier error) | Fail-closed: `NOT_AUTHORIZED` en la fila |
| 4 | Contadores | `validRows >= 0`, `invalidRows <= totalRows`, `validRows + invalidRows == totalRows` siempre |
| 5 | `mode` inválido | `400` (`MALFORMED_REQUEST_FIELD`, `field=mode`) en plantilla, validación, confirmación y reporte de errores |
| 6 | Cabecera cambiada / archivo > 5 MB / vacío / cifrado / > 1.000 filas | Un error por causa, con código estable y mensaje en español |
| 7 | Celda numérica, booleana o fórmula | Error de esa fila (`UNEXPECTED_CELL_TYPE` / `UNSUPPORTED_CELL`), la fila no se descarta |
| 8 | Nombre inválido | Se reporta bajo `Nombre de rol*` / `Nombre de perfil*`, no bajo `Aplicación` |
| 9 | Las dos pruebas `@Disabled` | Se activan y pasan; `verificar.ps1` completo en verde |

## 3. Reglas de negocio

No hay `Rule` nueva: la regla (administrar la aplicación) ya existe y se **consume** vía `PrincipalMustBeApplicationAdministratorValidator` (publicado por `authorization`, mismo módulo). El resto son validaciones del contrato del archivo, propias del adaptador web.

| # | Regla | Dónde vive | Puerto | Resultado |
|---|---|---|---|---|
| R1 | Quien carga una fila `APPLICATION` debe administrar esa aplicación | `PrincipalMustBeApplicationAdministratorValidator` (existente), invocado por `BulkCatalogImportService` | gate (OPA) | fila `NOT_AUTHORIZED` |
| R2 | La aplicación se resuelve por nombre **dentro del tenant del principal** | `ApplicationNameForTenantLookupValidator` (existente) | — | `APPLICATION_NOT_FOUND` |
| R3 | `mode` ∈ {`roles`,`profiles`} para validar/confirmar/reportar; ∈ {`roles`,`profiles`,`resources-roles`} para plantilla | `BulkCatalogImportService` / `BulkCatalogWorkbookService` | — | `UnsupportedBulkModeException` → 400 |
| R4 | Tope de 5 MB y 1.000 filas de datos por hoja; cabeceras exactas | `BulkCatalogWorkbookService` | — | códigos de la §6 |

## 4. Modelo de dominio afectado

Ninguno. Todo ocurre en `authorization/infrastructure/adapter/primary/web`. No hay VO, enum ni entidad nuevos.

## 5. Persistencia

No aplica. `RoleRepository.saveAllAtomically` / `ProfileRepository.saveAllAtomically` y los `existsByNameInScope` / `findByNameInScope` / `findIdByApplicationPathAndMethod` se usan como hoy.

## 6. Endpoint — contrato resultante

| Verbo | Ruta | Éxito | Cambia |
|---|---|---|---|
| GET | `/api/v1/resources/bulk/catalog-template/{mode}` | 200 xlsx | `mode` inválido: 400 con `ProblemDetail` (antes 400 sin cuerpo) |
| POST | `/api/v1/resources/bulk/catalog/{mode}/validate` | 200 `BULK_CATALOG_VALIDATED` | `mode` inválido 400; ahora resuelve `UserId` y aplica gate |
| POST | `/api/v1/resources/bulk/catalog/{mode}` | 200 `BULK_CATALOG_IMPORTED` | ídem |
| POST | `/api/v1/resources/bulk/catalog/{mode}/error-report` | 200 xlsx | ídem; ya no falla (500) con archivos ilegibles |

- **Autorización:** token; tenant del principal. Las rutas de catálogo pasan a usar `withUser(...)` (como las de recursos) para obtener `PdpPrincipal` + `UserId`.
- **Errores:** `mode` → `MalformedRequestFieldException("mode", ...)` → 400 por `ApiErrorHandler` (sin tocarlo). Todo lo demás sigue siendo 200 con `ready=false` y la lista de errores por fila (contrato actual).

### 6.1 Decisión 1 — cómo se aplica el gate por fila

1. Solo se gatean las filas de la hoja `Roles`/`Perfiles` con alcance `APPLICATION`. Las de alcance `TENANT` no tienen aplicación que administrar y **no se gatean**: es la misma regla que ya aplican `AdministerRoleDefinitionUseCaseImpl` y `AdministerProfileDefinitionUseCaseImpl` en la vía individual (§11, punto 1).
2. Orden por fila: (a) celdas obligatorias → (b) nombre (VO) y alcance → (c) aplicación por nombre en el tenant del principal → (d) **gate** → (e) `DUPLICATE_IN_FILE` → (f) `DUPLICATE_PERSISTED`. El gate va **antes** de cualquier comprobación de duplicados: un no administrador no debe poder sondear qué roles/perfiles existen en una aplicación ajena.
3. `AdministrationRequest(principal.tenantId(), application.id(), userId, principal.subject(), Set.of(), principal.authenticationContext())`, igual que `BulkResourceImportService`.
4. **El gate se invoca como máximo una vez por `ApplicationId` y por llamada a `validate`/`commit`** (memoización local a la invocación, el resultado —éxito o error— se reutiliza para las demás filas de esa aplicación). Evita hasta 1.000 llamadas a OPA por archivo; no cambia la semántica fail-closed.
5. Códigos: `NOT_AUTHORIZED` (campo `Aplicación`, descripción `No está autorizado para administrar esta aplicación`, igual que recursos). **Cualquier** error del gate —DENY, `INDETERMINATE`, `MfaEvidenceRequiredException`, error de red— produce `NOT_AUTHORIZED` (fail-closed, ADR-023 §6 / ADR-025). Solo se captura el error **del gate**; los fallos de otras consultas conservan su tratamiento actual. Difiere de recursos en un punto: allí el `onErrorResume` también cubre `existsByApplicationPathAndMethod`; aquí no.
6. **No se filtra la existencia en otro tenant:** la búsqueda es por (tenant del principal, nombre). Una aplicación de otro tenant devuelve vacío, igual que una inexistente → `APPLICATION_NOT_FOUND` con el **mismo** mensaje en ambos casos, y el gate no se invoca (nunca recibe un `ApplicationId` ajeno). El mensaje pasa a ser neutro: `No se encontró la aplicación en el tenant autenticado` (el actual, «no pertenece al tenant autenticado», insinúa que existe en otro). Se aplica también a las filas de relación (código `INVALID_REFERENCE`, que no cambia).
7. Las filas de relación (`RolesxRecurso`, `PerfilesxRol`) **no** invocan el gate: solo pueden colgar de roles/perfiles ya aceptados en la hoja principal (`entries`), que existen únicamente si pasaron el gate. Una fila `NOT_AUTHORIZED` hace que sus relaciones se reporten como `ROLE_NOT_FOUND` / `PROFILE_NOT_FOUND`, consistente con que ese rol no entra al lote.
8. `commit` no cambia de forma: llama a `validate` y solo guarda si `ready`. Cualquier `NOT_AUTHORIZED` impide guardar todo el lote.

### 6.2 Decisión 2 — tope de tamaño y cabeceras (alineado con `BulkResourceWorkbookService`)

- `MAX_BYTES = BulkResourceWorkbookService.MAX_BYTES` (5 MB) y `MAX_ROWS = BulkResourceWorkbookService.MAX_ROWS` (1.000 filas de datos **por hoja**); se declaran en `BulkCatalogWorkbookService` apuntando a las de recursos para que no diverjan.
- Cabeceras exactas, sensibles a mayúsculas, comparando las primeras N columnas (columnas extra permitidas, como en recursos). Una única fuente por hoja, usada también por la plantilla:

| Hoja | Cabeceras |
|---|---|
| `Roles` | `Nombre de rol*`, `Alcance*`, `Aplicación` |
| `Perfiles` | `Nombre de perfil*`, `Alcance*`, `Aplicación` |
| `RolesxRecurso` | `Nombre de rol*`, `Alcance*`, `Aplicación*`, `Ruta*`, `Método HTTP*` |
| `PerfilesxRol` | `Nombre de perfil*`, `Alcance del perfil*`, `Aplicación del perfil`, `Nombre de rol*`, `Alcance del rol*`, `Aplicación del rol` |

- Hoja principal ilegible → resultado corto (total 0, `ready=false`, un solo error global), como hoy. Hoja de relación presente pero con problema → error de esa hoja, el resto se evalúa.
- Si la hoja de relación no existe, sigue siendo «sin relaciones» (la hoja es opcional).
- Una primera fila de datos sin cabecera ya no se pierde: la cabecera inválida se rechaza (`INVALID_HEADERS`).

**Catálogo de códigos del archivo** (campo `file` y fila 0 salvo indicación):

| Código | Cuándo | Descripción |
|---|---|---|
| `EMPTY_FILE` | 0 bytes / `null` | `El archivo está vacío` |
| `FILE_TOO_LARGE` | > 5 MB | `El archivo supera el límite de 5 MB` |
| `ENCRYPTED_FILE` | `EncryptedDocumentException` | `No se aceptan libros cifrados` |
| `INVALID_WORKBOOK` | no es un libro Excel | `El archivo no es un libro Excel válido` |
| `MISSING_SHEET` | falta `Roles`/`Perfiles` | `Falta la hoja Roles` / `Falta la hoja Perfiles` |
| `EMPTY_FILE` | hoja principal sin filas de datos | `El archivo no contiene filas de datos` (antes `INVALID_WORKBOOK`) |
| `INVALID_HEADERS` | cabecera distinta, fila 1, campo = nombre de la hoja | `Los encabezados de la hoja {hoja} deben ser: {lista}` |
| `TOO_MANY_ROWS` | > 1.000 filas de datos, campo = nombre de la hoja | `La hoja {hoja} supera las 1.000 filas de datos`; se leen solo las primeras 1.000 |
| `UNEXPECTED_CELL_TYPE` | celda numérica/booleana | `La celda debe contener texto` (fila real, campo = cabecera; en hojas de relación, campo = nombre de la hoja) |
| `UNSUPPORTED_CELL` | fórmula / error | `No se admiten fórmulas ni celdas de error` |

**Códigos por fila** (los produce `BulkCatalogImportService`): `REQUIRED` (nuevo: celda con `*` vacía → `El nombre del rol es obligatorio`, `El nombre del perfil es obligatorio`, `El alcance es obligatorio`, y las equivalentes de relación; la fila no se evalúa más), `INVALID_VALUE` (nombre inválido → campo `Nombre de rol*`/`Nombre de perfil*`; alcance desconocido → `Alcance*`; incoherencia alcance/aplicación → `Aplicación`, mensajes actuales `Aplicación solo aplica al alcance APPLICATION` / `APPLICATION requiere Aplicación`), `APPLICATION_NOT_FOUND` (nuevo, campo `Aplicación`), `NOT_AUTHORIZED` (nuevo), `DUPLICATE_IN_FILE`, `DUPLICATE_PERSISTED`, `ROLE_NOT_FOUND`, `PROFILE_NOT_FOUND`, `INVALID_REFERENCE` (sin cambios). Una fila con error de celda del parser no se vuelve a evaluar (evita doble error por la misma celda).

### 6.3 Decisión 3 — contrato de los contadores

- **`totalRows` = filas de datos no vacías de la hoja principal** (`Roles` o `Perfiles`). **`invalidRows` = filas distintas de esa hoja con al menos un error** (errores de celda del parser + errores de resolución; solo filas ≥ 2). **`validRows = totalRows − invalidRows`.** Garantiza `0 ≤ invalid ≤ total` y `valid + invalid = total`.
- Los errores de las hojas de relación **no** entran en los contadores (sus números de fila pertenecen a otra hoja y no son roles/perfiles): se listan en `errors` (campo = nombre de la hoja), vuelven `ready=false`, pero `validRows` sigue significando «roles/perfiles del lote que no tienen error propio». Es lo que hace correcto el `Se registraron {validRows}` y evita mezclar numeraciones de hojas distintas.
- `ready == errors.isEmpty()` (sin cambios). Errores de fila 0 (globales, p. ej. `TOO_MANY_ROWS`) no cuentan como fila inválida pero fuerzan `ready=false`.
- Mecánica: el servicio mantiene **dos listas de errores** (hoja principal / relaciones) y concatena principal + relaciones en la respuesta (el orden actual se conserva). Cap defensivo `min(total, invalid)` como en recursos.
- `Result.roles()` solo se rellena en modo `roles` y `Result.profiles()` solo en `profiles` (hoy `result()` fabrica ambas listas y consume identificadores de más).

### 6.4 Decisión 4 — `mode` y traducción a HTTP

- `UnsupportedBulkModeException extends IllegalArgumentException` (**[N]**): sigue siendo `IllegalArgumentException`, así la prueba `@Disabled` de `template("otra")` se activa sin cambios.
- `BulkCatalogWorkbookService.template`: la rama `default` lanza `UnsupportedBulkModeException` **fuera** del `try` que envuelve `IOException` en `IllegalStateException` (causa raíz del 500).
- `BulkCatalogImportService.validate/commit`: `mode` distinto de `roles`/`profiles` (incluido `resources-roles`, que es solo de plantilla) → `Mono.error(UnsupportedBulkModeException)` **antes** de abrir el archivo, consultar repositorios o invocar el gate. `commit` hereda al delegar en `validate`.
- `BulkResourceController` (cuatro rutas de catálogo): traduce `UnsupportedBulkModeException` a `new MalformedRequestFieldException("mode", e.getMessage())` (`onErrorMap` en las reactivas, `catch` en la plantilla). `ApiErrorHandler` responde 400 `MALFORMED_REQUEST_FIELD` con `field=mode`; no hay handler nuevo.
- Los segmentos son exactos y en minúsculas (`roles`, `profiles`, `resources-roles`).

### 6.5 Menores incluidos

- `errorReport(bytes, errors)`: si el original no se puede abrir (vacío, corrupto, cifrado), devuelve un libro solo con la hoja `Errores` en vez de lanzar (hoy el endpoint de reporte responde 500 justo con los archivos del criterio 6).
- La hoja `Guía` de las plantillas añade una línea con los límites (5 MB, 1.000 filas por hoja). No cambia nombres ni orden de hojas.

## 7. SPEC — el contrato

Todo se apoya en `BulkResourceValidationResponse` y `BulkResourceRowError` existentes (no cambian). Rutas desde `src/main/java/`.

### [N] Pieza nueva (materializada como esqueleto)

```java
// co/edu/uco/seguridad/pdp/authorization/infrastructure/adapter/primary/web/service/UnsupportedBulkModeException.java
public final class UnsupportedBulkModeException extends IllegalArgumentException {
    public UnsupportedBulkModeException(String message) { super(message); }
}
```

### [M] `BulkCatalogImportService` (mismo paquete `.../web/service/`)

```java
public BulkCatalogImportService(BulkCatalogWorkbookService books, RoleRepository roles, ProfileRepository profiles,
        ProtectedResourceRepository resources, ApplicationNameForTenantLookupValidator applications,
        PrincipalMustBeApplicationAdministratorValidator administrators,   // NUEVO, tras `applications`
        IdentifierGenerator ids, TimeProvider time)

public Mono<Result> validate(String mode, byte[] bytes, PdpPrincipal principal, UserId userId)   // antes (mode, bytes, TenantId)
public Mono<Result> commit(String mode, byte[] bytes, PdpPrincipal principal, UserId userId)     // ídem
public record Result(BulkResourceValidationResponse response, List<Role> roles, List<Profile> profiles)  // sin cambios
```

Semántica: tenant = `principal.tenantId()`. `mode` inválido → `Mono.error(UnsupportedBulkModeException)` sin efectos. Gate, orden de comprobaciones, memoización, códigos y contadores según §6.1–6.3. Los nuevos parámetros no admiten `null` (`Objects.requireNonNull` con constante en `RequiredArgumentMessages` si falta).

### [M] `BulkCatalogWorkbookService` (mismo paquete)

```java
public static final int MAX_BYTES = BulkResourceWorkbookService.MAX_BYTES;
public static final int MAX_ROWS  = BulkResourceWorkbookService.MAX_ROWS;

public record Parsed<T>(List<T> rows, List<BulkResourceValidationResponse.BulkResourceRowError> errors) { }

public Parsed<CatalogRow> parse(String mode, byte[] bytes)              // antes List<CatalogRow> y lanzaba IAE
public Parsed<RoleResourceRow> parseRoleResources(byte[] bytes)         // antes List<RoleResourceRow>
public Parsed<ProfileRoleRow> parseProfileRoles(byte[] bytes)           // antes List<ProfileRoleRow>
public byte[] template(String mode)                                      // misma firma; modo inválido → UnsupportedBulkModeException
public byte[] errorReport(byte[] bytes, List<BulkResourceRowError> errors) // misma firma; original ilegible → libro solo con `Errores`

public record CatalogRow(int row, String name, String scope, String applicationId)            // sin cambios
public record RoleResourceRow(int row, String role, String scope, String application, String path, String method)  // sin cambios
public record ProfileRoleRow(int row, String profile, String profileScope, String profileApplication, String role, String roleScope, String roleApplication)  // sin cambios
```

Semántica de `parse*`: problemas de archivo/hoja principal → `Parsed(rows=[], errors=[uno])`; sin lanzar excepciones (salvo `UnsupportedBulkModeException` en `parse` si `mode` no es `roles`/`profiles`). Los errores de celda llevan fila real ≥ 2 y la fila **se conserva** en `rows` con `""` en la celda ilegible. Se sigue recortando (`trim`) y saltando filas totalmente vacías, conservando la numeración original. Hoja de relación ausente → `Parsed([], [])`.

### [M] `BulkResourceController` (`.../web/controller/`)

Firma pública sin cambios (constructor de 5 argumentos igual). Las tres rutas POST de catálogo usan `withUser(...)` y `imports.*(mode, bytes, user.principal(), user.id())`; las cuatro rutas traducen `UnsupportedBulkModeException` a `MalformedRequestFieldException("mode", ...)`.

### [M] `AuthorizationConfiguration` (`.../authorization/infrastructure/config/`)

```java
@Bean BulkCatalogImportService bulkCatalogImportService(BulkCatalogWorkbookService books, RoleRepository roles,
        ProfileRepository profiles, ProtectedResourceRepository resources,
        ApplicationNameForTenantLookupValidator applications,
        PrincipalMustBeApplicationAdministratorValidator administrators, IdentifierGenerator ids, TimeProvider time)
```

El validador de administración ya es un bean del mismo módulo: no hay nueva frontera de Modulith.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/infrastructure/
├── adapter/primary/web/
│   ├── controller/BulkResourceController.java                    [M] withUser en catálogo + traducción de mode
│   └── service/
│       ├── BulkCatalogImportService.java                          [M] gate, contadores, REQUIRED, orden, mode
│       ├── BulkCatalogWorkbookService.java                        [M] Parsed<T>, límites, cabeceras, celdas, template, errorReport
│       └── UnsupportedBulkModeException.java                      [N]
└── config/AuthorizationConfiguration.java                         [M] bean con el validador
pdp/src/test/.../authorization/infrastructure/adapter/primary/web/service/
├── BulkCatalogImportServiceTests.java                             [M] ver §9
└── BulkCatalogWorkbookServiceTests.java                           [M] ver §9
```

Ninguna clase nueva de `application`: nada nuevo que registrar salvo el cambio de bean. `RequiredArgumentMessages` solo si falta la constante del validador (`PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR` ya existe).

## 9. Casos de prueba esperados

Fakes y lambdas, `StepVerifier`, sin Mockito, sin Spring. Principal de prueba: `new PdpPrincipal(TENANT, "subject-1", "jti-1", Optional.of(ACTOR), AuthenticationContextEvidence.NONE)` (patrón de `BulkResourceImportServiceTests`).

**Pruebas existentes que cambian por el contrato (las modifica el implementador junto con el código):**
- Todas las llamadas `validate/commit(mode, file, TENANT)` → `(mode, file, PRINCIPAL, ACTOR)`; el constructor recibe el gate.
- `an_application_that_does_not_belong_to_the_tenant_is_rejected`: `APPLICATION_NOT_FOUND`, mensaje neutro nuevo.
- `inconsistent_scope_and_name_values_are_reported_per_row`: la fila 6 (nombre vacío) pasa a `REQUIRED` (el `containsOnly("INVALID_VALUE")` se acota).
- `role_resource_relations_are_checked_row_by_row`: la fila 8 (aplicación vacía) pasa a `REQUIRED`.
- `a_workbook_without_the_sheet…` → `MISSING_SHEET`; `a_workbook_without_data_rows…` → `EMPTY_FILE`; cifrado → `ENCRYPTED_FILE`.
- Workbook: `parse*` devuelven `Parsed`; los tests que esperaban `IllegalArgumentException` de archivo/hoja pasan a afirmar el error en `Parsed.errors()` (conservan descripciones); `the_error_report_cannot_be_built_from_a_broken_file` pasa a afirmar el libro solo con `Errores`.
- Se quitan los dos `@Disabled`: `an_unsupported_mode_is_a_client_error_not_an_internal_failure` (sin cambios de cuerpo) y `the_row_counters_never_go_negative_when_relation_rows_fail` (solo firma; añadir `validRows == 1` e `invalidRows == 0`).

**Casos nuevos**

| Capa | Clase | Casos |
|---|---|---|
| Servicio | `BulkCatalogImportServiceTests` | gate: administrador → ready; no administrador → `NOT_AUTHORIZED` en la fila y `saveAllAtomically` no se invoca en `commit`; mezcla de dos aplicaciones (una administrada, otra no) → solo falla la segunda y el lote no se guarda; gate que falla con `IllegalStateException`/MFA → `NOT_AUTHORIZED` (fail-closed); gate invocado 1 vez por aplicación con 3 filas de la misma; fila `TENANT` no invoca el gate; aplicación inexistente o de otro tenant → `APPLICATION_NOT_FOUND`, mismo mensaje, **el gate no se invoca**; el gate precede a `DUPLICATE_*` (no administrador + rol persistido → `NOT_AUTHORIZED`, y `existsByNameInScope` no se consulta); `mode` inválido (`otra`, `resources-roles`) en `validate` y `commit` → `UnsupportedBulkModeException` sin tocar repositorios, gate ni identificadores; mismo caso modo `profiles` con gate; contadores: 1 rol + 3 relaciones inválidas, y combinaciones por hoja (`valid + invalid == total`, nunca negativo); `REQUIRED` para nombre/alcance vacíos; nombre inválido → campo `Nombre de rol*`; celda numérica en `Roles` → `UNEXPECTED_CELL_TYPE` y la fila se cuenta como inválida (no se descarta); en modo `roles`, `Result.profiles()` vacío y no se consumen ids de más |
| Servicio | `BulkCatalogWorkbookServiceTests` | archivo de 0 bytes → `EMPTY_FILE` en español; > 5 MB → `FILE_TOO_LARGE`; cifrado → `ENCRYPTED_FILE`; cabecera cambiada y primera fila de datos en la fila 1 → `INVALID_HEADERS`; > 1.000 filas → `TOO_MANY_ROWS` y solo se leen 1.000; celda numérica y fórmula; cabecera inválida solo en `RolesxRecurso`; `template` genera cabeceras iguales a las constantes de validación (ida y vuelta); `template("otra")` → `UnsupportedBulkModeException`; `errorReport` con archivo ilegible |
| Controlador | `BulkResourceControllerTests` (nueva, controlador construido a mano, `FilePart` anónimo) | `catalogTemplate("otra")` → `Mono.error(MalformedRequestFieldException)` con `field()=="mode"`; `validateCatalog`/`commitCatalog`/`catalogErrorReport` con servicio que lanza `UnsupportedBulkModeException` → `MalformedRequestFieldException`; las rutas pasan `principal` y `UserId` resueltos al servicio |

Total orientativo: 22–26 casos (los existentes se ajustan, no se duplican).

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | Generado | 2026-10-30 |
| Contrato aprobado (gate 1) | Pendiente | |
| Pruebas en rojo | Pendiente | |
| Implementación en verde | Pendiente | |
| Validación | Pendiente | |
| Entrega (gate 2) | Pendiente | |

## ¿Rompe un contrato público? ¿Depende el front?

**Frontend: no depende.** `securityBaseline-vue/src/api/index.ts` solo llama `/api/v1/resources/bulk/{template,validate,"",error-report}` (recursos, que no cambia). Búsqueda de `catalog-template`, `bulk/catalog`, `BULK_CATALOG`, `/catalog/` en `securityBaseline-vue`, `security-ui` y `securityBaseline-fr`: **sin resultados**. Tampoco hay documentación REST de estas rutas en `security-platform-architecture`. Ninguna otra consumidora en el monorepo (PEP/OPA).

Cambios de contrato **sí existentes** en las rutas `/catalog/**` (hoy sin consumidores conocidos, pero verifica que ninguna integración externa las use antes de publicar):

| Cambio | Antes → Después | Tipo |
|---|---|---|
| Estado HTTP | `mode` inválido en plantilla: 500 → 400; en validate/commit: se procesaba como perfiles → 400 | corrige un defecto |
| Cuerpo del 400 de plantilla | vacío → `ProblemDetail` (`MALFORMED_REQUEST_FIELD`, `field=mode`) | aditivo |
| Modalidad `resources-roles` en validate/commit | procesada como perfiles → 400 (sigue válida en plantilla) | restrictivo |
| Autorización | cualquier principal del tenant → solo administradores de la aplicación; sin `UserId` resoluble el endpoint falla como ya hace el de recursos (`withUser`) | restrictivo (el objetivo) |
| Códigos de error del archivo | `INVALID_WORKBOOK` genérico → `EMPTY_FILE` / `FILE_TOO_LARGE` / `ENCRYPTED_FILE` / `MISSING_SHEET` / `INVALID_HEADERS` / `TOO_MANY_ROWS` | cambia valores de `code` |
| Código por fila | aplicación ajena `INVALID_VALUE` → `APPLICATION_NOT_FOUND`; celda obligatoria vacía `INVALID_VALUE`/`INVALID_REFERENCE` → `REQUIRED`; celda numérica: ignorada → `UNEXPECTED_CELL_TYPE` | cambia valores de `code` |
| Mensajes | «no pertenece al tenant autenticado» → «No se encontró la aplicación en el tenant autenticado»; campo del nombre inválido `Aplicación`/`row` → cabecera de la columna | cambia texto |
| Contadores | semántica por hoja principal, siempre ≥ 0 | corrige un defecto |
| Formato del libro | plantillas **sin cambios** de hojas/cabeceras/orden; **cabeceras ahora obligatorias** al leer (libros que no respeten la plantilla se rechazan) | restrictivo |
| Esquema JSON `BulkResourceValidationResponse` | sin cambios | — |

## 11. Ambigüedades y riesgos

1. **Decidido sin consultar, revisable:** las filas de alcance `TENANT` no se gatean (paridad exacta con `AdministerRoleDefinitionUseCaseImpl`/`AdministerProfileDefinitionUseCaseImpl`: «un rol TENANT delega directo, sin gate»). Esto deja a cualquier principal creando roles/perfiles de alcance tenant y concediéndoles recursos de cualquier aplicación del tenant, por la vía masiva **y la individual**; es un hueco de ADR-023 («sin rol global») anterior a esta historia y requiere su propia decisión/HU. La HU-035 solo cierra el hueco por aplicación.
2. `MFA_REQUIRED` se colapsa en `NOT_AUTHORIZED` por fila (paridad con recursos). Que el usuario sepa que le falta el segundo factor sería una mejora de UX aparte.
3. Distinguir `APPLICATION_NOT_FOUND` de `NOT_AUTHORIZED` revela a un no administrador que la aplicación existe en su propio tenant (igual que recursos). No revela nada entre tenants. Si se quisiera ocultar también esto, bastaría devolver `NOT_AUTHORIZED` en ambos casos; no se hizo para no perder la pista útil al administrador que se equivoca de nombre.
4. `withUser` devuelve `IllegalStateException` (500) si el principal no tiene `UserId` resoluble; es el patrón existente de recursos, no se corrige aquí.
5. `spring.codec.max-in-memory-size=6MB` corta a nivel framework antes del tope de 5 MB de la aplicación (entre 5 y 6 MB responde el código `FILE_TOO_LARGE`; por encima, el error del códec). Igual que recursos.
6. Estas rutas no auditan (`AdministrationAuditRepository`), a diferencia de la vía individual (HU-021). Fuera de alcance; HU futura.
7. El `description` de errores de consulta (p. ej. `catálogo de aplicaciones caído`) se sigue copiando del mensaje de la excepción, como hoy (hay una prueba que lo exige); es una fuga de detalle interno ya existente.
8. El mismo mensaje «no pertenece al tenant autenticado» sigue en `BulkResourceImportService` (recursos): no se toca aquí.
