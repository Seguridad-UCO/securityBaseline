# PLAN: HU-009 — Modelo y mecanismo de administración por aplicación

## Metadata

- **ID:** HU-009
- **Slice:** `authorization` (existente)
- **Tipo:** Escritura (infraestructura interna — sin endpoint HTTP nuevo)
- **Fecha:** 2026-09-13
- **Rama sugerida:** `feature/HU-009-mecanismo-administracion-aplicacion`
- **Fuentes:**
  - `pdp/docs/ai-harness/workspace/HU-009.md` (borrador original — marcado explícitamente "necesita
    una decisión de dominio (ADR) antes de planificarse", con 5 preguntas abiertas)
  - Decisiones tomadas interactivamente con Sebastián en esta sesión de planificación (ver §0)
  - Código real leído antes de planificar: `Role.java`/`RoleScopeLevel.java` (confirma que `Role` ya
    soporta 3 niveles de alcance desde HU-004), `Assignment.java` (confirma que `applicationId` es
    **obligatorio** — hoy no existe forma de representar una asignación global), `AccessRequest.java`
    (confirma que está modelado específicamente para acceso a `ResourcePath`+`HttpVerb`, no sirve
    para "puede administrar"), `PolicyDecisionPort`/`OpaPolicyDecisionAdapter`/DTOs de
    `policy/dto/*` (confirma que están atados al contrato versionado `pdp-opa/v1`), `AuthorizeUseCaseImpl`
    (patrón a espejar: resolver roles → delegar en el puerto de decisión → fail-closed),
    `ActiveRoleNamesLookupValidator`/`Impl` (reutilizable tal cual: ya resuelve
    (usuario, aplicación) → nombres de roles activos), `ApplicationMustExistForTenantValidator`
    (patrón de validador publicado con `@NamedInterface("rule")` a espejar), `OpaProperties.java`
    (confirma un único `@ConfigurationProperties(prefix = "pdp.opa")` con `baseUrl`/`decisionPath`/`timeout`)
  - `pdp/docs/ai-harness/workspace/HU-004.md` (confirma que "quién administra el catálogo" ya se
    difirió aquí, y que los roles globales existen en el modelo pero no se pueden crear por HTTP)
- **Criterios de la línea base que toca:** 1, 2, 3, 4, 9, 11, 12, 21, 22

## 0. Hallazgos y decisiones tomadas antes de planificar

El borrador de `HU-009.md` dejaba 5 preguntas abiertas y pedía explícitamente un ADR antes de
planificarse. En vez de inventar esas respuestas, se resolvieron interactivamente con Sebastián,
más dos hallazgos de código que obligaron a ajustar el mecanismo elegido y a recortar el alcance:

| # | Pregunta / hallazgo | Decisión |
|---|---|---|
| 1 | ¿Rol global, por aplicación, o ambos? | Ambos, en principio — pero ver hallazgo 6 |
| 2 | ¿Uno o varios administradores por aplicación? ¿Quién es el primero? | Varios; quien registra la aplicación queda como su primer administrador — **queda fuera de esta historia** (ver §Alcance): es un cambio a `RegisterApplicationUseCaseImpl`, y esta historia no cablea casos de uso existentes |
| 3 | ¿Delegación? | No — fuera de alcance. Un administrador asigna/revoca otros administradores como cualquier asignación de rol (HU-005), sin modelar de dónde vino el permiso |
| 4 | ¿Cómo se expresa la regla? | Como decisión de OPA, nunca como `if (rol == ADMIN)` en Java — consistente con la filosofía ya fijada en HU-004 ("el catálogo es dato de entrada para la política, jamás una decisión en Java") |
| 5 | ¿Quién crea roles globales? | Solo el ADMIN global — pero como el hallazgo 6 retira el ADMIN global de esta historia, **ese desbloqueo no ocurre todavía**: los roles globales siguen respondiendo 400 hasta una historia futura |
| 6 | **Hallazgo de código** — `Assignment.applicationId` es obligatorio (`Objects.requireNonNull`): hoy **no existe forma de asignarle un rol global a nadie**, aunque `Role` sí modele el nivel `GLOBAL` desde HU-004 | Se retira el ADMIN global de esta historia. Tocar `Assignment` para admitir asignaciones sin aplicación es un cambio a un agregado ya enviado (HU-005/006/008) — se pospone a una historia futura que asuma ese costo explícitamente |
| 7 | **Hallazgo de código** — `AccessRequest`/`PolicyDecisionPort` están modelados para "¿puede este sujeto acceder a esta `ResourcePath` con este `HttpVerb`?", con DTOs atados al contrato versionado `pdp-opa/v1`. Forzar una decisión de "¿puede administrar?" en esa forma exigiría inventar una ruta/verbo sintéticos | Puerto de decisión propio (`AdministrationDecisionPort`), mismo patrón de integración con OPA que `PolicyDecisionPort` mismo adaptador HTTP, pero con su propio payload y su propia ruta de decisión — sin tocar el contrato `pdp-opa/v1` |
| 8 | **Tamaño** — "todo el catálogo" (gatear los ~12 casos de uso de escritura de `applications`, `roles`, `resources`, `assignments`, `profiles`) es demasiado grande para un solo `PLAN.md` revisable en el Gate 1 | Esta historia entrega **solo el modelo y el mecanismo reutilizable**. Cablearlo en cada slice existente queda para historias futuras, una por slice, reutilizando el validador publicado aquí — mismo ritmo que el resto del backlog |

## 1. Resumen funcional

Esta historia **no cambia el comportamiento observable de ningún endpoint existente**. Construye la
pieza que las historias futuras necesitarán para gatear la administración del catálogo por
aplicación: un caso de uso (`AuthorizeAdministrationUseCase`) que resuelve los roles activos de un
sujeto para una aplicación y le pregunta a OPA si eso lo autoriza a administrarla, y un validador
publicado (`PrincipalMustBeApplicationAdministratorValidator`) que cualquier slice futuro puede
inyectar para rechazar una escritura con `NotAuthorizedToAdministerException` si la respuesta es
DENY o INDETERMINATE (fail-closed).

**No cubre:** cablear este mecanismo a `applications`, `roles`, `resources`, `assignments` o
`profiles` (queda para historias futuras); administrador **global** de plataforma (`Assignment` no
puede representarlo hoy — hallazgo 6); asignar automáticamente al registrador de una aplicación como
su primer administrador (toca `RegisterApplicationUseCaseImpl`, fuera de alcance); delegación con
origen; auditoría de decisiones de administración (mismo criterio que HU-013: no es una decisión de
acceso de usuario final); la política Rego real que reconoce quién es administrador (vive en
`security-policy-engine/`, fuera del alcance de este plan — mismo precedente que HU-004/006, que
construyeron el puerto de decisión antes de que existiera la política real, denegando por defecto
mientras tanto — ver ADR-012).

## 2. Criterios de aceptación

| # | Criterio | Resultado esperado |
|---|---|---|
| 1 | Un sujeto con el rol reconocido como administrador para una aplicación es autorizado | `AuthorizeAdministrationUseCase.execute` resuelve `ALLOW` cuando `AdministrationDecisionPort` responde `ALLOW` |
| 2 | Un sujeto sin ese rol es rechazado | El validador publicado lanza `NotAuthorizedToAdministerException` cuando la decisión es `DENY` |
| 3 | Fail-closed ante un fallo del motor de políticas | Un error de `AdministrationDecisionPort` (red, timeout, mapeo) se traduce a `INDETERMINATE` — nunca a `ALLOW`, nunca a un error que escape sin tipar — y el validador lo rechaza igual que un `DENY` |
| 4 | Cero regresión | `verificar.ps1` (suite completa) sigue en verde: ningún endpoint existente cambia de comportamiento, porque nada nuevo está cableado todavía |

## 3. Reglas de negocio

Ninguna regla pura nueva (no hay invariante de VO ni restricción de conjunto que decidir en Java —
la decisión la toma OPA). La única pieza de negocio es el validador publicado, que traduce una
decisión ya tomada por el puerto a una excepción — mismo criterio que "con una sola regla y ningún
otro consumidor, el propio caso de uso hace de validador", aplicado aquí a nivel de módulo: el
validador no decide, solo traduce.

| # | Regla | Dónde vive | Puerto que trae el dato | Excepción → HTTP |
|---|---|---|---|---|
| — | Un sujeto sin la decisión ALLOW no administra la aplicación | `PrincipalMustBeApplicationAdministratorValidatorImpl` (traduce, no decide) | `AuthorizeAdministrationUseCase` → `AdministrationDecisionPort` (OPA) | `NotAuthorizedToAdministerException` → 400 (`BusinessRuleViolationException`; no hay 403 en este proyecto todavía — ver Ambigüedades) |

## 4. Modelo de dominio afectado

### Entidad / agregado

Ninguna nueva. `Role` y `Assignment` (existentes, de `roles`/`assignments`) se reutilizan tal cual:
un "administrador de aplicación" es, para efectos de este mecanismo, un usuario con una asignación
activa a un rol de alcance `APPLICATION` para esa aplicación — qué **nombre** de rol reconoce OPA
como administrador es contenido de la política Rego, nunca una constante en Java (mismo principio
que HU-004 fijó para roles→recursos).

### Value objects

Ninguno nuevo en `commons` ni en `roles`/`assignments`. Nuevos DTOs de aplicación/infraestructura en
`authorization` — ver §7.

### Enums

Ninguno nuevo. Se reutilizan `DecisionState` (`ALLOW`/`DENY`/`INDETERMINATE`) y `ReasonCode`
(`POLICY_ALLOWED`/`POLICY_DENY`/`CONTEXT_UNAVAILABLE`… ya cubren el vocabulario que esta decisión
necesita, sin ampliar el catálogo cerrado compartido con PEP/OPA).

## 5. Persistencia

No toca la base de datos. No hay tabla ni esquema nuevos.

## 6. Endpoint

Ninguno. Esta historia es un mecanismo interno consumido por casos de uso, no un endpoint HTTP.

## 7. SPEC — el contrato

### `authorization/domain` — nuevos `[N]`

```java
// pdp/authorization/domain/exception/NotAuthorizedToAdministerException.java
public final class NotAuthorizedToAdministerException extends co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException {
    public NotAuthorizedToAdministerException(
            co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
            co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId) { ... }
}

// pdp/authorization/domain/message/AuthorizationMessages.java
public final class AuthorizationMessages {
    public static String notAuthorizedToAdminister(co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId) { ... }
}
```

### `authorization/application` — nuevos `[N]`

```java
// pdp/authorization/application/primaryport/request/AdministrationRequest.java
public record AdministrationRequest(
        co.edu.uco.seguridad.pdp.commons.model.TenantId tenantId,
        co.edu.uco.seguridad.pdp.commons.model.ApplicationId applicationId,
        co.edu.uco.seguridad.pdp.commons.model.UserId subjectUserId,
        String subject,
        java.util.Set<String> subjectRoles) {

    public AdministrationRequest withSubjectRoles(java.util.Set<String> roles) { ... }
}

// pdp/authorization/application/primaryport/response/AdministrationDecision.java
public record AdministrationDecision(
        co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState state,
        co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode reasonCode) {

    public boolean permits() { ... }
}

// pdp/authorization/application/secondaryport/AdministrationDecisionPort.java
public interface AdministrationDecisionPort
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AdministrationRequest, AdministrationDecision> {
}

// pdp/authorization/application/usecase/AuthorizeAdministrationUseCase.java
public interface AuthorizeAdministrationUseCase
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperation<AdministrationRequest, AdministrationDecision> {
}

// pdp/authorization/application/usecase/impl/AuthorizeAdministrationUseCaseImpl.java
public final class AuthorizeAdministrationUseCaseImpl implements AuthorizeAdministrationUseCase {
    public AuthorizeAdministrationUseCaseImpl(
            co.edu.uco.seguridad.pdp.authorization.application.rule.validator.ActiveRoleNamesLookupValidator rolesLookup,
            AdministrationDecisionPort policyDecisionPort) { ... }
    // execute: resuelve subjectRoles vía rolesLookup (ResolveActiveRolesRequest(subjectUserId, applicationId)),
    // delega en policyDecisionPort; cualquier error → AdministrationDecision(INDETERMINATE, CONTEXT_UNAVAILABLE)
}

// pdp/authorization/application/rule/validator/PrincipalMustBeApplicationAdministratorValidator.java
public interface PrincipalMustBeApplicationAdministratorValidator
        extends co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult<AdministrationRequest> {
}

// pdp/authorization/application/rule/validator/impl/PrincipalMustBeApplicationAdministratorValidatorImpl.java
public final class PrincipalMustBeApplicationAdministratorValidatorImpl
        implements PrincipalMustBeApplicationAdministratorValidator {
    public PrincipalMustBeApplicationAdministratorValidatorImpl(AuthorizeAdministrationUseCase useCase) { ... }
    // execute: si decision.permits() completa; si no, error(new NotAuthorizedToAdministerException(tenantId, applicationId))
}
```

### `authorization/infrastructure` — nuevos `[N]`

```java
// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaAdministrationEvaluationInput.java
public record OpaAdministrationEvaluationInput(
        co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaSubject subject,
        co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaTenant tenant,
        co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.dto.OpaApplication application) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/dto/OpaAdministrationEvaluationRequest.java
public record OpaAdministrationEvaluationRequest(OpaAdministrationEvaluationInput input) {
}

// pdp/authorization/infrastructure/adapter/secondary/policy/OpaAdministrationDecisionAdapter.java
public final class OpaAdministrationDecisionAdapter implements AdministrationDecisionPort {
    public OpaAdministrationDecisionAdapter(
            org.springframework.web.reactive.function.client.WebClient webClient,
            tools.jackson.databind.ObjectMapper objectMapper,
            co.edu.uco.seguridad.pdp.authorization.infrastructure.properties.OpaProperties properties) { ... }
    // execute: POST a properties.administrationDecisionPath(), envuelve con OpaAdministrationEvaluationRequest,
    // parsea con OpaResponse/OpaPolicyDecisionPayload (reutilizados — misma convención result.effect/result.reasonCode)
}
```

### Firmas modificadas `[M]` — el tester las aplica, el planificador ya las cableó donde era autocontenido

```java
// authorization/infrastructure/properties/OpaProperties.java — +1 componente, aditivo
@ConfigurationProperties(prefix = "pdp.opa")
public record OpaProperties(String baseUrl, String decisionPath, String administrationDecisionPath, Duration timeout) { ... }
// Rompe la compilación de OpaPolicyDecisionAdapterTests (construye OpaProperties directo) — el
// tester le añade el nuevo argumento, igual que en HU-014 con los fakes de ApplicationRepository.
```

### `shared/message/RequiredArgumentMessages.java` — nuevas constantes (planificador las agrega)

`ADMINISTRATION_DECISION_PORT`, `AUTHORIZE_ADMINISTRATION_USE_CASE`,
`PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR`, `OPA_ADMINISTRATION_DECISION_PATH`.

## 8. Árbol de archivos

```
pdp/src/main/java/co/edu/uco/seguridad/
├── shared/message/RequiredArgumentMessages.java                                              [M] -- +4 constantes
└── pdp/authorization/
    ├── domain/
    │   ├── exception/NotAuthorizedToAdministerException.java                                 [N]
    │   └── message/AuthorizationMessages.java                                                [N]
    ├── application/
    │   ├── primaryport/request/AdministrationRequest.java                                    [N]
    │   ├── primaryport/response/AdministrationDecision.java                                  [N]
    │   ├── secondaryport/AdministrationDecisionPort.java                                     [N]
    │   ├── usecase/AuthorizeAdministrationUseCase.java                                        [N]
    │   ├── usecase/impl/AuthorizeAdministrationUseCaseImpl.java                               [N]
    │   ├── rule/validator/PrincipalMustBeApplicationAdministratorValidator.java               [N]
    │   ├── rule/validator/impl/PrincipalMustBeApplicationAdministratorValidatorImpl.java       [N]
    │   └── rule/validator/package-info.java                                                   [N] -- @NamedInterface("rule"):
    │                                                                                                declara la frontera para que
    │                                                                                                slices futuros (applications,
    │                                                                                                roles, resources, assignments,
    │                                                                                                profiles) consuman el validador
    │                                                                                                sin abrir la caja de authorization
    ├── infrastructure/
    │   ├── adapter/secondary/policy/dto/OpaAdministrationEvaluationInput.java                 [N]
    │   ├── adapter/secondary/policy/dto/OpaAdministrationEvaluationRequest.java                [N]
    │   ├── adapter/secondary/policy/OpaAdministrationDecisionAdapter.java                     [N]
    │   ├── properties/OpaProperties.java                                                       [M] -- +administrationDecisionPath;
    │   │                                                                                            rompe OpaPolicyDecisionAdapterTests,
    │   │                                                                                            lo ajusta el tester
    │   └── config/AuthorizationConfiguration.java                                              [M] -- +3 beans (el planificador los
    │                                                                                                  cablea: clases enteramente nuevas,
    │                                                                                                  dependen solo de beans ya existentes
    │                                                                                                  — mismo criterio que HU-013/HU-014)
    └── package-info.java                                                                       [M] -- allowedDependencies ya incluye
                                                                                                        "assignments :: usecase" y
                                                                                                        "roles :: rule" (ambos consumidos
                                                                                                        transitivamente vía
                                                                                                        ActiveRoleNamesLookupValidator,
                                                                                                        ya existente) — sin cambio real,
                                                                                                        se revisó y no hace falta tocarlo
```

## 9. Casos de prueba esperados

| Capa | Clase de prueba | Casos |
|---|---|---|
| `authorization` domain | `NotAuthorizedToAdministerExceptionTests` (nueva) | Extiende `BusinessRuleViolationException`; el mensaje viene de `AuthorizationMessages.notAuthorizedToAdminister` (nunca un literal) |
| `authorization` application | `AuthorizeAdministrationUseCaseImplTests` (nueva) | Roles resueltos + decisión `ALLOW` del puerto → `AdministrationDecision.permits()` verdadero; decisión `DENY` → `permits()` falso; error del puerto (red/timeout) → `INDETERMINATE`/`CONTEXT_UNAVAILABLE`, nunca una excepción sin tipar propagada |
| `authorization` application | `PrincipalMustBeApplicationAdministratorValidatorImplTests` (nueva) | `permits()` verdadero → completa sin error; `permits()` falso (por `DENY` o por `INDETERMINATE`, fail-closed) → `NotAuthorizedToAdministerException` |
| `authorization` infrastructure | `OpaAdministrationDecisionAdapterTests` (nueva) | Mismo patrón que `OpaPolicyDecisionAdapterTests`: construye el payload esperado (`subject`/`tenant`/`application`, sin `resource`/`action`), parsea `result.effect`/`result.reasonCode` a `AdministrationDecision`, propaga el error si la llamada HTTP falla (sin capturarlo — lo captura `AuthorizeAdministrationUseCaseImpl`) |
| `authorization` infrastructure | `OpaPolicyDecisionAdapterTests` (existente, ajustada por el tester) | El único `new OpaProperties(...)` de la suite gana el nuevo argumento `administrationDecisionPath` |

## 10. Trazabilidad

| Fase | Estado | Fecha |
|---|---|---|
| Plan | ✅ Generado | 2026-09-13 |
| Contrato aprobado (gate 1) | ⏳ Pendiente | |
| Pruebas en rojo | ⏳ Pendiente | |
| Implementación en verde | ⏳ Pendiente | |
| Validación | ✅ Aprobada | 2026-09-13 |
| Entrega (gate 2) | ⏳ Pendiente | |

## 11. Ambigüedades pendientes

1. **{PENDIENTE: el proyecto no tiene un mapeo HTTP 403 hoy — `ApiErrorHandler` solo traduce a 400/409/500.**
   `NotAuthorizedToAdministerException` queda como `BusinessRuleViolationException` (400) por ahora,
   consistente con el catálogo de excepciones existente. Cuando una historia futura cablee este
   mecanismo a un endpoint real, hay que decidir si el proyecto adopta 403 para "autenticado pero sin
   permiso" (semánticamente más correcto) o si se queda con 400. No bloquea esta historia porque
   nada se cablea a HTTP todavía — pero sí bloqueará la primera historia que sí lo haga.}
2. **La política Rego real** (qué nombre/forma de rol reconoce como "administrador") no se escribe en
   este plan — vive en `security-policy-engine/`, un módulo distinto con su propia disciplina de
   autoría de políticas, fuera del alcance del harness de `pdp/`. Mientras no exista, cualquier
   llamada real a `properties.administrationDecisionPath()` fallará o (según cómo responda OPA sin
   esa política cargada) puede denegar por defecto — mismo estado transitorio que HU-004/HU-006
   tuvieron con `PolicyDecisionPort` antes de que la política de acceso existiera.
3. El alcance original de `HU-009.md` (rol global, primer administrador automático al registrar,
   cablear los ~12 casos de uso existentes) queda pendiente de trocearse en historias futuras, una
   por slice — no se numeran aquí porque no fue pedido, pero el mecanismo publicado en esta historia
   (`PrincipalMustBeApplicationAdministratorValidator`) es lo que esas historias futuras consumirán.
