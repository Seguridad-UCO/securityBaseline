package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HU-036: el actor de las escrituras de catálogo {@code TENANT} no tiene aplicación (la deduce el
 * validador); los cuatro componentes son obligatorios, incluida la evidencia de MFA (puede ser
 * {@code NONE}, nunca nula).
 */
class TenantAdministrationRequestTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final String SUBJECT = "test-subject";

    @Test
    void keeps_its_four_components() {
        TenantAdministrationRequest request =
                new TenantAdministrationRequest(TENANT, USER, SUBJECT, AuthenticationContextEvidence.NONE);

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.subjectUserId()).isEqualTo(USER);
        assertThat(request.subject()).isEqualTo(SUBJECT);
        assertThat(request.authenticationContext()).isEqualTo(AuthenticationContextEvidence.NONE);
    }

    @Test
    void rejects_a_null_tenant() {
        assertThatThrownBy(() -> new TenantAdministrationRequest(null, USER, SUBJECT, AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.TENANT_ID);
    }

    @Test
    void rejects_a_null_user() {
        assertThatThrownBy(() -> new TenantAdministrationRequest(TENANT, null, SUBJECT, AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.USER_ID);
    }

    @Test
    void rejects_a_null_subject() {
        assertThatThrownBy(() -> new TenantAdministrationRequest(TENANT, USER, null, AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.SUBJECT);
    }

    @Test
    void rejects_a_null_authentication_context() {
        assertThatThrownBy(() -> new TenantAdministrationRequest(TENANT, USER, SUBJECT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(RequiredArgumentMessages.AUTHENTICATION_CONTEXT_EVIDENCE);
    }
}
