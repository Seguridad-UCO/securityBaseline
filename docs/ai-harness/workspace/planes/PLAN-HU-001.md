# PLAN: Consulta de aplicaciones protegidas con filtros y paginación

## Metadata

- **ID:** HU-001
- **Slice:** `applications`
- **Tipo:** Consulta
- **Fecha:** 2026-08-31
- **Rama sugerida:** `feature/HU-001-consulta-aplicaciones-paginada`
- **Fuentes:** `docs/ai-harness/workspace/HU-001.md`; hallazgo de la revisión del 2026-08-31 en `docs/criteria-compliance-matrix.md`
- **Criterios de la línea base que toca:** 1, 2, 5, 6, 9, 11, 12, 13, 14, **16, 17, 18, 19**, 20, 21, 22

## 0. Lo que ya existe — no se reescribe

La FASE 2 encontró que buena parte de la historia ya está construida y solo hay que conectarla:

| Pieza | Estado | Consecuencia |
|---|---|---|
| `PageWindow` | Completo: `ofPage`, `ofRange`, `defaultWindow`, `MAX_LIMIT=100`, `DEFAULT_LIMIT=20`, `page()` | **Se reutiliza tal cual.** Los criterios 18 y 19 viven aquí |
| `ResultPage<T>` | Completo, con `map()` | Se reutiliza para pasar de dominio a DTO sin perder `total` ni `window` |
| `PageResponse<T>` | Completo (`shared/web`) | Es el DTO plano de salida |
| `ApplicationName.contains(String)` | Ya compara sin distinguir mayúsculas | **La specification lo usa; no se reimplementa el filtro** |
| Tenant desde el principal | `SecurityContext.currentPrincipal()` en `ListApplicationsInteractorImpl` | El criterio 7 de la historia ya está cubierto estructuralmente |
| `GET /api/v1/applications` | Ya existe | **La historia evoluciona un endpoint, no crea uno** |
| `ConflictingRequestParametersException` | Existe en `shared/web/exception` | Es la excepción de la ventana ambigua. No se crea una nueva |

## 1. Resumen funcional

`GET /api/v1/applications` pasa de devolver el catálogo completo del inquilino a devolver una
**página filtrable**. El filtro es por fragmento de nombre; la ventana admite `page`/`size` o
`offset`/`limit`. El inquilino sigue saliendo del principal autenticado.

No cubre: ordenamiento configurable por el cliente, filtros sobre recursos protegidos, ni cablear la
saga de compensación del criterio 10.

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Sin parámetros | Primera página (`offset=0`, `limit=20`) del inquilino del principal, orden estable por `registeredAt DESC` |
| 2 | `?name=port` | Solo las aplicaciones cuyo nombre contiene "port", sin distinguir mayúsculas |
| 3 | `?page=1&size=5` | Devuelve la ventana pedida y el `total` real del filtro, no el de la página |
| 4 | `?offset=5&limit=5` | Mismo contenido que `?page=1&size=5` |
| 5 | `?page=1&offset=5` | **400** `CONFLICTING_REQUEST_PARAMETERS` nombrando el campo |
| 6 | `?size=101` o `?size=0` | **400** `MALFORMED_REQUEST_FIELD` con la razón del rango |
| 7 | Principal de otro inquilino | Nunca ve aplicaciones ajenas, ni siquiera filtrando |
| 8 | Filtro sin resultados | Página vacía, `total=0`, **200** — nunca 404 |

## 3. Reglas de negocio

Esta historia **no añade ninguna `Rule`**: es una consulta, y una consulta no viola invariantes de
negocio. Todo lo que puede fallar es contrato de entrada, y eso se rechaza en el borde.

| # | Regla | Dónde vive | Excepción → HTTP |
|---|---|---|---|
| 1 | El `size`/`limit` está entre 1 y 100 | Constructor de `PageWindow` (ya existe) | `InvalidPageWindowException` → **400** |
| 2 | El `offset`/`page` no es negativo | Constructor de `PageWindow` (ya existe) | `InvalidPageWindowException` → **400** |
| 3 | No se mezclan `page`/`size` con `offset`/`limit` | `ListApplicationsRequestMapper` | `ConflictingRequestParametersException` → **400** |
| 4 | Los parámetros numéricos son enteros | `RequestFieldParser.parseInt` (ya existe) | `MalformedRequestFieldException` → **400** |
| 5 | Solo se ven aplicaciones del propio inquilino | `ApplicationCriteria.tenantId` es obligatorio y sale del principal | — (no hay caso de error: el filtro es estructural) |

> El aislamiento entre inquilinos **no es una regla que lance**: es un criterio obligatorio del
> objeto de consulta. `ApplicationCriteria` no se puede construir sin `TenantId`, así que una
> consulta sin inquilino no compila.

## 4. Modelo de dominio afectado

### Specification (nueva)

`ApplicationCriteria` — objeto de dominio que expresa *qué* se busca, sin decir *cómo* se ejecuta.
Expone `matches(Application)` para poder probarla aislada y para que cualquier adaptador futuro en
memoria la respete.

**Nombre:** se usa `ApplicationCriteria`, no `ProtectedApplicationCriteria` como decía la
documentación retirada: la entidad de hoy es `Application` y el vocabulario `ProtectedApplication*`
pertenece al esquema anterior al renombrado.

### Value objects

Ninguno nuevo. `PageWindow`, `ApplicationName` y `TenantId` ya existen y cubren la historia.

### Entidades

`Application` no cambia.

## 5. Persistencia

- **Tabla:** `application` (`ApplicationSchema.TABLE`, ya existe). Sin migración.
- **Consulta nueva** en el adaptador, dos sentencias en una sola llamada:

  ```
  SELECT * FROM application WHERE tenantId = $tenantId [AND string::lowercase(name) CONTAINS $name]
    ORDER BY registeredAt DESC LIMIT $limit START $offset;
  SELECT count() FROM application WHERE tenantId = $tenantId [AND ...] GROUP ALL;
  ```

  El fragmento del filtro se añade **solo si el criterio lo trae**; los valores viajan siempre como
  parámetros, nunca concatenados.

- **Se retira `findAllByTenant`** del puerto: con `findBy(criteria, window)` pasa a ser el método
  específico por consulta que el criterio 16 prohíbe. Si al implementarlo queda algún consumidor
  además de `ListApplicationsUseCaseImpl` y `SurrealRepositoryIntegrationTests`, **detente y
  repórtalo** en vez de dejar los dos caminos.

## 6. Endpoint

| Verbo | Ruta | Éxito | Entrada | Salida |
|---|---|---|---|---|
| GET | `/api/v1/applications` | **200** `APPLICATIONS_LISTED` | Query: `name`, `page`, `size`, `offset`, `limit` — **todos opcionales y todos `String`** | `ApiResponse<PageResponse<ApplicationWebResponse>>` |

- **Autorización:** requiere token. El inquilino sale del principal, **nunca de la query**.
- **Cambio de contrato:** `data` deja de ser un array y pasa a ser `PageResponse`. Acompaña el
  cambio del frontend (sección 8b).

### Resolución de la ventana (criterio 19)

| Parámetros presentes | Resultado |
|---|---|
| ninguno | `PageWindow.defaultWindow()` → `offset=0`, `limit=20` |
| `page` y/o `size` | `PageWindow.ofPage(page, size ?: 20)`, con `page` por defecto 0 |
| `offset` y/o `limit` | `PageWindow.ofRange(offset ?: 0, limit ?: 20)` |
| uno de cada grupo | `ConflictingRequestParametersException` |

## 7. SPEC — el contrato

> Firmas exactas. Las piezas marcadas **[N]** se materializan como esqueletos en la FASE 5; las
> marcadas **[M]** cambian un contrato existente y las ejecuta el implementador (ver la nota al
> final de esta sección).

### [N] Specification de dominio

```java
// pdp/applications/domain/ApplicationCriteria.java
public record ApplicationCriteria(TenantId tenantId, Optional<String> nameContains) {
    public ApplicationCriteria { }                                  // requireNonNull de ambos
    public static ApplicationCriteria of(TenantId tenantId, Optional<String> nameContains);
    public static ApplicationCriteria ofTenant(TenantId tenantId);  // sin filtro
    public boolean matches(Application application);
}
```

`matches` = mismo inquilino **y** (`nameContains` ausente **o** `application.name().contains(fragmento)`).

### [N] DTO de entrada al núcleo

```java
// pdp/applications/application/primaryport/request/ListApplicationsRequest.java
public record ListApplicationsRequest(ApplicationCriteria criteria, PageWindow window) {
    public ListApplicationsRequest { }                              // requireNonNull de ambos
}
```

### [N] DTO crudo HTTP

```java
// pdp/applications/infrastructure/adapter/primary/web/dto/request/raw/ListApplicationsRawRequest.java
public record ListApplicationsRawRequest(String name, String page, String size, String offset, String limit) { }
```

### [N] Mapper de consulta

```java
// pdp/applications/infrastructure/adapter/primary/web/mapper/ListApplicationsRequestMapper.java
public final class ListApplicationsRequestMapper {
    private ListApplicationsRequestMapper() { }
    public static ListApplicationsRequest toRequest(ListApplicationsRawRequest raw, TenantId tenantId);
}
```

Aquí vive toda la tabla de resolución de la ventana y el rechazo de la combinación ambigua.
Usa `RequestFieldParser.optional` y `RequestFieldParser.parseInt`; **no construye enteros a mano**.

### [M] Puerto de salida

```java
// pdp/applications/application/secondaryport/repository/ApplicationRepository.java
Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window);   // añadir
Flux<Application> findAllByTenant(TenantId tenantId);                                    // retirar
```

### [M] Caso de uso

```java
// pdp/applications/application/usecase/ListApplicationsUseCase.java
public interface ListApplicationsUseCase
        extends ReactiveOperation<ListApplicationsRequest, ResultPage<RegisteredApplicationResponse>> {
}
```

Antes era `ReactiveStreamOperation<TenantId, RegisteredApplicationResponse>`.
La implementación delega en `repository.findBy(...)` y cierra con `.map(page -> page.map(...))`.

### [M] Interactor

```java
// .../web/interactor/ListApplicationsInteractor.java
public interface ListApplicationsInteractor
        extends ReactiveOperation<ListApplicationsRawRequest, PageResponse<ApplicationWebResponse>> {
}
```

Obtiene el principal, llama al mapper con su `tenantId`, ejecuta el caso de uso y proyecta el
`ResultPage` a `PageResponse` (`content`, `total`, `page`, `offset`, `limit`).

### [M] Controller

```java
// .../web/controller/ApplicationController.java
@GetMapping
Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationWebResponse>>>> list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String page,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String offset,
        @RequestParam(required = false) String limit,
        ServerWebExchange exchange);
```

Todos `String` y `required = false` (criterio 6). El controller solo ensambla el raw y delega.

> **Nota sobre [N] frente a [M].** El planificador materializa únicamente lo nuevo. Materializar
> también las modificaciones dejaría el proyecto en rojo —los consumidores actuales y sus pruebas
> dejarían de compilar— y el planificador no toca `src/test`. Las firmas [M] son contrato igual que
> las [N]: el implementador las aplica tal cual y no las renegocia.

## 8. Árbol de archivos

```
src/main/java/co/edu/uco/seguridad/
└── pdp/applications/
    ├── domain/
    │   └── ApplicationCriteria.java                                            [N]
    ├── application/
    │   ├── primaryport/request/ListApplicationsRequest.java               [N]
    │   ├── secondaryport/repository/ApplicationRepository.java                [M]
    │   ├── usecase/ListApplicationsUseCase.java                                [M]
    │   └── usecase/impl/ListApplicationsUseCaseImpl.java                       [M]
    └── infrastructure/
        ├── adapter/primary/web/
        │   ├── controller/ApplicationController.java                           [M]
        │   ├── dto/request/raw/ListApplicationsRawRequest.java                 [N]
        │   ├── interactor/ListApplicationsInteractor.java                      [M]
        │   ├── interactor/impl/ListApplicationsInteractorImpl.java             [M]
        │   └── mapper/ListApplicationsRequestMapper.java                       [N]
        └── adapter/secondary/persistence/repository/SurrealApplicationRepository.java  [M]
```

`ApplicationsConfiguration` **no cambia**: los mappers son estáticos y ningún bean nuevo aparece.
Las firmas de los beans existentes se mantienen.

### 8b. Frontend (`securityBaseline-fr`)

| Archivo | Cambio |
|---|---|
| `src/api.js` | `listApplications` acepta parámetros opcionales y los serializa como query |
| `src/App.jsx` | `appResult.data` pasa a `appResult.data.content` |

Es el mínimo para que la consola siga funcionando. Paginación en la interfaz: fuera de alcance.

## 9. Casos de prueba esperados

| Capa | Clase | Casos |
|---|---|---|
| `domain` | `ApplicationCriteriaTests` **[N]** | Sin filtro acepta cualquiera del inquilino · con filtro acepta por fragmento sin distinguir mayúsculas · rechaza otro inquilino · rechaza nombre que no contiene · `tenantId` nulo lanza |
| `application` | `ListApplicationsUseCaseImplTests` **[M]** | Devuelve la página con `total` del filtro · página vacía con `total=0` · propaga la ventana al puerto sin alterarla |
| `infrastructure` | `ListApplicationsRequestMapperTests` **[N]** | Sin parámetros → ventana por defecto · `page`+`size` · `offset`+`limit` · `size` fuera de rango lanza · mezcla de grupos lanza `ConflictingRequestParametersException` · `page` no numérico lanza · `name` en blanco se trata como ausente |
| `infrastructure` | `ApplicationControllerTests` **[M]** | `list` ensambla el raw con los cinco parámetros y responde 200 con la página |
| integración | `SurrealApplicationRepositoryTests` o ampliar `SurrealRepositoryIntegrationTests` **[M]** | `findBy` filtra, ordena, recorta y devuelve el `total` completo |
| e2e | `ApplicationHttpTests` **[N]** | Flujo autenticado: sin filtro · con filtro · segunda página · ventana ambigua da 400 · **un inquilino no ve las aplicaciones de otro** |

Convenciones en `sb-testing`: **nada de Mockito**, fakes anónimos y lambdas, `StepVerifier` para lo
reactivo, `{Clase}Tests` y métodos en snake_case inglés.

`ApplicationHttpTests` extiende `AbstractSurrealDbIntegrationTest` y firma sus tokens con
`TestJwtSupport`. **Requiere Docker**, igual que las pruebas de persistencia ya existentes.

Presupuesto: **22-26 pruebas**.

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-08-31 |
| Contrato aprobado (gate 1) | ✅ Aprobado | 2026-08-31 |
| Pruebas en rojo | ✅ 22 pruebas fallando por `UnsupportedOperationException` | 2026-08-31 |
| Implementación en verde | ✅ 225 pruebas; 98-100 % de cobertura en lo nuevo | 2026-08-31 |
| Validación | ⏳ Pendiente | |
| Entrega (gate 2) | ⏳ Pendiente | |

### Desviaciones respecto al plan

| Desviación | Por qué |
|---|---|
| `RequiredArgumentMessages` gana tres constantes | El plan no las declaró, pero la convención exige que `requireNonNull` no lleve literales |
| `LIMIT`/`START` se interpolan en la consulta en vez de ir como parámetros | `SurrealDbClient.execute` solo acepta `Map<String,String>` y SurrealQL exige números ahí. Son dos `int` que `PageWindow` ya validó, no texto de usuario; el filtro de nombre, que sí lo es, va ligado |
| El mapper comprueba los rangos además del value object | El borde HTTP debe decir **qué campo** viene mal (criterio 6). No duplica ni el umbral (`PageWindow.MAX_LIMIT`) ni el texto (catálogo) |
| Se actualizaron cuatro clases de prueba existentes | Sus fakes del puerto implementaban `findAllByTenant`. Consecuencia directa de una firma `[M]` |

## 11. Al cerrar la historia

1. Borrar de `docs/ai-harness/drift-ignore.txt` el bloque "Pendientes de HU-001" (tres líneas).
2. Quitar los avisos de estado de los 13 documentos afectados.
3. Renombrar en la documentación `ProtectedApplicationHttpTests` → `ApplicationHttpTests` y
   `ProtectedApplicationCriteria` → `ApplicationCriteria`.
4. Actualizar `criteria-compliance-matrix.md`, `baseline-criteria-overview.md`,
   `evidence/verification-guide.md` y la skill `sb-criterios`: **16-19 pasan a cumplidos, 22/23**.
5. `drift.ps1 -Estricto` debe salir en verde sin las excepciones borradas.

## 12. Ambigüedades pendientes

Ninguna. Las tres decisiones abiertas se resolvieron en la FASE 3: evolucionar el endpoint y
acompañar el frontend, ejecutar filtro y paginación en la consulta a SurrealDB, y reponer la prueba
end-to-end con Testcontainers.
