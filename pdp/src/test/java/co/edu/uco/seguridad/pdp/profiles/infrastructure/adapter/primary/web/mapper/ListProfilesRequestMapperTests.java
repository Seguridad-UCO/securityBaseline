package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ListProfilesRequest;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Misma lógica que ListRolesRequestMapper (barrera C3), ya probada allí caso por caso.
 */
class ListProfilesRequestMapperTests {

    private static final TenantId UCO = new TenantId("universidad-uco");

    @Test
    void without_parameters_uses_the_default_window() {
        ListProfilesRequest request = map(new ListProfilesRawRequest(null, null, null, null));

        assertThat(request.window()).isEqualTo(PageWindow.defaultWindow());
        assertThat(request.criteria()).isEqualTo(ProfileCriteria.ofTenant(UCO));
    }

    @Test
    void rejects_mixing_page_with_offset() {
        assertThatThrownBy(() -> map(new ListProfilesRawRequest("1", null, "5", null)))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_a_size_below_one_naming_the_field() {
        assertThatThrownBy(() -> map(new ListProfilesRawRequest(null, "0", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("size");
    }

    private static ListProfilesRequest map(ListProfilesRawRequest raw) {
        return ListProfilesRequestMapper.toRequest(raw, UCO);
    }
}
