package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado de aplicación sale.
 *
 * <p>Cada campo se analiza de forma independiente con {@link RequestFieldParser#parse}, que delega
 * el formato al objeto de valor y nombra el campo si falla. No existe un DTO intermedio mutable:
 * a diferencia de la búsqueda, aquí no hay nada que ensamblar más allá de los cuatro objetos de
 * valor, así que un paso adicional de "validado" solo repetiría los mismos cuatro campos con los
 * mismos tipos.</p>
 */
public final class RegisterProtectedApplicationRequestMapper {

    private RegisterProtectedApplicationRequestMapper() {
    }

    public static RegisterProtectedApplicationRequest toRequest(RegisterProtectedApplicationRawRequest raw) {
        return new RegisterProtectedApplicationRequest(
                RequestFieldParser.parse("tenantId", raw.tenantId(), TenantId::new),
                RequestFieldParser.parse("applicationName", raw.applicationName(), ApplicationName::new),
                RequestFieldParser.parse("resourceCode", raw.resourceCode(), ResourceCode::new),
                RequestFieldParser.parse("action", raw.action(), ActionCode::new));
    }
}
