package security.authorization.application.guards

import data.security.authorization.domain.tenant
import rego.v1

same_tenant_guard if tenant.same_tenant

cross_tenant_guard(candidate) if {
	candidate.tenantScope == "CROSS_TENANT"
	tenant.cross_access_evidence
}

tenant_guard(_) if same_tenant_guard
tenant_guard(candidate) if cross_tenant_guard(candidate)
