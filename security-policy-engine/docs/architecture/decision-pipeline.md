# Pipeline de decisión

```text
PolicyEvaluationInput
  ├─ inválido ─────────────────────────────→ INDETERMINATE / INVALID_INPUT
  ├─ policy DENY que coincide ─────────────→ DENY / POLICY_DENY
  ├─ tenant distinto sin evidencia confiable→ DENY / TENANT_ISOLATION_FAILED
  ├─ una o más policy ALLOW que coinciden ─→ ALLOW / POLICY_ALLOWED
  └─ sin coincidencia ─────────────────────→ DENY / NO_APPLICABLE_POLICY
```

`DENY_OVERRIDES` vence a cualquier ALLOW. El guard de tenant exige igualdad por defecto.
Una policy `CROSS_TENANT` además exige evidencia resuelta por el PDP. Las obligaciones se
mantienen vacías hasta que PEP y PDP las soporten end-to-end.

Las PolicyDefinitions son datos del bundle bajo `data/policies.json`. El selector aplica
targeting genérico y el evaluador interpreta condiciones AST; ningún módulo Rego depende del
nombre de una aplicación. El entrypoint único `data.security.authorization.decision` aplica
las invariantes globales.
