package security.authorization_test

import data.security.authorization.decision
import rego.v1

core_input := {"schemaVersion":"1.0","request":{"id":"request-core"},"subject":{"id":"user-1","type":"USER","tenantId":"tenant-a","roles":["EDITOR"],"profiles":["OPERATIONS"],"entitlements":["resource:550e8400-e29b-41d4-a716-446655440000"]},"tenant":{"id":"tenant-a"},"application":{"id":"app-a"},"resource":{"type":"http","id":"/orders","attributes":{"resourceId":"550e8400-e29b-41d4-a716-446655440000","entitlementKey":"resource:550e8400-e29b-41d4-a716-446655440000","path":"/orders","method":"GET"}},"action":"GET"}

core := {"id":"core.resource-grant","version":"1.0.0","effect":"ALLOW","scope":{"level":"GLOBAL"},"target":{"tenantIds":[],"applicationIds":[],"resourceTypes":["http"],"resourceIds":[],"actions":[],"environments":[]},"tenantScope":"SAME_TENANT","condition":{"predicate":{"left":{"ref":["resource","attributes","entitlementKey"]},"operator":"IN","right":{"ref":["subject","entitlements"]}}},"obligations":[]}

test_core_resource_grant_allows_current_entitlement if {
  decision with input as core_input with data.policies as [core] == {"effect":"ALLOW","reasonCode":"POLICY_ALLOWED","policyReferences":[{"id":"core.resource-grant","version":"1.0.0"}],"obligations":[]}
}

test_absent_entitlement_fails_closed if {
  result := decision with input as object.union(core_input, {"subject": object.union(core_input.subject, {"entitlements": []})}) with data.policies as [core]
  result.reasonCode == "NO_APPLICABLE_POLICY"
}
