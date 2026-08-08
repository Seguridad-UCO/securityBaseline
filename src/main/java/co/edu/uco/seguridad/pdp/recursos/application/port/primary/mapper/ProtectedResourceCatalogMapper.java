package co.edu.uco.seguridad.pdp.recursos.application.port.primary.mapper;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;

/**
 * Proyección dominio → DTO de salida del puerto primario.
 * La usa el interactor tras ejecutar el caso de uso.
 */
public final class ProtectedResourceCatalogMapper {

    private ProtectedResourceCatalogMapper() {
    }

    public static ProtectedApplicationResponse toResponse(ProtectedResource resource) {
        return new ProtectedApplicationResponse(
                resource.applicationId(),
                resource.id(),
                resource.tenantId(),
                resource.applicationName(),
                resource.code(),
                resource.action(),
                resource.registeredAt());
    }
}
