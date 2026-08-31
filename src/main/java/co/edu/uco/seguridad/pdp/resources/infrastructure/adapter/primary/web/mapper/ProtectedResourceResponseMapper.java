package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;

import java.util.List;

/** Traducción de salida: DTO de aplicación a carga útil HTTP. Desenvuelve los objetos de valor. */
public final class ProtectedResourceResponseMapper {

    private ProtectedResourceResponseMapper() {
    }

    public static ProtectedResourceWebResponse toResponse(RegisteredProtectedResourceResponse response) {
        return new ProtectedResourceWebResponse(response.id().value().toString(),
                response.applicationId().value().toString(), response.tenantId().value(),
                response.path().value(), response.method().name(), response.registeredAt());
    }

    public static List<ProtectedResourceWebResponse> toResponseList(List<RegisteredProtectedResourceResponse> responses) {
        return responses.stream().map(ProtectedResourceResponseMapper::toResponse).toList();
    }
}
