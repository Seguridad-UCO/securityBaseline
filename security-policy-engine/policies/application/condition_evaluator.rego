package security.authorization.application.conditions

import rego.v1

# Rego rejects recursive rules. Four levels cover the published v1 AST; CI must reject deeper definitions.
matches(condition) if matches_3(condition)
matches_3(condition) if predicate_matches(condition)

matches_3(condition) if {
	children := condition.all
	count(children) > 0
	every child in children { matches_2(child) }
}

matches_3(condition) if {
	children := condition.any
	count(children) > 0
	some child in children
	matches_2(child)
}

matches_3(condition) if {
	child := condition.not
	predicate_evaluable(child)
	not matches_2(child)
}

matches_2(condition) if predicate_matches(condition)

matches_2(condition) if {
	children := condition.all
	count(children) > 0
	every child in children { matches_1(child) }
}

matches_2(condition) if {
	children := condition.any
	count(children) > 0
	some child in children
	matches_1(child)
}

matches_2(condition) if {
	child := condition.not
	predicate_evaluable(child)
	not matches_1(child)
}

matches_1(condition) if predicate_matches(condition)

matches_1(condition) if {
	children := condition.all
	count(children) > 0
	every child in children { matches_0(child) }
}

matches_1(condition) if {
	children := condition.any
	count(children) > 0
	some child in children
	matches_0(child)
}

matches_1(condition) if {
	child := condition.not
	predicate_evaluable(child)
	not matches_0(child)
}

matches_0(condition) if predicate_matches(condition)

predicate_matches(condition) if {
	predicate := condition.predicate
	left := resolve(predicate.left)
	right := resolve(predicate.right)
	operator_matches(predicate.operator, left, right)
}

predicate_evaluable(condition) if {
	predicate := condition.predicate
	resolve(predicate.left)
	resolve(predicate.right)
}

resolve(operand) := value if value := operand.value

resolve(operand) := value if {
	path := operand.ref
	allowed_path(path)
	count(path) == 1
	value := object.get(input, path[0], null)
	value != null
}

resolve(operand) := value if {
	path := operand.ref
	allowed_path(path)
	count(path) == 2
	value := object.get(object.get(input, path[0], {}), path[1], null)
	value != null
}

resolve(operand) := value if {
	path := operand.ref
	allowed_path(path)
	count(path) == 3
	value := object.get(object.get(object.get(input, path[0], {}), path[1], {}), path[2], null)
	value != null
}

resolve(operand) := value if {
	path := operand.ref
	allowed_path(path)
	count(path) == 4
	value := object.get(object.get(object.get(object.get(input, path[0], {}), path[1], {}), path[2], {}), path[3], null)
	value != null
}

allowed_path(path) if {
	count(path) > 0
	path[0] in {"subject", "tenant", "application", "resource", "context", "security", "relationships"}
	every segment in path { is_string(segment) }
}

operator_matches("EQ", left, right) if {
	type_name(left) == type_name(right)
	left == right
}

operator_matches("NEQ", left, right) if {
	type_name(left) == type_name(right)
	left != right
}

operator_matches("CONTAINS", left, right) if {
	is_array(left)
	right in left
}

operator_matches("IN", left, right) if {
	is_array(right)
	left in right
}

operator_matches("EXISTS", left, _) if left != null

operator_matches("GT", left, right) if {
	is_number(left)
	is_number(right)
	left > right
}

operator_matches("GTE", left, right) if {
	is_number(left)
	is_number(right)
	left >= right
}

operator_matches("LT", left, right) if {
	is_number(left)
	is_number(right)
	left < right
}

operator_matches("LTE", left, right) if {
	is_number(left)
	is_number(right)
	left <= right
}
