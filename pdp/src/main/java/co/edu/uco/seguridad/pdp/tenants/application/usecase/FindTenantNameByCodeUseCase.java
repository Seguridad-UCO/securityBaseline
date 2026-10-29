package co.edu.uco.seguridad.pdp.tenants.application.usecase;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.response.TenantNameResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Consulta por código del tenant actual; no expone el agregado. */
public interface FindTenantNameByCodeUseCase extends ReactiveOperation<TenantId, TenantNameResponse> {
}
