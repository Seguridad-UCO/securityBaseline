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

/**
 * El {@code UserId} ya llega resuelto (PLAN-HU-015.md §14): este mapper no decide de dónde sale
 * —eso lo resuelve el interactor, con fallback a {@code identity} cuando el principal no lo trae—,
 * solo lo recibe y arma el resto del contrato.
 */
class ApplicationAdministrationRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final PdpPrincipal PRINCIPAL = new PdpPrincipal(TENANT, "test-subject", "jwt-1", Optional.of(USER));

    @Test
    void builds_the_administration_request_from_the_route_the_principal_and_the_resolved_user_id() {
        AdministrationRequest request = ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest(APPLICATION.value().toString()), PRINCIPAL, USER);

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.applicationId()).isEqualTo(APPLICATION);
        assertThat(request.subjectUserId()).isEqualTo(USER);
        assertThat(request.subject()).isEqualTo("test-subject");
        assertThat(request.subjectRoles()).isEmpty();
    }

    @Test
    void requires_the_application_id_field() {
        assertThatThrownBy(() -> ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest(null), PRINCIPAL, USER))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }

    @Test
    void rejects_an_application_id_that_is_not_a_valid_identifier() {
        assertThatThrownBy(() -> ApplicationAdministrationRequestMapper.toAdministrationRequest(
                new ApplicationAdministrationRawRequest("not-a-uuid"), PRINCIPAL, USER))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("applicationId");
    }
}
