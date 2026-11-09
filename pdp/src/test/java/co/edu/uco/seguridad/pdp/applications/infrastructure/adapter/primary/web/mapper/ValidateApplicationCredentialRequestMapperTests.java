package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateApplicationCredentialRequestMapperTests {

    private static final String APPLICATION_NAME = "notas";

    @Test
    void requires_the_application_name_field() {
        assertThatThrownBy(() -> map(new ValidateApplicationCredentialRawRequest(null, "secreto")))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationName");
    }

    @Test
    void rejects_an_application_name_outside_its_allowed_length() {
        assertThatThrownBy(() -> map(new ValidateApplicationCredentialRawRequest("no", "secreto")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationName");
    }

    @Test
    void builds_the_request_and_leaves_the_secret_untouched() {
        ValidateApplicationCredentialRequest request = map(
                new ValidateApplicationCredentialRawRequest(APPLICATION_NAME, "  secreto-en-claro  "));

        assertThat(request.applicationName().value()).isEqualTo(APPLICATION_NAME);
        assertThat(request.secret()).isEqualTo("  secreto-en-claro  ");
    }

    private static ValidateApplicationCredentialRequest map(ValidateApplicationCredentialRawRequest raw) {
        return ValidateApplicationCredentialRequestMapper.toRequest(raw);
    }
}
