package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import java.util.Set;

public record CreateServiceTechnicalRoleRawRequest(String code, String name, Set<String> resourceIds) {
}
