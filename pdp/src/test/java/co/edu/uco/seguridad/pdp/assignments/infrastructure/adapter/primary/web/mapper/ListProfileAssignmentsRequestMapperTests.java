package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListProfileAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Misma lógica que ListAssignmentsRequestMapper (barrera C1), ya probada allí caso por caso — aquí
 * solo se confirma que assignments la reutiliza sobre profileId en vez de roleId.
 */
class ListProfileAssignmentsRequestMapperTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final String PROFILE_ID = UUID.randomUUID().toString();

    @Test
    void without_parameters_uses_the_default_window() {
        ListProfileAssignmentsRequest request = map(new ListProfileAssignmentsRawRequest(PROFILE_ID, null, null, null, null));

        assertThat(request.window()).isEqualTo(PageWindow.defaultWindow());
        assertThat(request.criteria()).isEqualTo(ProfileAssignmentCriteria.of(ProfileId.of(PROFILE_ID), UCO));
    }

    @Test
    void rejects_mixing_page_with_offset() {
        assertThatThrownBy(() -> map(new ListProfileAssignmentsRawRequest(PROFILE_ID, "1", null, "5", null)))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_a_size_below_one_naming_the_field() {
        assertThatThrownBy(() -> map(new ListProfileAssignmentsRawRequest(PROFILE_ID, null, "0", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("size");
    }

    private static ListProfileAssignmentsRequest map(ListProfileAssignmentsRawRequest raw) {
        return ListProfileAssignmentsRequestMapper.toRequest(raw, UCO);
    }
}
