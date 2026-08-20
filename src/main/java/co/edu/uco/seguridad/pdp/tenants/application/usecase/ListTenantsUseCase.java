package co.edu.uco.seguridad.pdp.tenants.application.usecase;

import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

/** Puerto primario: listar el catálogo completo de tenants. Operación administrativa, no por-tenant. */
public interface ListTenantsUseCase extends ReactiveOperationWithoutInput<java.util.List<TenantResponse>> {
}
