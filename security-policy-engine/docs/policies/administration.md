# Política de administración (HU-009 del PDP)

Segundo entrypoint del motor, independiente de `security.authorization` y de su contrato
versionado `pdp-opa/v1`. Responde una sola pregunta: **¿este sujeto administra esta
aplicación?** — no "¿puede acceder a este recurso?".

```text
POST /v1/data/security/administration/decision
```

## Entrada

Sin `schemaVersion`, `request`, `resource` ni `action` — no hay ruta ni verbo HTTP que
evaluar, y forzar esa forma habría sido la misma "regla escondida" que HU-004 ya prohibió
para el catálogo de roles.

```json
{
  "subject": {"id": "...", "type": "USER", "tenantId": "...", "roles": ["..."]},
  "tenant": {"id": "..."},
  "application": {"id": "..."}
}
```

`subject.roles` es evidencia ya resuelta por el PDP: los nombres de los roles activos del
sujeto para esa aplicación (mismo mecanismo que `subject.roles` en `security.authorization`,
vía `ActiveRoleNamesLookupValidator`).

## Decisión

Una sola regla, sin composición de candidatos ni guardas de tenant — a diferencia de
`security.authorization`, aquí no hay múltiples aplicaciones compitiendo por la misma
decisión:

1. Entrada inválida (falta algún campo obligatorio) → `INDETERMINATE / INVALID_INPUT`.
2. `subject.roles` contiene el rol reservado `ADMIN` → `ALLOW / POLICY_ALLOWED`.
3. En cualquier otro caso → `DENY / POLICY_DENY`.

`effect` y `reasonCode` salen del mismo vocabulario único que `security.authorization`
(`contracts/reason-codes.md`) — un consumidor que ya sepa interpretar una decisión de
acceso no aprende nada nuevo aquí.

## La convención `ADMIN` — qué cubre y qué no

`policies/administration/role.rego` fija el nombre de rol reservado. Es una decisión de
esta política, no del PDP: el catálogo (`roles` + `assignments`, HU-004/HU-005) solo aporta
evidencia, nunca decide en Java (`if (rol == ADMIN)` está prohibido por diseño, ver
`sb-estandares` del repo `securityBaseline`).

**Consecuencia que hay que conocer antes de cablear cualquier endpoint contra esta
política:** el PDP no reserva todavía el nombre `ADMIN` en su catálogo de roles — cualquier
tenant puede hoy crear un rol llamado `ADMIN` sin intención administrativa, y cualquier
administrador de tenant puede asignárselo a quien quiera (HU-005, sin gate propio). Mientras
eso no se resuelva (reservar el nombre como se reservan nombres de aplicación en
`ApplicationCatalogProperties`, o un mecanismo equivalente), esta política es el único
control real. Es una superficie a endurecer, no un defecto de esta política — documentado
también en `PLAN-HU-009.md` (`securityBaseline`), ambigüedad 2.

## Qué no cubre

- **Administrador global de plataforma.** El modelo de asignaciones del PDP
  (`Assignment.applicationId` obligatorio) no puede representar hoy una asignación sin
  aplicación — no hay "rol global asignado a alguien" que evaluar. Cuando esa historia
  exista, esta política gana una segunda rama (`input.application` ausente → evalúa el rol
  global) sin tocar la evidencia por aplicación que ya tiene.
- **Delegación con origen.** Un `ALLOW` de hoy no distingue "me lo asignaron directo" de
  "heredé el permiso de otro administrador" — no existe esa relación en el catálogo todavía.
- **Auditoría.** Esta decisión no se audita (mismo criterio que la validación de
  credenciales de aplicación, HU-013 del PDP): es una decisión de administración interna,
  no de acceso de usuario final a un recurso.

## Pruebas

`tests/unit/administration_role_test.rego` cubre la regla pura (`role.is_administrator`).
`tests/security/administration_test.rego` cubre el entrypoint completo: entrada inválida,
rol presente/ausente, un rol de nombre parecido que no debe colar, y que toda decisión trae
una referencia de política versionada. `make test` los corre junto con el resto.
