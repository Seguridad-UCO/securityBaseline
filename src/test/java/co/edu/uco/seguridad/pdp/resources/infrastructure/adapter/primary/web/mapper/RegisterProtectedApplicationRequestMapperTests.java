package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The two-level input strategy under test: raw strings in, value objects out, and every rejection
 * naming the field that caused it. {@code tenantId} no longer travels through the raw payload
 * (ADR-0003): it is supplied here exactly as the interactor would supply it, already read from the
 * authenticated principal.
 */
class RegisterProtectedApplicationRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void turns_a_well_formed_payload_into_a_fully_typed_dto() {
        var dto = RegisterProtectedApplicationRequestMapper.toRequest(
                new RegisterProtectedApplicationRawRequest("gestion-academica", "estudiantes", "consultar"),
                TENANT);

        assertThat(dto.tenantId()).isEqualTo(TENANT);
        assertThat(dto.applicationName()).isEqualTo(new ApplicationName("gestion-academica"));
        assertThat(dto.resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
        assertThat(dto.action()).isEqualTo(new ActionCode("consultar"));
    }

    @Test
    void trims_incidental_whitespace_before_building_the_value_objects() {
        var dto = RegisterProtectedApplicationRequestMapper.toRequest(
                new RegisterProtectedApplicationRawRequest(" gestion-academica ", " estudiantes ", " consultar "),
                TENANT);

        assertThat(dto.tenantId()).isEqualTo(TENANT);
        assertThat(dto.action()).isEqualTo(new ActionCode("consultar"));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL, estudiantes, consultar, applicationName",
            "gestion-academica, NULL, consultar, resourceCode",
            "gestion-academica, estudiantes, NULL, action"
    })
    void reports_a_missing_field_by_name(String name, String code, String action, String expected) {
        var raw = new RegisterProtectedApplicationRawRequest(name, code, action);
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toRequest(raw, TENANT))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(error -> ((MissingRequestFieldException) error).field())
                .isEqualTo(expected);
    }

    @Test
    void treats_a_blank_field_as_missing_rather_than_as_an_empty_value() {
        var raw = new RegisterProtectedApplicationRawRequest("   ", "estudiantes", "consultar");
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toRequest(raw, TENANT))
                .isInstanceOf(MissingRequestFieldException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "ab, estudiantes, consultar, applicationName",
            "gestion-academica, Estudiantes, consultar, resourceCode",
            "gestion-academica, estudiantes, CONSULTAR, action"
    })
    void reports_a_malformed_field_by_name(String name, String code, String action, String expected) {
        var raw = new RegisterProtectedApplicationRawRequest(name, code, action);
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toRequest(raw, TENANT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo(expected);
    }

    @Test
    void reports_a_value_that_is_out_of_range() {
        var raw = new RegisterProtectedApplicationRawRequest("x".repeat(101), "estudiantes", "consultar");
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toRequest(raw, TENANT))
                .isInstanceOf(MalformedRequestFieldException.class)
                .hasMessageContaining("applicationName")
                .hasMessageContaining("100 caracteres");
    }

    @Test
    void carries_the_reason_stated_by_the_value_object_instead_of_restating_the_format() {
        var raw = new RegisterProtectedApplicationRawRequest("gestion-academica", "Estudiantes", "consultar");
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toRequest(raw, TENANT))
                .hasMessageContaining("kebab-case");
    }
}
