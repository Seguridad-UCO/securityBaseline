package security.authorization

import data.security.authorization.application.composition
import data.security.authorization.application.guard
import data.security.authorization.application.validation
import rego.v1

# Los reasonCode y los effect salen del vocabulario unico acordado entre PDP, PEP y OPA:
# contracts/reason-codes.md. INDETERMINATE marca lo que es un defecto -- de la entrada o del
# conjunto de politicas -- y no una denegacion de negocio: el consumidor falla cerrado igual, pero
# el motivo es honesto y se puede alertar sobre el.

decision := {
	"effect": "INDETERMINATE",
	"reasonCode": "INVALID_INPUT",
	"policyReferences": [{"id": "core.input-validation", "version": "1.0"}],
	"obligations": [],
} if {
	not validation.valid
}

decision := {
	"effect": "DENY",
	"reasonCode": "POLICY_DENY",
	"policyReferences": composition.references(composition.matched_denies),
	"obligations": [],
} if {
	validation.valid
	count(composition.matched_denies) > 0
}

decision := {
	"effect": "DENY",
	"reasonCode": "TENANT_ISOLATION_FAILED",
	"policyReferences": [{"id": "core.tenant-guard", "version": "1.0"}],
	"obligations": [],
} if {
	validation.valid
	count(composition.matched_denies) == 0
	count(composition.matched_allows) > 0
	not guard.tenant_allowed
}

decision := {
	"effect": "ALLOW",
	"reasonCode": "POLICY_ALLOWED",
	"policyReferences": composition.references(composition.matched_allows),
	"obligations": [],
} if {
	validation.valid
	count(composition.matched_denies) == 0
	count(composition.matched_allows) > 0
	guard.tenant_allowed
}

decision := {
	"effect": "DENY",
	"reasonCode": "NO_APPLICABLE_POLICY",
	"policyReferences": [{"id": "core.composition", "version": "1.0"}],
	"obligations": [],
} if {
	validation.valid
	count(composition.matched_denies) == 0
	count(composition.matched_allows) == 0
}
