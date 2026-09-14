# Reporte de validación — HU-016

## Metadata

- **Slice:** `roles` (pierde dos escrituras) y `authorization` (las recibe, orquestadas)
- **Fecha:** 2026-09-14
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-016.md`
- **Rama:** `feature/HU-016-gatear-roles-administracion`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1` (sin flags — `mvnw clean verify`).

```
ESTADO: VERDE  (mvnw clean verify, 116,4s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 668, Failures: 0, Errors: 0, Skipped: 0
```

`jacoco-check` corrió esta vez (línea 861 del log: `jacoco:0.8.15:check (jacoco-check)`) y no reportó
ninguna violación de umbral — cobertura confirmada por el build, no asumida.

**Nota sobre el primer intento de esta validación:** la corrida inicial dio `ROJO` por
`InternalSecurityChainIntegrationTests.rejects_without_a_client_certificate` (`expected: 403 but was:
200`). Verificado con `git stash -u` que fallaba igual en `develop` limpio, sin ningún archivo de
HU-016 — era ajeno a esta historia. Causa raíz encontrada: PR #49 agregó
`InternalMtlsProperties.enabled` y en `application.properties` (perfil por defecto, el que usa esta
prueba) quedó con default `false` — el filtro mTLS se salía sin validar nada. Corregido fijando
`pdp.security.internal.mtls.enabled=true` explícitamente en el `@DynamicPropertySource` de la propia
prueba (no depende del default de ningún perfil). El default `false` en `application.properties`
queda como hallazgo aparte, no corregido aquí — ver Observaciones.

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 668 pruebas, 0 fallos |
| Cobertura (≥ 80 % en código nuevo) | ✅ `jacoco-check` corrió y no reportó violaciones |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |
| `consistencia.ps1` | ✅ CONSISTENTE — 8 slices |
| `drift.ps1` | ✅ SIN DERIVA |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

- **`identity/application/primaryport/request/package-info.java` y `.../response/package-info.java`
  (nuevos), y `"identity :: dto"` en `authorization/package-info.java`** — no están en el árbol §8 de
  `PLAN-HU-016.md` porque no son parte de esta historia: corrigen una omisión real de la PR #49
  (`feature/ajustes_integracion_completa`, fusionada a `develop` mientras HU-016 estaba en curso).
  `InternalAccessDecisionInteractorImpl` ya construía `ResolveExternalIdentityRequest` sin que ese
  paquete estuviera publicado — `ModulithStructureTests` lo marcó al fusionar. Corregido en el mismo
  tramo de trabajo porque bloqueaba `verificar.ps1` para *cualquier* historia, no solo esta. Documentado
  en `CHECKPOINT.md` (2026-09-14). No rompe ninguna convención; es la misma forma que `roles :: dto`.
- **`AssignmentHttpTests.java` y `RoleHttpTests.java` (modificados)** — tampoco están en el árbol §8
  del plan original. Ambas clases usaban `POST /api/v1/roles` (alcance `APPLICATION`) como *fixture*
  de sus propios escenarios, sin necesitar OPA porque ese endpoint no estaba gateado antes de esta
  historia. Al gatearlo, sus fixtures empezaron a fallar (`NOT_AUTHORIZED_TO_ADMINISTER`, fail-closed
  sin OPA disponible) aunque el usuario de prueba sí administra la aplicación — un problema real
  causado por esta historia, corregido añadiéndoles el mismo `OpaFixtureServer` embebido que
  `ApplicationHttpTests` ya usa para el mismo propósito desde HU-015. Consecuencia directa y necesaria
  de HU-016, aunque el plan no la previó explícitamente.
- **`InternalSecurityChainIntegrationTests.java` (modificado, fuera de esta historia)** — se le agregó
  `pdp.security.internal.mtls.enabled=true` a su `@DynamicPropertySource`. No es un cambio de HU-016;
  corrige un hallazgo real encontrado *durante* esta validación (ver Resultado del build) y aprobado
  por Sebastián para aplicarlo en el mismo tramo en vez de dejarlo pendiente. El default `false` de
  `pdp.security.internal.mtls.enabled` en `application.properties` (perfil por defecto) **sigue sin
  corregirse** — contradice ADR-025 (Zero Trust, fail-closed) al apagar la validación mTLS por
  defecto fuera de `dev`/`prod`. Queda como hallazgo para el equipo del PEP (David, autor de la PR
  #49 que introdujo el flag), no resuelto aquí porque cambiar un default de producción es una
  decisión de postura, no una corrección de prueba.
- **`RoleAdministrationController.java`** — el plan lo marcaba `[N]` pero con una nota operativa de que
  el planificador lo dejaría sin materializar por riesgo de colisión de rutas con `RoleController`
  mientras ambos estuvieran activos a la vez. Se creó (real, no esqueleto) en la fase de
  `@2-tester-spec`, en el mismo paso que se retiraron `define()`/`grantResource()` de `RoleController`
  — exactamente como el plan anticipaba que debía suceder.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅ | Ver tabla detallada abajo — los 9 criterios de la §2 del plan están cubiertos por una prueba o un archivo concreto |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español) | ✅ | Clases nuevas (`AdministerRoleDefinitionUseCase`, `RoleApplicationLookupValidator`…) en inglés; Javadoc y los dos mensajes movidos a `AuthorizationMessages` (`globalScopeNotAdministrableYet`, `applicationIdNotApplicableForTenantScope`) en español; métodos de prueba en inglés descriptivo |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` → SIN DERIVA, 16 excepciones preexistentes sin cambio. `PROJECT-MAP.md` y `CHECKPOINT.md` actualizados en el mismo tramo |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | Ver detalle abajo |

**Detalle del juicio 1 — criterios de aceptación:**

| # Plan §2 | Criterio | Evidencia |
|---|---|---|
| 1 | Definir rol `APPLICATION` como admin → 201 | `AdministerRoleDefinitionUseCaseImplTests.defines_the_role_when_the_principal_administers_the_application`; `RoleHttpTests.refuses_granting_a_resource_from_another_application_to_an_application_scoped_role` usa `defineApplicationScopedRole` como fixture con éxito (201) |
| 2 | Definir rol `APPLICATION` sin ser admin → 400 `NOT_AUTHORIZED_TO_ADMINISTER` | `AdministerRoleDefinitionUseCaseImplTests.never_defines_the_role_when_the_principal_does_not_administer_the_application` |
| 3 | Definir rol `TENANT` → 201, sin gate | `AdministerRoleDefinitionUseCaseImplTests.defines_a_tenant_scoped_role_without_gating_when_administration_is_empty`; `RoleHttpTests.defines_a_tenant_scoped_role` (e2e) |
| 4 | Conceder recurso a rol `APPLICATION` como admin → 200 | `AdministerResourceGrantUseCaseImplTests.grants_the_resource_when_the_principal_administers_the_application`; `RoleHttpTests.refuses_granting_a_resource_from_another_application_to_an_application_scoped_role` llega a `RESOURCE_OUTSIDE_ROLE_SCOPE` (no a `NOT_AUTHORIZED_TO_ADMINISTER`) — prueba indirecta de que el gate permitió pasar |
| 5 | Conceder recurso a rol `APPLICATION` sin ser admin → 400 | `AdministerResourceGrantUseCaseImplTests.never_grants_the_resource_when_the_principal_does_not_administer_the_application` |
| 6 | Conceder recurso a rol `TENANT` → 200, sin gate | `AdministerResourceGrantUseCaseImplTests.grants_a_resource_to_a_tenant_scoped_role_without_gating_when_administration_is_empty` |
| 7 | `GET /api/v1/roles` sin cambios | `RoleControllerTests.list_delegates_to_the_interactor_and_replies_with_200`; `RoleHttpTests.the_catalog_never_shows_roles_of_another_tenant` |
| 8 | Rol inexistente en `POST /{roleId}/resources` → rechazo del dominio | `AdministerResourceGrantInteractorImplTests.propagates_role_not_found_without_reaching_the_use_case` (`RoleNotFoundException`, mapeado a 400 por `ApiErrorHandler`, mismo criterio que el resto del proyecto) |
| 9 | `verificar.ps1` en verde | ✅ 668/668, incluido `jacoco-check` |

**Detalle del juicio 4 — capa correcta:**

- `AdministerRoleDefinitionUseCaseImpl`/`AdministerResourceGrantUseCaseImpl`: el gate es
  `input.administration().map(...).orElseGet(Mono::empty)` — no es un `if/throw` de negocio, es control
  de flujo funcional sobre un `Optional` ya resuelto por el interactor. Ninguna `Rule` nueva hacía
  falta (el plan §3 ya lo señalaba: R3 no es una regla, decide si hay algo que gatear).
- `RoleAdministrationController`: solo delega a los interactores y envuelve con `ApiResponse` —
  mismo patrón que todos los controllers del proyecto, sin decidir nada.
- `DefineRoleRequestMapper`/`GrantResourceRequestMapper`: la validación de formato vive en
  `RequestFieldParser` + los constructores de `RoleName`/`RoleScope`/`ApplicationId`, no en el mapper.
- `RoleApplicationLookupValidatorImpl`: hace la E/S (consulta el repositorio) y delega la decisión de
  "existe o no" a la `Rule` pura `RoleMustExistForTenantRule` — no reimplementa la regla.
- Sin anotaciones de Spring en `domain` ni `application` de ninguna de las dos piezas nuevas.
- `allowedDependencies` de `authorization` se amplió tres veces (`roles :: usecase`, `roles :: dto`,
  `roles :: model`) — las tres estaban ya previstas en el propio plan (§8) como consecuencia directa
  del diseño aprobado en el gate 1, no relajaciones ad-hoc para que compilara. La cuarta
  (`identity :: dto`) es la corrección de la omisión real de la PR #49, discutida en Observaciones.

## Criterios de la línea base

> Los que el plan declaró: 1, 2, 3, 9, 11, 12, 21, 22.

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ 🤖 | `LayeredArchitectureTests` corrió en verde antes del fallo ajeno; sin anotaciones de Spring en `domain`/`application` de las piezas nuevas |
| 2 | Contratos de servicios | ✅ | `RoleApplicationLookupValidator`, `AdministerRoleDefinitionUseCase`, `AdministerResourceGrantUseCase`, ambos interactores: interfaces vacías extendiendo `ReactiveOperation` |
| 3 | Reglas e integridad | ✅ | Ninguna `Rule` nueva necesaria (ver Juicio 4); `RoleApplicationLookupValidatorImpl` reutiliza `RoleMustExistForTenantRule` sin duplicarla |
| 9 | Excepciones | ✅ | Reutiliza `NotAuthorizedToAdministerException`/`RoleNotFoundException` existentes, sin excepción nueva; `ApiErrorHandler` sin tocar |
| 11 | Interacción entre capas | ✅ | Controller → interactor → use case → (validador/regla) — verificado archivo por archivo en Juicio 4 |
| 12 | SOLID | ✅ | Contratos de una operación; `AdministerRoleDefinitionUseCaseImpl`/`AdministerResourceGrantUseCaseImpl` dependen de interfaces (`PrincipalMustBeApplicationAdministratorValidator`, `DefineRoleUseCase`/`GrantResourceToRoleUseCase`) inyectadas por constructor |
| 21 | Modelo refinado | ✅ | `AdministerRoleDefinitionRequest`/`AdministerResourceGrantRequest`: records inmutables con `Objects.requireNonNull` en cada componente (verificado con el grep de records vacíos — ninguno) |
| 22 | Arquitectura reactiva | ✅ | `Mono` en toda la cadena nueva; sin `block()`; `Mono.defer(...)` para diferir la ejecución del caso de uso real tras el gate, mismo patrón que HU-015 |

## Desviaciones respecto al plan

| Archivo | Plan decía | Código hace | ¿Justificado? |
|---|---|---|---|
| `authorization/package-info.java` | Agregar `roles :: usecase`, `roles :: dto` y `roles :: model` | Las tres presentes | Sí — ya estaba en el texto del plan, solo faltaba aplicarlo (corregido en la fase de implementación) |
| `authorization/package-info.java` | No mencionaba `identity :: dto` | Se agregó | Sí — corrige una omisión real de la PR #49 ajena a esta historia (ver Observaciones), no una relajación para HU-016 |
| `identity/application/primaryport/{request,response}/package-info.java` | No declarados | Creados | Sí — mismo motivo que arriba |
| `AssignmentHttpTests.java`, `RoleHttpTests.java` | No declarados como `[M]` | Se les agregó `OpaFixtureServer` | Sí — consecuencia directa de gatear un endpoint que sus fixtures usaban sin saberlo (ver Observaciones) |
| `RoleAdministrationController.java` | `[N]`, con nota de creación diferida | Creado en la fase de tester, no de planificación | Sí — el propio plan anticipaba esta secuencia por el riesgo de colisión de rutas |
| `InternalSecurityChainIntegrationTests.java` | No declarado | Se le agregó `pdp.security.internal.mtls.enabled=true` a su `@DynamicPropertySource` | Sí — corrige un hallazgo real de la PR #49 descubierto durante esta validación, aprobado por Sebastián; no es alcance de HU-016 pero se aplicó en el mismo tramo |

## Datos para la entrega

- **Mensaje de commit:** `feat(roles): gatea DefineRole/GrantResourceToRole por administración de aplicación (HU-016)`
- **Cuerpo:** Mueve la escritura de roles de alcance `APPLICATION` (definir rol, conceder recurso) a
  `authorization`, gateada por `PrincipalMustBeApplicationAdministratorValidator` (HU-009/ADR-023).
  Roles `TENANT` no se gatean (sin administrador de tenant todavía, ADR-024). Nuevo
  `RoleApplicationLookupValidator` en `roles` resuelve la aplicación dueña de un rol para el gate de
  concesión de recursos. `RoleController` conserva solo la consulta; `RoleAdministrationController`
  (nuevo, en `authorization`) expone las dos escrituras en las mismas rutas de siempre
  (`POST /api/v1/roles`, `POST /api/v1/roles/{roleId}/resources`). Incluye una corrección ajena
  (Modulith `identity :: dto`, PR #49) y dos ajustes de *fixtures* de prueba (`AssignmentHttpTests`,
  `RoleHttpTests`) necesarios por el nuevo gate.
- **Rama:** `feature/HU-016-gatear-roles-administracion`
- **Archivos a incluir:** todo `pdp/src/main` y `pdp/src/test` tocado en este tramo (listado completo
  en `git status` — 27 archivos de producción/config + 11 de prueba, contando la corrección de la
  PR #49). El plan (`PLAN-HU-016.md`) y este reporte se versionan aparte, en
  `pdp/docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega). Pendiente aparte, sin bloquear esta historia: revisar con el equipo
del PEP si `pdp.security.internal.mtls.enabled` debe defaultear a `true` también en el perfil por
defecto de `application.properties` (ver Observaciones) — es una decisión de postura de seguridad,
no un defecto de esta entrega.

> **Gate 2 — antes de que esto salga del repositorio.** El reporte está aprobado. Confirma para
> proceder con commit y push.
