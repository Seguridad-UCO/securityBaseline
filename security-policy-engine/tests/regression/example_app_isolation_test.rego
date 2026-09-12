package security.authorization.regression

import data.security.authorization.decision
import rego.v1

# Estos tests corren contra el `decision` real, con los modulos de `policies/applications/`
# ya mezclados por OPA -- sin `with data....allow_candidates as ...`. Es la prueba de que el
# candidato de un modulo de aplicacion no puede autorizar una peticion de otra aplicacion,
# tal como pide tests/regression/README.md.

example_app_input := {
	"schemaVersion": "1.0",
	"request": {"id": "req-example-app-1"},
	"subject": {"id": "user-1", "type": "USER", "tenantId": "tenant-a", "roles": ["reader"]},
	"tenant": {"id": "tenant-a"},
	"application": {"id": "example-app"},
	"resource": {"type": "document"},
	"action": "document:read",
}

test_example_app_candidate_allows_its_own_application if {
	result := decision with input as example_app_input
	result.effect == "ALLOW"
	result.reasonCode == "POLICY_ALLOWED"
	result.policyReferences == [{"id": "application.example-app.read", "version": "1.0"}]
}

test_example_app_candidate_cannot_authorize_a_different_application if {
	foreign_app := object.union(example_app_input, {"application": {"id": "another-app"}})
	result := decision with input as foreign_app
	result.effect == "DENY"
	result.reasonCode == "NO_APPLICABLE_POLICY"
}

test_example_app_candidate_ignores_a_subject_without_the_required_role if {
	unprivileged := object.union(example_app_input, {"subject": {"id": "user-2", "type": "USER", "tenantId": "tenant-a", "roles": []}})
	result := decision with input as unprivileged
	result.effect == "DENY"
	result.reasonCode == "NO_APPLICABLE_POLICY"
}
