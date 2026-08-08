package co.edu.uco.seguridad.pdp.aplicaciones.domain;

import co.edu.uco.seguridad.pdp.aplicaciones.domain.event.ApplicationRegistered;
import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de aplicación: Java puro, inmutable, sin setters, sin Lombok y sin anotaciones de framework.
 *
 * <p>Un registro porque el concepto es exactamente sus cuatro valores y no tiene estado oculto. La
 * fábrica {@link #register} es la ruta de creación nombrada; el constructor compacto garantiza que ninguna
 * instancia puede existir a medio construir, por lo que nada más abajo tiene que re-verificar nulos.</p>
 */
public record Application(ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {

    public Application {
        Objects.requireNonNull(id, "se requiere id de aplicación");
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
        Objects.requireNonNull(name, "se requiere nombre de aplicación");
        Objects.requireNonNull(registeredAt, "se requiere instante de registro");
    }

    public static Application register(ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {
        return new Application(id, tenantId, name, registeredAt);
    }

    /**
     * Igual que {@link #register}, pero empareja la aplicación con el evento que su registro produce
     * (ver {@link AggregateRoot}). Es la fábrica que debe usar el caso de uso de escritura;
     * {@link #register} sigue existiendo para reconstrucción desde persistencia, donde publicar un
     * evento de "se registró" cada vez que se lee una fila sería incorrecto.
     */
    public static AggregateRoot<Application, ApplicationRegistered> registerWithEvent(
            ApplicationId id, TenantId tenantId, ApplicationName name, Instant registeredAt) {
        Application application = register(id, tenantId, name, registeredAt);
        return AggregateRoot.of(application, ApplicationRegistered.of(application));
    }

    public boolean belongsTo(TenantId candidate) {
        return tenantId.equals(candidate);
    }

    public boolean isNamed(ApplicationName candidate) {
        return name.sameAs(candidate);
    }
}
