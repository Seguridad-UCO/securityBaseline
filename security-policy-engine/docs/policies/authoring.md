# Autoría de políticas

Una PolicyDefinition combina únicamente los hechos que necesita: roles, profiles,
entitlements, relaciones, atributos y contexto de seguridad. Estados de recurso, ownership,
MFA, riesgo, red y relaciones no son invariantes globales.

Publique un objeto que cumpla `contracts/policy-definition.schema.json` dentro del bundle.
Use `target.applicationIds` si aplica a una aplicación, nunca código Rego por aplicación.
No modifique el entrypoint para una aplicación nueva. Durante esta primera fase
`obligations` debe ser siempre `[]`.

Los `reasonCode` y los `effect` que puede emitir el motor son los del vocabulario unico de
`contracts/reason-codes.md`; no invente codigos nuevos sin acordarlos alli primero.
# Gobierno de definiciones

`Role.resources`, `Profile.roles`, las asignaciones de rol y de perfil son hechos del PDP. No se crea una `PolicyDefinition` para duplicarlos. `core.resource-grant` permite el recurso solamente cuando el PDP proyecta su entitlement efectivo.

Las definiciones adicionales existen para reglas que esas relaciones no representan (MFA, perfil requerido, estado LOCKED, riesgo, horario, ownership o separación de funciones). Una restricción debe ser `DENY`: `DENY_OVERRIDES` prevalece sobre el ALLOW base. Un ALLOW fuera de los grants es una excepción revisable.
