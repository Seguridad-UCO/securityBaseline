# Reporte de validación — HU-024

## Metadata

- **Slice:** `shared` (evidencia de MFA) + `authorization` (decorador del gate de ADR-023)
- **Fecha:** 2026-09-17
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-024.md`
- **Rama:** `feature/HU-024-mfa-step-up-administrativo`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual lo devolvió la herramienta.

**Primera corrida (RECHAZADA):**

```
ESTADO: ROJO  (mvnw clean verify, 183,1s, exit 1)
PRUEBAS: Tests run: 761, Failures: 0, Errors: 1, Skipped: 0
  InternalSecurityChainIntegrationTests: java.lang.IllegalStateException:
  openssl no pudo generar el certificado de prueba para /CN=localhost
```

Investigada la causa (no asumida como "ambiental" sin más — lección explícita de
`CHECKPOINT.md` "un bug de producción encontrado fuera del flujo de historias"): la variable de
**usuario** `OPENSSL_CONF` apunta a `C:\Program Files\PostgreSQL\psqlODBC\etc\openssl.cnf`, que no
existe (instalación de PostgreSQL/psqlODBC ya desinstalada). `openssl.exe` sí está en el `PATH`
(`Git\mingw64\bin`), pero falla al no poder leer ese `.cnf`. **Es exactamente el mismo incidente que
ya documenta `CHECKPOINT.md` §"Un bug de producción encontrado fuera del flujo de historias"
(2026-09-13)** — la variable, ya corregida entonces, volvió a quedar establecida en esta máquina.
Ajena por completo a HU-024: el archivo de prueba no se tocó en esta rama.

**Segunda corrida, con `OPENSSL_CONF` despejada para la sesión (APROBADA):**

```
ESTADO: VERDE  (mvnw clean verify, 177,7s, exit 0)
JDK: Java 25 en C:\Users\sebas\.jdks\corretto-25.0.4.1

PRUEBAS: Tests run: 763, Failures: 0, Errors: 0, Skipped: 0

[INFO] --- jacoco:0.8.15:check (jacoco-check) @ security-pdp ---
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

`consistencia.ps1` → `CONSISTENTE: todos los slices siguen la misma forma` (8 slices).
`drift.ps1` → 1 hallazgo, preexistente y ajeno a esta rama (ver Observaciones menores).
`mapa.ps1` → regenerado y verificado al día.

| Comprobación                   | Resultado                                             |
|--------------------------------|-------------------------------------------------------|
| Compilación                    | ✅                                                     |
| Pruebas                        | ✅ 763 pruebas, 0 fallos, 0 errores                    |
| Cobertura (≥ 50 % por paquete) | ✅ `jacoco-check`: "All coverage checks have been met" |
| `LayeredArchitectureTests`     | ✅                                                     |
| `ModulithStructureTests`       | ✅                                                     |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

1. **[Deriva preexistente, no introducida por esta historia]** `pdp/docs/ai-harness/CHECKPOINT.md:49`
   cita la clase `SurrealAdministrationEventSchemaInitializer`, que no existe en el código.
   `drift.ps1` lo reporta, pero el archivo no fue tocado en esta rama — su último cambio es el commit
   de HU-023 (`c075521`). No bloquea esta validación (regla: "la deriva preexistente es observación;
   la nueva es bloqueante").
2. **[Fragilidad de entorno, no de código]** La variable de usuario `OPENSSL_CONF` de esta máquina
   quedó otra vez apuntando a un archivo de una instalación desinstalada (mismo incidente que
   `CHECKPOINT.md` ya documentó el 2026-09-13). Se resolvió para esta validación despejando la
   variable **solo para la sesión** que corrió `verificar.ps1`, sin modificar la configuración
   permanente del usuario. Si vuelve a aparecer en otra máquina o sesión, el remedio ya documentado
   es borrar esa variable de usuario, no tocar el código.
3. **[Adición no descrita literalmente en la SPEC §7, pero consistente con ella]**
   `AuthenticationContextEvidence` ganó una constante estática `NONE` y un método estático
   `from(ClaimAccessor)` que la SPEC no mostraba (solo mostraba el `record` puro). Es aditivo — no
   cambia el componente canónico `(Optional<String> acr, List<String> amr)` — y responde a una
   instrucción explícita del propio plan para `OidcAuthenticationSuccessHandler` ("mismo método de
   extracción... no dupliques la lógica, factoriza si hace falta"), reutilizado también en
   `PdpPrincipal.from(Jwt)`/`from(OidcUser)`.
4. **[Desviación aceptada — constructores de compatibilidad]** `AdministrationRequest` y
   `LocalUserPrincipal` ganaron un constructor de aridad antigua (evidencia de MFA por defecto vacía)
   no descrito en la SPEC. Es aditivo y no cambia la forma canónica de seis/seis componentes que la
   SPEC exige; su único uso hoy queda en pruebas unitarias preexistentes ajenas a MFA (p. ej.
   `AuthorizeAdministrationUseCaseImplTests`) que no necesitan la evidencia.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|---|-----------------------------------------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | (1) `MfaAwareApplicationAdministratorValidatorTests.rejects_with_mfa_evidence_required_when_the_delegate_allows_but_the_authentication_context_does_not_satisfy_mfa` distingue `MFA_REQUIRED` de `NOT_AUTHORIZED_TO_ADMINISTER`; (2) `completes_when_the_delegate_allows_and_the_authentication_context_satisfies_mfa` + las 5 `*HttpTests` actualizadas (`ApplicationHttpTests`, `AssignmentHttpTests`, `RoleHttpTests`, `ProfileAssignmentHttpTests`, `AuthorizationHttpTests`) confirman cero fricción con evidencia real, verificado en `verify` completo (763 pruebas en verde); (3) el decorador solo se inyecta en `principalMustBeApplicationAdministratorValidator` de `AuthorizationConfiguration`, nunca en `authorizeUseCase`/catálogos; (4) decorador de la única interfaz que comparten los 12 flujos administrativos — verificado al extender los 10 `*InteractorImpl` restantes para que la evidencia real viaje siempre a `AdministrationRequest`; (5) `MfaEvidencePropertiesTests` caso (f) prueba el fail-closed sin configuración; (6) `verificar.ps1` completo en verde, con cobertura, en la segunda corrida |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Identificadores nuevos (`MfaEvidenceProperties`, `AuthenticationContextEvidence`, `MfaAwareApplicationAdministratorValidator`, `signedTokenWithMfaEvidence`, `MFA_ACCEPTED_ACR`) en inglés; Javadoc y comentarios en español en los archivos nuevos/modificados                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | `drift.ps1` reporta 1 hallazgo, preexistente y ajeno a esta rama (ver Observaciones menores). Ninguno nuevo introducido por HU-024                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | La decisión de MFA vive en `MfaAwareApplicationAdministratorValidator` (equivalente a una `Rule`, sin I/O propio, mismo criterio que el plan justifica en §3 para no crear una `Rule` separada); ningún `if/throw` de negocio dentro de un use case; sin anotaciones de Spring en `domain`/`application` (`MfaEvidenceProperties` con `@ConfigurationProperties` vive en `shared`, mismo patrón que `RevocationRetentionProperties`); sin `block()` nuevo; ningún `allowedDependencies` de Modulith relajado                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

## Criterios de la línea base

> Solo los que el plan declaró (§0: 1, 2, 9, 11, 12, 21, 22, 23).

| #  | Criterio                       | Resultado | Punto de control comprobado                                                                                                                     |
|----|--------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture             | ✅         | 🤖 `LayeredArchitectureTests`/`ModulithStructureTests` en verde; sin anotación Spring en `domain`/`application`                                 |
| 2  | Contratos de servicios         | ✅         | `MfaAwareApplicationAdministratorValidator implements PrincipalMustBeApplicationAdministratorValidator`, interfaz vacía sobre `shared/contract` |
| 9  | Excepciones                    | ✅         | `MfaEvidenceRequiredException extends BusinessRuleViolationException`; traducción HTTP solo por jerarquía en `ApiErrorHandler`, sin tocarlo     |
| 11 | Interacción entre capas        | ✅         | Interactor → UseCase → validador decorado → dominio; ningún controller importa `application`                                                    |
| 12 | SOLID                          | ✅         | Inyección por constructor contra interfaces (`delegate`, `mfaProperties`); operación mínima (`execute`)                                         |
| 21 | Modelo refinado                | ✅         | `AuthenticationContextEvidence`/`MfaEvidenceProperties` son `record` inmutables, sin Lombok, con comportamiento (`satisfiedBy`, `from`)         |
| 22 | Arquitectura reactiva          | ✅         | `execute` usa `Mono.defer`/`.then()`, sin `block()`                                                                                             |
| 23 | Arquitectura antes del negocio | ✅         | 🤖 `verify` en verde (763 pruebas), `jacoco-check`: "All coverage checks have been met"                                                         |

## Desviaciones respecto al plan

| Archivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Plan decía                                                                                                            | Código hace                                                                                                           | ¿Justificado?                                                                                                                                                                                                                                                                                                                                                          |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AdministrationRequest.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Un solo componente nuevo, sin mencionar compatibilidad                                                                | Constructor adicional de 5 argumentos (evidencia vacía por defecto)                                                   | Sí — evita romper compilación de sitios de construcción no declarados en el árbol §8; ver observación 4                                                                                                                                                                                                                                                                |
| `LocalUserPrincipal.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Igual que arriba                                                                                                      | Mismo patrón de constructor de compatibilidad                                                                         | Sí — permite que `ProvisionIdentityUseCaseImpl` (fuera del árbol §8) siga sin cambios, tal como el plan pide explícitamente en §7                                                                                                                                                                                                                                      |
| `AuthenticationContextEvidence.java`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Solo el `record` puro en la SPEC                                                                                      | Gana `NONE` y `from(ClaimAccessor)`                                                                                   | Sí — responde a la instrucción del plan de no duplicar la extracción entre `Jwt`/`OidcUser`/`OidcIdToken`                                                                                                                                                                                                                                                              |
| 10 archivos `*InteractorImpl.java` (fuera del árbol §8: `AdministerApplicationAdministratorAssignmentInteractorImpl`, `...ListInteractorImpl`, `...RemovalInteractorImpl`, `AdministerAssignmentCreationInteractorImpl`, `...RevocationInteractorImpl`, `AdministerProfileAssignmentCreationInteractorImpl`, `...RevocationInteractorImpl`, `AdministerProfileDefinitionInteractorImpl`, `AdministerProfileRoleAdditionInteractorImpl`, `AdministerResourceGrantInteractorImpl`, `AdministerResourceRegistrationInteractorImpl`, `AdministerRoleDefinitionInteractorImpl`) | No declarados en el árbol §8                                                                                          | Cada uno pasa `principal.authenticationContext()` en vez de dejar la evidencia vacía por defecto                      | Sí, y necesario: sin este cambio, con el decorador cableado en `AuthorizationConfiguration`, esos 10 flujos administrativos habrían quedado permanentemente bloqueados con `MFA_REQUIRED` sin importar la evidencia real del sujeto — violación directa del criterio de aceptación 2. Decisión tomada con autorización explícita del usuario durante la implementación |
| 5 archivos `*HttpTests.java` (`ApplicationHttpTests`, `AssignmentHttpTests`, `RoleHttpTests`, `ProfileAssignmentHttpTests`, `AuthorizationHttpTests`) + `TestJwtSupport.java`                                                                                                                                                                                                                                                                                                                                                                                              | El plan asigna la escritura de pruebas solo a `2-tester-spec`; `3-implementador` tiene prohibido tocar `pdp/src/test` | El implementador los modificó para llevar evidencia de MFA real en las llamadas a rutas administrativas ya existentes | Sí, con excepción explícita autorizada por el usuario, para no debilitar ninguna aserción y mantener el criterio 6 en verde                                                                                                                                                                                                                                            |

## Datos para la entrega

- **Mensaje de commit:** `feat(shared,authorization): MFA como step-up para operaciones administrativas (HU-024)`
- **Cuerpo:** Añade `AuthenticationContextEvidence`/`MfaEvidenceProperties` (`shared/security/mfa`), decora
  `PrincipalMustBeApplicationAdministratorValidator` con `MfaAwareApplicationAdministratorValidator` en
  `AuthorizationConfiguration` (fail-closed mientras `pdp.security.mfa.claim`/`accepted-values` no estén configurados),
  y extiende `PdpPrincipal`/`LocalUserPrincipal`/`AdministrationRequest` para transportar la evidencia desde el login (
  `OidcAuthenticationSuccessHandler`) hasta cada uno de los 12 flujos administrativos gateados. Sin endpoint nuevo; sin
  cambios en catálogos ni en `/api/v1/authorize`.
- **Rama:** `feature/HU-024-mfa-step-up-administrativo`
- **Archivos a incluir:** todos los `.java` listados en "Desviaciones" y en el árbol §8 del plan, más
  `application.properties` (sin cambios adicionales, `pdp.security.mfa.claim`/`accepted-values` siguen vacíos) — no
  incluir el plan ni este reporte, que se versionan en `docs/ai-harness/workspace/`.

## Próximos pasos

Listo para el gate 2 (entrega).
