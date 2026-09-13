# Reporte de validacion — HU-013

## Metadata

- **Slice:** `applications` (existente), con `[M]` en `shared/port` y `shared/config`
- **Fecha:** 2026-09-13
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-013.md`
- **Rama:** `feature/HU-013-validacion-credencial-aplicacion`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 75,9s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 597, Failures: 0, Errors: 0, Skipped: 0

Log completo: pdp/target/verificar-ultimo.log (736 lineas)
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | ✅ |
| Pruebas | ✅ 597 pruebas, 0 fallos |
| Cobertura (≥ 50 % por paquete) | ✅ (el build llegó a `jacoco-check` y no lo bloqueó) |
| `LayeredArchitectureTests` | ✅ (incluida en la corrida verde) |
| `ModulithStructureTests` | ✅ (incluida en la corrida verde — HU-013 no declara ninguna frontera nueva; el endpoint vive en `applications`, mismo módulo que ya lo posee todo lo que usa) |

Comprobaciones adicionales ejecutables:

```
consistencia.ps1 → CONSISTENTE: los 8 slices siguen la misma forma
drift.ps1        → 1 hallazgo: PepRegistrationProperties (MAPA-PLATAFORMA-SEGURIDAD.md) —
                    mismo hallazgo preexistente ya reportado en REPORTE-HU-011.md y
                    REPORTE-HU-012.md, sin relación con esta historia. Observación, no bloqueante.
```

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

Ninguna.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅ | Ver tabla de criterios más abajo — cada fila de la sección 2 del plan la ejercita una prueba concreta, incluida la prueba que demuestra explícitamente "no distingue causa" (`an_application_that_does_not_exist_is_rejected_without_ever_comparing_a_secret`, poison-pill en `CredentialHasher`) |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol) | ✅ | Identificadores en inglés (`ApplicationCredentialMustBeValidRule`, `ValidateApplicationCredentialUseCase`); mensaje único y genérico en español (`"La aplicación o la credencial no son válidas"`, sin parámetros — a propósito, para no filtrar la causa); nombres de prueba en inglés descriptivo |
| 3 | ¿Introdujo deriva doc↔codigo? | ✅ | `drift.ps1` muestra 1 hallazgo, preexistente y sin relación (ver Observaciones de HU-012, mismo caso) |
| 4 | ¿La logica quedo en la capa correcta? | ✅ | `grep -rl "org.springframework" .../applications/{domain,application}` → vacío (fuera de `package-info.java`). Cero `if/throw` de negocio en `ValidateApplicationCredentialUseCaseImpl.execute` — la única decisión ("¿es válida?") vive en `ApplicationCredentialMustBeValidRuleImpl`, pura y síncrona, invocada vía `doOnNext`. El use case solo resuelve el hash, delega la comparación a `CredentialHasher.matches` (puerto transversal) y colapsa "no existe"/"no coincide" al mismo booleano antes de pasarlo a la regla — así la regla nunca necesita saber la causa. Controller delega al interactor; interactor mapea-delega-mapea sin decidir nada. Cero `block()` (grep vacío) |

## Criterios de la línea base

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ 🤖 | `LayeredArchitectureTests`/`ModulithStructureTests` verdes; cero Spring en `domain`/`application` |
| 2 | Contratos de servicios | ✅ | `ValidateApplicationCredentialUseCase`/`ValidateApplicationCredentialInteractor` son interfaces vacías sobre `ReactiveOperation`; `ApplicationCredentialMustBeValidRule` sobre `OperationWithoutResult` |
| 4 | Capacidades transversales | ✅ | `CredentialHasher.matches` inyectado por constructor; sin `BCryptPasswordEncoder` en línea en `application` — vive solo en `SharedPortsConfiguration` |
| 9 | Excepciones | ✅ | `InvalidApplicationCredentialException` extiende `BusinessRuleViolationException`; ningún `RestControllerAdvice` nuevo |
| 11 | Interacción entre capas | ✅ | `InternalApplicationCredentialController` delega al interactor, nunca al use case directamente |
| 12 | SOLID | ✅ | Constructor de `ValidateApplicationCredentialUseCaseImpl` valida sus 3 dependencias con `Objects.requireNonNull` |
| 13 | DTOs | ✅ | `ValidateApplicationCredentialRawRequest` (Strings) → mapper → `ValidateApplicationCredentialRequest` (VOs); `TenantId` → mapper → `ApplicationCredentialValidationWebResponse` (plano) |
| 14 | DTOs seguros | ✅ | `ValidateApplicationCredentialRequest` y `ApplicationCredentialValidity` tienen `Objects.requireNonNull` completo — verificado abriendo ambos archivos, no solo corriendo pruebas |
| 15 | Validación de dominio | ✅ | La regla nunca acepta un estado sin decidir: `valid=true` no lanza, `valid=false` siempre lanza la misma excepción |
| 21 | Modelo refinado | ✅ | Sin cambios al agregado `Application`; el `record` de hecho resuelto (`ApplicationCredentialValidity`) sigue el mismo patrón que el resto del proyecto |
| 22 | Arquitectura reactiva | ✅ | Cadena `Mono` completa (`findCredentialHashById` → `map` → `defaultIfEmpty` → `doOnNext` → `then(Mono.defer(...))`); sin `block()` (grep confirmado) |

## Desviaciones respecto al plan

| Archivo | Plan decia | Codigo hace | ¿Justificado? |
|---|---|---|---|
| Sección 9 del plan (`InternalApplicationCredentialHttpTests`) | Un E2E con mTLS+JWKS reales, mirroring `InternalSecurityChainIntegrationTests` | Se sustituyó por `ValidateApplicationCredentialInteractorImplTests` (unit-level) | Sí — el tester verificó que esa cadena mTLS+JWKS existe **una sola vez** en todo el repo y prueba genéricamente la seguridad de *cualquier* ruta bajo `/internal/v1/**`; ningún otro endpoint interno (incluido el de HU-003) repite esa infraestructura por su cuenta. El comportamiento de negocio específico del endpoint ya queda cubierto por `ValidateApplicationCredentialInteractorImplTests` + `InternalApplicationCredentialControllerTests`, mismo patrón que sigue `InternalAccessDecisionInteractorImplTests` para el endpoint hermano de HU-003. Verificado: la decisión fue del tester y quedó documentada en su cierre, no una improvisación del implementador |

Ninguna otra desviación: el árbol de la sección 8 coincide con lo existente, y ningún archivo quedó
lanzando `UnsupportedOperationException`.

## Datos para la entrega

- **Mensaje de commit:** `feat(applications): agrega HU-013 — el PDP valida la credencial de una aplicación`
- **Cuerpo:** Nuevo endpoint interno `POST /internal/v1/applications/{applicationId}/credential-validations`
  (protegido por la misma cadena mTLS + evidencia JWT de `/internal/v1/**`, sin tocar
  `InternalSecurityConfiguration`): recibe el secreto en texto plano y responde con el `tenantId`
  dueño si coincide con el hash guardado (HU-012). Si la aplicación no existe o el secreto no
  coincide, responde exactamente el mismo error en ambos casos, sin distinguir la causa.
  `CredentialHasher` gana `matches(...)`. Sin regla de negocio nueva más allá de la validez de la
  credencial misma. 597 pruebas, cobertura y arquitectura en verde.
- **Rama:** `feature/HU-013-validacion-credencial-aplicacion`
- **Archivos a incluir:** todo lo nuevo/modificado bajo `pdp/src/main` y `pdp/src/test` para esta
  historia (slice `applications`, más `shared/port`/`shared/config`), el archivo de la historia
  `pdp/docs/ai-harness/workspace/HU-013.md`, y `pdp/docs/ai-harness/PROJECT-MAP.md` (regenerado).
  El plan y este reporte se versionan en `pdp/docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
