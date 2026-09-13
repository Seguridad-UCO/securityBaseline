package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RotateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RotateApplicationCredentialRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final String APPLICATION_ID = UUID.randomUUID().toString();

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> map(new RotateApplicationCredentialRawRequest(null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> map(new RotateApplicationCredentialRawRequest("not-a-uuid")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void builds_the_request_with_the_application_id_from_the_path_and_the_tenant_from_the_token() {
        RotateApplicationCredentialRequest request = map(new RotateApplicationCredentialRawRequest(APPLICATION_ID));

        assertThat(request.applicationId()).isEqualTo(ApplicationId.of(APPLICATION_ID));
        assertThat(request.tenantId()).isEqualTo(TENANT);
    }

    private static RotateApplicationCredentialRequest map(RotateApplicationCredentialRawRequest raw) {
        return RotateApplicationCredentialRequestMapper.toRequest(raw, TENANT);
    }
}
