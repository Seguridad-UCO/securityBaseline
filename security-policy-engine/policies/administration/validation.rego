package security.administration.validation

import rego.v1

# Entrada mínima de la decisión de administración (HU-009 del PDP): sin schemaVersion,
# request, resource ni action — a propósito, no está atada al contrato pdp-opa/v1 (ver
# ../../docs/policies/administration.md).
valid if {
	nonempty_string(input.subject.id)
	nonempty_string(input.subject.type)
	nonempty_string(input.subject.tenantId)
	nonempty_string(input.tenant.id)
	nonempty_string(input.application.id)
}

nonempty_string(value) if {
	is_string(value)
	count(value) > 0
}
