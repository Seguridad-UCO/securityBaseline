package security.authorization_test

import data.security.authorization.decision
import rego.v1

base_input := {"schemaVersion":"1.0","request":{"id":"request-1"},"subject":{"id":"user-1","type":"USER","tenantId":"tenant-a","roles":["reader"]},"tenant":{"id":"tenant-a"},"application":{"id":"app-a"},"resource":{"type":"protected-http-resource","id":"resource-a","attributes":{"status":"OPEN"}},"action":"GET","context":{"environment":"prod"}}

allow := {"id":"generic.allow","version":"1.0","effect":"ALLOW","scope":{"level":"APPLICATION"},"target":{"applicationIds":["app-a"],"actions":["GET"]},"tenantScope":"SAME_TENANT","condition":{"predicate":{"left":{"ref":["subject","roles"]},"operator":"CONTAINS","right":{"value":"reader"}}},"obligations":[]}
deny := object.union(allow, {"id":"generic.deny","effect":"DENY"})

test_invalid_input_is_indeterminate if {
	result := decision with input as object.remove(base_input, ["tenant"])
	result == {"effect":"INDETERMINATE","reasonCode":"INVALID_INPUT","policyReferences":[{"id":"core.input-validation","version":"1.0"}],"obligations":[]}
}
test_no_policy_fails_closed if {
	result := decision with input as base_input with data.policies as []
	result.effect == "DENY"
	result.reasonCode == "NO_APPLICABLE_POLICY"
}
test_multiple_allows_compose if {
	second := object.union(allow, {"id":"generic.allow.second"})
	result := decision with input as base_input with data.policies as [allow, second]
	result.effect == "ALLOW"
	count(result.policyReferences) == 2
}
test_deny_overrides_allow if {
	result := decision with input as base_input with data.policies as [allow, deny]
	result.effect == "DENY"
	result.reasonCode == "POLICY_DENY"
}
test_missing_fact_is_not_positive_evidence if {
	needs_mfa := object.union(allow, {"condition":{"predicate":{"left":{"ref":["security","authenticationLevel"]},"operator":"EQ","right":{"value":"MFA"}}}})
	result := decision with input as base_input with data.policies as [needs_mfa]
	result.reasonCode == "NO_APPLICABLE_POLICY"
}
test_cross_tenant_requires_evidence if {
	cross := object.union(allow, {"tenantScope":"CROSS_TENANT"})
	cross_input := object.union(base_input, {"tenant":{"id":"tenant-b"}})
	result := decision with input as cross_input with data.policies as [cross]
	result.reasonCode == "TENANT_ISOLATION_FAILED"
}
