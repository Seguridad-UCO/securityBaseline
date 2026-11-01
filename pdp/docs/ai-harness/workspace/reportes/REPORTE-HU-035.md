# REPORTE — HU-035 (endurecer la carga masiva de roles y perfiles) — 2026-10-30

**Veredicto: APROBADO.**

## Cómo se validó

Flujo completo del harness sobre el código de `develop` (PR #81): `@1-planificador` → compuerta 1 (revisión del contrato por el responsable)
→ `@2-tester-spec` (≈120 casos, escritos contra las firmas del plan) → `@3-implementador` → `mvnw clean verify`, `consistencia.ps1`,
`drift.ps1`, y lectura del diff de producción centrada en el gate. Rama apilada sobre `feature/seguridad-waf-sesiones` (las pruebas que
activan los dos `@Disabled` viven allí).

## Criterios

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | Quien no administra la aplicación recibe `NOT_AUTHORIZED` por fila y no se crea nada; el gate va **antes** de los duplicados (no se puede sondear qué roles existen) | ✅ | `BulkCatalogImportServiceTests` (administrador pasa, no administrador rechazado, error del gate ⇒ `NOT_AUTHORIZED` fail-closed, una sola consulta por aplicación y por llamada, `existsByNameInScope` sin consultar si el gate deniega) |
| 1b | Aplicación de otro tenant ⇒ `APPLICATION_NOT_FOUND` con mensaje neutro y **sin invocar el gate** | ✅ | idem |
| 2 | `validRows` e `invalidRows` nunca negativos; relaciones bloquean `ready=false` pero no entran en los contadores | ✅ | prueba antes `@Disabled`, ahora activa |
| 3 | `mode` inválido ⇒ `400` en plantilla, validación, confirmación y reporte de errores | ✅ | prueba antes `@Disabled`, ahora activa; `BulkResourceControllerTests` (traducción a `MalformedRequestFieldException("mode")`) |
| 4 | Cabeceras exactas por hoja, tope de tamaño (5 MB) y de filas (1.000), archivo vacío/cifrado/no Excel con código y mensaje en español | ✅ | `BulkCatalogWorkbookServiceTests` (52 casos) |
| 5 | Menores: celda numérica con error, nombre inválido bajo su propio campo, archivo de 0 bytes en español, `errorReport` con archivo ilegible sin `500` | ✅ | idem |
| 6 | `verificar.ps1` completo, `consistencia`, `drift` | ✅ | **1019 pruebas, 0 fallos, 0 omitidas**, JaCoCo ≥ 50 % por paquete; CONSISTENTE; SIN DERIVA |

## Revisión de seguridad del diff (responsable)

- El gate usa el mismo `AdministrationRequest` y `PrincipalMustBeApplicationAdministratorValidator` que la carga de recursos.
- La aplicación se busca siempre **dentro del tenant del principal**: una aplicación ajena es indistinguible de una inexistente.
- Las hojas de relaciones solo operan sobre roles/perfiles **aceptados** en el mismo lote (ya gateados). Para perfil→rol, la vía individual
  tampoco exige compatibilidad de alcance (`AddRoleToProfileRulesValidatorImpl` solo exige que existan en el tenant), así que no se abre
  una puerta nueva.

## Cambios de contrato público (aceptados)

`INVALID_WORKBOOK` se reparte en códigos específicos (`EMPTY_FILE`, `FILE_TOO_LARGE`, `ENCRYPTED_FILE`, `MISSING_SHEET`, `INVALID_HEADERS`,
`TOO_MANY_ROWS`, `REQUIRED`, `UNEXPECTED_CELL_TYPE`, `UNSUPPORTED_CELL`); `INVALID_VALUE` pasa a `APPLICATION_NOT_FOUND` o `REQUIRED`;
`resources-roles` en validar/confirmar es `400` (sigue válido en la plantilla). Ningún frontend usa estas rutas y el endpoint acababa de
entrar a `develop`.

## Límites conocidos (historias)

- Roles y perfiles de alcance **TENANT** siguen sin gate, en paridad con la vía individual: **HU-036** (depende de ADR-024).
- `MFA_REQUIRED` se colapsa en `NOT_AUTHORIZED` por fila, igual que en recursos.
- Estas rutas no auditan y `withUser` responde `500` sin `UserId` en el principal (igual que recursos).
- Los mensajes son constantes privadas de cada clase y no un catálogo `{Slice}Messages`.
