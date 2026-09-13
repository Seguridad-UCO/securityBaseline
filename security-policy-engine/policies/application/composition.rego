package security.authorization.application.composition

import data.security.authorization.application.conditions
import data.security.authorization.application.selector
import rego.v1

matched contains policy if {
	policy := selector.applicable[_]
	conditions.matches(object.get(policy, "condition", {"all": [{"predicate": {"left": {"value": true}, "operator": "EQ", "right": {"value": true}}}]}))
}

matched_denies contains policy if {
	policy := matched[_]
	policy.effect == "DENY"
}

matched_allows contains policy if {
	policy := matched[_]
	policy.effect == "ALLOW"
}

references(policies) := sort([{"id": policy.id, "version": policy.version} | policy := policies[_]])
