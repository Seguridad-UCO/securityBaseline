# PLAN: El endpoint de decisión — contrato completo, denegación por defecto

## Metadata

- **ID:** HU-002
- **Slice:** `nuevo: authorization`
- **Tipo:** Mixto (endpoint de escritura semántica — evalúa, no persiste — con lecturas cross-slice)
- **Fecha:** 2026-09-06
- **Rama sugerida:** `feature/hu-002-endpoint-decision` (ya en uso)
- **Fuentes:** `docs/ai-harness/workspace/HU-002.md` (dictada por el usuario) ·
  `security-platform-architecture/docs/02-domain/08-use-cases.md` (UC-01 EvaluarAcceso) ·
  `docs/02-domain/02-ubiquitous-language.md` · `docs/02-domain/06-invariants.md` (INV-POL-04) ·
  `docs/02-domain/10-entities.md` / `11-aggregates.md` / `12-value-objects.md` (confirmado: sin
  esquema de campos — esta historia lo define por primera vez) · ADR-012 (denegación por defecto) ·
  ADR-014 (mapeo HTTP) · ADR-018 (inquilino desde el principal)
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 5, 6, 7, 9, 11, 12, 13, 14, 20, 21, 22

## Decisiones tomadas en el gate 1 (con el usuario)

| # | Decisión                                          | Elegida                                                                                    |
|---|---------------------------------------------------|--------------------------------------------------------------------------------------------|
| 1 | Idioma de los campos JSON                         | **Inglés** (`subject`, `resource`, `action`, `decision`, `reasonCode`)                     |
| 2 | Identidad del recurso en la solicitud             | **`applicationId` + `resourcePath` + `httpMethod`** (clave natural de `ProtectedResource`) |
| 3 | Qué es `action`                                   | **El verbo HTTP** — se reutiliza `HttpVerb` de `resources` tal cual                        |
| 4 | Publicar validador de existencia en `resources`   | **Sí** — `ProtectedResourceMustExistValidator`, mismo patrón que `applications`            |
| 5 | Prioridad entre aplicación y recurso desconocidos | **`TENANT_MISMATCH` primero** — si la aplicación falla, no se mira el recurso              |

**Nombres de clase en inglés.** El vocabulario aceptado nombra los conceptos en español
(`SolicitudAcceso`, `DecisionAcceso`) porque así se escribió el *ubiquitous language*, pero
`sb-estandares` es taxativa: cero clases en español, sin excepción, y ningún tipo del proyecto la
tiene hoy (`Tenant`, no `Inquilino`). Se traducen a `AccessRequest` / `AccessDecision`, documentando
en el Javadoc de cada uno a qué concepto del dominio corresponde. Si el equipo prefiere los nombres
en español pese a la convención, es una **ambigüedad pendiente** — ver sección 11.

**Ninguna `Rule` nueva en `authorization`.** Las dos comprobaciones de existencia (aplicación,
recurso) ya están resueltas por los validadores publicados de `applications` y `resources`: no hay
una decisión de negocio nueva que tomar, solo traducir un rechazo ya decidido por otro módulo al
vocabulario de esta historia (`ReasonCode`). Eso es orquestación reactiva (`.onErrorResume`), no un
`if/throw` inventado — el caso de uso no decide nada, solo mapea qué excepción se convirtió en qué
código.

## 1. Resumen funcional

Expone `POST /api/v1/authorize`: el PEP envía una solicitud normalizada (aplicación, recurso,
acción) y recibe una `AccessDecision` estructurada y tri-estado. En esta historia la decisión real
la toma un adaptador que deniega por defecto (`NO_APPLICABLE_POLICY`, ADR-012) — HU-004 lo
sustituirá por el cliente real de OPA sin tocar el contrato HTTP. No resuelve roles ni perfiles
(HU-003), no llama a OPA (HU-004), no persiste ni publica evidencia de auditoría (HU-005).

## 2. Criterios de aceptación

| # | Criterio                                     | Resultado esperado                                                                                                   |
|---|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| 1 | Solicitud válida sin política aplicable      | `200` con `AccessDecision` = `DENY`, `reasonCode` = `NO_APPLICABLE_POLICY`, `decisionId` y `correlationId` presentes |
| 2 | Decisión estructurada                        | Nunca un booleano: `state`, `reasonCode`, `decisionId`, `correlationId`, `policyReferences` (vacía) — INV-POL-04     |
| 3 | Tri-estado en el contrato                    | `DecisionState` admite `ALLOW`, `DENY`, `INDETERMINATE` desde el día uno                                             |
| 4 | El sujeto sale del token                     | `subject`/`tenant` del principal autenticado; enviarlos en el cuerpo no los cambia                                   |
| 5 | Aplicación desconocida o de otro inquilino   | `200` `DENY` con `reasonCode` = `TENANT_MISMATCH`                                                                    |
| 6 | Recurso desconocido                          | `200` `DENY` con `reasonCode` = `NO_APPLICABLE_POLICY`                                                               |
| 7 | Campos obligatorios ausentes                 | `400` nombrando el campo (`applicationId`, `resourcePath` o `action`)                                                |
| 8 | Fallo del contexto (el catálogo no responde) | `INDETERMINATE`, nunca `ALLOW`                                                                                       |
| 9 | Correlación                                  | `X-Correlation-Id` entrante se propaga; si no viene, se genera                                                       |

## 3. Reglas de negocio

No hay reglas nuevas propias del slice. Se **reutilizan** dos reglas ya publicadas por sus dueños:

| # | Regla (publicada por su dueño)                                              | Cómo la consume `authorization`                                              | Excepción que traduce                                 | `ReasonCode` resultante |
|---|-----------------------------------------------------------------------------|------------------------------------------------------------------------------|-------------------------------------------------------|-------------------------|
| 1 | `ApplicationMustExistForTenantValidator` (`applications :: rule`)           | `.execute(new ApplicationOwnershipQuery(tenantId, applicationId))`           | `ApplicationNotFoundException`                        | `TENANT_MISMATCH`       |
| 2 | `ProtectedResourceMustExistValidator` (`resources :: rule`, **nueva**, [N]) | `.execute(new ProtectedResourceLookup(applicationId, resourcePath, action))` | `ProtectedResourceNotFoundException` (**nueva**, [N]) | `NO_APPLICABLE_POLICY`  |

La regla 2 es trabajo nuevo en `resources` (ver sección 8), siguiendo exactamente el patrón de
`ApplicationMustExistForTenantRule` + `ApplicationExistence`: un `record` con el hecho ya resuelto
en `resources/domain/rule/model/`, la regla pura en `resources/domain/rule/`, y el validador
reactivo en `resources/application/rule/validator/` que consulta `existsByApplicationPathAndMethod`
(puerto ya existente, sin tocarlo).

**Orden de ejecución (decisión 5):** primero la regla 1; si falla, la cadena corta con
`TENANT_MISMATCH` y la regla 2 no se evalúa. Se implementa encadenando
`applicationExists.then(resourceExists)` — el propio operador reactivo aplica la prioridad, no un
`if`.

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguno. `authorization` no persiste nada en esta historia: evalúa y responde. No hay `{Entidad}.java`
en la raíz de `domain/authorization/`.

### Value objects y enums (todos nuevos, en `domain/authorization/model/`)

| Tipo              | Nuevo/existente | Forma                                                                                         | Comportamiento                                                                          |
|-------------------|-----------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `DecisionState`   | Nuevo           | enum `ALLOW, DENY, INDETERMINATE`                                                             | `isAllow()`                                                                             |
| `ReasonCode`      | Nuevo           | enum `NO_APPLICABLE_POLICY, POLICY_DENY, TENANT_MISMATCH, TOKEN_INVALID, CONTEXT_UNAVAILABLE` | — (catálogo cerrado, sin comportamiento propio)                                         |
| `PolicyReference` | Nuevo           | `record(String policyId, String version)`                                                     | corresponde a `ReferenciaPolitica` (§12 del modelo de dominio: "Id + versión evaluada") |

`TenantId`, `ApplicationId` (de `commons/model`) y `ResourcePath`, `HttpVerb` (de `resources/domain/model`)
se **reutilizan tal cual** — cero VOs nuevos para identidad o recurso.

## 5. Persistencia

No aplica. `authorization` no tiene adaptador de persistencia propio en esta historia: lee la
existencia de aplicación/recurso a través de los validadores publicados por sus dueños, nunca
consultando sus repositorios ni sus tablas directamente.

## 6. Endpoint

| Verbo  | Ruta                | Código de éxito | Cuerpo de entrada     | Cuerpo de salida                         |
|--------|---------------------|-----------------|-----------------------|------------------------------------------|
| `POST` | `/api/v1/authorize` | `200`           | `AuthorizeRawRequest` | `ApiResponse<AccessDecisionWebResponse>` |

- **Autorización:** requiere token. El `tenantId` y el `subject` salen del principal
  (`SecurityContext.currentPrincipal()`), nunca del cuerpo.
- **Errores esperados:** campo ausente/mal formado → `MissingRequestFieldException` /
  `MalformedRequestFieldException` → `400`. Ningún otro camino de esta historia produce un error
  HTTP: `TENANT_MISMATCH`, `NO_APPLICABLE_POLICY` e `INDETERMINATE` son `200` con la decisión
  estructurada en el cuerpo — la decisión es un **valor de negocio**, no una excepción.

## 7. SPEC — el contrato

### Contratos nuevos — `authorization`

```java
// pdp/authorization/application/usecase/AuthorizeUseCase.java                                  [N]
public interface AuthorizeUseCase extends ReactiveOperation<AccessRequest, AccessDecision> {
}

// pdp/authorization/application/secondaryport/PolicyDecisionPort.java                           [N]
public interface PolicyDecisionPort extends ReactiveOperation<AccessRequest, AccessDecision> {
}

// pdp/authorization/infrastructure/adapter/primary/web/interactor/AuthorizeInteractor.java       [N]
public interface AuthorizeInteractor extends ReactiveOperation<AuthorizeRawRequest, AccessDecisionWebResponse> {
}
```

### Contratos nuevos — `resources` (regla 2, reutilizable desde `authorization`)

```java
// pdp/resources/domain/rule/ProtectedResourceMustExistRule.java                                 [N]
public interface ProtectedResourceMustExistRule extends OperationWithoutResult<ProtectedResourceExistence> {
}

// pdp/resources/application/rule/validator/ProtectedResourceMustExistValidator.java              [N]
public interface ProtectedResourceMustExistValidator extends ReactiveOperationWithoutResult<ProtectedResourceLookup> {
}
```

### Firmas de value objects, entidades y DTOs — `authorization`

```java
// pdp/authorization/domain/model/DecisionState.java                                              [N]
public enum DecisionState { ALLOW, DENY, INDETERMINATE }                     // + isAllow()

// pdp/authorization/domain/model/ReasonCode.java                                                  [N]
public enum ReasonCode { NO_APPLICABLE_POLICY, POLICY_DENY, TENANT_MISMATCH, TOKEN_INVALID, CONTEXT_UNAVAILABLE }

// pdp/authorization/domain/model/PolicyReference.java                                             [N]
public record PolicyReference(String policyId, String version) { }

// pdp/authorization/application/primaryport/request/AccessRequest.java                            [N]
public record AccessRequest(TenantId tenantId, String subject, ApplicationId applicationId,
        ResourcePath resourcePath, HttpVerb action, String correlationId) { }

// pdp/authorization/application/primaryport/response/AccessDecision.java                          [N]
public record AccessDecision(UUID decisionId, DecisionState state, ReasonCode reasonCode,
        List<PolicyReference> policyReferences, String correlationId, Instant decidedAt) { }
```

### Firmas de value objects y DTOs — `resources` (regla 2)

```java
// pdp/resources/domain/rule/model/ProtectedResourceExistence.java                                 [N]
public record ProtectedResourceExistence(ApplicationId applicationId, ResourcePath path, HttpVerb method,
        boolean registered) { }

// pdp/resources/application/primaryport/request/ProtectedResourceLookup.java                       [N]
public record ProtectedResourceLookup(ApplicationId applicationId, ResourcePath path, HttpVerb method) { }
```

### Firmas de DTOs web — `authorization`

```java
// pdp/authorization/infrastructure/adapter/primary/web/dto/request/raw/AuthorizeRawRequest.java   [N]
public record AuthorizeRawRequest(String applicationId, String resourcePath, String action) { }

// .../dto/response/AccessDecisionWebResponse.java                                                  [N]
public record AccessDecisionWebResponse(String decisionId, String state, String reasonCode,
        List<PolicyReferenceWebResponse> policyReferences, String correlationId, String decidedAt) { }

// .../dto/response/PolicyReferenceWebResponse.java                                                 [N]
public record PolicyReferenceWebResponse(String policyId, String version) { }
```

### Excepción nueva — `resources`

```java
// pdp/resources/domain/exception/ProtectedResourceNotFoundException.java                          [N]
public final class ProtectedResourceNotFoundException extends BusinessRuleViolationException {
    public ProtectedResourceNotFoundException(ApplicationId applicationId, ResourcePath path, HttpVerb method) { … }
}
```

## 8. Árbol de archivos

```
pdp/authorization/
├── domain/
│   └── model/
│       ├── DecisionState.java                                    [N]
│       ├── ReasonCode.java                                       [N]
│       └── PolicyReference.java                                  [N]
├── application/
│   ├── primaryport/
│   │   ├── request/AccessRequest.java                            [N]
│   │   ├── request/package-info.java          @NamedInterface("dto")           [N]
│   │   └── response/AccessDecision.java                          [N]
│   ├── secondaryport/
│   │   └── PolicyDecisionPort.java                                [N]
│   └── usecase/
│       ├── AuthorizeUseCase.java                                  [N]
│       └── impl/AuthorizeUseCaseImpl.java                         [N]
└── infrastructure/
    ├── adapter/
    │   ├── primary/web/
    │   │   ├── controller/AuthorizationController.java            [N]
    │   │   ├── dto/request/raw/AuthorizeRawRequest.java            [N]
    │   │   ├── dto/response/AccessDecisionWebResponse.java         [N]
    │   │   ├── dto/response/PolicyReferenceWebResponse.java        [N]
    │   │   ├── interactor/AuthorizeInteractor.java                 [N]
    │   │   ├── interactor/impl/AuthorizeInteractorImpl.java        [N]
    │   │   └── mapper/
    │   │       ├── AuthorizeRequestMapper.java                     [N]
    │   │       └── AccessDecisionResponseMapper.java                [N]
    │   └── secondary/
    │       └── policy/DenyByDefaultPolicyDecisionAdapter.java       [N]
    └── config/AuthorizationConfiguration.java                       [N]  ← registra TODO lo de application/

pdp/resources/                                     (extensión: regla de existencia publicada)
├── domain/
│   ├── rule/
│   │   ├── ProtectedResourceMustExistRule.java                     [N]
│   │   ├── rule/model/ProtectedResourceExistence.java               [N]
│   │   └── impl/ProtectedResourceMustExistRuleImpl.java             [N]
│   └── exception/ProtectedResourceNotFoundException.java            [N]
├── application/
│   ├── primaryport/request/ProtectedResourceLookup.java             [N]
│   ├── primaryport/request/package-info.java  @NamedInterface("dto")  [N]
│   └── rule/validator/
│       ├── ProtectedResourceMustExistValidator.java                 [N]
│       ├── package-info.java  @NamedInterface("rule")   (si no existe ya)  [N]
│       └── impl/ProtectedResourceMustExistValidatorImpl.java         [N]
└── infrastructure/config/ResourcesConfiguration.java   [M]  ← añade el bean del nuevo validador y regla

pdp/resources/domain/model/package-info.java   [N]
@org.springframework.modulith.NamedInterface("model")
  ↳ Hallazgo real al compilar Fase 5, no anticipado en el diseño: en cuanto `resources` gana su
    primer @NamedInterface (el de `rule/validator` o `primaryport/request` de esta misma historia),
    Modulith deja de exponer implícitamente el resto del módulo. `ResourcePath`/`HttpVerb` nunca
    habían sido consumidos desde fuera de `resources` hasta esta historia, así que la brecha nunca
    se había manifestado. Se corrige exportando `domain/model` como interfaz nombrada explícita
    ("model") — no relajando ninguna frontera, declarándola.

pdp/authorization/package-info.java   [N]
@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto",
        "resources", "resources :: rule", "resources :: dto", "resources :: model"})

shared/message/RequiredArgumentMessages.java   [N]  ← nuevas constantes, ninguna existente se toca:
  ACCESS_REQUEST, POLICY_DECISION_PORT, AUTHORIZE_USE_CASE, DECISION_STATE, REASON_CODE, SUBJECT (ya existe, reutilizar)

shared/web/message/WebContractMessages.java   [N]  ← nuevo mensaje de éxito: successAccessEvaluated()
```

**[M] real, no cosmético:** `ResourcesConfiguration.java` gana tres `@Bean` nuevos
(`protectedResourceMustExistRule`, `protectedResourceMustExistValidator` — ambos sin cambiar ninguna
firma existente del archivo). No hay ninguna otra modificación de firma en todo el plan: todo lo
demás es [N] puro. Por eso el implementador no necesita renegociar nada — solo añadir.

## 9. Casos de prueba esperados

| Capa                            | Clase de prueba                            | Casos                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|---------------------------------|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain` (`authorization`)      | `DecisionStateTests`                       | `isAllow()` verdadero solo para `ALLOW`                                                                                                                                                                                                                                                                                                                                                                                                   |
| `domain` (`resources`)          | `ProtectedResourceMustExistRuleTests`      | registrado → no lanza; no registrado → `ProtectedResourceNotFoundException`                                                                                                                                                                                                                                                                                                                                                               |
| `application` (`resources`)     | `ProtectedResourceMustExistValidatorTests` | existe / no existe (repositorio falso)                                                                                                                                                                                                                                                                                                                                                                                                    |
| `application` (`authorization`) | `AuthorizeUseCaseImplTests`                | aplicación no existe → `TENANT_MISMATCH`; **consulta de propiedad con el tenant e id del propio solicitante, y deniega igual si la aplicación es de otro inquilino** (propuesto por `@2-tester-spec` al cerrar, aprobado); recurso no existe → `NO_APPLICABLE_POLICY`; ambos existen → delega en el puerto y devuelve su decisión; el puerto falla → `INDETERMINATE`; el chequeo de aplicación falla por motivo técnico → `INDETERMINATE` |
| `infrastructure`                | `AuthorizeRequestMapperTests`              | cada campo ausente → excepción con su nombre; `action` inválido → `MalformedRequestFieldException`                                                                                                                                                                                                                                                                                                                                        |
| `infrastructure`                | `AccessDecisionResponseMapperTests`        | estado y `reasonCode` se aplanan a `String`; `policyReferences` vacía se aplana a lista vacía                                                                                                                                                                                                                                                                                                                                             |
| `infrastructure`                | `AuthorizationControllerTests`             | delega al interactor, responde `200`                                                                                                                                                                                                                                                                                                                                                                                                      |
| **E2E (Netty)**                 | `AuthorizationHttpTests`                   | los 9 criterios de aceptación, sobre el flujo autenticado completo — sigue el patrón de `ApplicationHttpTests`                                                                                                                                                                                                                                                                                                                            |

Presupuesto: **14-16 pruebas**, coherente con el rango orientativo de `sb-testing` para un endpoint
con dos reglas reutilizadas.

## 10. Trazabilidad

| Fase                       | Estado                                                              | Fecha      |
|----------------------------|---------------------------------------------------------------------|------------|
| Plan                       | ✅ Generado                                                          | 2026-09-06 |
| Contrato aprobado (gate 1) | ⏳ Pendiente                                                         |            |
| Pruebas en rojo            | ⏳ Pendiente                                                         |            |
| Implementación en verde    | ⏳ Pendiente                                                         |            |
| Validación                 | ✅ APROBADO — ver [REPORTE-HU-002.md](../reportes/REPORTE-HU-002.md) | 2026-09-06 |
| Entrega (gate 2)           | ⏳ Pendiente                                                         |            |

## 11. Ambigüedades pendientes

1. **Nombres en inglés vs español para `AccessRequest`/`AccessDecision`.** Elegí inglés por
   `sb-estandares` invariante #1, sin excepción hoy en el código. Si el equipo (sobre todo el
   compañero del PEP) ya circuló "SolicitudAcceso"/"DecisionAcceso" como los nombres literales a
   integrar, dímelo antes del gate 2 y renombro — es un cambio de nombre, no de forma.
2. **`CONTEXT_UNAVAILABLE` como `ReasonCode` para `INDETERMINATE`.** UC-01 no fija el código exacto
   para el camino técnico-no-confiable; propuse uno nuevo, estable y no sensible. Si
   `security-platform-architecture` prefiere otro nombre, es un cambio de constante, no de diseño.
