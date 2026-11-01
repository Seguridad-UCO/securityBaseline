package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
record ServiceTechnicalRoleResourceWebResponse(String id, String path, String method) {
    static ServiceTechnicalRoleResourceWebResponse from(ProtectedResource resource) { return new ServiceTechnicalRoleResourceWebResponse(resource.id().value().toString(),resource.path().value(),resource.method().name()); }
}
