package co.edu.uco.seguridad.pdp.authorization.infrastructure.properties;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Cliente OPA de HU-006 (D10 del handoff PDP-PEP-OPA): mismo estilo de nombrado simétrico que
 * {@code pep.pdp.*} en el PEP.
 *
 * @param baseUrl      base HTTP de la instancia OPA (p. ej. {@code http://localhost:8181})
 * @param decisionPath ruta del endpoint de decisión (p. ej. {@code /v1/data/security/authorization/decision})
 * @param timeout      tiempo máximo de espera de una evaluación antes de tratarla como {@code CONTEXT_UNAVAILABLE}
 */
@ConfigurationProperties(prefix = "pdp.opa")
public record OpaProperties(String baseUrl, String decisionPath, Duration timeout) {

    public OpaProperties {
        Objects.requireNonNull(baseUrl, RequiredArgumentMessages.OPA_BASE_URL);
        Objects.requireNonNull(decisionPath, RequiredArgumentMessages.OPA_DECISION_PATH);
        Objects.requireNonNull(timeout, RequiredArgumentMessages.OPA_TIMEOUT);
    }
}
