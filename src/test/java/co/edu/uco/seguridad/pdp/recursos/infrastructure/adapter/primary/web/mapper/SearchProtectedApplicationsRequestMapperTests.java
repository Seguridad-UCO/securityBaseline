package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Criteria 17 to 19 at the boundary: optional filters, two ways of expressing a window, and no
 * ambiguous combination allowed through. {@code tenantId} no longer travels through the query
 * string (ADR-0003): it is supplied here exactly as the interactor would, already read from the
 * authenticated principal.
 */
class SearchProtectedApplicationsRequestMapperTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void an_empty_query_string_yields_no_optional_filters_and_the_default_window() {
        var dto = map(new SearchProtectedApplicationsRawRequest(null, null, null, null, null, null));

        assertThat(dto.criteria().tenantId()).isEqualTo(TENANT);
        assertThat(dto.criteria().nameContains()).isEmpty();
        assertThat(dto.criteria().resourceContains()).isEmpty();
        assertThat(dto.window()).isEqualTo(PageWindow.defaultWindow());
    }

    @Test
    void builds_the_criteria_from_whichever_filters_arrived() {
        var dto = map(new SearchProtectedApplicationsRawRequest("academica", null, null, null, null, null));

        assertThat(dto.criteria().tenantId()).isEqualTo(TENANT);
        assertThat(dto.criteria().nameContains()).contains("academica");
        assertThat(dto.criteria().resourceContains()).isEmpty();
    }

    @Test
    void accepts_page_and_size() {
        assertThat(map(raw("2", "25", null, null)).window()).isEqualTo(PageWindow.ofPage(2, 25));
    }

    @Test
    void defaults_the_missing_half_of_a_paging_request() {
        assertThat(map(raw("3", null, null, null)).window())
                .isEqualTo(PageWindow.ofPage(3, PageWindow.DEFAULT_LIMIT));
        assertThat(map(raw(null, "5", null, null)).window()).isEqualTo(PageWindow.ofPage(0, 5));
    }

    @Test
    void accepts_an_explicit_offset_and_limit() {
        assertThat(map(raw(null, null, "10", "5")).window()).isEqualTo(PageWindow.ofRange(10, 5));
    }

    @Test
    void refuses_an_offset_without_a_limit_because_the_intent_is_ambiguous() {
        var request = raw(null, null, "10", null);
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(ConflictingRequestParametersException.class)
                .hasMessageContaining("juntos");
    }

    @Test
    void refuses_a_limit_without_an_offset() {
        var request = raw(null, null, null, "5");
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void refuses_paging_and_ranges_in_the_same_request() {
        var request = raw("1", "10", "10", "5");
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(ConflictingRequestParametersException.class)
                .hasMessageContaining("not both");
    }

    @Test
    void reports_a_non_numeric_window_parameter_as_a_wrong_type() {
        var request = raw("first", "10", null, null);
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo("page");
    }

    @Test
    void reports_a_window_beyond_the_protective_limit_as_out_of_range() {
        var request = raw(null, "500", null, null);
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(MalformedRequestFieldException.class)
                .hasMessageContaining("entre 1 y 100");
    }

    @Test
    void reports_a_negative_offset_as_out_of_range() {
        var request = raw(null, null, "-1", "5");
        assertThatThrownBy(() -> map(request))
                .isInstanceOf(MalformedRequestFieldException.class);
    }

    private static SearchProtectedApplicationsRawRequest raw(String page, String size, String offset, String limit) {
        return new SearchProtectedApplicationsRawRequest(null, null, page, size, offset, limit);
    }

    private static co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest map(
            SearchProtectedApplicationsRawRequest raw) {
        var request = SearchProtectedApplicationsRequestMapper.toValidatedRequest(raw);
        return SearchProtectedApplicationsRequestMapper.toRequest(request, TENANT);
    }
}
