package security.administration

import data.security.administration.role
import data.security.administration.validation
import rego.v1

# Segundo entrypoint público del motor (HU-009 del PDP): "¿este sujeto administra esta
# aplicación?", separado a propósito de security.authorization — no es una decisión de
# acceso a un recurso externo (resource/action), es sobre el propio catálogo del PDP. Los
# reasonCode y effect salen del mismo vocabulario único que security.authorization
# (contracts/reason-codes.md): un consumidor que ya sepa interpretar una decisión de
# security.authorization no aprende nada nuevo aquí.
#
# Sin composición de candidatos ni guardas de tenant: a diferencia de una política de
# aplicación externa, aquí no hay múltiples módulos que puedan competir por la misma
# decisión — una sola regla (role.is_administrator) sobre evidencia que el PDP ya resolvió
# y filtró por tenant/aplicación antes de preguntar.

decision := {
	"effect": "INDETERMINATE",
	"reasonCode": "INVALID_INPUT",
	"policyReferences": [{"id": "administration.core.input-validation", "version": "1.0"}],
	"obligations": [],
} if {
	not validation.valid
}

decision := {
	"effect": "ALLOW",
	"reasonCode": "POLICY_ALLOWED",
	"policyReferences": [{"id": "administration.core.role-check", "version": "1.0"}],
	"obligations": [],
} if {
	validation.valid
	role.is_administrator
}

decision := {
	"effect": "DENY",
	"reasonCode": "POLICY_DENY",
	"policyReferences": [{"id": "administration.core.role-check", "version": "1.0"}],
	"obligations": [],
} if {
	validation.valid
	not role.is_administrator
}
