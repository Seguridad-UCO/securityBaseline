package security.authorization

import data.security.authorization.application
import data.security.authorization.application.guards
import data.security.authorization.application.validation
import rego.v1

# Los reasonCode y los effect salen del vocabulario unico acordado entre PDP, PEP y OPA:
# contracts/reason-codes.md. INDETERMINATE marca lo que es un defecto -- de la entrada o del
# conjunto de politicas -- y no una denegacion de negocio: el consumidor falla cerrado igual, pero
# el motivo es honesto y se puede alertar sobre el.

decision := {
	"effect": "INDETERMINATE",
	"reasonCode": "INVALID_INPUT",
	"policyReferences": application.core_references("core.input-validation"),
	"obligations": [],
} if {
	not validation.valid
}

decision := {
	"effect": "DENY",
	"reasonCode": "POLICY_DENY",
	"policyReferences": application.core_references("core.composition"),
	"obligations": [],
} if {
	validation.valid
	count(application.explicit_denies) > 0
}

decision := {
	"effect": "INDETERMINATE",
	"reasonCode": "POLICY_OUTPUT_INVALID",
	"policyReferences": application.candidate_references(candidate),
	"obligations": [],
} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) > 0
	candidate := application.invalid_allow_candidates[_]
}

decision := {
	"effect": "INDETERMINATE",
	"reasonCode": "POLICY_AMBIGUITY",
	"policyReferences": application.core_references("core.composition"),
	"obligations": [],
} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	count(application.valid_allow_candidates) > 1
}

decision := {
	"effect": "DENY",
	"reasonCode": "TENANT_ISOLATION_FAILED",
	"policyReferences": application.core_references("core.tenant-guard"),
	"obligations": [],
} if {
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

decision := {
	"effect": "DENY",
	"reasonCode": "NO_APPLICABLE_POLICY",
	"policyReferences": application.core_references("core.composition"),
	"obligations": [],
} if {
	validation.valid
	count(application.explicit_denies) == 0
	count(application.invalid_allow_candidates) == 0
	guards.same_tenant_guard
	count(application.valid_allow_candidates) == 0
}
