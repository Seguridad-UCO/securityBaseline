# Pipeline de decisión

```text
PolicyEvaluationInput
  ├─ inválido ─────────────────────────────→ DENY / INVALID_INPUT
  ├─ explicit deny de aplicación ──────────→ DENY
  ├─ tenant distinto sin candidato + prueba → DENY / TENANT_ISOLATION_FAILED
  ├─ candidato ALLOW válido ───────────────→ ALLOW
  └─ sin coincidencia ─────────────────────→ DENY / NO_POLICY_MATCH
```

El `explicit deny` vence a cualquier allow. El guard de tenant exige igualdad por
defecto. Una aplicación puede solicitar `tenantScope: CROSS_TENANT`, pero el core exige
evidencia resuelta por el PDP (`tenant:cross-access` o el atributo booleano
`crossTenantAccess`) y añade `AUDIT` a todo allow cross-tenant.

El directorio `policies/domain` contiene capacidades de evidencia reutilizables, no
autorización completa. Las políticas reales emiten candidatos y el entrypoint único
`data.security.authorization.decision` aplica las invariantes globales.

