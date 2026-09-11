package security.authorization_test

import data.security.authorization.decision
import rego.v1

base_input := {
	"schemaVersion": "1.0",
	"request": {"id": "request-1"},
	"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a"},
	"tenant": {"id": "tenant-a"},
	"application": {"id": "real-app"},
	"resource": {"type": "record"},
	"action": "record:read",
}

test_invalid_input_fails_closed if {
	result := decision with input as object.remove(base_input, ["tenant"])
	result.effect == "DENY"
	result.reasonCode == "INVALID_INPUT"
}

test_unknown_application_action_fails_closed if {
	result := decision with input as base_input
	result.effect == "DENY"
	result.reasonCode == "NO_POLICY_MATCH"
}

test_cross_tenant_without_evidence_fails_closed if {
	cross_tenant := object.union(base_input, {"tenant": {"id": "tenant-b"}})
	result := decision with input as cross_tenant
	result.effect == "DENY"
	result.reasonCode == "TENANT_ISOLATION_FAILED"
}

test_absent_optional_attributes_are_not_evidence if {
	result := decision with input as base_input
	result.effect == "DENY"
}

test_explicit_deny_precedes_allow_candidate if {
	allow := {"effect": "ALLOW", "applicationId": "real-app", "policyId": "application.real-system", "tenantScope": "SAME_TENANT", "obligations": []}
	deny := {"effect": "DENY", "applicationId": "real-app", "policyId": "application.real-system", "reasonCode": "RESOURCE_LOCKED"}
	result := decision with input as base_input with data.security.authorization.application.allow_candidates as {allow} with data.security.authorization.application.deny_candidates as {deny}
	result.effect == "DENY"
	result.reasonCode == "EXPLICIT_DENY"
}

test_cross_tenant_allow_requires_evidence_and_adds_audit if {
	cross_tenant := object.union(base_input, {
		"tenant": {"id": "tenant-b"},
		"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a", "entitlements": ["tenant:cross-access"]},
	})
	allow := {"effect": "ALLOW", "applicationId": "real-app", "policyId": "application.real-system", "tenantScope": "CROSS_TENANT", "obligations": []}
	result := decision with input as cross_tenant with data.security.authorization.application.allow_candidates as {allow}
	result.effect == "ALLOW"
	{"type": "AUDIT", "parameters": {}} in result.obligations
}

test_candidate_cannot_authorize_a_different_application if {
	foreign_allow := {"effect": "ALLOW", "applicationId": "another-app", "policyId": "application.another-app", "tenantScope": "SAME_TENANT", "obligations": []}
	result := decision with input as base_input with data.security.authorization.application.allow_candidates as {foreign_allow}
	result.effect == "DENY"
	result.reasonCode == "NO_POLICY_MATCH"
}

test_multiple_allow_candidates_fail_closed_as_ambiguous if {
	first := {"effect": "ALLOW", "applicationId": "real-app", "policyId": "application.one", "tenantScope": "SAME_TENANT", "obligations": []}
	second := {"effect": "ALLOW", "applicationId": "real-app", "policyId": "application.two", "tenantScope": "SAME_TENANT", "obligations": []}
	result := decision with input as base_input with data.security.authorization.application.allow_candidates as {first, second}
	result.effect == "DENY"
	result.reasonCode == "POLICY_AMBIGUITY"
}
