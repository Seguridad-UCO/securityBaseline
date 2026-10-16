package co.edu.uco.seguridad.pep.ingress.application.port.secondary;

import reactor.core.publisher.Mono;

/**
 * Validates, through the PDP internal channel, the credential presented by an application.
 */
public interface ApplicationCredentialValidationPort {
    Mono<Void> validate(String applicationName, String secret);

    /** Validación contextual para integraciones que declaran ambiente. */
    default Mono<Void> validate(String applicationName, String environment, String secret) {
        return validate(applicationName, secret);
    }
}
