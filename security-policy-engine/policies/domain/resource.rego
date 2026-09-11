package security.authorization.domain.resource

import rego.v1

attribute(name) := value if {
	value := object.get(object.get(input.resource, "attributes", {}), name, null)
	value != null
}
