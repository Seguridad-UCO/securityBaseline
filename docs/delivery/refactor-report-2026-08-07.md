# Informe técnico de la refactorización arquitectónica

**Fecha:** 2026-08-07 · **Base:** commit `6a31033` · **Alcance:** arquitectura, DTOs, reglas,
excepciones, pipelines, SonarQube y manejo de secretos.

---

## A. Resumen ejecutivo

### Qué se encontró

El código funcionaba y sus tres pruebas pasaban, pero era considerablemente más pequeño que la
arquitectura que la documentación describía. El hallazgo central no fue un desvío de detalle:

> **La documentación describía clases que nunca existieron.** Ocho de los veintitrés criterios
> estaban documentados como implementados sin tener código detrás.

Clases citadas en `docs/` que no existían en `src/`: `ProtectedApplication`,
`ProtectedApplicationCriteria`, `PageWindow`, `ApplicationPage`, `SearchProtectedApplicationsUseCase`,
`ProtectedApplicationMapper`, `ProtectedApplicationRepository`, `InMemoryProtectedApplicationRepository`,
`ReactiveTransactionPort`, `SnapshotReactiveTransactionAdapter`, `InMemoryAuditAdapter`,
`DomainException`, `TimeProvider`, `ApplicationIdGenerator`, `ResourceIdentifier`,
`ProtectedApplicationTests`, `ProtectedApplicationServiceTests`.

Esto pasó inadvertido porque **todos los enlaces de “Ubicación verificable” apuntaban al directorio
del paquete y no al archivo**, de modo que ninguno estaba roto aunque el destino no existiera. La
documentación también afirmaba “ocho pruebas”; había tres.

### Qué estaba mal, además de eso

- Todo el código estaba escrito en una sola línea por clase: constructores, campos y métodos
  comprimidos, imports con comodín. Ilegible y sin posibilidad real de revisión.
- Las reglas de negocio eran ternarios dentro de los servicios. No se podían probar por separado ni
  nombrar en una conversación con el negocio.
- Solo existían dos excepciones, ambas sin jerarquía común, y el handler traducía
  `IllegalArgumentException` genérica como “error de validación”.
- El adaptador de auditoría era `(app, resource) -> Mono.empty()`: un no-op presentado como
  adaptador.
- `ReactiveLogContext` existía completo y **no se invocaba desde ningún sitio**. Código muerto.
- `Instant.now()` y `UUID.randomUUID()` en línea dentro de los casos de uso: imposible fijar el
  tiempo o el identificador en una prueba.
- Los puertos eran interfaces anidadas dentro de sus propias implementaciones
  (`ApplicationsService.ApplicationStore`), lo que invierte la dependencia al revés.
- El controlador devolvía el modelo de lectura del núcleo directamente como JSON.
- El DTO de entrada dependía de anotaciones Jakarta, justo lo que Farid pidió eliminar.
- No existía consulta de catálogo: sin criterio, sin paginación, sin rangos.
- No existía frontera transaccional.
- El pipeline solo disparaba en `main` y no tenía análisis estático ni despliegue.
- Siete archivos `.DS_Store` versionados.

### Qué se modificó

117 clases de producción y 14 de prueba, organizadas en cuatro módulos más un paquete transversal.
109 pruebas, 92,7 % de cobertura de instrucciones. Pipeline de tres ambientes con Quality Gate.
Azure Key Vault provisionado por Bicep. Los 23 criterios con evidencia verificable.

**Ninguna funcionalidad existente se eliminó.** La prueba HTTP original sigue pasando sin cambios en
sus aserciones, que es la comprobación de que el contrato público se conservó.

---

## B. Arquitectura antes

```text
co.edu.uco.seguridad
├── shared/
│   ├── observability/ReactiveLogContext        ← nunca invocado
│   └── web/{ApiResponse, CorrelationWebFilter, RequestContext}
└── pdp/
    ├── commons/{TenantId, ApplicationId, ResourceId}
    ├── tenants/
    │   ├── TenantModuleApi (requireActive: API + regla mezcladas)
    │   ├── TenantUnavailableException           ← dos causas distintas, un solo tipo
    │   ├── application/TenantLookupService
    │   │       └── interface TenantStore        ← puerto anidado en la implementación
    │   └── infrastructure/TenantConfiguration   ← tenants hardcodeados en un Set.of(...)
    ├── aplicaciones/
    │   ├── ApplicationsModuleApi · RegisterApplicationCommand(TenantId, String)
    │   ├── domain/{Application, ApplicationName} ← records anémicos, sin factoría
    │   ├── application/ApplicationsService       ← reglas como ternarios; Instant.now() en línea
    │   │       └── interface ApplicationStore    ← puerto anidado
    │   └── infrastructure/ApplicationsConfiguration ← dummy como clase interna del @Configuration
    └── recursos/
        ├── RegisterProtectedApplicationUseCase
        ├── RegisterProtectedApplicationCommand(String, String, String, String)  ← sin tipar
        ├── domain/{ProtectedResource, ResourceCode, ActionCode}
        ├── application/RegisterProtectedApplicationUseCaseImpl
        │       ├── interface ResourceStore       ← puerto anidado
        │       └── interface AuditPort           ← implementado con Mono.empty()
        └── infrastructure/web/
            ├── ProtectedApplicationController    ← construía el comando; devolvía el tipo del núcleo
            ├── RegisterProtectedApplicationRequest ← @NotBlank @Size @Pattern
            └── ApiErrorHandler                   ← sin manejo de fallos no previstos

Flujo:  HTTP → @Valid → Controller → UseCase(reglas embebidas) → Store
```

Ausentes: interactores, rules, rules validators, jerarquía de excepciones, criterio de consulta,
paginación, rangos, transacción, entidades de persistencia, mappers, reloj, generador de
identificadores.

---

## C. Arquitectura después

```text
co.edu.uco.seguridad
├── shared/                                      capacidades transversales (fuera del análisis Modulith)
│   ├── rule/      BusinessRule · ReactiveBusinessRule · ReactiveBusinessRuleWithResult
│   ├── port/      TimeProvider · IdentifierGenerator · ReactiveTransactionPort
│   ├── web/       ApiResponse · PageResponse · RequestContext · CorrelationWebFilter
│   │              RequestContractException + 3 subtipos
│   ├── observability/ ReactiveLogContext        ← ahora aplicado en los 4 casos de uso
│   └── config/    SharedPortsConfiguration
└── pdp/
    ├── commons/                                 shared kernel, Java puro, sin Reactor
    │   ├── TenantId · ApplicationId · ResourceId · ApplicationName
    │   ├── PageWindow · ResultPage
    │   └── DomainException → InvalidValueException | BusinessRuleViolationException
    ├── tenants/
    │   ├── TenantModuleApi (consulta)  ·  TenantMustBeActiveRule (decisión)
    │   ├── TenantNotFoundException · TenantNotActiveException · TenantStatus
    │   ├── domain/Tenant
    │   ├── application/{TenantLookupService, port/out/TenantRepository, rule/…}
    │   └── infrastructure/{TenantConfiguration, TenantCatalogProperties, persistence/…}
    ├── aplicaciones/
    │   ├── ApplicationsModuleApi · comando tipado · DuplicateApplicationException
    │   │   · ReservedApplicationNameException
    │   ├── domain/Application (factoría + comportamiento)
    │   ├── application/
    │   │   ├── ApplicationsService
    │   │   ├── port/out/ApplicationRepository
    │   │   └── rule/  RegisterApplicationRulesValidator
    │   │              ApplicationNameMustNotBeReservedRule      (sin repositorio)
    │   │              ApplicationNameMustBeUniqueForTenantRule  (con repositorio)
    │   └── infrastructure/{ApplicationsConfiguration, persistence/{Entity, Mapper, Repository}}
    └── recursos/
        ├── RegisterProtectedApplicationUseCase · SearchProtectedApplicationsUseCase
        ├── comando y query tipados · ProtectedApplicationCatalogEntry
        ├── DuplicateProtectedResourceException · ResourceTenantMismatchException
        ├── domain/{ProtectedResource, ResourceCode, ActionCode,
        │           ProtectedApplicationCriteria, 2 excepciones de valor}
        ├── application/
        │   ├── RegisterProtectedApplicationUseCaseImpl · SearchProtectedApplicationsUseCaseImpl
        │   ├── mapper/ProtectedResourceCatalogMapper
        │   ├── port/out/{ProtectedResourceRepository, AuditPort}
        │   └── rule/  RegisterProtectedApplicationRulesValidator
        │              SearchProtectedApplicationsRulesValidator
        │              ProtectedResourceMustBelongToApplicationTenantRule (sin repositorio)
        │              ProtectedResourceMustBeUniqueRule                  (con repositorio)
        └── infrastructure/
            ├── ResourcesConfiguration
            ├── audit/InMemoryAuditAdapter
            ├── persistence/{Entity, Mapper, Repository, SnapshotReactiveTransactionAdapter}
            └── web/
                ├── ProtectedApplicationController · ApiErrorHandler · WebConfiguration
                ├── dto/     RawRequest · Request(setters) · RawQuery · Request · Response
                │            RequestFieldParser
                ├── mapper/  3 mappers
                └── interactor/ 2 interfaces + 2 implementaciones
```

---

## D. Cambios de código

### Interfaces creadas (18)

`BusinessRule`, `ReactiveBusinessRule`, `ReactiveBusinessRuleWithResult`, `TimeProvider`,
`IdentifierGenerator`, `ReactiveTransactionPort`, `TenantMustBeActiveRule`, `TenantRepository`,
`ApplicationRepository`, `ProtectedResourceRepository`, `AuditPort`,
`SearchProtectedApplicationsUseCase`, `RegisterProtectedApplicationInteractor`,
`SearchProtectedApplicationsInteractor`, más las 4 interfaces de regla individuales.

### Rules creadas (5) y sus validators (3)

| Rule | Repositorio | Validator que la compone |
|---|---|---|
| `ApplicationNameMustNotBeReservedRule` | no | `RegisterApplicationRulesValidator` |
| `TenantMustBeActiveRule` | sí | `RegisterApplicationRulesValidator`, `SearchProtectedApplicationsRulesValidator` |
| `ApplicationNameMustBeUniqueForTenantRule` | sí | `RegisterApplicationRulesValidator` |
| `ProtectedResourceMustBelongToApplicationTenantRule` | no | `RegisterProtectedApplicationRulesValidator` |
| `ProtectedResourceMustBeUniqueRule` | sí | `RegisterProtectedApplicationRulesValidator` |

Cada una con interfaz e implementación `Default…`, registrada como bean para poder sustituirse.

### Excepciones creadas (15)

Bases: `DomainException`, `InvalidValueException`, `BusinessRuleViolationException`,
`RequestContractException`.
Valor: `InvalidTenantIdException`, `InvalidApplicationNameException`, `InvalidIdentifierException`,
`InvalidPageWindowException`, `InvalidResourceCodeException`, `InvalidActionCodeException`.
Regla: `TenantNotFoundException`, `TenantNotActiveException`, `ReservedApplicationNameException`,
`DuplicateProtectedResourceException`, `ResourceTenantMismatchException`.
Contrato HTTP: `MissingRequestFieldException`, `MalformedRequestFieldException`,
`ConflictingRequestParametersException`.

`TenantUnavailableException` **se eliminó** y se dividió en dos: un tenant desconocido es una
petición equivocada, uno suspendido es una decisión temporal de política, y el cliente hace cosas
distintas con cada uno.

### Value objects

Creados: `PageWindow`, `ResultPage`, `TenantStatus`.
Movido: `ApplicationName` de `aplicaciones/domain` a `commons` — ambos módulos hablan del mismo
concepto y `recursos` no puede importar el interior de `aplicaciones`.
Reforzados: `TenantId` (formato, antes solo no-vacío), `ApplicationId` y `ResourceId` (factoría
`of(String)`), `ResourceCode` y `ActionCode` (excepción propia en vez de `IllegalArgumentException`).

### Records: dónde sí y dónde no

Se usaron records para todas las entidades y value objects del dominio, los comandos, los resultados
publicados y los DTO crudos y de respuesta.

Se usó **clase mutable** en exactamente dos sitios, ambos justificados y ambos fuera del dominio:
entidades de persistencia (los drivers lo exigen) y DTO validados (la validación por setter es la
estrategia pedida). Un record no habría servido para el DTO validado: su constructor canónico
valida todo de una vez y no deja un lugar con nombre por campo.

### DTOs y mappers

| Antes | Después |
|---|---|
| `RegisterProtectedApplicationRequest` con Jakarta | `RegisterProtectedApplicationRawRequest` (record, `String`) + `RegisterProtectedApplicationRequest` (setters validadores) |
| — | `SearchProtectedApplicationsRawQuery` + `SearchProtectedApplicationsRequest` |
| Se devolvía `ProtectedApplicationCatalogEntry` | `ProtectedApplicationResponse` + `PageResponse<T>` |
| — | `RegisterProtectedApplicationRequestMapper`, `SearchProtectedApplicationsRequestMapper`, `ProtectedApplicationResponseMapper`, `ProtectedResourceCatalogMapper`, 3 mappers de persistencia |

### Clases eliminadas

`ApplicationsService.ApplicationStore`, `RegisterProtectedApplicationUseCaseImpl.ResourceStore`,
`RegisterProtectedApplicationUseCaseImpl.AuditPort` (puertos anidados, extraídos a `port/out`);
`TenantLookupService.TenantStore`; `TenantUnavailableException`; el DTO anotado con Jakarta;
`aplicaciones/domain/ApplicationName` (movido); 7 archivos `.DS_Store`.

### Dependencias

- **Retirada:** `spring-boot-starter-validation`. No basta con dejar de usar las anotaciones —
  mientras la API esté en el classpath alguien acabará añadiendo un `@NotBlank`. Sin la
  dependencia, el compilador lo impide.
- **Añadida:** `jacoco-maven-plugin` 0.8.15 más las propiedades `sonar.*`.

---

## E. Pipelines

| Rama | Build · Test · Sonar | Despliegue |
|---|---|---|
| `feature/*` | sí | ninguno |
| `develop` | sí | **DEV** |
| `qa` | sí | **QA** |
| `main` | sí | **PRODUCCIÓN** |
| PR a `develop`/`qa`/`main` | sí | ninguno |

Antes: trigger únicamente en `main`, dos stages, sin análisis, sin empaquetado, sin despliegue.

Un solo pipeline con plantillas (`ci/templates`, `ci/variables`), no uno por ambiente: la
verificación debe ser idéntica en las tres ramas y duplicar el YAML es cómo se logra que deje de
serlo. El stage de despliegue está parametrizado y se instancia tres veces.

Los stages de despliegue comprueban la rama exacta **y** que la ejecución no sea de un pull request.
Lo segundo no es redundante: sin ello, un PR hacia `main` desplegaría producción con código todavía
no aprobado.

**SonarQube.** `SonarQubePrepare@7` → `Maven@4 verify` con `sonarQubeRunAnalysis` → `SonarQubePublish@7`.
El Quality Gate rompe el build gracias a `sonar.qualitygate.wait=true`; sin esa propiedad el
análisis se publica pero el pipeline sigue en verde, que es la falla silenciosa habitual de esta
integración. Cobertura desde JaCoCo, con `*Configuration.java` excluido porque es cableado y
cubrirlo infla el porcentaje sin probar ninguna regla.

**Secretos.** Ninguno en el YAML. Llegan por service connection y por `AzureKeyVault@2`.

Detalle: `verify` corre compilación, pruebas, reporte JaCoCo y análisis en un solo reactor, porque
Sonar necesita las clases, los reportes de surefire y el XML del **mismo** workspace.

---

## F. GitHub + Azure DevOps

```text
Desarrollador ──push──► GitHub (Seguridad-UCO/securityBaseline)
                            │ webhook
                            ▼
                     Azure Pipelines
                     build · test · JaCoCo · SonarQube
                            ▼
                       Quality Gate
                            │
       status check ◄───────┘
                            ▼
   PR bloqueado hasta que el check esté en verde
                            │ merge
                            ▼
   Azure Pipelines ──► Key Vault ──► App Service (DEV/QA/PROD)
```

El repositorio vive en GitHub y el pipeline en Azure DevOps, conectados por service connection. El
resultado vuelve al PR como status check y se marca requerido en la protección de rama, de modo que
un PR con el gate en rojo no se puede mezclar.

Aprobaciones: las de código en GitHub (branch protection), las de despliegue en Azure DevOps sobre
los objetos `environment`. Deliberadamente separadas — una edición del YAML no puede eliminar una
compuerta de producción.

---

## G. Key Vault

**La respuesta directa a la pregunta de Farid: hoy la aplicación no tiene ningún secreto.** No hay
base de datos, ni proveedor de identidad, ni exportador remoto de telemetría; todos los adaptadores
son dummies en memoria. La auditoría del repositorio lo confirma — las únicas coincidencias de
`password|secret|token|credential` son la documentación del propio wrapper de Maven. No hay `.env`,
`.pem`, `.jks` ni credenciales en `application.properties`.

No es que los secretos estuvieran mal guardados: es que todavía no existen. Lo que se entrega es el
mecanismo para cuando aparezcan.

**Provisionado** (`infra/keyvault/main.bicep`, un vault por ambiente): RBAC en lugar de access
policies, `Key Vault Secrets User` para los dos principals, soft delete con purge protection,
retención de 90 días en producción y 7 en el resto, y diagnósticos `AuditEvent` a Log Analytics.
El template **no crea valores de secreto**: un secreto escrito en una plantilla es un secreto
versionado en Git.

**Reservados:** `pdp-datasource-password` (SurrealDB, E-2), `pdp-oidc-client-secret` (IdP, E-3),
`pdp-otlp-token` (colector).

**Cómo se consumen:** el pipeline los lee con `AzureKeyVault@2` y los inyecta como app settings; la
aplicación los verá como variables de entorno y los referenciará por marcador
(`${PDP_DATASOURCE_PASSWORD}`), nunca por valor. La aplicación tiene además su propia identidad
administrada con acceso de lectura, dejando lista la ruta de `spring-cloud-azure-starter-keyvault-secrets`
sin activarla: añadir hoy ese starter cargaría un componente que no tiene ningún secreto que
resolver.

**Fuera del vault, y por qué:** identificadores de tenant, nombres reservados, configuración de
Actuator y nombres de recursos de Azure. Son configuración pública; tratar todo como secreto hace
que el vault deje de significar algo.

---

## H. Los 23 criterios

La matriz completa —estado inicial, problema, cambio y estado final— está en
[`docs/criteria-compliance-matrix.md`](../criteria-compliance-matrix.md).

Resumen del punto de partida: **1 criterio cumplía, 14 estaban parciales y 8 no se cumplían pese a
estar documentados como cumplidos.** Los 8 sin implementar eran 2, 7, 10, 16, 17, 18, 19 y 8 (este
último por código muerto). Estado final: los 23 con evidencia verificable.

---

## I. Validaciones

```text
JSON
 │   binding de Jackson — SIEMPRE tiene éxito: todos los campos son String sin restricciones
 ▼
RegisterProtectedApplicationRawRequest          (record inerte, no promete nada)
 │   RegisterProtectedApplicationRequestMapper.toValidatedRequest
 │   invoca los cuatro setters en orden
 ▼
RegisterProtectedApplicationRequest             ◄── AQUÍ ocurre la validación
 │   setTenantId        → presencia → TenantId       → MISSING_ / MALFORMED_REQUEST_FIELD
 │   setApplicationName → presencia → ApplicationName
 │   setResourceCode    → presencia → ResourceCode
 │   setAction          → presencia → ActionCode
 ▼
 │   toCommand — puro renombrado: ya no queda nada por comprobar
 ▼
RegisterProtectedApplicationCommand             (todos los campos son value objects)
 ▼
Use Case → Rules Validator → Rules → Domain
```

**Dónde se ejecuta cada validación y por qué ahí:**

| Validación | Dónde | Motivo |
|---|---|---|
| Presencia del campo | setter del DTO validado | Es lo único que sabe qué nombre de campo reportar |
| Formato y longitud | constructor del value object | Debe aplicar también cuando el caso de uso no viene de HTTP |
| Combinación de parámetros | `setResultWindow` | La validez es de la combinación, no de un valor suelto |
| Política de negocio | rules, vía rules validator | Puede requerir repositorio y puede cambiar sin tocar el tipo |

El punto clave del diseño: `RequestFieldParser.parse` **delega el formato al value object** y solo
añade qué campo lo traía. Si repitiera aquí la expresión regular, la frontera tendría una segunda
definición de “código válido” que podría divergir de la del dominio.

Y el motivo de todo el esquema: con anotaciones Jakarta, un valor inválido lo rechaza el framework
antes de que nuestro código lo vea, y el cliente recibe un error que no controlamos, sin código
estable y sin identificadores de correlación. Con todos los campos `String` y sin restricciones, el
binding nunca falla y **cada rechazo es una decisión nuestra**.

---

## J. Rules

```text
Use Case
   │
   ▼
Rules Validator                     uno por funcionalidad; el caso de uso depende de él
   │                                y no de las reglas individuales
   ├── Rules SIN repositorio        void verify(I)   — síncronas, se ejecutan primero
   │   ├── ApplicationNameMustNotBeReservedRule            → ReservedApplicationNameException
   │   └── ProtectedResourceMustBelongToApplicationTenantRule → ResourceTenantMismatchException
   │
   └── Rules CON repositorio        Mono<Void> verify(I)  ·  Mono<O> verify(I) si devuelve valor
       ├── TenantMustBeActiveRule                         → TenantNotFoundException
       │                                                    TenantNotActiveException
       ├── ApplicationNameMustBeUniqueForTenantRule       → DuplicateApplicationException
       └── ProtectedResourceMustBeUniqueRule              → DuplicateProtectedResourceException
```

**Orden:** las reglas sin repositorio primero. Una petición inválida se rechaza sin tocar el
almacenamiento.

**Sin duplicación:** `TenantMustBeActiveRule` la publica `tenants` y la consumen `aplicaciones` y
`recursos`. Es una única implementación inyectada, no una comprobación copiada, de modo que la
decisión no puede divergir. Y `recursos` **no** vuelve a validar el tenant al registrar: ya lo hizo
`aplicaciones` con esa misma regla. Repetirlo sería una segunda decisión sobre lo mismo y una
consulta de más.

**Una regla, una excepción, y en los dos sentidos:** cada regla lanza exactamente una excepción y
cada excepción la lanza exactamente una regla.

---

## K. Validación final

| Comprobación | Resultado |
|---|---|
| Compilación | **BUILD SUCCESS** (`clean verify`) |
| Pruebas | **109 ejecutadas, 0 fallos, 0 errores, 0 omitidas** |
| Cobertura de instrucciones | **92,7 %** (3051 / 3291) |
| Cobertura de líneas | **91,5 %** (635 / 694) |
| Cobertura de ramas | **89,5 %** (111 / 124) |
| Clases con cobertura | 90 de 90 |
| Verificación Modulith | Pasa |
| Imports sin usar | 0 |
| Imports con comodín | 0 |
| Lombok | 0 usos |
| Jakarta Validation | 0 usos, dependencia retirada |
| Secretos hardcodeados | 0 |
| Enlaces rotos en `docs/` | 0 |
| YAML de pipeline | 8 archivos, sin tabulaciones, estructura válida |

Comando de verificación (el POM y CI usan Java 25):

```bash
./mvnw --batch-mode clean verify
```

### Advertencias relevantes

- La única advertencia recurrente del build es la auto-adjunción del agente de Mockito, propia del
  starter de pruebas y ajena a este cambio.
- El POM exige Java 25 y ningún equipo local lo tiene; la validación con JDK 25 la hará el pipeline.

### Lo que no se pudo verificar aquí, y se dice claramente

- **El Quality Gate no se ha ejecutado contra un servidor SonarQube real.** El YAML está completo y
  correcto, pero requiere la extensión de SonarSource instalada en la organización y la service
  connection `SonarQube-UCO` creada. Hasta entonces el análisis no ha corrido nunca.
- **Los stages de despliegue no se han ejecutado.** Requieren las service connections de Azure, los
  App Service y los `environment` de Azure DevOps.
- **Las ramas `develop` y `qa` no existen en el remoto.** Al momento de la auditoría `origin` solo
  tiene `main`; ni siquiera `develop` existía, pese a lo asumido. Los comandos para crearlas están
  en [estrategia de ramas](branching-strategy.md) y no se ejecutaron porque crean estado remoto
  compartido.
- **Los Key Vault no se han desplegado.** El Bicep está listo; falta completar los `object id` en
  los archivos de parámetros.
