package co.edu.uco.seguridad.pdp.resources.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code ProtectedResourceMustBeUniqueRule}: la terna aplicación+ruta+método
 * y si ya estaba registrada. La unicidad es sobre la terna, no sobre la ruta sola.
 */
public record ProtectedResourceAvailability(ApplicationId applicationId, ResourcePath path, HttpVerb method,
                                            boolean alreadyRegistered) {

    public ProtectedResourceAvailability {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(path, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
    }
}
