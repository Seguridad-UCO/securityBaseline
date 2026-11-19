package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCredentialValidationResponseMapperTests {

    @Test
    void toResponse_flattens_the_tenant_id() {
        ApplicationCredentialValidationWebResponse response = ApplicationCredentialValidationResponseMapper
                .toResponse(new TenantId("universidad-uco"));

        assertThat(response).isEqualTo(new ApplicationCredentialValidationWebResponse("universidad-uco"));
    }
}
