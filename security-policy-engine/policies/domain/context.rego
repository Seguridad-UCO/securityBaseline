package security.authorization.domain.context

import rego.v1

attribute(name) := value if {
	value := object.get(object.get(input, "context", {}), "attributes", {})[name]
}

authentication_level := value if {
	value := object.get(object.get(input, "security", {}), "authenticationLevel", "")
}
