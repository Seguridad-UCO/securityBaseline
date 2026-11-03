package security.administration.role

import rego.v1

# Convención reservada: el nombre de rol que el catálogo del PDP (roles + asignaciones,
# HU-004/HU-005) reconoce como "administra esta aplicación". No es una constante en el
# código del PDP — el catálogo solo aporta evidencia (qué roles tiene el sujeto para esta
# aplicación); qué evidencia cuenta como "administra" es una decisión de política, y vive
# aquí (PLAN-HU-009.md, hallazgo 7 y ambigüedad 2).
#
# Mientras el PDP no tenga un mecanismo para reservar este nombre en el catálogo de roles
# (ningún tenant está impedido hoy de nombrar un rol "ADMIN" sin intención administrativa),
# esta política es la única puerta: un tenant que otorgue ese rol a alguien sin querer
# volverlo administrador se corrige revocando la asignación (HU-005), no cambiando esta
# política.
administrator_role_name := "ADMIN"

is_administrator if {
	administrator_role_name in object.get(input.subject, "roles", [])
}
