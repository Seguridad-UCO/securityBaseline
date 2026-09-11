package security.authorization.domain.tenant

import data.security.authorization.domain.subject
import rego.v1

same_tenant if input.subject.tenantId == input.tenant.id

# This evidence is trusted only because the PDP resolves it before invoking OPA.
cross_access_evidence if subject.has_entitlement("tenant:cross-access")

cross_access_evidence if subject.attribute("crossTenantAccess") == true
