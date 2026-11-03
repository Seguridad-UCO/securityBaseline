package security.authorization.application.guard

import data.security.authorization.application.composition
import rego.v1

same_tenant if input.subject.tenantId == input.tenant.id
cross_tenant_evidence if "tenant:cross-access" in object.get(input.subject, "entitlements", [])
cross_tenant_evidence if object.get(object.get(input.subject, "attributes", {}), "crossTenantAccess", false) == true

cross_tenant_allowed if {
	some policy in composition.matched_allows
	policy.tenantScope == "CROSS_TENANT"
	cross_tenant_evidence
}

tenant_allowed if same_tenant
tenant_allowed if cross_tenant_allowed
