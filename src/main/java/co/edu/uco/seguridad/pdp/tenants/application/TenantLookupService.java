package co.edu.uco.seguridad.pdp.tenants.application;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Fachada del módulo {@code tenants}: implementa {@link TenantModuleApi} delegando al caso de uso
 * de búsqueda, igual que {@code ApplicationsService} en el módulo de aplicaciones.
 *
 * <p>No contiene ninguna regla: decidir qué significa un inquilino inactivo es trabajo de
 * {@code TenantMustBeActiveRule}.</p>
 */
public final class TenantLookupService implements TenantModuleApi {

    private final FindTenantUseCase findTenantUseCase;

    public TenantLookupService(FindTenantUseCase findTenantUseCase) {
        this.findTenantUseCase = Objects.requireNonNull(findTenantUseCase, "se requiere caso de uso de búsqueda");
    }

    @Override
    public Mono<TenantResponse> find(TenantId tenantId) {
        return findTenantUseCase.execute(tenantId);
    }
}
