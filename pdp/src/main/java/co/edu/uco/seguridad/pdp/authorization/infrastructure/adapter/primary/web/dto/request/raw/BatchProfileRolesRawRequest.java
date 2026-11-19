package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

import java.util.List;

/** Conjunto completo de roles para una operación masiva sobre un perfil. */
public record BatchProfileRolesRawRequest(List<String> roleIds) {
    public BatchProfileRolesRawRequest {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
