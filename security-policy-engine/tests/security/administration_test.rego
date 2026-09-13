package security.administration_test

import data.security.administration.decision
import rego.v1

base_input := {
	"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a", "roles": []},
	"tenant": {"id": "tenant-a"},
	"application": {"id": "app-1"},
}

test_invalid_input_is_indeterminate_not_a_denial if {
	result := decision with input as object.remove(base_input, ["tenant"])
	result.effect == "INDETERMINATE"
	result.reasonCode == "INVALID_INPUT"
}

test_a_subject_without_the_administrator_role_is_denied if {
	result := decision with input as base_input
	result.effect == "DENY"
	result.reasonCode == "POLICY_DENY"
}

test_a_subject_with_the_administrator_role_is_allowed if {
	admin_input := object.union(base_input, {"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a", "roles": ["ADMIN"]}})
	result := decision with input as admin_input
	result.effect == "ALLOW"
	result.reasonCode == "POLICY_ALLOWED"
}

test_a_similarly_named_role_does_not_grant_administration if {
	almost_input := object.union(base_input, {"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a", "roles": ["Administrador de aplicación"]}})
	result := decision with input as almost_input
	result.effect == "DENY"
}

test_a_missing_roles_field_is_not_evidence_of_administration if {
	no_roles_input := object.union(base_input, {"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a"}})
	result := decision with input as no_roles_input
	result.effect == "DENY"
}

test_every_decision_carries_a_versioned_policy_reference if {
	result := decision with input as base_input
	count(result.policyReferences) == 1
	result.policyReferences[0].version == "1.0"
}
