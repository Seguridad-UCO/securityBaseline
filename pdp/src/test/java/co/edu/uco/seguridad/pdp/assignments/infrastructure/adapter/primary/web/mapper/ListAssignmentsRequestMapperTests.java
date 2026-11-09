package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Misma lógica que ListRolesRequestMapper/ListApplicationsRequestMapper (barrera C1), ya probada
 * allí caso por caso — aquí solo se confirma que assignments la reutiliza.
 */
class ListAssignmentsRequestMapperTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final String ROLE_ID = UUID.randomUUID().toString();

    @Test
    void without_parameters_uses_the_default_window() {
        ListAssignmentsRequest request = map(new ListAssignmentsRawRequest(ROLE_ID, null, null, null, null));

        assertThat(request.window()).isEqualTo(PageWindow.defaultWindow());
        assertThat(request.criteria()).isEqualTo(AssignmentCriteria.of(RoleId.of(ROLE_ID), UCO));
    }

    @Test
    void rejects_mixing_page_with_offset() {
        assertThatThrownBy(() -> map(new ListAssignmentsRawRequest(ROLE_ID, "1", null, "5", null)))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_a_size_below_one_naming_the_field() {
        assertThatThrownBy(() -> map(new ListAssignmentsRawRequest(ROLE_ID, null, "0", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(e -> ((MalformedRequestFieldException) e).field())
                .isEqualTo("size");
    }

    private static ListAssignmentsRequest map(ListAssignmentsRawRequest raw) {
        return ListAssignmentsRequestMapper.toRequest(raw, UCO);
    }
}
