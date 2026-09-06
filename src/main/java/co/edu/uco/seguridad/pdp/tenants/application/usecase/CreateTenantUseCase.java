package co.edu.uco.seguridad.pdp.tenants.application.usecase;

import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Puerto primario: dar de alta un nuevo tenant. */
public interface CreateTenantUseCase extends ReactiveOperation<CreateTenantRequest, TenantResponse> {
}
