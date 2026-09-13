package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ValidateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateApplicationCredentialRequestMapperTests {

    private static final String APPLICATION_ID = UUID.randomUUID().toString();

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> map(new ValidateApplicationCredentialRawRequest(null, "secreto")))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new ValidateApplicationCredentialRawRequest("not-a-uuid", "secreto")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void builds_the_request_and_leaves_the_secret_untouched() {
        ValidateApplicationCredentialRequest request = map(
                new ValidateApplicationCredentialRawRequest(APPLICATION_ID, "  secreto-en-claro  "));

        assertThat(request.applicationId()).isEqualTo(ApplicationId.of(APPLICATION_ID));
        assertThat(request.secret()).isEqualTo("  secreto-en-claro  ");
    }

    private static ValidateApplicationCredentialRequest map(ValidateApplicationCredentialRawRequest raw) {
        return ValidateApplicationCredentialRequestMapper.toRequest(raw);
    }
}
