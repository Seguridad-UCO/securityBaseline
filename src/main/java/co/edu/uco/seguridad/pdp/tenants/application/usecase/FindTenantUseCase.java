package co.edu.uco.seguridad.pdp.tenants.application.usecase;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Caso de uso: buscar un inquilino por id.
 *
 * <p>Devuelve un {@code Mono} vacío para un inquilino desconocido — la ausencia es un resultado de
 * consulta legítimo, no un fallo.</p>
 */
public interface FindTenantUseCase extends ReactiveOperation<TenantId, TenantResponse> {
}
