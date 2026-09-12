# Application policies

`example_app.rego` in this directory is a **demonstration module only** — no real
application named `example-app` is registered in the PDP catalog. It exists to show the
pattern end-to-end (see `tests/unit/example_app_test.rego` and
`tests/regression/example_app_isolation_test.rego`), not to be extended into a real
policy. Add a module for a real application only when it supplies its own facts and
authorization semantics — it can live alongside `example_app.rego`, keyed by its own
`applicationId`, or replace it. The module emits a candidate into package
`security.authorization.application`:

```rego
package security.authorization.application
import rego.v1

allow_candidates contains {
  "effect": "ALLOW",
  "applicationId": "real-system",
  "policyId": "application.real-system",
  "policyVersion": "1.0",
  "tenantScope": "SAME_TENANT",
  "obligations": []
} if {
  # combine roles, profiles, entitlements, relationships and relevant context here
}
```

`policyVersion` is required on an allow candidate: an auditable decision has to say which
version decided (see `contracts/README.md`, D-U4). An allow candidate without it is reported as
`INDETERMINATE / POLICY_OUTPUT_INVALID`, not silently dropped.

`applicationId` must exactly match `input.application.id`; a candidate for one consumer
cannot influence another consumer. `tenantScope: "CROSS_TENANT"` additionally requires PDP-resolved cross-access evidence.
The core adds `AUDIT` to such an allow. Policies may emit explicit deny candidates but
must never return a final decision or bypass the core package.
