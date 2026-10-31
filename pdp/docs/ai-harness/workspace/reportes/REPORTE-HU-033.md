# REPORTE — HU-033 (`MFA_REQUIRED` responde `403`) — 2026-10-30

**Veredicto: APROBADO.**

## Cómo se validó

Flujo del harness: `@2-tester-spec` (rojo por aserción: `expected 403 but was 400`) → `@3-implementador` (una condición en
`ApiErrorHandler.businessRule`) → `mvnw clean verify`, `consistencia.ps1`, `drift.ps1`.

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | Operación administrativa sin evidencia de MFA → `403`, `code=MFA_REQUIRED`, `problem+json` | ✅ | `ApiErrorHandlerTests` (unitaria) y `ApplicationSecurityHttpTests.an_administrative_read_without_mfa_evidence_is_a_403_mfa_required_problem_detail` (HTTP con Testcontainers) |
| 2 | Validación → `400`; conflicto → `409`; `NOT_AUTHORIZED_TO_ADMINISTER` → `403` | ✅ | pruebas existentes de `ApiErrorHandlerTests`, sin cambios |
| 3 | `verificar.ps1` completo, `consistencia`, `drift` | ✅ | **902 pruebas, 0 fallos, 0 errores**, JaCoCo ≥ 50 % por paquete; CONSISTENTE; SIN DERIVA |

## Origen

ADR-031 dice que, si el host no eleva el token, las operaciones administrativas "reciben 403". El código respondía `400` por omisión
(toda regla de negocio no listada es `400`). Ningún consumidor (`securityBaseline-vue`, `security-ui`) dependía del `400`.

## Hallazgo fuera de alcance (importante)

El `application.properties` de `src/test/resources` **sombrea** al de producción y no declara `pdp.security.mfa.enabled`, así que en
las pruebas HTTP el step-up de MFA queda **apagado**. Las demás clases HTTP (`AuthorizationHttpTests`, `RoleHttpTests`,
`ApplicationHttpTests`, `AssignmentHttpTests`, `ProfileAssignmentHttpTests`) fijan claim y valores aceptados pero no activan MFA, por lo
que hoy no ejercitan el gate. Solo la prueba nueva lo activa (en su `@DynamicPropertySource`). Conviene decidir si el perfil de pruebas
debe activarlo por defecto; es una decisión de pruebas, no de esta historia.
