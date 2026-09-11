package security.authorization.domain_test

import data.security.authorization.domain.subject
import rego.v1

test_subject_evidence_predicates if {
	subject.has_role("reader") with input as {"subject": {"roles": ["reader"], "profiles": ["employee"], "entitlements": ["document:read"], "attributes": {}}}
	subject.has_profile("employee") with input as {"subject": {"roles": [], "profiles": ["employee"], "entitlements": [], "attributes": {}}}
	subject.has_entitlement("document:read") with input as {"subject": {"roles": [], "profiles": [], "entitlements": ["document:read"], "attributes": {}}}
}
