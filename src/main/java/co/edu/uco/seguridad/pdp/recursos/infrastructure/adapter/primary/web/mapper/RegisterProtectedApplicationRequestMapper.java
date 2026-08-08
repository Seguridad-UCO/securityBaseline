package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado de aplicación sale.
 *
 * <p>Dos pasos, en este orden. {@code toValidatedRequest} recorre cada setter, y cada uno o bien
 * produce un objeto de valor o lanza una excepción de límite que nombra su campo — aquí es donde
 * ocurre realmente la validación. {@code toRequest} es entonces un simple renombrado, porque en ese
 * punto ya no queda nada por verificar.</p>
 */
public final class RegisterProtectedApplicationRequestMapper {

    private RegisterProtectedApplicationRequestMapper() {
    }

    public static RegisterProtectedApplicationRequest toValidatedRequest(RegisterProtectedApplicationRawRequest raw) {
        RegisterProtectedApplicationRequest request = new RegisterProtectedApplicationRequest();
        request.setTenantId(raw.tenantId());
        request.setApplicationName(raw.applicationName());
        request.setResourceCode(raw.resourceCode());
        request.setAction(raw.action());
        return request;
    }

    public static co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest toRequest(
            RegisterProtectedApplicationRequest request) {
        return new co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest(
                request.tenantId(),
                request.applicationName(),
                request.resourceCode(),
                request.action());
    }
}
