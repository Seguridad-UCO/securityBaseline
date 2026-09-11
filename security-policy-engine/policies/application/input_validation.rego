package security.authorization.application.validation

import rego.v1

valid if {
	input.schemaVersion == "1.0"
	nonempty_string(input.request.id)
	nonempty_string(input.subject.id)
	nonempty_string(input.subject.type)
	nonempty_string(input.subject.tenantId)
	nonempty_string(input.tenant.id)
	nonempty_string(input.application.id)
	nonempty_string(input.resource.type)
	nonempty_string(input.action)
}

nonempty_string(value) if {
	is_string(value)
	count(value) > 0
}
