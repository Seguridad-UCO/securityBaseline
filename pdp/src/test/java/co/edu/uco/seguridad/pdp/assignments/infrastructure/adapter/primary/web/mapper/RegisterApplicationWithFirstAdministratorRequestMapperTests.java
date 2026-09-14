package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RegisterApplicationWithFirstAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterApplicationWithFirstAdministratorRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId REGISTRAR = new UserId(UUID.randomUUID());

    @Test
    void maps_the_raw_fields_and_carries_the_tenant_and_registrar_from_the_principal() {
        RegisterApplicationWithFirstAdministratorRawRequest raw = new RegisterApplicationWithFirstAdministratorRawRequest(
                "Moodle", "  LMS institucional  ", "https://moodle.uco.edu.co");

        RegisterApplicationWithFirstAdministratorRequest request = map(raw);

        assertThat(request.registrarUserId()).isEqualTo(REGISTRAR);
        assertThat(request.application().tenantId()).isEqualTo(TENANT);
        assertThat(request.application().name().value()).isEqualTo("Moodle");
        assertThat(request.application().description()).isEqualTo("LMS institucional");
        assertThat(request.application().baseUrl().value()).isEqualTo("https://moodle.uco.edu.co");
    }

    @Test
    void normalizes_a_null_description_to_an_empty_string() {
        RegisterApplicationWithFirstAdministratorRawRequest raw = new RegisterApplicationWithFirstAdministratorRawRequest(
                "Moodle", null, "https://moodle.uco.edu.co");

        assertThat(map(raw).application().description()).isEmpty();
    }

    @Test
    void requires_the_name_field() {
        RegisterApplicationWithFirstAdministratorRawRequest raw = new RegisterApplicationWithFirstAdministratorRawRequest(
                null, "LMS", "https://moodle.uco.edu.co");

        assertThatThrownBy(() -> map(raw))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("name");
    }

    @Test
    void requires_the_base_url_field() {
        RegisterApplicationWithFirstAdministratorRawRequest raw = new RegisterApplicationWithFirstAdministratorRawRequest(
                "Moodle", "LMS", null);

        assertThatThrownBy(() -> map(raw))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("baseUrl");
    }

    @Test
    void rejects_a_base_url_that_is_not_a_valid_url() {
        RegisterApplicationWithFirstAdministratorRawRequest raw = new RegisterApplicationWithFirstAdministratorRawRequest(
                "Moodle", "LMS", "not-a-url");

        assertThatThrownBy(() -> map(raw))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("baseUrl");
    }

    private static RegisterApplicationWithFirstAdministratorRequest map(
            RegisterApplicationWithFirstAdministratorRawRequest raw) {
        return RegisterApplicationWithFirstAdministratorRequestMapper.toRequest(raw, TENANT, REGISTRAR);
    }
}
