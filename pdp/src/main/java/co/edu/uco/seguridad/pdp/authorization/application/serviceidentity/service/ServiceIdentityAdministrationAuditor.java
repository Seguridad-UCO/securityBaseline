package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/** Registra intentos M2M sin exponer secretos ni cambiar el resultado de la operación. */
public final class ServiceIdentityAdministrationAuditor {
    private final AdministrationAuditRepository audit;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public ServiceIdentityAdministrationAuditor(AdministrationAuditRepository audit, IdentifierGenerator identifiers,
                                                TimeProvider time) {
        this.audit = Objects.requireNonNull(audit);
        this.identifiers = Objects.requireNonNull(identifiers);
        this.time = Objects.requireNonNull(time);
    }

    public Mono<Void> record(AdministrationRequest administration, AdministrationOperation operation,
                             AdministrationOutcome outcome) {
        UUID eventId = identifiers.next();
        AdministrationEvent event = new AdministrationEvent(eventId, eventId.toString(), administration.tenantId(),
                administration.applicationId(), administration.subject(), operation, outcome, time.now());
        return audit.save(event).onErrorResume(error -> Mono.empty());
    }
}
