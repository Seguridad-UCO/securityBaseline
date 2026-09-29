# Reporte de validación — HU-008

## Metadata

- **Slice:** `authorization` (con cambios en `shared/security`, `roles`, `assignments`)
- **Fecha:** 2026-09-12
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-008.md`
- **Rama:** `feature/HU-008-identidad-bff-roles-opa`

## Resultado del build

```
ESTADO: VERDE  (mvnw clean verify, 66,5s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 478, Failures: 0, Errors: 0, Skipped: 0
```

```
CONSISTENTE: todos los slices siguen la misma forma.
Slices verificados: applications, assignments, authorization, identity, resources, roles, tenants
```

```
CLASES CITADAS QUE NO EXISTEN: 1
  PepRegistrationProperties  (pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md)

Excepciones declaradas activas: 16 (ver drift-ignore.txt)
```

`PepRegistrationProperties` es la excepción preexistente ya conocida (`drift.ps1` no escanea `pep/`)
— no la introdujo este cambio. Cero hallazgos nuevos.

| Comprobación                   | Resultado                                                                                                                                                                                                                                                                                                         |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Compilación                    | ✅                                                                                                                                                                                                                                                                                                                 |
| Pruebas                        | ✅ 478 pruebas, 0 fallos (7 nuevas/extendidas: `RoleNamesLookupValidatorImplTests` ×3, `ActiveRoleNamesLookupValidatorImplTests` ×2, `AuthorizeUseCaseImplTests` +1, `AuthorizeRequestMapperTests` +1; más los ajustes mecánicos de firma en `OpaPolicyDecisionAdapterTests` y `PdpPrincipalSecurityContextTests`) |
| Cobertura (≥ 50 % por paquete) | ✅ (`jacoco-check` pasó dentro de `clean verify`)                                                                                                                                                                                                                                                                  |
| `LayeredArchitectureTests`     | ✅                                                                                                                                                                                                                                                                                                                 |
| `ModulithStructureTests`       | ✅ — valida las tres fronteras nuevas (`assignments :: usecase`, `assignments :: dto`, `roles :: rule` en `authorization`) sin relajar nada existente                                                                                                                                                              |

## Estado final

> ✅ APROBADO — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 1, criterio 6] — El canal interno no afirma explícitamente que `subjectUserId`/`subjectRoles` queden vacíos

- **Archivo:**
  `pdp/src/test/java/co/edu/uco/seguridad/pdp/authorization/application/usecase/impl/EvaluateInternalAccessUseCaseImplTests.java`
- **Qué pasa:** el test `resolves_the_owner_tenant_and_delegates_to_authorize_use_case` captura el
  `AccessRequest` que `EvaluateInternalAccessUseCaseImpl` construye y afirma siete de sus nueve
  campos — no `subjectUserId()` ni `subjectRoles()`. El plan (criterio 6) solo pedía que este test
  siguiera pasando "sin cambios de expectativa", y así fue: no es un bloqueante. Pero como este test
  es la evidencia concreta de que el canal interno queda deliberadamente sin identidad resuelta (la
  decisión de alcance más importante del plan, §0), una aserción explícita
  `assertThat(built.subjectUserId()).isEmpty()` haría esa garantía verificable por una prueba y no
  solo por lectura de código (`Optional.empty(), Set.of()` en la línea de construcción). No hace
  falta abrir un ciclo nuevo por esto — es una mejora de precisión, no una corrección.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|---|-----------------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | Fila 1: `PdpPrincipalSecurityContextTests.creates_principals_from_jwt_and_oidc_claims_with_sid_fallback` (`userId()` ausente) + `rejects_missing_required_principal_values_and_resolves_supported_session_identities` (`LocalUserPrincipal` → `userId()` presente con el valor correcto). Fila 2: `AuthorizeUseCaseImplTests.delegates_to_the_policy_port_with_resolved_role_names_when_subject_user_id_is_present`. Fila 3: los seis tests existentes de `AuthorizeUseCaseImplTests` reutilizan `NEVER_ROLES_LOOKUP` (poison pill) con `REQUEST` (`subjectUserId` ausente) — nunca se alcanza. Fila 4: `RoleNamesLookupValidatorImplTests.omits_a_role_id_that_no_longer_exists_without_failing`. Fila 5: `OpaPolicyDecisionAdapterTests.sends_the_input_wrapped_exactly_as_the_contract_expects`, extendido para afirmar `subject.roles`. Fila 6: ver observación menor arriba — pasa, sin aserción explícita nueva. Fila 7: `AuthorizationHttpTests` e `InternalSecurityChainIntegrationTests` (e2e) en verde dentro de las 478, sin tocar sus aserciones |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Clases nuevas: `RoleNamesLookupValidator(Impl)`, `ActiveRoleNamesLookupValidator(Impl)` — inglés. Javadoc de las cuatro clases y de los tres `package-info.java` nuevos de `assignments`, en español. Los cinco mensajes nuevos de `RequiredArgumentMessages` (`PRINCIPAL_USER_ID`, `SUBJECT_ROLES`, `ROLE_NAMES_LOOKUP_VALIDATOR`, `ACTIVE_ROLE_NAMES_LOOKUP_VALIDATOR`), en español. Métodos de prueba en `snake_case` inglés descriptivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | `drift.ps1` solo muestra la excepción preexistente — cero hallazgos nuevos                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | `AuthorizeUseCaseImpl.resolveRoles(...)` usa `Optional.map/orElseGet` para decidir si hay `subjectUserId` con qué preguntar — es despacho sobre la forma del dato (mismo idioma que `RoleScope`'s `Optional` o `RequestFieldParser.optional`), no un `if` de negocio: no rechaza nada ni decide ALLOW/DENY, solo evita construir una consulta que no tiene con qué resolverse. `RoleNamesLookupValidatorImpl`/`ActiveRoleNamesLookupValidatorImpl` hacen I/O real (repositorio, otro caso de uso) — el `Mono`/`Flux` está justificado, no es una regla pura envuelta sin necesidad. Cero anotaciones de Spring fuera de `infrastructure`. Las tres fronteras nuevas de Modulith (`assignments :: usecase`, `assignments :: dto`, `roles :: rule` en `authorization`) son cruces reales y documentados, no un relajamiento para que compilara — `assignments` public sus primeras interfaces nombradas exactamente como el propio `ResolveActiveRolesUseCase` anticipaba desde HU-005                                                                         |

## Criterios de la línea base

> Solo los que el plan declaró. Los marcados 🤖 los resuelve el build, no la lectura.

| #  | Criterio                  | Resultado | Punto de control comprobado                                                                                                                                                                                                                 |
|----|---------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture        | 🤖 ✅      | `LayeredArchitectureTests`/`ModulithStructureTests` en verde dentro de `clean verify`                                                                                                                                                       |
| 2  | Contratos de servicios    | ✅         | `RoleNamesLookupValidator`/`ActiveRoleNamesLookupValidator` son interfaces vacías que extienden `ReactiveOperation`, sin métodos propios                                                                                                    |
| 4  | Capacidades transversales | ✅         | Ningún `Instant.now()`/`UUID.randomUUID()` en línea nuevo; `resolveRoles` solo delega                                                                                                                                                       |
| 9  | Excepciones               | ✅         | `RoleNamesLookupValidatorImpl` omite un id ausente vía `Flux`/`flatMap` (el `Mono.empty()` de `findById` simplemente no emite) — no hay excepción que atrapar ni que fabricar                                                               |
| 11 | Interacción entre capas   | ✅         | Ningún controller ni interactor cambia su forma de delegar; `AuthorizeInteractorImpl` solo agrega un campo más al mapeo del principal                                                                                                       |
| 12 | SOLID                     | ✅         | `ActiveRoleNamesLookupValidatorImpl` depende de dos interfaces mínimas (`ResolveActiveRolesUseCase`, `RoleNamesLookupValidator`), inyectadas por constructor, cada una validada con `Objects.requireNonNull`                                |
| 21 | Modelo refinado           | ✅         | `AccessRequest.withSubjectRoles(...)` sigue el mismo patrón wither que `Role.withResource(...)`/`Assignment.revoke(...)`; `PdpPrincipal`/`AccessRequest` siguen siendo `record` inmutables                                                  |
| 22 | Arquitectura reactiva     | ✅         | `Flux.fromIterable(...).flatMap(...).collect(...)` en `RoleNamesLookupValidatorImpl`; cadena `map`/`flatMap` sin `block()` en `ActiveRoleNamesLookupValidatorImpl`; `Mono.defer(...)` preservado en el paso nuevo de `AuthorizeUseCaseImpl` |

## Desviaciones respecto al plan

Ninguna. El árbol de archivos final coincide exactamente con la sección 8 del plan: los siete `[N]`
existen y compilan con lógica real, los trece `[M]` se aplicaron tal cual (confirmado contra
`git status` en el cierre del implementador).

## Datos para la entrega

- **Mensaje de commit:**
  `feat(authorization): resolver identidad de usuario en el canal BFF para que subject.roles llegue a OPA (HU-008)`
- **Cuerpo:** `PdpPrincipal` deja de descartar el `UserId` que `LocalUserPrincipal` ya resolvía en el
  primer login; `AccessRequest` lo transporta como `subjectUserId` y `AuthorizeUseCaseImpl` lo usa
  para resolver los roles activos del usuario (`assignments.ResolveActiveRolesUseCase`, ya existente
  desde HU-005) y traducirlos a nombres (`roles.RoleNamesLookupValidator`, nuevo) antes de llamar a
  `PolicyDecisionPort`. `OpaPolicyDecisionAdapter` ya envía esos nombres en `subject.roles`. El canal
  interno (PEP) queda explícitamente fuera — requeriría cambiar `contracts/pep-pdp/v1/`, decisión que
  no corresponde solo al PDP.
- **Rama:** `feature/HU-008-identidad-bff-roles-opa`
- **Archivos a incluir:** los listados en el árbol de la sección 8 del plan bajo `pdp/src/main` y
  `pdp/src/test` — no el plan ni este reporte, que se versionan aparte en
  `pdp/docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega).
