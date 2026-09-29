# Reporte de validacion — HU-012

## Metadata

- **Slice:** `applications` (existente), con `[M]` en `shared/port` y `shared/config`
- **Fecha:** 2026-09-13
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-012.md`
- **Rama:** `feature/HU-012-credencial-de-aplicacion`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Resumen tal cual.

```
ESTADO: VERDE  (mvnw clean verify, 91,6s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS: Tests run: 586, Failures: 0, Errors: 0, Skipped: 0

Log completo: pdp/target/verificar-ultimo.log (725 lineas)
```

| Comprobacion                   | Resultado                                                                      |
|--------------------------------|--------------------------------------------------------------------------------|
| Compilacion                    | ✅                                                                              |
| Pruebas                        | ✅ 586 pruebas, 0 fallos                                                        |
| Cobertura (≥ 50 % por paquete) | ✅ (el build llegó a `jacoco-check` y no lo bloqueó)                            |
| `LayeredArchitectureTests`     | ✅ (incluida en la corrida verde)                                               |
| `ModulithStructureTests`       | ✅ (incluida en la corrida verde — HU-012 no toca ninguna frontera de Modulith) |

Comprobaciones adicionales ejecutables:

```
consistencia.ps1 → CONSISTENTE: los 8 slices siguen la misma forma
drift.ps1        → 1 hallazgo: PepRegistrationProperties (MAPA-PLATAFORMA-SEGURIDAD.md) —
                    mismo hallazgo preexistente ya reportado en REPORTE-HU-011.md, sin relación con
                    esta historia. Observación, no bloqueante.
```

## Estado final

> ✅ **APROBADO** — sin bloqueantes.

## Bloqueantes

Ninguno.

## Observaciones menores

### [Juicio 2 — idioma/higiene] Javadoc desactualizado en `RegisterApplicationUseCaseImpl`

- **Archivo:**
  `pdp/src/main/java/co/edu/uco/seguridad/pdp/applications/application/usecase/impl/RegisterApplicationUseCaseImpl.java:28-30`
- **Problema:** el Javadoc de la clase todavía dice *"execute pendiente de HU-012: generar el
  secreto..."*, redactado durante la fase de pruebas cuando el método era un esqueleto. El método ya
  está implementado; el comentario describe un estado que ya no existe.
- **Referencia:** no es una regla de una skill puntual, sino higiene general de documentación — un
  Javadoc que describe un "pendiente" ya resuelto confunde a quien lo lea después.
- **Correccion esperada:** actualizar el párrafo para describir lo que el método hace, no lo que
  hacía falta hacer. No bloquea porque no afecta comportamiento ni contrato.

## Los cuatro juicios

| # | Juicio                                                          | Resultado | Evidencia                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|---|-----------------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ✅         | Ver tabla de criterios de la línea base más abajo — cada fila de la sección 2 del plan la ejercita una prueba concreta                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol)  | ✅         | Identificadores en inglés (`SecretGenerator`, `CredentialHasher`, `ApplicationCredentialHash`); mensajes en español en `RequiredArgumentMessages`/`ValueObjectMessages` (`"se requiere el generador de secretos"`, `"El hash de la credencial de la aplicación es inválido: ..."`); nombres de prueba en inglés descriptivo (`never_persists_the_plaintext_secret_only_its_hash`). Única observación: Javadoc desactualizado (ver arriba)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 3 | ¿Introdujo deriva doc↔codigo?                                   | ✅         | `drift.ps1` muestra 1 hallazgo, preexistente y sin relación (ver Observaciones)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 4 | ¿La logica quedo en la capa correcta?                           | ✅         | `grep -rl "org.springframework" .../applications/{domain,application}` → vacío (fuera de `package-info.java`). Cero `if/throw` de negocio en `RegisterApplicationUseCaseImpl.execute` — solo orquesta `rules.execute`, `secretGenerator`, `hasher`, `repository.save`. `ApplicationCredentialHash` valida en su constructor compacto, pura y síncrona. `SecretGenerator`/`CredentialHasher` son capacidades transversales en `shared/port`, cableadas una sola vez en `SharedPortsConfiguration` — ningún caso de uso instancia `SecureRandom`/`BCryptPasswordEncoder` en línea. Cero `block()` en `applications` (grep vacío). El secreto en claro nunca se pasa a `LOG.*` (grep vacío) — solo viaja por el DTO de respuesta, que además está separado de `RegisteredApplicationResponse`/`ApplicationWebResponse` (usados por `ListApplicationsUseCase`), así que el listado nunca puede exponerlo (Hallazgo 2 del plan, verificado en código) |

## Criterios de la línea base

| #  | Criterio                  | Resultado | Punto de control comprobado                                                                                                                                                                                    |
|----|---------------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Clean Architecture        | ✅ 🤖      | `LayeredArchitectureTests`/`ModulithStructureTests` verdes; cero Spring en `domain`/`application` (grep vacío)                                                                                                 |
| 2  | Contratos de servicios    | ✅         | `RegisterApplicationUseCase` sigue siendo una interfaz vacía sobre `ReactiveOperation`; `SecretGenerator`/`CredentialHasher` son `@FunctionalInterface` sin métodos propios más allá de su única operación     |
| 4  | Capacidades transversales | ✅         | `SecretGenerator`/`CredentialHasher` inyectados por constructor, cableados en `SharedPortsConfiguration`; sin `SecureRandom`/`BCryptPasswordEncoder` en línea en `application`                                 |
| 8  | Logging e instrumentación | ✅         | `.transform(ReactiveLogContext.withContext(LOG, "application.register"))` preservado; el secreto en claro nunca llega a un `LOG.*` (grep confirmado)                                                           |
| 9  | Excepciones               | ✅         | `InvalidApplicationCredentialHashException` extiende `InvalidValueException`; ningún `RestControllerAdvice` nuevo                                                                                              |
| 11 | Interacción entre capas   | ✅         | `ApplicationController` sigue delegando al interactor, nunca al use case directamente (sin cambios en esa relación)                                                                                            |
| 12 | SOLID                     | ✅         | Constructor de `RegisterApplicationUseCaseImpl` valida las 6 dependencias con `Objects.requireNonNull`, cada una con su constante                                                                              |
| 13 | DTOs                      | ✅         | `ApplicationRegistrationResponse` (núcleo) → `ApplicationRegisteredWebResponse` (HTTP, plano) — dos niveles, igual que el resto del proyecto                                                                   |
| 14 | DTOs seguros              | ✅         | `ApplicationRegistrationResponse` y `ApplicationCredentialHash` tienen `Objects.requireNonNull`/validación completa en su constructor compacto — verificado abriendo ambos archivos, no solo corriendo pruebas |
| 15 | Validación de dominio     | ✅         | `ApplicationCredentialHash` nunca existe en estado inválido (null/blank rechazados en el constructor compacto)                                                                                                 |
| 21 | Modelo refinado           | ✅         | `Application` sigue siendo `record` inmutable con factoría con nombre (`register`); el nuevo componente no rompe ese patrón                                                                                    |
| 22 | Arquitectura reactiva     | ✅         | Cadena `Mono` completa (`rules.execute` → `Mono.fromSupplier` → `flatMap` → `save`); sin `block()` en el camino de la petición (grep confirmado)                                                               |

## Desviaciones respecto al plan

| Archivo                         | Plan decia                                                                                     | Codigo hace                                                                      | ¿Justificado?                                                                                                                                                                                                                                  |
|---------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RequiredArgumentMessages.java` | Sección 11 listaba solo `SECRET_GENERATOR`, `CREDENTIAL_HASHER`, `APPLICATION_CREDENTIAL_HASH` | Además se añadieron `REGISTERED_APPLICATION_RESPONSE` y `APPLICATION_CREDENTIAL` | Sí — necesarias para validar `ApplicationRegistrationResponse` (criterio 14); el implementador las agregó siguiendo la convención "si falta la constante, se añade", como reportó en su cierre. Omisión menor del plan, no un cambio de diseño |

Ninguna otra desviación: el árbol de la sección 8 coincide con lo existente, y ningún archivo quedó
lanzando `UnsupportedOperationException`.

## Datos para la entrega

- **Mensaje de commit:**
  `feat(applications): agrega HU-012 — el PDP emite la credencial de una aplicación al registrarla`
- **Cuerpo:** Al registrar una aplicación (`POST /api/v1/applications`), el PDP genera un secreto
  aleatorio de alta entropía (256 bits, base64url), lo hashea con BCrypt (mismo algoritmo que ya usa
  `pep/RouteRegistry`) y persiste solo el hash. La respuesta de ese único registro
  (`ApplicationRegisteredWebResponse`) incluye el secreto en texto plano una sola vez; ninguna
  consulta posterior (`GET /api/v1/applications`) vuelve a exponerlo. Nuevas capacidades
  transversales `SecretGenerator`/`CredentialHasher` en `shared/port`. Sin regla de negocio nueva —
  la unicidad/reserva de nombre y el estado del tenant siguen sin cambios. 586 pruebas, cobertura y
  arquitectura en verde.
- **Rama:** `feature/HU-012-credencial-de-aplicacion`
- **Archivos a incluir:** todo lo nuevo/modificado bajo `pdp/src/main` y `pdp/src/test` para esta
  historia (slice `applications`, más `shared/port`/`shared/config`/`shared/message`), el archivo de
  la historia `pdp/docs/ai-harness/workspace/HU-012.md`, y `pdp/docs/ai-harness/PROJECT-MAP.md`
  (regenerado). El plan y este reporte se versionan en `pdp/docs/ai-harness/workspace/`.

## Proximos pasos

Listo para el gate 2 (entrega).
