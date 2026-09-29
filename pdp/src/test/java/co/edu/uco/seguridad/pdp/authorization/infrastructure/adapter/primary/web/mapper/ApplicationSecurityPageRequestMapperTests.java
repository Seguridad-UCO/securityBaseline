package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationSecurityPageRequestMapperTests {
    @Test void maps_page_and_size() { var w = ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest("2", "15", null, null)); assertThat(w.offset()).isEqualTo(30); assertThat(w.limit()).isEqualTo(15); }
    @Test void maps_offset_and_limit() { var w = ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(null, null, "30", "15")); assertThat(w.page()).isEqualTo(2); }
    @Test void rejects_mixed_modes() { assertThatThrownBy(() -> ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest("0", null, "0", null))).isInstanceOf(ConflictingRequestParametersException.class); }
    @Test void rejects_invalid_limits() { assertThatThrownBy(() -> ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest("0", "101", null, null))).isInstanceOf(MalformedRequestFieldException.class); }
}
