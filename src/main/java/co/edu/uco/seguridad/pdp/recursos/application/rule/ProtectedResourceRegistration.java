package co.edu.uco.seguridad.pdp.recursos.application.rule;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;

import java.util.Objects;

/**
 * El hecho que inspeccionan las reglas de recurso: lo que se pidió, más la aplicación que realmente
 * se registró.
 *
 * <p>Existe porque estas reglas comparan ambos — una regla que solo viera el DTO no podría
 * detectar una aplicación perteneciente a otro inquilino.</p>
 */
public record ProtectedResourceRegistration(RegisterProtectedApplicationRequest dto,
                                            RegisteredApplicationResponse application) {

    public ProtectedResourceRegistration {
        Objects.requireNonNull(dto, "se requiere dto");
        Objects.requireNonNull(application, "se requiere aplicación registrada");
    }
}
