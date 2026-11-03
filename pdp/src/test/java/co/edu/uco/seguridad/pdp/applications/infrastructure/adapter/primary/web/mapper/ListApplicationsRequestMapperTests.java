package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListApplicationsRequestMapperTests {

    private static final TenantId UCO = new TenantId("universidad-uco");

    @Test
    void without_parameters_uses_the_default_window() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest(null, null, null, null, null));

        assertThat(request.window()).isEqualTo(PageWindow.defaultWindow());
        assertThat(request.criteria().nameContains()).isEmpty();
        assertThat(request.criteria().tenantId()).isEqualTo(UCO);
    }

    @Test
    void keeps_the_name_filter_when_it_is_present() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest("  portal  ", null, null, null, null));

        assertThat(request.criteria().nameContains()).contains("portal");
    }

    @Test
    void treats_a_blank_name_as_absent() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest("   ", null, null, null, null));

        assertThat(request.criteria().nameContains()).isEmpty();
    }

    @Test
    void resolves_a_window_from_page_and_size() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest(null, "2", "5", null, null));

        assertThat(request.window()).isEqualTo(PageWindow.ofPage(2, 5));
        assertThat(request.window().offset()).isEqualTo(10);
        assertThat(request.window().limit()).isEqualTo(5);
    }

    @Test
    void resolves_a_window_from_offset_and_limit() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest(null, null, null, "10", "5"));

        assertThat(request.window()).isEqualTo(PageWindow.ofRange(10, 5));
    }

    @Test
    void page_and_offset_windows_converge_on_the_same_result() {
        ListApplicationsRequest byPage = map(new ListApplicationsRawRequest(null, "1", "5", null, null));
        ListApplicationsRequest byRange = map(new ListApplicationsRawRequest(null, null, null, "5", "5"));

        assertThat(byPage.window()).isEqualTo(byRange.window());
    }

    @Test
    void a_lone_size_still_starts_at_the_first_page() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest(null, null, "5", null, null));

        assertThat(request.window()).isEqualTo(PageWindow.ofPage(0, 5));
    }

    @Test
    void a_lone_limit_still_starts_at_the_first_element() {
        ListApplicationsRequest request = map(new ListApplicationsRawRequest(null, null, null, null, "5"));

        assertThat(request.window()).isEqualTo(PageWindow.ofRange(0, 5));
    }

    @Test
    void rejects_mixing_page_with_offset() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, "1", null, "5", null)))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_mixing_size_with_limit() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, null, "5", null, "5")))
                .isInstanceOf(ConflictingRequestParametersException.class);
    }

    @Test
    void rejects_a_size_over_the_maximum_naming_the_field() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, null, "101", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo("size");
    }

    @Test
    void rejects_a_size_below_one() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, null, "0", null, null)))
                .isInstanceOf(MalformedRequestFieldException.class);
    }

    @Test
    void rejects_a_negative_offset_naming_the_field() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, null, null, "-1", null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo("offset");
    }

    @Test
    void rejects_a_page_that_is_not_a_number() {
        assertThatThrownBy(() -> map(new ListApplicationsRawRequest(null, "primera", null, null, null)))
                .isInstanceOf(MalformedRequestFieldException.class)
                .extracting(error -> ((MalformedRequestFieldException) error).field())
                .isEqualTo("page");
    }

    private static ListApplicationsRequest map(ListApplicationsRawRequest raw) {
        return ListApplicationsRequestMapper.toRequest(raw, UCO);
    }
}
