package security.authorization.application

# Modulo de ejemplo, no una politica de produccion. Ninguna aplicacion real llamada
# "example-app" existe todavia en el catalogo del PDP; este archivo solo demuestra el
# patron que policies/applications/README.md pide: emitir candidatos en el set parcial
# security.authorization.application.allow_candidates, combinando roles, relaciones y
# el propio candidato -- sin tocar el entrypoint ni el core (composition.rego, guards.rego).
#
# Cuando una aplicacion real se integre, este archivo se reemplaza por el suyo (o convive
# junto a el, con su propio applicationId) -- no se edita para que "sirva para todos".

import data.security.authorization.domain.relationship
import data.security.authorization.domain.subject
import rego.v1

# Cualquier sujeto con el rol "reader" puede leer un documento de la aplicacion.
allow_candidates contains {
	"effect": "ALLOW",
	"applicationId": "example-app",
	"policyId": "application.example-app.read",
	"policyVersion": "1.0",
	"tenantScope": "SAME_TENANT",
	"obligations": [],
} if {
	input.application.id == "example-app"
	input.action == "document:read"
	subject.has_role("reader")
}

# Un "editor" solo puede escribir sobre un documento del que el propio sujeto es dueno
# (relacion "owner" ya resuelta por el PDP en input.relationships). Se audita porque es
# una escritura, no porque el tenantScope lo exija -- el core solo agrega AUDIT por
# cruce de tenant (with_audit en decision.rego); un modulo de aplicacion es libre de pedir
# sus propias obligaciones.
allow_candidates contains {
	"effect": "ALLOW",
	"applicationId": "example-app",
	"policyId": "application.example-app.write-own",
	"policyVersion": "1.0",
	"tenantScope": "SAME_TENANT",
	"obligations": [{"type": "AUDIT", "parameters": {}}],
} if {
	input.application.id == "example-app"
	input.action == "document:write"
	subject.has_role("editor")
	relationship.has("owner", input.subject.id, input.resource.id)
}
