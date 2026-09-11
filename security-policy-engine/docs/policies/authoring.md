# Autoría de políticas

Una política de aplicación combina únicamente los hechos que necesita: roles, profiles,
entitlements, relaciones, atributos y contexto de seguridad. Estados de recurso,
ownership, MFA, riesgo, red y relaciones no son invariantes globales.

Agregue el módulo bajo `policies/applications/`, emita candidatos en el set parcial
`security.authorization.application.allow_candidates` o `deny_candidates`, y cubra el
módulo con pruebas. No modifique el entrypoint para una aplicación nueva. Cada candidato
de allow debe incluir `effect`, `policyId`, `policyVersion`, `tenantScope` y `obligations`.

Los `reasonCode` y los `effect` que puede emitir el motor son los del vocabulario unico de
`contracts/reason-codes.md`; no invente codigos nuevos sin acordarlos alli primero.

