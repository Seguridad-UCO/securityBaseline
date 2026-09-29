# PLAN: Endpoint interno `POST /internal/v1/access-decisions` para el PEP

## Metadata

- **ID:** HU-003
- **Slice:** `authorization` (existente, creado en HU-002) + una firma nueva en `applications`
- **Tipo:** Consulta (evalúa una decisión; no persiste)
- **Fecha:** 2026-09-11
- **Rama sugerida:** `feature/HU-003-endpoint-interno-pep`
- **Fuentes:** `pdp/docs/ai-harness/workspace/HANDOFF-INTEGRACION-PEP-OPA.md` (D1–D10, T1–T5) ·
  `contracts/pep-pdp/v1/{openapi.yaml,request.schema.json,decision.schema.json}` ·
  `contracts/pep-pdp/v1/PDP-INTEGRATION-GUIDE.md` · código real: `authorization/*` (HU-002),
  `applications/*`, `shared/config/{SecurityConfiguration,KeycloakSecurityConfiguration}`,
  `shared/security/{PdpPrincipal,JwtSecurityProperties,CorsProperties}` ·
  `pep/src/test/.../PdpTlsIntegrationTests.java` (patrón mTLS de referencia, T2) ·
  Confirmado con el usuario: confianza mTLS por PEM + lista de sujetos en properties (no keystore);
  evidencia JWT contra JWKS del mismo Keycloak, sin exigir el claim `tenant` (no HMAC, no JWKS aparte).
- **Criterios de la línea base que toca:** 1, 2, 4, 9, 11, 12, 13, 14, 21, 22 (ver §2)

## 1. Resumen funcional

Segundo adaptador primario para `AuthorizeUseCase` (ya existe desde HU-002): un endpoint interno,
`POST /internal/v1/access-decisions`, que el PEP consume con un canal de confianza distinto al del
panel — mTLS de servicio + un JWT de evidencia del usuario final, en vez de sesión por cookie.

El PEP manda `application.id`, no `tenantId`: el PDP resuelve el inquilino dueño desde el catálogo
de `applications` (D3) y arma el `AccessRequest` que ya consume `AuthorizeUseCase`. La regla de
decisión (`DenyByDefaultPolicyDecisionAdapter` hoy, OPA en HU-005) no se toca — sigue siendo la
misma para los dos canales.

**No cubre:** validar issuer/audiencia por aplicación (HU-004), evaluación real de políticas
(HU-005), auditoría durable (HU-006), ni el camino de despliegue en Azure App Service donde
`SslInfo` no existe (T3 — es Etapa 5 del plan del PEP, no una historia del PDP).

## 2. Criterios de aceptación

| #  | Criterio                  | Resultado esperado                                                                                                                                                     |
|----|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture        | `InternalMtlsWebFilter`/`InternalSecurityConfiguration` en `shared`; nada de Spring en `domain`/`application` de `authorization` ni `applications`                     |
| 2  | Contratos de servicios    | `EvaluateInternalAccessUseCase` es interfaz vacía sobre `ReactiveOperation`; reutiliza `PolicyDecisionPort` sin cambiarlo                                              |
| 4  | Capacidades transversales | Cadena de seguridad nueva (mTLS + evidencia), separada por canal; `IdentifierGenerator`/`TimeProvider` no se reinventan (los usa `AuthorizeUseCaseImpl`, ya existente) |
| 9  | Excepciones               | Reutiliza `ApplicationNotFoundException` y `ConflictingRequestParametersException`/`MalformedRequestFieldException` ya existentes; ninguna excepción nueva de negocio  |
| 11 | Interacción entre capas   | Controller → interactor → caso de uso puente → `AuthorizeUseCase` → reglas/puertos                                                                                     |
| 12 | SOLID                     | El puente depende de `ApplicationRepository` (puerto) y de `AuthorizeUseCase` (contrato), no de implementaciones                                                       |
| 13 | DTOs                      | `AccessDecisionRawRequest` (Strings anidados) → `InternalAccessRequest` (value objects)                                                                                |
| 14 | DTOs seguros              | Tres barreras + las de D7 (versión, coherencia de IDs) — sin Jakarta Validation                                                                                        |
| 21 | Modelo refinado           | `InternalAccessRequest` es `record` con `requireNonNull`; sin Lombok                                                                                                   |
| 22 | Arquitectura reactiva     | Toda la cadena en `Mono`; sin `block()`                                                                                                                                |

**Deliberadamente NO declarado:** criterio 5 (Manejo de mensajes) — D6 documenta que la respuesta
**no** se envuelve en `ApiResponse` a propósito; es una desviación registrada, no un defecto.

## 3. Reglas de negocio

No hay una regla de negocio *nueva*: la única condición de rechazo propia de esta historia
—"la aplicación no existe"— ya tiene su excepción (`ApplicationNotFoundException`, de
`ApplicationMustExistForTenantRule`) y su tratamiento en `AuthorizeUseCaseImpl.onErrorResume`. Lo
que hace falta es **resolver** el dueño en vez de **verificarlo** contra un tenant ya conocido —
justo la asimetría que D3 señala.

| # | Regla                                                                                             | Dónde vive                                                                                                                                                                                                                                                                            | Puerto que trae el dato                                                                                                                    | Excepción → HTTP                                                                                                                                                                 |
|---|---------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| — | La aplicación referenciada por `application.id` debe existir; si existe, resuelve su tenant dueño | `applications/application/rule/validator/ApplicationOwnerLookupValidator` (**nuevo**, es un *finder con rechazo*, no una `Rule` pura — misma naturaleza que `ApplicationMustExistForTenantRule` resuelta en el ESTUDIO de Fase A: el resultado se necesita después, no es solo sí/no) | `ApplicationRepository.findTenantIdById(ApplicationId)` — **[M]**, nuevo método, responde solo el id del tenant, no la aplicación completa | `ApplicationNotFoundException` (ya existe) → **403** vía `TENANT_MISMATCH` en `AuthorizeUseCaseImpl` (mismo mapeo que hoy usa el canal BFF cuando la app no pertenece al tenant) |

Barreras de contrato (D7, no son reglas de negocio — son validación de forma, igual que un campo
mal formado):

| #  | Regla                                                                                                                                    | Dónde vive                                                                                                                                       | Excepción → HTTP                                                      |
|----|------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| C1 | `version` debe ser exactamente `"1"`                                                                                                     | `AccessDecisionRawRequestMapper`                                                                                                                 | `MalformedRequestFieldException("version", …)` → **400**              |
| C2 | `requestId` del header `X-Request-Id` debe coincidir con `requestId` del cuerpo                                                          | `InternalAccessDecisionInteractorImpl` (compara contra `CorrelationWebFilter.context(exchange)`, que ya resolvió el header — no se reimplementa) | `ConflictingRequestParametersException("requestId", …)` → **400**     |
| C3 | `correlationId` del header `X-Correlation-Id` debe coincidir con `correlationId` del cuerpo                                              | ídem                                                                                                                                             | `ConflictingRequestParametersException("correlationId", …)` → **400** |
| C4 | `timestamp` debe ser un `Instant` ISO-8601 válido (presencia y formato; el valor no se propaga: no forma parte del contexto de decisión) | `AccessDecisionRawRequestMapper`                                                                                                                 | `MalformedRequestFieldException("timestamp", …)` → **400**            |

No hay regla de unicidad ni de estado — es una consulta de decisión, no una escritura.

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguna nueva. `Application` (existente) ya tiene `tenantId` como campo — lo único que faltaba era
un puerto que lo devolviera sin exigir el tenant como entrada.

### Value objects

Ninguno nuevo: `ApplicationId`, `ResourcePath`, `HttpVerb`, `TenantId` ya existen y se reutilizan tal
cual en el mapper del canal interno (mismos `RequestFieldParser.parse(...)` que usa el canal BFF).

### DTOs nuevos (ver §7 para firmas exactas)

| DTO                                                                                   | Capa                                               | Rol                                                                   |
|---------------------------------------------------------------------------------------|----------------------------------------------------|-----------------------------------------------------------------------|
| `InternalAccessRequest`                                                               | `authorization/application/primaryport/request`    | Entrada al caso de uso puente — **sin** `tenantId` (D4)               |
| `AccessDecisionRawRequest` (+ anidados `RawApplication`, `RawResource`, `RawContext`) | `authorization/infrastructure/.../dto/request/raw` | Espejo de `SolicitudAcceso v1`, Strings desnudos                      |
| `AccessDecisionInternalWebResponse`                                                   | `authorization/infrastructure/.../dto/response`    | Espejo de `DecisionAcceso v1`, **sin envolver en `ApiResponse`** (D6) |

### Enums

Ninguno nuevo. `DecisionState`, `ReasonCode` (de HU-002) se reutilizan sin cambios.

## 5. Persistencia

- **Tabla:** ninguna nueva.
- **Consulta nueva en el puerto:** `Mono<TenantId> findTenantIdById(ApplicationId applicationId)` en
  `ApplicationRepository` — **[M]**. Responde solo el `TenantId` (o vacío si no existe), nunca la
  `Application` completa: es lo mínimo que el validador necesita (mismo criterio que ya aplicó Fase A
  a `existsByTenantAndId`).
- **Implementación:** `SurrealApplicationRepository` — **[M]**, un `SELECT tenant_id FROM application
  WHERE id = $id` (o equivalente SurrealQL parametrizado; el implementador decide la consulta exacta
  siguiendo el patrón de los métodos `existsBy*` ya presentes).
- **Inicializador de esquema:** existente, sin cambios (no hay tabla ni índice nuevo).

## 6. Endpoint

| Verbo | Ruta                            | Código de éxito | Cuerpo de entrada                                              | Cuerpo de salida                                                               |
|-------|---------------------------------|-----------------|----------------------------------------------------------------|--------------------------------------------------------------------------------|
| POST  | `/internal/v1/access-decisions` | 200             | `AccessDecisionRawRequest` (JSON, según `request.schema.json`) | `AccessDecisionInternalWebResponse` (JSON plano, según `decision.schema.json`) |

- **Autorización:** cadena de seguridad **propia**, separada de `SecurityConfiguration` /
  `KeycloakSecurityConfiguration` (que siguen siendo "el único lugar por canal" que decide qué
  necesita cada ruta — el comentario de `SecurityConfiguration` que dice "el único lugar que decide"
  se actualiza a "el único lugar **para el canal BFF**"). Dos capas, en este orden:
    1. **mTLS** (D2): `client-auth=want` a nivel de servidor + `InternalMtlsWebFilter`, que falla
       cerrado con **403** si no hay certificado de cliente o su sujeto no está en la lista admitida.
    2. **Evidencia JWT** (D5): `Authorization: Bearer` validado contra el JWKS del Keycloak ya
       desplegado (confirmado con el usuario), exigiendo `sub`/`exp`/`nbf`/`iss`/`aud`, **sin** exigir
       el claim `tenant` (T1) — de ahí que no pueda ser el mismo `ReactiveJwtDecoder` de
       `SecurityConfiguration`. Token inválido o ausente → **401**, antes del caso de uso (D8).

    - El inquilino **nunca** sale de este JWT ni del cuerpo: sale del catálogo (D3, §3).
    - **El `securityMatcher` es `/internal/v1/**`, no `/internal/**`** — `/internal/oauth2/**` ya
      existe hoy en `KeycloakSecurityConfiguration` para el flujo de login OIDC del panel y no debe
      tocar mTLS. Hallazgo de la fase 2, no un supuesto: verificado leyendo
      `KeycloakSecurityConfiguration.keycloakSecurityWebFilterChain`.
- **Errores esperados:**

  | Causa | HTTP |
    |---|---|
  | Sin certificado de cliente, o sujeto no admitido | 403 |
  | JWT de evidencia ausente/inválido/expirado | 401 |
  | `version` ≠ `"1"`, `timestamp` mal formado | 400 |
  | `X-Request-Id`/`X-Correlation-Id` no coincide con el cuerpo | 400 |
  | `application.id`, `resource.path` o `resource.action` mal formados | 400 |
  | La aplicación no existe | 403 (`TENANT_MISMATCH`, mismo camino que hoy) |
  | El motor de políticas fallara (hoy no aplica: `DenyByDefaultPolicyDecisionAdapter` no falla nunca) | — |

## 7. SPEC — el contrato

### Contratos nuevos

```java
// authorization/application/usecase/EvaluateInternalAccessUseCase.java                          [N]
public interface EvaluateInternalAccessUseCase
        extends ReactiveOperation<InternalAccessRequest, AccessDecision> {
}
```

```java
// applications/application/rule/validator/ApplicationOwnerLookupValidator.java                   [N]
public interface ApplicationOwnerLookupValidator
        extends ReactiveOperation<ApplicationId, TenantId> {
}
```

```java
// authorization/infrastructure/adapter/primary/web/interactor/InternalAccessDecisionInteractor.java   [N]
public interface InternalAccessDecisionInteractor
        extends ReactiveOperation<AccessDecisionRawRequest, AccessDecisionInternalWebResponse> {
}
```

### Firmas de DTOs

```java
// authorization/application/primaryport/request/InternalAccessRequest.java                       [N]
// Como AccessRequest, pero SIN tenantId (D4): el puente lo resuelve antes de delegar.
public record InternalAccessRequest(String subject, ApplicationId applicationId,
        ResourcePath resourcePath, HttpVerb action, String requestId, String correlationId) {
    // requireNonNull de los seis componentes, mismas constantes que AccessRequest donde ya existan.
}
```

```java
// authorization/infrastructure/adapter/primary/web/dto/request/raw/AccessDecisionRawRequest.java  [N]
// Espejo de SolicitudAcceso v1. Todo String desnudo; los anidados son records propios, no un Map.
public record AccessDecisionRawRequest(String version, String requestId, String correlationId,
        String timestamp, RawApplication application, RawResource resource, RawContext context) {

    public record RawApplication(String id, String environment) { }
    public record RawResource(String path, String action) { }
    public record RawContext(String method, String channel) { }
}
```

```java
// authorization/infrastructure/adapter/primary/web/dto/response/AccessDecisionInternalWebResponse.java  [N]
// Espejo de DecisionAcceso v1. Sin ApiResponse (D6). "obligations" se omite: la v1 del PEP
// solo admite ausente/null/vacío y AccessDecision no tiene obligaciones que transportar todavía.
public record AccessDecisionInternalWebResponse(String decision, String decisionId, String reasonCode,
        List<PolicyReferenceWebResponse> policyReferences, String requestId, String correlationId) {
}
```

```java
// shared/security/InternalMtlsProperties.java                                                    [N]
@ConfigurationProperties(prefix = "pdp.security.internal.mtls")
public record InternalMtlsProperties(String trustCertificate, List<String> allowedSubjects) {
    // trustCertificate: ruta a un PEM (CA), vía server.ssl.trust-certificate — ver §8 nota de wiring.
    // allowedSubjects: Subject DN/CN exactos admitidos. Vacío → ningún certificado pasa (fail-closed).
}
```

```java
// shared/security/InternalEvidenceJwtProperties.java                                             [N]
@ConfigurationProperties(prefix = "pdp.security.internal.evidence")
public record InternalEvidenceJwtProperties(String jwkSetUri, String issuer, String audience) {
    // Solo JWKS (confirmado con el usuario) — sin el modo HMAC de JwtSecurityProperties: este
    // decoder valida evidencia de terceros, no emite el PDP su propio token aquí.
}
```

### Firmas nuevas en puertos existentes

```java
// applications/application/secondaryport/repository/ApplicationRepository.java                   [M]
Mono<TenantId> findTenantIdById(ApplicationId applicationId);
```

### Clase nueva sin contrato base (filtro de infraestructura, no de dominio)

```java
// shared/security/InternalMtlsWebFilter.java                                                     [N]
public final class InternalMtlsWebFilter implements org.springframework.web.server.WebFilter {
    public InternalMtlsWebFilter(InternalMtlsProperties properties) { /* … */ }
    // filter(exchange, chain): lee exchange.getRequest().getSslInfo(); sin certificado o con
    // Subject DN fuera de allowedSubjects -> responde 403 y NO llama a chain.filter(...) (D2).
}
```

## 8. Árbol de archivos

> Rutas completas desde `pdp/src/main/java/co/edu/uco/seguridad/`. `[N]` nuevo, `[M]` modificado.

```
pdp/applications/
├── application/secondaryport/repository/ApplicationRepository.java              [M] +findTenantIdById
├── application/rule/validator/ApplicationOwnerLookupValidator.java              [N]
├── application/rule/validator/impl/ApplicationOwnerLookupValidatorImpl.java     [N]
└── infrastructure/
    ├── adapter/secondary/persistence/repository/SurrealApplicationRepository.java  [M] +findTenantIdById
    └── config/ApplicationsConfiguration.java                                    [M] registra el validador nuevo

pdp/authorization/
├── application/
│   ├── primaryport/request/InternalAccessRequest.java                          [N]
│   └── usecase/
│       ├── EvaluateInternalAccessUseCase.java                                  [N]
│       └── impl/EvaluateInternalAccessUseCaseImpl.java                         [N]
└── infrastructure/
    ├── adapter/primary/web/
    │   ├── controller/InternalAccessDecisionController.java                    [N]
    │   ├── dto/request/raw/AccessDecisionRawRequest.java                       [N]
    │   ├── dto/response/AccessDecisionInternalWebResponse.java                 [N]
    │   ├── interactor/InternalAccessDecisionInteractor.java                    [N]
    │   ├── interactor/impl/InternalAccessDecisionInteractorImpl.java          [N]
    │   └── mapper/
    │       ├── AccessDecisionRawRequestMapper.java                             [N]
    │       └── AccessDecisionInternalResponseMapper.java                       [N]
    └── config/AuthorizationConfiguration.java                                  [M] registra puente + interactor

pdp/shared/
├── security/
│   ├── InternalMtlsProperties.java                                            [N]
│   ├── InternalEvidenceJwtProperties.java                                     [N]
│   └── InternalMtlsWebFilter.java                                             [N]
├── config/
│   ├── InternalSecurityConfiguration.java                                     [N] cadena /internal/v1/**
│   └── SecurityConfiguration.java                                             [M] comentario: "por canal"
└── message/RequiredArgumentMessages.java                                      [M] +constantes nuevas
```

**Nota de wiring TLS del servidor (no es código de aplicación, es `application.properties`):**
`server.ssl.client-auth=want`, `server.ssl.trust-certificate=${pdp.security.internal.mtls.trust-certificate}`,
`server.ssl.certificate`/`server.ssl.certificate-private-key` para el certificado del propio servidor
(PEM nativo de Spring Boot 4.1 — sin keystore). Va en `application-prod.properties` / variables de
entorno de despliegue, no en el árbol de clases; el implementador lo documenta en el plan de
propiedades pero no crea un archivo de producción con secretos.

## 9. Casos de prueba esperados

| Capa                                                                                                            | Clase de prueba                                                   | Casos                                                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application` (applications)                                                                                    | `ApplicationOwnerLookupValidatorImplTests`                        | aplicación existente → `TenantId` correcto; aplicación inexistente → `ApplicationNotFoundException`                                                                                                                                                                           |
| `application` (authorization)                                                                                   | `EvaluateInternalAccessUseCaseImplTests`                          | camino feliz (resuelve tenant, delega en `AuthorizeUseCase`, devuelve su `AccessDecision` tal cual); aplicación inexistente → `AccessDecision` `DENY`/`TENANT_MISMATCH` (mismo mapeo que ya hace `AuthorizeUseCaseImpl`, verificar que el puente no lo duplica ni lo oculta)  |
| `infrastructure` — mapper                                                                                       | `AccessDecisionRawRequestMapperTests`                             | `version` ausente/≠"1" → `MalformedRequestFieldException`; `application.id` mal formado (no UUID) → `MalformedRequestFieldException`; `resource.action` no soportado → ídem; `timestamp` mal formado → ídem; camino feliz → `InternalAccessRequest` correcto (sin `tenantId`) |
| `infrastructure` — mapper                                                                                       | `AccessDecisionInternalResponseMapperTests`                       | `AccessDecision` → DTO plano; lista de `policyReferences` vacía se preserva vacía (no `null`)                                                                                                                                                                                 |
| `infrastructure` — controller                                                                                   | `InternalAccessDecisionControllerTests`                           | delega al interactor; responde 200 con el cuerpo plano (sin `ApiResponse`)                                                                                                                                                                                                    |
| `infrastructure` — interactor                                                                                   | `InternalAccessDecisionInteractorImplTests`                       | `requestId` de header ≠ cuerpo → `ConflictingRequestParametersException`; ídem `correlationId`; coinciden → delega                                                                                                                                                            |
| `infrastructure` — seguridad                                                                                    | `InternalMtlsWebFilterTests`                                      | sin certificado → 403, no llama a la cadena; certificado con sujeto no admitido → 403; sujeto admitido → continúa                                                                                                                                                             |
| `infrastructure` — seguridad (Testcontainers no aplica; `@SpringBootTest` sí, es una de las cuatro excepciones) | `InternalSecurityChainIntegrationTests`                           | end-to-end con certificados efímeros generados en el test (mismo patrón que `PdpTlsIntegrationTests` del PEP, T2): mTLS admitido + JWT válido → 200; sin certificado → 403; certificado admitido + JWT sin `sub` → 401; JWT válido pero aplicación inexistente → 403          |
| `infrastructure` — persistencia                                                                                 | `SurrealApplicationRepositoryTests` (extiende la clase existente) | `findTenantIdById` con aplicación registrada → el `TenantId` correcto; con id inexistente → `Mono` vacío                                                                                                                                                                      |

Presupuesto total estimado: **18–22 pruebas** — más que el rango orientativo de `sb-testing`
(10–15) porque esta historia introduce una cadena de seguridad completa, no solo un caso de uso.

## 10. Trazabilidad

| Fase                       | Estado                                                                                                   | Fecha      |
|----------------------------|----------------------------------------------------------------------------------------------------------|------------|
| Plan                       | ✅ Generado                                                                                               | 2026-09-11 |
| Contrato aprobado (gate 1) | ✅ Aprobado                                                                                               | 2026-09-11 |
| Pruebas en rojo            | ✅ 301/301 pendientes por `UnsupportedOperationException` (incluye 6 reescritas + 1 nueva de integración) | 2026-09-11 |
| Implementación en verde    | ✅ 302 pruebas                                                                                            | 2026-09-11 |
| Validación                 | ✅ APROBADO — sin bloqueantes                                                                             | 2026-09-11 |
| Entrega (gate 2)           | ⏳ Pendiente                                                                                              |            |

## 11. Ambigüedades pendientes

Ninguna que bloquee el contrato. Dos notas para el implementador, no para el usuario:

1. **La consulta SurrealQL exacta de `findTenantIdById`** no está fijada aquí a propósito —
   sigue el patrón de los `existsBy*` ya escritos en `SurrealApplicationRepository`, que el
   planificador no necesita reproducir letra por letra para que el contrato sea válido.
2. **El valor exacto de `server.ssl.certificate`/`-private-key`** para el propio servidor (no el
   trust store del cliente) es configuración de despliegue, no parte del código: se documenta en
   `application-{env}.properties` con la misma convención que `pdp.persistence.surrealdb.password`
   (nunca un valor real en el repo).
