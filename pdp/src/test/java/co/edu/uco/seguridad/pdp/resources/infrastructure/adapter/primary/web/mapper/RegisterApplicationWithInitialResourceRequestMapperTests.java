package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterApplicationWithInitialResourceRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterApplicationWithInitialResourceRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void requires_the_name_field() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                null, "", "https://example.com", "/estudiantes", "GET")))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("name");
    }

    @Test
    void rejects_a_name_that_is_too_short() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "ab", "", "https://example.com", "/estudiantes", "GET")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("name");
    }

    @Test
    void requires_the_base_url_field() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", null, "/estudiantes", "GET")))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("baseUrl");
    }

    @Test
    void rejects_a_base_url_without_scheme_or_host() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", "not-a-url", "/estudiantes", "GET")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("baseUrl");
    }

    @Test
    void requires_the_resource_path_field() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", "https://example.com", null, "GET")))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("resourcePath");
    }

    @Test
    void rejects_a_resource_path_without_a_leading_slash() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", "https://example.com", "estudiantes", "GET")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("resourcePath");
    }

    @Test
    void requires_the_resource_method_field() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", "https://example.com", "/estudiantes", null)))
                .isInstanceOf(MissingRequestFieldException.class)
                .extracting(e -> ((MissingRequestFieldException) e).field())
                .isEqualTo("resourceMethod");
    }

    @Test
    void rejects_a_resource_method_that_is_not_a_supported_verb() {
        assertThatThrownBy(() -> map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", "", "https://example.com", "/estudiantes", "no-existe")))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("resourceMethod");
    }

    @Test
    void builds_the_request_with_the_tenant_from_the_token_and_normalizes_the_description() {
        RegisterApplicationWithInitialResourceRequest request = map(new RegisterApplicationWithInitialResourceRawRequest(
                "gestion-academica", null, "https://example.com", "/estudiantes", "get"));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.name().value()).isEqualTo("gestion-academica");
        assertThat(request.description()).isEmpty();
        assertThat(request.baseUrl().value()).isEqualTo("https://example.com");
        assertThat(request.resourcePath().value()).isEqualTo("/estudiantes");
        assertThat(request.resourceMethod().name()).isEqualTo("GET");
    }

    private static RegisterApplicationWithInitialResourceRequest map(RegisterApplicationWithInitialResourceRawRequest raw) {
        return RegisterApplicationWithInitialResourceRequestMapper.toRequest(raw, TENANT);
    }
}
