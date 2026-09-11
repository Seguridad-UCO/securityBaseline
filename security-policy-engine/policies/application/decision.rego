package security.authorization.application

import data.security.authorization.application.guards
import rego.v1

deny_from(candidate) := {
	"effect": "DENY",
	"reasonCode": object.get(candidate, "reasonCode", "EXPLICIT_DENY"),
	"policyId": object.get(candidate, "policyId", "application.policy-principal"),
	"obligations": [],
}

allow_from(candidate) := {
	"effect": "ALLOW",
	"reasonCode": object.get(candidate, "reasonCode", "POLICY_ALLOWED"),
	"policyId": candidate.policyId,
	"obligations": with_audit(candidate),
}

with_audit(candidate) := obligations if {
	guards.cross_tenant_guard(candidate)
	obligations := array.concat(candidate_obligations(candidate), [{"type": "AUDIT", "parameters": {}}])
}

with_audit(candidate) := candidate_obligations(candidate) if not guards.cross_tenant_guard(candidate)
