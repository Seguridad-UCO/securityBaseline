# Reporte de validacion — HU-014

## Metadata

- **Slice:** `applications`
- **Fecha:** 2026-09-13
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-014.md`
- **Rama:** `feature/HU-014-rotacion-credencial-aplicacion`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 82,4s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 606, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion                                 | Resultado                                                                   |
|----------------------------------------------|-----------------------------------------------------------------------------|
| Compilacion                                  | ✅                                                                           |
| Pruebas                                      | ✅ 606 pruebas                                                               |
| Cobertura (≥ 50 % por paquete, jacoco-check) | ✅ (`jacoco:check` corrió dentro de `verify` y el build fue `BUILD SUCCESS`) |
| `LayeredArchitectureTests`                   | ✅ (3/3)                                                                     |
| `ModulithStructureTests`                     | ✅ (1/1)                                                                     |

`consistencia.ps1` → `CONSISTENTE: todos los slices siguen la misma forma` (8 slices verificados).

`drift.ps1` → 1 hallazgo preexistente y ajeno a esta historia (ver Observaciones menores).

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 3 — drift] `PepRegistrationProperties` citada y no encontrada por `drift.ps1`

- **Archivo:** `pdp/docs/ai-harness/workspace/MAPA-PLATAFORMA-SEGURIDAD.md:179`
- **Problema:** `drift.ps1` reporta que `PepRegistrationProperties` no existe. La clase sí existe
  (`pep/starter/src/main/java/co/edu/uco/seguridad/pep/starter/PepRegistrationProperties.java`) —
  parece que el detector no cubre el módulo `pep/`. Preexistente: ningún archivo de esta historia
  toca `pep/` ni `MAPA-PLATAFORMA-SEGURIDAD.md` (confirmado contra `git status`), y HU-014 es
  enteramente del slice `applications` del PDP.
- **Referencia:** Regla invariante 5 del validador — "la deriva preexistente es observación; la
  nueva es bloqueante".
- **Correccion esperada:** Ninguna a cargo de esta historia. Si se quiere cerrar, es una tarea
  aparte sobre `drift.ps1` o sobre esa documentación del PEP.

### [Juicio 4 — capa correcta] Javadoc desactualizado en `RotateApplicationCredentialUseCaseImpl`

- **Archivo:**
  `pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RotateApplicationCredentialUseCaseImpl.java:19-25`
- **Problema:** El Javadoc de la clase sigue redactado como "Pendiente: resolver la aplicación con
  `findByIdForTenant`…" — texto que el planificador dejó en el esqueleto y el implementador no
  actualizó tras completar `execute`. No es un defecto de arquitectura ni de comportamiento, solo un
  comentario que ya no describe un pendiente sino lo que el método hace.
- **Referencia:** `sb-estandares` — "Javadoc… explican el porqué", no debe quedar como nota de
  trabajo pendiente una vez resuelta.
- **Correccion esperada:** Reescribir el Javadoc en indicativo (lo que el método hace), no en
  "pendiente".

## Los cuatro juicios

> Lo que ninguna prueba puede verificar. Cada uno se responde con evidencia, no con una impresión.

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|---|-----------------------------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptación del plan (no solo compila)? | ✅         | Ver tabla siguiente                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 2 | ¿Convención de idioma? (código en inglés, mensajes en español)  | ✅         | Identificadores nuevos en inglés (`RotateApplicationCredentialUseCase`, `findByIdForTenant`, `updateCredentialHash`…); mensajes nuevos en español: `WebContractMessages.successApplicationCredentialRotated()` → "La credencial de la aplicación se rotó correctamente"; `RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE`/`_INTERACTOR` en español                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 3 | ¿Introdujo deriva doc↔código?                                   | ✅         | `drift.ps1` solo reporta el hallazgo preexistente de `pep/`, ajeno a esta historia (ver Observaciones menores). Ningún archivo de `pdp/docs/` referencia por ruta algo que este cambio movió o renombró                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 4 | ¿La lógica quedó en la capa correcta?                           | ✅         | `RotateApplicationCredentialUseCaseImpl.execute` no tiene `if/throw` de negocio: la decisión vive en `ApplicationMustExistForTenantRule` (reutilizada, ya existente), invocada vía `doOnNext`/`switchIfEmpty` — mismo patrón que `AddRoleToProfileRulesValidatorImpl`. `ApplicationController.rotate` solo delega al interactor y envuelve la respuesta — no construye DTOs de aplicación ni decide nada. `RotateApplicationCredentialRequestMapper` delega el formato a `ApplicationId::of` vía `RequestFieldParser`, no valida a mano. `SurrealApplicationRepository.findByIdForTenant`/`updateCredentialHash` son consultas puras, sin decisión de negocio. Cero anotaciones de Spring en `domain`/`application` (grep sin resultados salvo los `package-info.java` de Modulith preexistentes, no tocados por esta historia). Ningún `allowedDependencies` fue modificado. Sin `.block()` en el código de producción de `applications` |

## Criterios de la linea base

> Solo los que el plan declaró (metadata): 1, 2, 4, 9, 11, 12, 13, 15, 21, 22.

| #  | Criterio                  | Resultado | Punto de control comprobado                                                                                                                                                                                                               |
|----|---------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture        | 🤖 ✅      | `LayeredArchitectureTests` (3/3) + `ModulithStructureTests` (1/1) en verde; sin anotaciones de Spring en `domain`/`application` del cambio                                                                                                |
| 2  | Contratos de servicios    | ✅         | `RotateApplicationCredentialUseCase`/`RotateApplicationCredentialInteractor` son interfaces vacías que extienden `ReactiveOperation`; puerto de salida explícito en `ApplicationRepository` (`findByIdForTenant`, `updateCredentialHash`) |
| 4  | Capacidades transversales | ✅         | Sin `Instant.now()`/`UUID.randomUUID()` en línea: la historia no genera identificador ni marca de tiempo nueva (identidad y `registeredAt` se conservan); `SecretGenerator`/`CredentialHasher` inyectados como en HU-012                  |
| 9  | Excepciones               | ✅         | Reutiliza `ApplicationNotFoundException` (ya bajo `DomainException`); sin `RuntimeException` cruda; traducción HTTP solo en `ApiErrorHandler` (no tocado, no hizo falta)                                                                  |
| 11 | Interacción entre capas   | ✅         | `ApplicationController.rotate` → `RotateApplicationCredentialInteractorImpl` → `RotateApplicationCredentialUseCaseImpl` → `ApplicationMustExistForTenantRule`/`ApplicationRepository`. El controller no importa nada de `application`     |
| 12 | SOLID                     | ✅         | Contratos mínimos (una operación), dependencias inyectadas por constructor contra interfaces (`ApplicationRepository`, `ApplicationMustExistForTenantRule`, `SecretGenerator`, `CredentialHasher`)                                        |
| 13 | DTOs                      | ✅         | `RotateApplicationCredentialRawRequest` (String desnudo) → `RotateApplicationCredentialRequestMapper` → `RotateApplicationCredentialRequest` (value objects)                                                                              |
| 15 | Validación de dominio     | ✅         | `RotateApplicationCredentialRequest` con `Objects.requireNonNull` por componente (lo añadió el implementador, el planificador lo dejó vacío); `ApplicationCredentialHash` (VO de HU-012) reutilizado sin cambios                          |
| 21 | Modelo refinado           | ✅         | `Application.withCredentialHash(...)` es una transición inmutable (`record` nuevo, mismos campos salvo el hash) — mismo patrón que `Profile.withRole`                                                                                     |
| 22 | Arquitectura reactiva     | ✅         | Cadena `Mono` completa (`findByIdForTenant` → regla → `flatMap` → `updateCredentialHash`); sin `.block()` en el código de producción del slice                                                                                            |

## Desviaciones respecto al plan

Ninguna. El árbol de archivos de la sección 8 coincide exactamente con lo creado/modificado
(confirmado contra `git status`); ninguna firma de la SPEC (sección 7) cambió; `ApplicationsConfiguration`
y `WebContractMessages` fueron cableados por el propio planificador, como el plan anticipaba.

## Datos para la entrega

- **Mensaje de commit:** `feat(applications): rotación de credencial de aplicación (HU-014)`
- **Cuerpo:** Nuevo endpoint `POST /api/v1/applications/{applicationId}/credential-rotations` (201):
  genera un secreto nuevo, lo hashea y sobrescribe el hash guardado — invalidación inmediata, sin
  período de gracia. La identidad de la aplicación no cambia. Capas tocadas: `domain`
  (`Application.withCredentialHash`), `application` (`RotateApplicationCredentialUseCase`/`Impl`,
  puerto `ApplicationRepository` +2 métodos), `infrastructure` (controller, interactor, mapper,
  adaptador SurrealDB, cableado en `ApplicationsConfiguration`, mensaje en `WebContractMessages`).
  Reutiliza `ApplicationMustExistForTenantRule` y la forma de respuesta de HU-012
  (`ApplicationRegistrationResponse`/`ApplicationRegisteredWebResponse`) sin crear DTOs nuevos.
- **Rama:** `feature/HU-014-rotacion-credencial-aplicacion`
- **Archivos a incluir:** todos los `.java` de `pdp/src/main` y `pdp/src/test` listados como
  modificados/nuevos en `git status` para esta historia (ver Metadata del PLAN-HU-014, sección 8,
  para la lista completa). El plan y este reporte se versionan aparte, en
  `pdp/docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
