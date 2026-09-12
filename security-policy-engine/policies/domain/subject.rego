package security.authorization.domain.subject

import rego.v1

has_role(role) if {
	role in object.get(input.subject, "roles", [])
}

has_profile(profile) if {
	profile in object.get(input.subject, "profiles", [])
}

has_entitlement(entitlement) if {
	entitlement in object.get(input.subject, "entitlements", [])
}

attribute(name) := value if {
	value := object.get(object.get(input.subject, "attributes", {}), name, null)
	value != null
}
