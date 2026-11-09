package security.administration.role_test

import data.security.administration.role
import rego.v1

test_is_administrator_matches_the_reserved_role_name_exactly if {
	role.is_administrator with input as {"subject": {"roles": ["ADMIN"]}}
	not role.is_administrator with input as {"subject": {"roles": ["admin"]}}
	not role.is_administrator with input as {"subject": {"roles": []}}
	not role.is_administrator with input as {"subject": {}}
}
