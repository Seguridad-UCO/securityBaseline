package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListRolesRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Misma lógica que ListApplicationsRequestMapper (barrera C3), ya probada allí caso por caso —
 * aquí solo se confirma que roles la reutiliza, no se repite la matriz completa.
 */
class ListRolesRequestMapperTests {

    private static final TenantId UCO = new TenantId("universidad-uco");

    @Test
    void without_parameters_uses_the_default_window() {
        ListRolesRequest request = map(new ListRolesRawRequest(null, null, null, null));

        assertThat(request.window()).isEqualTo(PageWindow.defaultWindow());
        assertThat(request.criteria()).isEqualTo(co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria.ofTenant(UCO));
    }

    @Test
    void rejects_mixing_page_with_offset() {
        assertThatThrownBy(() -> map(new ListRolesRawRequest("1", null, "5", null)))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_a_size_below_one_naming_the_field() {
        assertThatThrownBy(() -> map(new ListRolesRawRequest(null, "0", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("size");
    }

    private static ListRolesRequest map(ListRolesRawRequest raw) {
        return ListRolesRequestMapper.toRequest(raw, UCO);
    }
}
