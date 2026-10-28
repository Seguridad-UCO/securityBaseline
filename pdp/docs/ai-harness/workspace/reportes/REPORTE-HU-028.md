# Reporte de validación — HU-028

## Metadata

- **Slice:** `shared` (`shared/web/session`, `shared/security/revocation`)
- **Fecha:** 2026-10-21
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-028.md`
- **Rama:** `feature/HU-028-revocacion-al-logout`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`.

```
ESTADO: VERDE  (mvnw test, 7,2s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS:
- KeycloakLogoutControllerTests: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
- SessionRevocationControllerTests: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 6 pruebas en verde (0 fallos, 0 errores) |
| `consistencia.ps1` | ✅ Sin inconsistencias en `shared` |
| `drift.ps1` | ✅ 0 derivas nuevas introducidas |

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

1. `KeycloakLogoutController.logout` utiliza `Mono.defer(session::invalidate)` para garantizar la evaluación diferida reactiva; si la revocación en Redis falla, la invalidación de la sesión WebFlux no se ejecuta (comportamiento fail-closed).

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan? | ✅ | `KeycloakLogoutControllerTests` verifica invocación a `TokenRevocationPort.revokeAllSince` y comportamiento fail-closed; `SessionRevocationControllerTests` verifica endpoint `DELETE /api/v1/session` para identidades `PdpPrincipal` y `LocalUserPrincipal` con respuesta 204. |
| 2 | ¿Convención de idioma? | ✅ | Código y pruebas en inglés; Javadoc y comentarios en español. |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` reporta 0 hallazgos nuevos. |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | Invocación de `TokenRevocationPort` desde adaptadores web en `shared/web/session`, sin lógica de persistencia en controladores ni bloqueos reactivos. |

## Criterios de la línea base

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ | Controladores web delegan en `TokenRevocationPort` y `TimeProvider` |
| 2 | Contratos de servicios | ✅ | Reutiliza puerto `TokenRevocationPort` de HU-022 |
| 9 | Mapeo de errores | ✅ | Error de Redis propaga excepción al handler centralizado |
| 21 | Modelos explícitos | ✅ | `UserId` inmutable; `PdpPrincipal` y `LocalUserPrincipal` |
| 22 | Arquitectura reactiva | ✅ | Todo el flujo reactivo con `Mono`, sin `block()` |
| 23 | Arquitectura antes de negocio | ✅ | Pruebas compiladas y en verde |

## Desviaciones respecto al plan

Ninguna.

## Datos para la entrega

- **Mensaje de commit:** `feat(shared): cablear revocación de tokens al logout del BFF y exponer DELETE /api/v1/session (HU-028)`
- **Cuerpo:** Inyecta `TokenRevocationPort` en `KeycloakLogoutController` para revocar tokens antes de invalidar la sesión WebFlux y redirigir al RP-initiated logout de Keycloak; añade `SessionRevocationController` para clientes bearer autenticados.
- **Rama:** `feature/HU-028-revocacion-al-logout`
- **Archivos a incluir:**
  - `pdp/src/main/java/co/edu/uco/seguridad/shared/web/session/KeycloakLogoutController.java`
  - `pdp/src/main/java/co/edu/uco/seguridad/shared/web/session/SessionRevocationController.java`
  - `pdp/src/test/java/co/edu/uco/seguridad/shared/web/session/KeycloakLogoutControllerTests.java`
  - `pdp/src/test/java/co/edu/uco/seguridad/shared/web/session/SessionRevocationControllerTests.java`

## Próximos pasos

Listo para el gate 2 (entrega).
