package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

import java.util.Set;

/**
 * Traduce el raw request + el principal autenticado a {@link AdministrationRequest} (HU-015).
 * {@code subjectRoles} arranca vacío — lo resuelve
 * {@code AuthorizeAdministrationUseCaseImpl} internamente, igual que en {@code AuthorizeInteractorImpl}.
 */
public final class ApplicationAdministrationRequestMapper {

    private ApplicationAdministrationRequestMapper() {
    }

    public static AdministrationRequest toAdministrationRequest(ApplicationAdministrationRawRequest raw,
            PdpPrincipal principal) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        return new AdministrationRequest(principal.tenantId(), applicationId,
                principal.userId().orElseThrow(() -> new IllegalStateException(
                        "El principal no trae userId resuelto — solo ocurre fuera del perfil 'keycloak'")),
                principal.subject(), Set.of());
    }
}
