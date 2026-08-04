package co.edu.uco.seguridad.applications.application.service;

import co.edu.uco.seguridad.applications.application.port.in.*;
import co.edu.uco.seguridad.applications.application.port.out.*;
import co.edu.uco.seguridad.applications.domain.*;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public final class ProtectedApplicationService implements RegisterProtectedApplicationUseCase, SearchProtectedApplicationsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ProtectedApplicationService.class);
    private final ProtectedApplicationRepository repository;
    private final AuditPort audit;
    private final ReactiveTransactionPort transaction;
    private final ApplicationIdGenerator ids;
    private final TimeProvider clock;
    private final ObservationRegistry observations;

    public ProtectedApplicationService(ProtectedApplicationRepository repository, AuditPort audit,
            ReactiveTransactionPort transaction, ApplicationIdGenerator ids, TimeProvider clock, ObservationRegistry observations) {
        this.repository = repository; this.audit = audit; this.transaction = transaction;
        this.ids = ids; this.clock = clock; this.observations = observations;
    }

    @Override public Mono<ProtectedApplication> register(RegisterProtectedApplicationCommand command) {
        return Mono.defer(() -> {
            Observation observation = Observation.start("protected_application.register", observations);
            return transaction.execute(() -> repository.existsByTenantAndName(command.tenantId(), command.name())
            .flatMap(exists -> exists ? Mono.<ProtectedApplication>error(new DuplicateProtectedApplicationException(command.name(), command.tenantId()))
                : Mono.just(ProtectedApplication.register(ids.next(), command.name(), command.tenantId(), command.resource(), clock.now())))
            .flatMap(repository::save)
            .flatMap(app -> audit.registered(app).thenReturn(app)))
            .doOnError(observation::error)
            .doFinally(signal -> observation.stop());
        })
            .transform(ReactiveLogContext.withContext(log, "protected_application.registered"))
            .name("protected_application.register");
    }

    @Override public Mono<ApplicationPage<ProtectedApplication>> search(ProtectedApplicationCriteria criteria, PageWindow window) {
        return repository.findBy(criteria, window).transform(ReactiveLogContext.withContext(log, "protected_application.searched"));
    }
}
