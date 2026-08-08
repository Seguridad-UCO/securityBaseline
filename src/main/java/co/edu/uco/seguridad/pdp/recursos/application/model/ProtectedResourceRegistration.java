package co.edu.uco.seguridad.pdp.recursos.application.model;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;

import java.util.Objects;

/**
 * Contexto de aplicación que inspeccionan las reglas de registro de recurso: lo pedido más la
 * aplicación que realmente quedó registrada en el módulo {@code aplicaciones}.
 *
 * <p>No es una regla: es el hecho compuesto que el caso de uso arma después de registrar la
 * aplicación y pasa al {@code RulesValidator}.</p>
 */
public record ProtectedResourceRegistration(RegisterProtectedApplicationRequest dto,
                                            RegisteredApplicationResponse application) {

    public ProtectedResourceRegistration {
        Objects.requireNonNull(dto, "se requiere dto");
        Objects.requireNonNull(application, "se requiere aplicación registrada");
    }
}
