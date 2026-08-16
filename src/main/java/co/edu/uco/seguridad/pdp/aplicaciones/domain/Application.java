package co.edu.uco.seguridad.pdp.aplicaciones.domain;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.event.ApplicationRegistered;
import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.time.Instant;
import java.util.Objects;

/** Entidad de aplicación: Java puro, inmutable, sin setters ni anotaciones de framework. */
public record Application(ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {

    public Application {
        Objects.requireNonNull(id, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }

    public static Application register(ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {
        return new Application(id, tenantId, name, registeredAt);
    }

    /** Igual que {@link #register}, pero empareja la aplicación con su evento — usar al escribir, no al leer. */
    public static AggregateRoot<Application, ApplicationRegistered> registerWithEvent(
            ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {
        Application application = register(id, tenantId, name, registeredAt);
        return AggregateRoot.of(application, ApplicationRegistered.of(application));
    }
}
