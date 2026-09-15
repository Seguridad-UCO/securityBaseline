# Reporte de validacion — HU-021

## Metadata

- **Slice:** `shared`, `authorization`, `assignments`
- **Fecha:** 2026-09-15
- **Plan validado:** `docs/ai-harness/workspace/planes/PLAN-HU-021.md`
- **Rama:** `feature/HU-021-auditoria-operaciones-administrativas`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`. Pegar el resumen tal cual, no el log.

```
ESTADO: VERDE  (mvnw clean verify, 231,3s, exit 0)
JDK: Java 25 en C:\Users\sebas\.jdks\corretto-25.0.4.1

PRUEBAS: Tests run: 706, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobacion | Resultado |
|---|---|
| Compilacion | ✅ |
| Pruebas | ✅ 706 pruebas |
| Cobertura (≥ 50 % por paquete) | ✅ |
| `LayeredArchitectureTests` | ✅ |
| `ModulithStructureTests` | ✅ |

El build en verde **no es evidencia suficiente aquí**: los tres bloqueantes de abajo son código que
compila y pasa sus pruebas actuales precisamente porque nunca se ejecuta con éxito — el propio
`onErrorResume` que hace que "un fallo de auditoría no bloquee el resultado" (criterio 3) también
esconde que la auditoría real nunca ocurre.

## Estado final

> ⛔ RECHAZADO — hay 3 bloqueante(s).

**Un solo bloqueante = RECHAZADO**, aunque todo lo demas este bien.

## Bloqueantes

### [FASE 2 — archivo que existe pero sigue lanzando `UnsupportedOperationException`] — Persistencia real de la auditoría sin implementar

- **Archivo:** `pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/infrastructure/adapter/secondary/persistence/repository/SurrealAdministrationAuditRepository.java`
- **Problema:** `save(...)` y `findByCorrelationId(...)` siguen siendo `throw new UnsupportedOperationException("pendiente: HU-021")`. Ningún `AdministrationEvent` llega nunca a SurrealDB — el `onErrorResume(error -> Mono.empty())` que los 13 casos de uso retrofit ahora invocan atrapa exactamente esta excepción en cada llamada real.
- **Referencia:** `4-validador.md` FASE 2 — "Archivo que existe pero sigue lanzando `UnsupportedOperationException`: Bloqueante — es un esqueleto sin implementar".
- **Corrección esperada:** implementar el SurrealQL literal contra `AdministrationEventSchema.TABLE` (mismo patrón que `SurrealAccessAuditRepository`), lo que a su vez exige materializar `AdministrationEventEntity` y `AdministrationEventPersistenceMapper` — declarados en PLAN-HU-021.md §8 y todavía inexistentes.

### [FASE 2 — archivo que existe pero sigue lanzando `UnsupportedOperationException`] — Observabilidad de la auditoría sin implementar (criterio de aceptación #4 sin cumplir)

- **Archivo:** `pdp/src/main/java/co/edu/uco/seguridad/pdp/authorization/infrastructure/adapter/secondary/observability/ObservedAdministrationAuditRepository.java`
- **Problema:** `save(...)`/`findByCorrelationId(...)` también lanzan `UnsupportedOperationException`. No incrementa `security.administration.events`, no emite el log estructurado. PLAN-HU-021.md §2, criterio 4 ("El evento queda expuesto a Prometheus y a los logs estructurados") declara como evidencia `ObservedAdministrationAuditRepositoryImplTests`, que no existe.
- **Referencia:** Juicio 1 (¿cumple los criterios de aceptación?) — un criterio sin prueba que lo cubra es observación, pero aquí el criterio está además contradicho por el código (el decorador no decora nada).
- **Corrección esperada:** implementar el `doOnSuccess`/contador Prometheus + log estructurado (mismo patrón que `ObservedAccessAuditRepository`, HU-007) y su clase de prueba dedicada.

### [FASE 2 — archivo del plan que no existe] — Árbol de PLAN-HU-021.md §8 incompleto

- **Archivo:** `pdp/authorization/infrastructure/adapter/secondary/persistence/entity/AdministrationEventEntity.java`, `.../mapper/AdministrationEventPersistenceMapper.java`, `.../schema/SurrealAdministrationEventSchemaInitializer.java`
- **Problema:** los tres están declarados `[N]` en PLAN-HU-021.md §8 y ninguno existe en el árbol real.
- **Referencia:** `4-validador.md` FASE 2 — "Archivo del plan que no existe: Bloqueante".
- **Corrección esperada:** materializarlos (mismo patrón que sus pares de `AccessEvent`) como parte de la misma implementación que resuelve los dos bloqueantes anteriores — son la base que `SurrealAdministrationAuditRepository` necesita para dejar de lanzar.

## Observaciones menores

### [Plan] — PLAN-HU-021.md §8 y la tabla de retrofit quedaron desactualizados a mitad de la implementación

- **Archivo:** `docs/ai-harness/workspace/planes/PLAN-HU-021.md`
- **Observación:** la nota agregada en la segunda pasada de `2-tester-spec` (retirar `RegisterApplicationWithFirstAdministratorUseCaseImpl`/`AssignApplicationAdministratorUseCaseImpl` de la tabla de retrofit, de 15 a 13 filas) está bien documentada y es correcta — ambas clases carecían de `subject` para construir el evento, y auditar en el wrapper `Administer*` que ya las envuelve evita el evento duplicado. Pero el §8 (árbol de archivos) nunca se actualizó a juego: sigue diciendo "15 archivos, tabla de arriba" y sigue listando `AssignmentsConfiguration.java — 2 use cases ganan AdministrationAuditRepository`, que ya no aplica (0 casos de uso de `assignments` lo ganan).
- **Justificado:** la decisión en sí sí — quedó razonada y registrada. El desajuste entre §7/nota de retrofit y §8 es simplemente una actualización pendiente, no una decisión distinta.

## Los cuatro juicios

> Lo que ninguna prueba puede verificar. Cada uno se responde con evidencia, no con una impresion.

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptacion del plan (no solo compila)? | ⛔ | Criterios 1–3 (auditar ALLOWED/DENIED, no bloquear en fallo) sí — ver trazabilidad abajo. Criterio 4 (Prometheus + logs) no — ver bloqueantes |
| 2 | ¿Convencion de idioma? (codigo en ingles, mensajes en espanol) | ✅ | `AdministrationEvent`, `AdministrationOperation`, `recordAudit` en inglés; Javadoc de las 13 clases retrofit y `RequiredArgumentMessages`/`AssignmentsMessages` en español |
| 3 | ¿Introdujo deriva doc↔codigo? | ✅ | `drift.ps1` → `SIN DERIVA: todos los enlaces resuelven y toda clase citada existe` (el árbol §8 incompleto es una desviación de plan, no una afirmación falsa en `pdp/docs/`, que es lo que `drift.ps1` vigila) |
| 4 | ¿La logica quedo en la capa correcta? | ✅ | Auditar no decide nada de negocio (§3 del plan, correcto); el `Mono.defer(() -> audit.save(event))` en las 13 clases evita el defecto de evaluación anticipada; cero anotaciones Spring en `domain`/`application`; ningún `if` de negocio nuevo |

### Trazabilidad de criterios (PLAN-HU-021.md §2)

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| 1 | Operación exitosa queda persistida con `outcome=ALLOWED` | ⚠️ Parcial | Las 13 pruebas `*_and_audits_allowed` confirman que `audit.save(...)` se invoca con `ALLOWED` — pero nada llega a SurrealDB (bloqueante 1) |
| 2 | Operación rechazada queda persistida con `outcome=DENIED` | ⚠️ Parcial | Ídem, con `*_and_audits_denied` — mismo bloqueante |
| 3 | Fallo al guardar no impide devolver el resultado ya resuelto | ✅ | `does_not_block_the_result_when_the_audit_repository_fails` en las 13 clases, con `TestAdministrationAuditRepositories.failing()` |
| 4 | El evento queda expuesto a Prometheus y a los logs estructurados | ⛔ | No implementado — bloqueante 2 |
| 5 | Ningún endpoint HTTP nuevo | ✅ | Sin controller nuevo en este plan |
| 6 | Suite completa | ✅ | `verificar.ps1` en verde (706/706) |

## Criterios de la linea base

Ninguno declarado por el plan (HU-021 no toca la matriz `pdp/docs/criteria-compliance-matrix.md`).

## Desviaciones respecto al plan

| Archivo | Plan decia | Codigo hace | ¿Justificado? |
|---|---|---|---|
| Tabla de retrofit (§7) | 15 casos de uso ganan `AdministrationAuditRepository` | 13 — se retiraron `RegisterApplicationWithFirstAdministratorUseCaseImpl` y `AssignApplicationAdministratorUseCaseImpl` | Sí, y está documentado en el propio plan (nota agregada en la segunda pasada de `2-tester-spec`) |
| Los 13 casos de uso retrofit (constructor) | Solo `AdministrationAuditRepository audit` (§7 original) | También ganan `IdentifierGenerator identifiers` y `TimeProvider time` | Sí — necesarios para `eventId`/`occurredOn` sin violar la convención invariante de no llamar `UUID.randomUUID()`/`Instant.now()` en `application`; documentado en el plan |
| `AdministrationEvent.correlationId` | Sin fuente definida | `correlationId = eventId.toString()` (autocorrelación) | Parcialmente — es una decisión razonable ante la ausencia de un `correlationId` real en `AdministrationRequest`, pero **no está en el plan**, solo en los mensajes de cierre de las fases anteriores. Debe incorporarse a PLAN-HU-021.md antes del siguiente intento |
| §8 (árbol de archivos) | 15 archivos + `AdministrationEventEntity`/`Mapper`/`SchemaInitializer` | Ver bloqueantes 1 y 3 | No — es la deuda pendiente que rechaza esta validación |

## Datos para la entrega

No aplica — HU-021 no está lista para el gate 2.

## Proximos pasos

`@3-implementador` corrige los bloqueantes y se repite la validación. En orden:

1. Materializar `AdministrationEventEntity`, `AdministrationEventPersistenceMapper` y
   `AdministrationEventSchema` (este último ya existe) — mismo patrón que sus pares de `AccessEvent`.
2. Implementar `SurrealAdministrationAuditRepository.save()`/`findByCorrelationId()` con SurrealQL
   literal contra `AdministrationEventSchema.TABLE`.
3. Materializar `SurrealAdministrationEventSchemaInitializer` y registrar su bean en
   `AuthorizationConfiguration` (mismo patrón que `accessEventSchemaInitializer`).
4. Implementar `ObservedAdministrationAuditRepository` (contador Prometheus + log estructurado,
   mismo patrón que `ObservedAccessAuditRepository`) — esto requiere que `2-tester-spec` escriba
   primero `ObservedAdministrationAuditRepositoryImplTests`, ya que el implementador no toca
   `pdp/src/test`.
5. Actualizar PLAN-HU-021.md §8 para que refleje la tabla de retrofit de 13 filas y documentar ahí
   la decisión de `correlationId = eventId.toString()`.
