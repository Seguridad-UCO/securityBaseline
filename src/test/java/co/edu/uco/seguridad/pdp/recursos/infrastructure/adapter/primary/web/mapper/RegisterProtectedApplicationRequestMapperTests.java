package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The two-level input strategy under test: raw strings in, value objects out, and every rejection
 * naming the field that caused it.
 */
class RegisterProtectedApplicationRequestMapperTests {

    @Test
    void turns_a_well_formed_payload_into_a_fully_typed_dto() {
        RegisterProtectedApplicationRequest request = RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(
                        "universidad-uco", "gestion-academica", "estudiantes", "consultar"));

        var dto = RegisterProtectedApplicationRequestMapper.toRequest(request);

        assertThat(dto.tenantId()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(dto.applicationName()).isEqualTo(new ApplicationName("gestion-academica"));
        assertThat(dto.resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
        assertThat(dto.action()).isEqualTo(new ActionCode("consultar"));
    }

    @Test
    void trims_incidental_whitespace_before_building_the_value_objects() {
        RegisterProtectedApplicationRequest request = RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(
                        "  universidad-uco  ", " gestion-academica ", " estudiantes ", " consultar "));

        assertThat(request.tenantId()).isEqualTo(new TenantId("universidad-uco"));
        assertThat(request.action()).isEqualTo(new ActionCode("consultar"));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "NULL, gestion-academica, estudiantes, consultar, tenantId",
            "universidad-uco, NULL, estudiantes, consultar, applicationName",
            "universidad-uco, gestion-academica, NULL, consultar, resourceCode",
            "universidad-uco, gestion-academica, estudiantes, NULL, action"
    })
    void reports_a_missing_field_by_name(String tenant, String name, String code, String action, String expected) {
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(tenant, name, code, action)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(error -> ((MissingRequestFieldException) error).field())
                .isEqualTo(expected);
    }

    @Test
    void treats_a_blank_field_as_missing_rather_than_as_an_empty_value() {
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest("   ", "gestion-academica", "estudiantes", "consultar")))
                .isInstanceOf(MissingRequestFieldException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "tenant with spaces, gestion-academica, estudiantes, consultar, tenantId",
            "universidad-uco, ab, estudiantes, consultar, applicationName",
            "universidad-uco, gestion-academica, Estudiantes, consultar, resourceCode",
            "universidad-uco, gestion-academica, estudiantes, CONSULTAR, action"
    })
    void reports_a_malformed_field_by_name(String tenant, String name, String code, String action, String expected) {
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(tenant, name, code, action)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo(expected);
    }

    @Test
    void reports_a_value_that_is_out_of_range() {
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(
                        "universidad-uco", "x".repeat(101), "estudiantes", "consultar")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .hasMessageContaining("applicationName")
                .hasMessageContaining("100 caracteres");
    }

    @Test
    void carries_the_reason_stated_by_the_value_object_instead_of_restating_the_format() {
        assertThatThrownBy(() -> RegisterProtectedApplicationRequestMapper.toValidatedRequest(
                new RegisterProtectedApplicationRawRequest(
                        "universidad-uco", "gestion-academica", "Estudiantes", "consultar")))
                .hasMessageContaining("kebab-case");
    }
}
