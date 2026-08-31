---
name: sb-criterios
description: Los 23 criterios de la línea base como contrato de aceptación de securityBaseline — qué exige cada uno y qué mirar en el código para darlo por cumplido. Cargar al planificar (para declarar qué criterios toca la historia) y al validar (para comprobarlos).
---

# Skill: sb-criterios

Los 23 criterios son el **contrato de aceptación** del proyecto. Ninguna historia puede bajarlos: si
una implementación cumple su criterio funcional pero rompe uno de estos, está rechazada.

- El planificador **declara** en el PLAN qué criterios toca la historia.
- El validador **comprueba** esos criterios sobre el código, no sobre la documentación.

## Estado real: 18 de 23 (verificado 2026-08-31)

Cinco criterios **no están cumplidos**, aunque la documentación los daba por buenos hasta que se
auditó. Están declarados así en `docs/criteria-compliance-matrix.md`.

| # | Estado | Por qué |
|---|---|---|
| 10 | ⛔ No cumple | `RemoveApplicationUseCase` existe como operación compensatoria y **ningún caso de uso la invoca**. No hay saga cableada |
| 16 | ⛔ No cumple | Ningún puerto expone `findBy(criteria, window)`; siguen siendo métodos concretos |
| 17 | ⛔ No cumple | No existe la specification de filtros (`ProtectedApplicationCriteria` nunca se creó) |
| 18 | ⚠️ Parcial | `PageWindow`/`ResultPage`/`PageResponse` existen y están probados, pero **nadie los usa** |
| 19 | ⚠️ Parcial | La lógica de rangos existe y está probada, pero ningún endpoint la expone |

Cerrarlos —y reponer la prueba HTTP end-to-end— es el alcance de la historia **HU-001**.

**No planifiques ni valides como si estuvieran hechos.** Si una historia toca paginación o consulta
por criterios, está construyendo el criterio, no reutilizándolo.

## La evidencia es el archivo, no la tabla

La deriva de la documentación ya está corregida y ahora es **verificable**:

```
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/tools/drift.ps1
```

Comprueba que todo enlace de `docs/` resuelva y que toda clase citada exista. Las excepciones
declaradas viven en `docs/ai-harness/drift-ignore.txt` y son solo para lo pendiente a propósito.

**Regla:** la evidencia de un criterio es el archivo que abres con `Read`, o la prueba que ejecutas.
Nunca la fila de una tabla.

---

## Tabla de control

Para cada criterio: qué exige, y el **punto de control** — lo concreto que hay que mirar para darlo
por cumplido. Los criterios marcados 🤖 los verifica una prueba automatizada: no los razones, córrelas.

| # | Criterio | Punto de control |
|---:|---|---|
| 1 | Clean Architecture | 🤖 `LayeredArchitectureTests` + `ModulithStructureTests`. Además: ninguna anotación de Spring en `domain`/`application` |
| 2 | Contratos de servicios | Todo `UseCase`/`Rule`/`Interactor` es una interfaz **vacía** que extiende un contrato de `shared/contract`. Puertos de salida explícitos en `application/port/secondary/` |
| 3 | Reglas e integridad | Una clase `Rule` por restricción, con interfaz e impl separadas. Regla con repositorio ⇒ reactiva; sin I/O ⇒ síncrona. **Cero `if/throw` de negocio en el use case** |
| 4 | Capacidades transversales | Sin `Instant.now()` ni `UUID.randomUUID()` en línea: `TimeProvider` e `IdentifierGenerator` de `shared/port`. Correlación por `CorrelationWebFilter` |
| 5 | Manejo de mensajes | Éxito envuelto en `ApiResponse.success(code, message, data, context)` con código estable. Error como `ProblemDetail` desde `ApiErrorHandler` |
| 6 | Manejo de parámetros | Todo parámetro entra como `String` y se convierte en el mapper con `RequestFieldParser`. Nunca un tipo rico en la firma del controller |
| 7 | Adaptadores de persistencia | El adaptador implementa el puerto y no decide nada de negocio. Tabla en `{X}Schema`, valores como parámetros |
| 8 | Logging e instrumentación | Contexto de log aplicado con `.transform(...)` y `ReactiveLogContext`. Sin `Observation` manual. Sin datos sensibles |
| 9 | Excepciones | Una excepción por condición, bajo la jerarquía `DomainException`. Traducción HTTP **solo** en `ApiErrorHandler`. Nunca `RuntimeException` cruda |
| 10 | Transacciones | ⛔ **pendiente.** Saga con compensación **explícita por paso**. No hay puerto de transacción genérico: si un paso falla, el anterior se compensa a mano y se documenta |
| 11 | Interacción entre capas | Controller → interactor → use case → rules → dominio/puertos. Un controller que importe `application` es un defecto |
| 12 | SOLID | Contratos mínimos (una operación), reglas sustituibles por bean, dependencias inyectadas por constructor contra interfaces |
| 13 | DTOs | Dos niveles: raw de `String` → mapper → DTO tipado con value objects |
| 14 | DTOs seguros | Sin Jakarta Validation. Tres barreras: campo presente, VO válido, `requireNonNull` en el record. La respuesta sale plana |
| 15 | Validación de dominio | Invariantes en el constructor compacto del VO/entidad. Un VO nunca existe inválido |
| 16 | Repositorios dinámicos | ⛔ **pendiente.** Consulta por criterio y ventana, no un método por combinación de filtros |
| 17 | Consultas dinámicas | ⛔ **pendiente.** Filtros opcionales normalizados a `Optional` y aplicados en tiempo de ejecución |
| 18 | Paginación | ⚠️ **parcial.** `PageWindow` de `pdp/commons` (máximo 100) existe; falta que un endpoint lo use |
| 19 | Rangos | ⚠️ **parcial.** `offset`/`limit` convergentes en `PageWindow`; falta exponerlos |
| 20 | Adaptadores limpios | Controller, mappers y adaptadores sin reglas de negocio. El mapper delega el formato al VO |
| 21 | Modelo refinado | `record` inmutables, Java puro, **sin Lombok**, con factorías con nombre y comportamiento. Nada de entidades anémicas |
| 22 | Arquitectura reactiva | `Mono`/`Flux` en toda la cadena. **Sin `block()` en el camino de una petición** (solo en `ApplicationRunner` de arranque) |
| 23 | Arquitectura antes del negocio | 🤖 `./mvnw verify` en verde, cobertura ≥ 50 % por paquete, pipeline y secretos fuera del repositorio |

---

## Cómo se usa

**Al planificar.** Declara en el PLAN los criterios que la historia toca. Como mínimo siempre toca
1, 2, 9, 11, 12, 21 y 22 (son estructurales). Añade 3 si hay reglas; 5, 6, 13, 14 y 20 si hay
endpoint; 15 si hay value objects nuevos; 16–19 si hay consulta paginada; 7 si hay persistencia;
10 si hay más de una escritura que deba compensarse; 4 y 8 si hay tiempo, identificadores o logging.

**Al validar.** Por cada criterio declarado, abre el archivo y comprueba el punto de control.
Los marcados 🤖 se resuelven ejecutando `./mvnw verify`, no leyendo código.

**Al detectar deriva.** Si la evidencia documentada de un criterio apunta a algo que no existe, es
una observación del reporte — y el arreglo entra en el mismo cambio, no en un pendiente.

---

## Regla invariante

Un criterio no se da por cumplido porque una tabla de `docs/` lo diga. Se da por cumplido porque
abriste el archivo, o porque una prueba lo verifica.
