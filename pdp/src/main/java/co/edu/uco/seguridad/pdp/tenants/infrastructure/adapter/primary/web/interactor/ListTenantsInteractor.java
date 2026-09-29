package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutInput;

import java.util.List;

/**
 * Adaptador primario HTTP: lista el catálogo completo de tenants. Sin entrada: operación administrativa.
 */
public interface ListTenantsInteractor extends ReactiveOperationWithoutInput<List<TenantWebResponse>> {
}
