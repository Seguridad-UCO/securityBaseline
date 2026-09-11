package security.authorization.domain.relationship

import rego.v1

has(type, subject_id, resource_id) if {
	some relation in object.get(input, "relationships", [])
	relation.type == type
	relation.subjectId == subject_id
	relation.resourceId == resource_id
}
