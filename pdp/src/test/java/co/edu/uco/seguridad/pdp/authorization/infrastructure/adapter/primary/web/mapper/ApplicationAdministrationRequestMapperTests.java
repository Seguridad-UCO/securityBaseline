package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationAdministrationRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void builds_the_administration_request_from_the_route_and_the_principal() {
        PdpPrincipal principal = new PdpPrincipal(TENANT, "test-subject", "jwt-1", Optional.of(USER));

        AdministrationRequest request = ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest(APPLICATION.value().toString()), principal);

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.applicationId()).isEqualTo(APPLICATION);
        assertThat(request.subjectUserId()).isEqualTo(USER);
        assertThat(request.subject()).isEqualTo("test-subject");
        assertThat(request.subjectRoles()).isEmpty();
    }

    @Test
    void requires_the_application_id_field() {
        PdpPrincipal principal = new PdpPrincipal(TENANT, "test-subject", "jwt-1", Optional.of(USER));

        assertThatThrownBy(() -> ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest(null), principal))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        PdpPrincipal principal = new PdpPrincipal(TENANT, "test-subject", "jwt-1", Optional.of(USER));

        assertThatThrownBy(() -> ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest("not-a-uuid"), principal))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void fails_fast_when_the_principal_has_no_resolved_user_id() {
        PdpPrincipal principal = new PdpPrincipal(TENANT, "test-subject", "jwt-1", Optional.empty());

        assertThatThrownBy(() -> ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest(APPLICATION.value().toString()), principal))
                .isInstanceOf(IllegalStateException.class);
    }
}
