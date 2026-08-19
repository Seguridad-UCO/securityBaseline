package co.edu.uco.seguridad.pdp.resources.application.model;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Contexto que inspeccionan las reglas de registro de recurso: lo pedido más la aplicación que
 * realmente quedó registrada. No es una regla, es el hecho compuesto que el caso de uso arma y pasa
 * al {@code RulesValidator}.
 */
public record ProtectedResourceRegistration(RegisterProtectedApplicationRequest dto,
                                            RegisteredApplicationResponse application) {

    public ProtectedResourceRegistration {
        Objects.requireNonNull(dto, RequiredArgumentMessages.DTO);
        Objects.requireNonNull(application, RequiredArgumentMessages.REGISTERED_APPLICATION);
    }
}
