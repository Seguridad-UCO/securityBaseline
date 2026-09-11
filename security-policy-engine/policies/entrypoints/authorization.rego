package security.authorization

import data.security.authorization.application
import data.security.authorization.application.guards
import data.security.authorization.application.validation
import rego.v1

decision := {"effect": "DENY", "reasonCode": "INVALID_INPUT", "policyId": "core.input-validation", "obligations": []} if {
	not validation.valid
}

decision := {"effect": "DENY", "reasonCode": "EXPLICIT_DENY", "policyId": "core.composition", "obligations": []} if {
	validation.valid
	count(application.explicit_denies) > 0
}

decision := {"effect": "DENY", "reasonCode": "POLICY_OUTPUT_INVALID", "policyId": candidate.policyId, "obligations": []} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) > 0
	candidate := application.invalid_allow_candidates[_]
}

decision := {"effect": "DENY", "reasonCode": "POLICY_AMBIGUITY", "policyId": "core.composition", "obligations": []} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	count(application.valid_allow_candidates) > 1
}

decision := {"effect": "DENY", "reasonCode": "TENANT_ISOLATION_FAILED", "policyId": "core.tenant-guard", "obligations": []} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	count(application.valid_allow_candidates) <= 1
	not guards.same_tenant_guard
	not cross_tenant_candidate_with_evidence
}

cross_tenant_candidate_with_evidence if {
	candidate := application.valid_allow_candidates[_]
	not candidate in application.invalid_allow_candidates
	guards.cross_tenant_guard(candidate)
}

decision := application.allow_from(candidate) if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	count(application.valid_allow_candidates) == 1
	candidate := application.valid_allow_candidates[_]
	not candidate in application.invalid_allow_candidates
	guards.tenant_guard(candidate)
}

decision := {"effect": "DENY", "reasonCode": "NO_POLICY_MATCH", "policyId": "core.composition", "obligations": []} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	guards.same_tenant_guard
	count(application.valid_allow_candidates) == 0
}
