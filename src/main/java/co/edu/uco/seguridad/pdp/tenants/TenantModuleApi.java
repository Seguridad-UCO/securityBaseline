package co.edu.uco.seguridad.pdp.tenants;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import reactor.core.publisher.Mono;

/**
 * Contrato de búsqueda publicado de Inquilinos: responde <em>qué</em> es un inquilino y nunca decide
 * si se permite una operación. Esa decisión pertenece a {@link TenantMustBeActiveRule}.
 *
 * <p>Devuelve un {@code Mono} vacío para un inquilino desconocido en lugar de lanzar una excepción, porque la ausencia es
 * un resultado de consulta legítimo, no un fallo.</p>
 */
public interface TenantModuleApi {

    Mono<TenantResponse> find(TenantId tenantId);
}
