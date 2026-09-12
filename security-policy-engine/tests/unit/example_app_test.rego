package security.authorization.application_test

import data.security.authorization.application
import rego.v1

reader_input := {
	"application": {"id": "example-app"},
	"action": "document:read",
	"subject": {"id": "user-1", "roles": ["reader"]},
}

editor_owner_input := {
	"application": {"id": "example-app"},
	"action": "document:write",
	"subject": {"id": "user-1", "roles": ["editor"]},
	"resource": {"id": "doc-1"},
	"relationships": [{"type": "owner", "subjectId": "user-1", "resourceId": "doc-1"}],
}

test_reader_can_read_a_document if {
	candidate := {
		"effect": "ALLOW",
		"applicationId": "example-app",
		"policyId": "application.example-app.read",
		"policyVersion": "1.0",
		"tenantScope": "SAME_TENANT",
		"obligations": [],
	}
	candidate in application.allow_candidates with input as reader_input
}

test_reader_cannot_write if {
	write_attempt := object.union(reader_input, {"action": "document:write"})
	count({c | some c in application.allow_candidates with input as write_attempt}) == 0
}

test_editor_can_write_their_own_document if {
	candidate := {
		"effect": "ALLOW",
		"applicationId": "example-app",
		"policyId": "application.example-app.write-own",
		"policyVersion": "1.0",
		"tenantScope": "SAME_TENANT",
		"obligations": [{"type": "AUDIT", "parameters": {}}],
	}
	candidate in application.allow_candidates with input as editor_owner_input
}

test_editor_cannot_write_a_document_they_do_not_own if {
	foreign_document := object.union(editor_owner_input, {
		"resource": {"id": "doc-2"},
		"relationships": [{"type": "owner", "subjectId": "user-1", "resourceId": "doc-1"}],
	})
	count({c | some c in application.allow_candidates with input as foreign_document}) == 0
}

test_candidates_do_not_apply_to_a_different_application if {
	other_app := object.union(reader_input, {"application": {"id": "another-app"}})
	count({c | some c in application.allow_candidates with input as other_app}) == 0
}
