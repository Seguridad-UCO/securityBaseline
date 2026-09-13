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
