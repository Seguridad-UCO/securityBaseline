package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * raw a {@link ValidateApplicationCredentialRequest} con {@link RequestFieldParser} (HU-013).
 * {@code secret} pasa tal cual: es texto en tránsito, sin invariante de dominio propio.
 */
public final class ValidateApplicationCredentialRequestMapper {

    private ValidateApplicationCredentialRequestMapper() {
    }

    public static ValidateApplicationCredentialRequest toRequest(ValidateApplicationCredentialRawRequest raw) {
        return new ValidateApplicationCredentialRequest(
                RequestFieldParser.parse("applicationName", raw.applicationName(), ApplicationName::new), raw.secret());
    }
}
