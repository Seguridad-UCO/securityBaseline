package co.edu.uco.seguridad.pdp.profiles.application.usecase;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;
public interface RemoveProfileUseCase extends ReactiveOperationWithoutResult<RemoveProfileUseCase.Request> {
    record Request(TenantId tenantId, ProfileId profileId) { }
}
