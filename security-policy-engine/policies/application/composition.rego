package security.authorization.application

import rego.v1

# Partial sets are deliberate extension points. A real application adds a module in
# policies/applications/ using this package and emits a complete candidate. It cannot
# bypass validation, deny precedence, tenant guards, or obligation validation below.
allow_candidates contains candidate if {
	candidate := {"effect": "ALLOW", "applicationId": "__placeholder__", "policyId": "__placeholder__", "tenantScope": "SAME_TENANT", "obligations": []}
	false
}

deny_candidates contains candidate if {
	candidate := {"effect": "DENY", "applicationId": "__placeholder__", "policyId": "__placeholder__"}
	false
}

explicit_denies contains candidate if {
	some candidate in deny_candidates
	candidate.effect == "DENY"
	candidate.applicationId == input.application.id
	is_string(candidate.policyId)
	count(candidate.policyId) > 0
}

valid_allow_candidates contains candidate if {
	some candidate in allow_candidates
	candidate.effect == "ALLOW"
	candidate.applicationId == input.application.id
	is_string(candidate.policyId)
	count(candidate.policyId) > 0
	candidate.tenantScope == "SAME_TENANT"
	is_array(candidate_obligations(candidate))
}

valid_allow_candidates contains candidate if {
	some candidate in allow_candidates
	candidate.effect == "ALLOW"
	candidate.applicationId == input.application.id
	is_string(candidate.policyId)
	count(candidate.policyId) > 0
	candidate.tenantScope == "CROSS_TENANT"
	is_array(candidate_obligations(candidate))
}

candidate_obligations(candidate) := object.get(candidate, "obligations", [])

known_obligation(obligation) if obligation.type == "AUDIT"
known_obligation(obligation) if obligation.type == "MASK_FIELDS"
known_obligation(obligation) if obligation.type == "REQUIRE_MFA"
known_obligation(obligation) if obligation.type == "READ_ONLY"
known_obligation(obligation) if obligation.type == "LOG_SECURITY_EVENT"

invalid_allow_candidates contains candidate if {
	some candidate in valid_allow_candidates
	some obligation in candidate_obligations(candidate)
	not known_obligation(obligation)
}
