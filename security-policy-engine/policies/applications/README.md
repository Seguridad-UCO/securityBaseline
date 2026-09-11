# Application policies

This directory intentionally has no example business policy. Add a module only when a
real application supplies its facts and authorization semantics. The module emits a
candidate into package `security.authorization.application`:

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
