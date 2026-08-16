package co.edu.uco.seguridad.pdp.aplicaciones.domain.event;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.Application;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Se registró una aplicación. Sin consumidor todavía dentro de esta línea base — es la fundación de
 * la que hablará el módulo de aplicaciones el día que algo más allá de {@code recursos} necesite
 * reaccionar a un registro (ver ADR-0002).
 */
public record ApplicationRegistered(ApplicationId applicationId,
                                    TenantId tenantId,
                                    ApplicationName applicationName,
                                    Instant occurredOn) implements DomainEvent {

    public ApplicationRegistered {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationName, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.EVENT_OCCURRED_ON);
    }

    public static ApplicationRegistered of(Application application) {
        return new ApplicationRegistered(
                application.id(), application.tenantId(), application.name(), application.registeredAt());
    }
}
