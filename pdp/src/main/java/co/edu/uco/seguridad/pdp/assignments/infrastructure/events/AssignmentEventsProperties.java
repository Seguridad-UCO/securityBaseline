package co.edu.uco.seguridad.pdp.assignments.infrastructure.events;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("pdp.assignment-events")
public record AssignmentEventsProperties(String exchange, Duration dispatchDelay, Duration confirmTimeout) {
    public AssignmentEventsProperties {
        exchange = exchange == null || exchange.isBlank() ? "security.assignment.events" : exchange;
        dispatchDelay = dispatchDelay == null ? Duration.ofSeconds(5) : dispatchDelay;
        confirmTimeout = confirmTimeout == null ? Duration.ofSeconds(5) : confirmTimeout;
    }
}
