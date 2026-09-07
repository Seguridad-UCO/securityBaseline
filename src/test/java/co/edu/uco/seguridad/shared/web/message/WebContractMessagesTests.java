package co.edu.uco.seguridad.shared.web.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cada mensaje se arma con interpolación de String — sin un test directo, un typo en cualquiera de
 * ellos solo se nota leyendo una respuesta HTTP a mano.
 */
class WebContractMessagesTests {

    @Test
    void names_the_missing_field() {
        assertThat(WebContractMessages.missingField("applicationName"))
                .isEqualTo("El campo 'applicationName' es requerido");
    }

    @Test
    void names_the_malformed_field_and_the_reason() {
        assertThat(WebContractMessages.malformedField("page", "debe ser un número entero"))
                .isEqualTo("El campo 'page' es inválido: debe ser un número entero");
    }

    @Test
    void has_a_fixed_message_for_conflicting_paging_modes() {
        assertThat(WebContractMessages.conflictingPagingModes())
                .isEqualTo("Use either page/size or offset/limit, not both");
    }

    @Test
    void has_a_fixed_message_for_offset_and_limit_needing_each_other() {
        assertThat(WebContractMessages.offsetLimitTogether())
                .isEqualTo("offset y limit deben suministrarse juntos");
    }

    @Test
    void has_a_fixed_message_for_an_unreadable_body() {
        assertThat(WebContractMessages.unreadableBody())
                .isEqualTo("El cuerpo de la solicitud no pudo ser leído");
    }

    @Test
    void has_a_fixed_message_for_an_internal_error() {
        assertThat(WebContractMessages.internalError())
                .isEqualTo("La solicitud no pudo ser completada");
    }

    @Test
    void has_a_fixed_message_for_unauthorized() {
        assertThat(WebContractMessages.unauthorized())
                .isEqualTo("Token ausente, inválido o expirado");
    }

    @Test
    void has_a_fixed_message_for_forbidden() {
        assertThat(WebContractMessages.forbidden())
                .isEqualTo("El token es válido pero no autoriza esta operación");
    }

    @Test
    void has_a_fixed_success_message_for_application_registration() {
        assertThat(WebContractMessages.successApplicationRegistered())
                .isEqualTo("Aplicación protegida y recurso inicial registrados");
    }

    @Test
    void has_a_fixed_success_message_for_catalog_queries() {
        assertThat(WebContractMessages.successCatalogQueried())
                .isEqualTo("Catálogo de aplicaciones protegidas consultado");
    }

    @Test
    void has_a_fixed_message_for_a_non_integer_field() {
        assertThat(WebContractMessages.mustBeInteger()).isEqualTo("debe ser un número entero");
    }

    @Test
    void has_a_fixed_message_requiring_an_error_code() {
        assertThat(WebContractMessages.requireErrorCode()).isEqualTo("se requiere código de error de contrato");
    }

    @Test
    void has_a_fixed_message_requiring_an_offending_field() {
        assertThat(WebContractMessages.requireOffendingField()).isEqualTo("se requiere campo ofensor");
    }
}
