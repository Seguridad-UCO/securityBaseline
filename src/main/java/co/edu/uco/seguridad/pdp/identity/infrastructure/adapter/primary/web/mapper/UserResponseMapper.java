package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;

import java.util.List;

/** Traducción de salida: DTO de aplicación a carga útil HTTP. Desenvuelve los objetos de valor. */
public final class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static UserWebResponse toResponse(UserResponse response) {
        return new UserWebResponse(response.id().value().toString(), response.email(), response.name(),
                response.provider(), response.tenantId().value(), response.createdAt(), response.lastLoginAt());
    }

    public static List<UserWebResponse> toResponseList(List<UserResponse> responses) {
        return responses.stream().map(UserResponseMapper::toResponse).toList();
    }
}
