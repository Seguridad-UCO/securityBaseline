package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * Entrada de {@code AdministerResourceGrantUseCase} (HU-016): la solicitud de concesión de
 * recurso, más la administración a gatear si el rol que la recibe es de alcance {@code APPLICATION}.
 * Misma semántica de {@code administration} vacío que {@link AdministerRoleDefinitionRequest}.
 */
public record AdministerResourceGrantRequest(Optional<AdministrationRequest> administration, GrantResourceRequest grant) {

    public AdministerResourceGrantRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION);
        Objects.requireNonNull(grant, RequiredArgumentMessages.GRANT_RESOURCE_REQUEST);
    }
}
