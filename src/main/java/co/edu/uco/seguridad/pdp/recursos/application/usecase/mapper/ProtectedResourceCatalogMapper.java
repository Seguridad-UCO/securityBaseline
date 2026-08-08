package co.edu.uco.seguridad.pdp.recursos.application.usecase.mapper;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;

/**
 * Mapper Entity → DTO del caso de uso: proyecta el agregado de dominio al DTO del puerto primario.
 *
 * <p>Campo por campo y libre de decisiones — si necesitara un {@code if} de negocio, esa lógica
 * pertenecería a una regla.</p>
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
