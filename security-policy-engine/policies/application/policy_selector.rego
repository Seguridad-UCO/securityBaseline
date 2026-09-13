package security.authorization.application.selector

import rego.v1

# Empty target dimensions impose no restriction. Definitions are bundle data, never request data.
applicable contains policy if {
	policy := data.policies[_]
	target := object.get(policy, "target", {})
	matches(target, "tenantIds", input.tenant.id)
	matches(target, "applicationIds", input.application.id)
	matches(target, "resourceTypes", input.resource.type)
	matches(target, "resourceIds", object.get(input.resource, "id", ""))
	matches(target, "actions", input.action)
	matches(target, "environments", object.get(object.get(input, "context", {}), "environment", ""))
}

matches(target, field, _) if count(object.get(target, field, [])) == 0
matches(target, field, value) if value in object.get(target, field, [])
