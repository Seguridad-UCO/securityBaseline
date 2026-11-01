# REPORTE — HU-036 (actor de las escrituras de catálogo de alcance TENANT) — 2026-10-30

**Veredicto: APROBADO.**

## Cómo se validó

Flujo completo del harness: `@1-planificador` → compuerta 1 (el responsable aceptó P1–P8, incluida la ampliación a todas las vías de edición) →
`@2-tester-spec` (≈ 216 casos unitarios, reescritura de los que afirmaban «TENANT sin gate → éxito») → `@3-implementador` → validación del
responsable: `mvnw clean verify` con sockets reales (**1125 pruebas, 0 fallos, 0 omitidas**, JaCoCo), `consistencia.ps1` CONSISTENTE,
`drift.ps1` SIN DERIVA, y lectura del diff centrada en el gate. Decisión de fondo: [ADR-036](../../../../../../security-platform-architecture/docs/01-governance/adr/ADR-036-tenant-catalog-write-actor.md).

## Criterios

| # | Criterio | Resultado |
|---|---|---|
| C1–C3 | Sin administrar ninguna aplicación del tenant: 403 `NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG` al crear/editar/borrar roles y perfiles TENANT, conceder recursos y componer perfiles; la escritura no ocurre | ✅ |
| C4 | Carga masiva: error por fila `NOT_AUTHORIZED` (`Alcance*`), `ready=false`, nada se guarda; una consulta del gate por llamada, antes de duplicados | ✅ |
| C5–C6 | Administrador con MFA procede; sin evidencia de MFA ⇒ `MFA_REQUIRED` (individual) / `NOT_AUTHORIZED` por fila (masiva) | ✅ |
| C7–C9 | Fail-closed ante error, DENY, INDETERMINATE, sin aplicaciones o fallo al resolver (también si el use case lanza síncrono); orden estable, secuencial, corte al primer permiso; solo aplicaciones del tenant del principal | ✅ |
| C11 | Auditoría en la rama TENANT con la aplicación que concedió (ALLOWED) o la primera evaluada (DENIED) | ✅ |
| C12–C13 | Roles/perfiles TENANT existentes siguen resolviéndose; sin regresión en alcance APPLICATION | ✅ |
| C14–C15 | Arquitectura, consistencia, drift y `verify` completo | ✅ |

## Revisión de seguridad del diff (responsable)

- `PrincipalMustAdministerAtLeastOneApplicationValidatorImpl`: estado por ejecución dentro de `Mono.defer` (el bean es singleton), `concatMap`
  con corte, `Exception` ⇒ no autoriza, `Error` de la JVM se propaga. No decide quién administra: compone el gate existente (con MFA).
- El tipo sellado `CatalogAdministration` elimina el estado «sin gate»: un interactor que lo olvide no compila.
- **Fallo abierto preexistente cerrado:** en `CatalogMaintenanceController`, si el `UserId` no se resolvía, el gate se omitía y el
  `PATCH`/`DELETE` se ejecutaba igual (aplicación, recurso, rol, perfil). Ahora responde 403.

## Cambios de comportamiento (deliberados)

Quien hoy crea o edita roles/perfiles TENANT sin administrar ninguna aplicación recibe 403; un inquilino sin aplicaciones no puede definir
catálogo TENANT hasta registrar una. No se revisaron pipelines externos.

## Límites conocidos

- El actor es amplio: administrar **cualquier** aplicación habilita catálogo de todo el tenant (lo estrecha ADR-024). Endurecimiento posible:
  exigir administrar la aplicación del recurso al conceder.
- Un principal sin aplicaciones no deja evento `DENIED` (`AdministrationEvent.applicationId` es obligatorio).
- Coste: un sujeto sin permiso paga hasta N consultas de decisión; las concesiones en lote repiten el gate por recurso.
- Sin prueba HTTP dedicada del cableado del bean para `POST /api/v1/roles` TENANT sin aplicaciones (propuesta del tester, no añadida).
- Front: `security-ui` ya distingue el código (HU-037/commit de mensajes por código); `securityBaseline-vue` muestra el `detail`.
