package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveProfileUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RemoveProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RemoveRoleUseCase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Las solicitudes de borrado transportan inquilino e identificador, y son iguales por valor. */
class RemoveUseCaseRequestTests {

    private static final TenantId TENANT = new TenantId("tenant-a");
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void profile_request_carries_tenant_and_profile() {
        var request = new RemoveProfileUseCase.Request(TENANT, new ProfileId(ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.profileId()).isEqualTo(new ProfileId(ID));
        assertThat(request).isEqualTo(new RemoveProfileUseCase.Request(TENANT, new ProfileId(ID)));
    }

    @Test
    void protected_resource_request_carries_tenant_and_resource() {
        var request = new RemoveProtectedResourceUseCase.Request(TENANT, new ResourceId(ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.resourceId()).isEqualTo(new ResourceId(ID));
        assertThat(request).isEqualTo(new RemoveProtectedResourceUseCase.Request(TENANT, new ResourceId(ID)));
    }

    @Test
    void role_request_carries_tenant_and_role() {
        var request = new RemoveRoleUseCase.Request(TENANT, new RoleId(ID));

        assertThat(request.tenantId()).isEqualTo(TENANT);
        assertThat(request.roleId()).isEqualTo(new RoleId(ID));
        assertThat(request).isEqualTo(new RemoveRoleUseCase.Request(TENANT, new RoleId(ID)));
    }
}
