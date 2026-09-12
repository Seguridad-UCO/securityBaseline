# Reporte de validación — HU-003

## Metadata

- **Slice:** `authorization` (existente, HU-002) + `applications` (nueva firma) + `shared/security`, `shared/config` (canal interno nuevo)
- **Fecha:** 2026-09-11
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-003.md`
- **Rama:** `feature/HU-003-endpoint-interno-pep`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `clean verify`), de forma independiente.

```
ESTADO: ROJO  (mvnw clean verify, 52,3s, exit 1)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 302, Failures: 0, Errors: 1, Skipped: 0

[ERROR] InternalSecurityChainIntegrationTests
java.lang.IllegalStateException: openssl no pudo generar el certificado de prueba para /CN=localhost
```

**Esto no es un bloqueante — es un error de entorno de esta máquina, verificado de forma
independiente, no aceptado de memoria del implementador:**

1. `[System.Environment]::GetEnvironmentVariable("OPENSSL_CONF", "Machine")` devuelve
   `C:\Program Files\PostgreSQL\psqlODBC\etc\openssl.cnf` — una variable de entorno **a nivel de
   máquina**, ajena a este repositorio, dejada por una instalación de PostgreSQL/psqlODBC.
2. `Test-Path` sobre esa ruta devuelve `False`: el archivo no existe. `openssl` falla al arrancar
   para **cualquier** invocación en esta máquina, no solo para esta prueba.
3. Limpiando *solo* esa variable para el proceso (`$env:OPENSSL_CONF=''`) y repitiendo
   `mvnw -f pdp/pom.xml clean verify` con el JDK correcto: **exit 0**, las 302 pruebas en verde,
   Quality Gate de JaCoCo íntegro. Ningún archivo de código cambió entre una corrida y la otra.
4. El pipeline de Azure corre en `ubuntu-latest` (`ci/templates/stage-build-analyze.yml`) — no tiene
   PostgreSQL/psqlODBC instalado ni esa variable, así que este problema no viaja con la rama.

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 302 pruebas — 301 verdes en esta máquina tal cual; las 302 verificadas con `OPENSSL_CONF` limpio |
| Cobertura (≥ 50 % por paquete, `jacoco-check`) | ✅ — confirmado en la corrida con `OPENSSL_CONF` limpio |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |
| `consistencia.ps1` | ✅ CONSISTENTE — 5 slices |
| `drift.ps1` | ✅ SIN DERIVA — 14 excepciones preexistentes, ninguna nueva |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

- **Comentario desactualizado en `InternalAccessDecisionInteractorImpl`** (líneas 21-28): el Javadoc
  de la clase sigue empezando con *"Pendiente (HU-003): compara..."*, redactado cuando la clase aún
  era el esqueleto. El método `execute` ya está implementado — el texto describe correctamente qué
  hace, pero el encabezado "Pendiente" es engañoso para quien lo lea después. No bloquea: es una
  limpieza de una línea, sin riesgo, que puede ir en el mismo commit o en el siguiente cambio a ese
  archivo.
- **`Objects.requireNonNull(interactor)` sin mensaje** en `InternalAccessDecisionController` — sin
  la constante de `RequiredArgumentMessages` que el resto del proyecto usa. **No es deriva de esta
  historia**: verificado abriendo `AuthorizationController` (HU-002), que tiene exactamente el mismo
  patrón sin mensaje. Es consistente con el precedente existente, no algo que HU-003 introdujo — se
  deja como observación heredada, no como bloqueante.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan? | ✅ | Ver tabla de criterios abajo — cada fila de la sección 2 del plan tiene su prueba o archivo abierto |
| 2 | ¿Convención de idioma? | ✅ | Identificadores nuevos en inglés (`ApplicationOwnerLookupValidator`, `InternalMtlsWebFilter`…); Javadoc en español; los 3 mensajes nuevos de `WebContractMessages` y las 5 constantes nuevas de `RequiredArgumentMessages` en español, verificados abriendo ambos archivos |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` en verde, 0 hallazgos nuevos sobre las 14 excepciones preexistentes |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | `EvaluateInternalAccessUseCaseImpl` no tiene `if` de negocio (usa `onErrorResume` sobre la excepción que ya lanza el validador prestado); el controller solo delega al interactor; el `if` de versión en `AccessDecisionRawRequestMapper` es barrera de contrato (C1 del plan), no regla de negocio, mismo tratamiento que `ListApplicationsRequestMapper.conflictingPagingModes` ya existente; `InternalMtlsWebFilter` vive en `shared/security` (capacidad técnica), no en dominio; ningún `allowedDependencies` de Modulith se tocó |

## Criterios de la línea base

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | 🤖 ✅ | `LayeredArchitectureTests` + `ModulithStructureTests` verdes; `InternalMtlsWebFilter`/`InternalSecurityConfiguration` viven en `shared`, cero Spring en `domain`/`application` de `authorization`/`applications` |
| 2 | Contratos de servicios | ✅ | `EvaluateInternalAccessUseCase`, `ApplicationOwnerLookupValidator`, `InternalAccessDecisionInteractor` son interfaces vacías sobre `ReactiveOperation`, verificado abriendo los tres |
| 4 | Capacidades transversales | ✅ | Sin `Instant.now()`/`UUID.randomUUID()` en línea — `EvaluateInternalAccessUseCaseImpl.deny(...)` usa `identifiers.next()`/`time.now()`; correlación vía `CorrelationWebFilter`, leída en el interactor por contexto de Reactor, mismo mecanismo que `AuthorizeInteractorImpl` |
| 9 | Excepciones | ✅ | Ninguna excepción nueva de negocio — reutiliza `ApplicationNotFoundException` (ya existente) y `MissingRequestFieldException`/`MalformedRequestFieldException`/`ConflictingRequestParametersException` (ya existentes en `shared/web/exception`) |
| 11 | Interacción entre capas | ✅ | Controller → interactor → caso de uso puente → `AuthorizeUseCase` → reglas/puertos, verificado leyendo la cadena completa de los 4 archivos |
| 12 | SOLID | ✅ | El puente depende de `ApplicationOwnerLookupValidator` y `AuthorizeUseCase` (contratos), no de implementaciones concretas |
| 13 | DTOs | ✅ | `AccessDecisionRawRequest` (Strings anidados, sin anotaciones) → mapper con `RequestFieldParser` → `InternalAccessRequest` (value objects) |
| 14 | DTOs seguros | ✅ | Las tres barreras completas en `InternalAccessRequest` (`requireNonNull` por componente) y en `InternalMtlsProperties`/`InternalEvidenceJwtProperties` (también validados, no dejados vacíos) — barrido explícito con `grep` sobre `authorization` y `shared/security`: cero constructores compactos vacíos |
| 21 | Modelo refinado | ✅ | Todo `record`, sin Lombok, `List.copyOf` en `InternalMtlsProperties.allowedSubjects` y en `AccessDecision.policyReferences` (ya existente) |
| 22 | Arquitectura reactiva | ✅ | Toda la cadena en `Mono`; `InternalMtlsWebFilter.filter()` devuelve `Mono<Void>` sin `block()`; `currentEvidenceSubject()` usa `ReactiveSecurityContextHolder` reactivo, no bloqueante |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `application.properties` | §11: la configuración TLS/mTLS es "configuración de despliegue... no en el árbol de clases" — implícitamente sin entradas en el archivo base | Añade 5 propiedades por defecto (`pdp.security.internal.mtls.*`, `pdp.security.internal.evidence.*`), todas no permisivas (lista de sujetos vacía, URL JWKS que no resuelve) | ✅ Sí — sin ellas, `@EnableConfigurationProperties` con valores `null` rompe el arranque de **todo** el contexto de Spring, no solo del canal interno (`NimbusReactiveJwtDecoder.withJwkSetUri(null)` falla al construir el bean). Los valores mantienen el fallo cerrado; verificado que `SecurityWebFilterChainTests`/`ApplicationHttpTests`/etc. (ninguno relacionado con HU-003) siguen en verde |
| `InternalMtlsWebFilter` | §7 SPEC: clase nueva sin contrato base, sin especificar si es `@Bean` | El implementador determinó que **no puede** ser un `@Bean` independiente — Spring Boot registra automáticamente cualquier bean `WebFilter` en la cadena global de WebFlux. Se construye con `new` dentro de `InternalSecurityConfiguration` | ✅ Sí — verificado por el propio implementador rompiendo y arreglando: con el filtro como `@Bean`, **toda** la suite HTTP existente (`/actuator/health`, `/api/v1/applications`, el panel) devolvía 403. Repetí la comparación yo mismo abriendo el archivo final: el filtro ya no es bean, y `consistencia.ps1`/`drift.ps1`/la suite completa confirman que no quedó rastro del problema |
| Sección 6 del plan, fila "La aplicación no existe" | Decía `403 (TENANT_MISMATCH, mismo camino que hoy)` | El código responde **200** con `decision=DENY`/`reasonCode=TENANT_MISMATCH` (`InternalSecurityChainIntegrationTests.mtls_and_valid_evidence_reach_the_controller_and_receive_a_structured_decision`) | ✅ Sí — la fila del plan se contradecía a sí misma: "mismo camino que hoy" es precisamente el código de `AuthorizeUseCaseImpl` que ya devuelve 200/DENY (verificado en `AuthorizationHttpTests.reports_tenant_mismatch_for_an_application_that_does_not_belong_to_the_tenant`, HU-002). Además, `contracts/pep-pdp/v1/openapi.yaml` declara que toda decisión —incluida DENY— vive bajo la respuesta `200`; `403` en ese contrato significa "PEP no admitido", no un resultado de decisión. El código sigue el contrato externo real y el precedente, no la fila del plan |
| `WebContractMessages` | No listado en el árbol de la sección 8 | Gana 3 métodos (`mustBeVersion1`, `mustBeIso8601`, `headerBodyMismatch`) | ✅ Sí — es exactamente el catálogo que `sb-estandares` exige para no dejar literales de mensaje sueltos; detalle de implementación esperable, no una desviación de arquitectura |
| `InternalSecurityChainIntegrationTests` | Nombrada en la sección 9 (casos de prueba esperados) pero no creada por el tester en la primera pasada | Se añadió en una segunda pasada del tester, a pedido explícito del usuario | ✅ Sí — cierra exactamente el hueco que el propio plan había señalado |

## Datos para la entrega

- **Mensaje de commit:** `feat(authorization): endpoint interno para el PEP con mTLS y evidencia JWT`
- **Cuerpo:** `POST /internal/v1/access-decisions`, segundo adaptador primario sobre el
  `AuthorizeUseCase` ya existente (HU-002) — mismo motor de decisión para los dos canales. Cadena de
  seguridad propia (`InternalSecurityConfiguration`, `securityMatcher("/internal/v1/**")`): mTLS de
  servicio (`InternalMtlsWebFilter`, falla cerrado) + evidencia JWT vía JWKS
  (`InternalEvidenceJwtProperties`), sin exigir el claim `tenant` que sí exige el canal BFF. El
  tenant se resuelve desde el catálogo de aplicaciones
  (`ApplicationRepository.findTenantIdById` + `ApplicationOwnerLookupValidator`), nunca del token ni
  del cuerpo. Respuesta plana sin `ApiResponse` (D6), conforme a `contracts/pep-pdp/v1`. 302 pruebas
  totales, 27 nuevas de esta historia (incluida una de integración de extremo a extremo con
  certificados efímeros y un servidor JWKS real).
- **Rama:** `feature/HU-003-endpoint-interno-pep`
- **Archivos a incluir:** todo `pdp/src/main` y `pdp/src/test` tocado por esta historia, más
  `pdp/src/main/resources/application.properties`. El plan y este reporte se versionan aparte, en
  `pdp/docs/ai-harness/workspace/`. `pdp/docs/ai-harness/PROJECT-MAP.md` ya está regenerado.

## Próximos pasos

Listo para el gate 2 (entrega).
