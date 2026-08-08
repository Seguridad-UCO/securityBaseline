package co.edu.uco.seguridad.pdp.tenants.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario publicado: consultar un inquilino por id.
 * Un solo {@code execute}; la decisión de "activo" pertenece a {@code TenantMustBeActiveRule}.
 */
public interface FindTenantInteractor extends ReactiveOperation<TenantId, TenantResponse> {
}
