# Reporte de validación — HU-029

> **Estado supersedido al 2026-10-28:** las pruebas del controlador/adaptador no validan por sí solas
> el flujo de una aplicación externa. El diseño actualmente probado almacena el refresh token del
> cliente BFF; la decisión más reciente es que cada aplicación renueve con su propio cliente
> directamente contra Keycloak. El contrato de HU-029 debe reconciliarse antes de aprobarse. Usar el
> [handoff vigente](../HANDOFF-VALIDACION-HU-027-029.md); no tomar “APROBADO” ni los conteos de este
> reporte como resultado actual.

## Metadata

- **Slice:** `shared` (`shared/web/session`, `shared/auth`)
- **Fecha:** 2026-10-21
- **Plan validado:** `pdp/docs/ai-harness/workspace/planes/PLAN-HU-029.md`
- **Rama:** `feature/HU-029-refresh-token-externo`

## Resultado del build

> Ejecutado con `.claude/tools/verificar.ps1`.

```
ESTADO: VERDE  (mvnw test, 6,7s, exit 0)
JDK: Java 25 en C:\Users\Sebastian\.jdks\temurin-25.0.4

PRUEBAS:
- TokenRefreshControllerTests: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

| Comprobación | Resultado |
|---|---|
| Compilación | ✅ |
| Pruebas | ✅ 3 pruebas en verde (0 fallos, 0 errores) |
| `consistencia.ps1` | ✅ Sin inconsistencias en `shared` |
| `drift.ps1` | ✅ 0 derivas nuevas introducidas |

## Veredicto anterior (supersedido)

El reporte original marcó la historia como aprobada con pruebas del controlador. Ese veredicto queda
retirado como estado actual hasta reconciliar el contrato externo y validar el flujo acordado.

## Bloqueantes

Para la aprobación vigente: reconciliar el contrato HU-029 con la decisión de renovación directa
por cada aplicación en Keycloak y validar el flujo resultante. Ver el handoff.

## Observaciones menores

1. `TokenRefreshController.refresh` revoca el token anterior (`tokenRevocationPort.revokeAllSince`) únicamente tras la emisión exitosa del nuevo token por parte del `TokenRefreshPort`, preservando el principio de fail-closed seguro si Keycloak falla.

## Los cuatro juicios

| # | Juicio | Resultado | Evidencia |
|---|---|---|---|
| 1 | ¿Cumple los criterios de aceptación del plan? | ✅ | `TokenRefreshControllerTests` valida respuesta 200 con `{ accessToken, expiresIn, tokenType }`, revocación inmediata del token anterior, propagación de 503 sin revocación cuando falla el IdP, y respuesta 401 `SESSION_EXPIRED` cuando la sesión expira. |
| 2 | ¿Convención de idioma? | ✅ | Identificadores en inglés; mensajes de error y documentación en español. |
| 3 | ¿Introdujo deriva doc↔código? | ✅ | `drift.ps1` reporta 0 hallazgos nuevos. |
| 4 | ¿La lógica quedó en la capa correcta? | ✅ | `TokenRefreshController` en capa web expone endpoint HTTP delegando en `TokenRefreshPort` y `TokenRevocationPort`. `TokenRefreshResponse` es record inmutable con validación compacta. |

## Criterios de la línea base

| # | Criterio | Resultado | Punto de control comprobado |
|---|---|---|---|
| 1 | Clean Architecture | ✅ | Desacoplamiento de detalles IdP a través del puerto `TokenRefreshPort` |
| 2 | Contratos de servicios | ✅ | DTO `TokenRefreshResponse` y puerto reactivo `TokenRefreshPort` |
| 9 | Mapeo de errores | ✅ | `ResponseStatusException` 401 y 503 según estado del IdP |
| 14 | Validación de records | ✅ | `TokenRefreshResponse` valida no-nulidad de `accessToken` y `tokenType` |
| 21 | Modelos inmutables | ✅ | `TokenRefreshResponse` es record inmutable |
| 22 | Arquitectura reactiva | ✅ | Encadenamiento puramente reactivo con `Mono`, sin `block()` |
| 23 | Arquitectura y pruebas | ✅ | Pruebas compiladas y en verde |

## Desviaciones respecto al plan

Ninguna.

## Datos para la entrega

- **Mensaje de commit:** `feat(shared): endpoint de refresco de token para aplicaciones externas (HU-029)`
- **Cuerpo:** Expone `POST /api/v1/session/token/refresh` para clientes que renuevan sesión con su token actual; renueva mediante `TokenRefreshPort` e invalida el token previo mediante `TokenRevocationPort`.
- **Rama:** `feature/HU-029-refresh-token-externo`
- **Archivos a incluir:**
  - `pdp/src/main/java/co/edu/uco/seguridad/shared/auth/port/TokenRefreshPort.java`
  - `pdp/src/main/java/co/edu/uco/seguridad/shared/web/session/TokenRefreshController.java`
  - `pdp/src/main/java/co/edu/uco/seguridad/shared/web/session/dto/TokenRefreshResponse.java`
  - `pdp/src/test/java/co/edu/uco/seguridad/shared/web/session/TokenRefreshControllerTests.java`

## Próximos pasos

Listo para el gate 2 (entrega).
