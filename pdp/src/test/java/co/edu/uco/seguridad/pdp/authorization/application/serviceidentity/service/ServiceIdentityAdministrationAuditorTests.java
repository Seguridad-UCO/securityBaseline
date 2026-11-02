package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

class ServiceIdentityAdministrationAuditorTests {

    @Test
    void keeps_the_business_operation_successful_when_audit_repository_throws_synchronously() {
        AdministrationAuditRepository unavailable = new AdministrationAuditRepository() {
            @Override public reactor.core.publisher.Mono<Void> save(AdministrationEvent event) {
                throw new UnsupportedOperationException("audit unavailable");
            }

            @Override public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                return Flux.empty();
            }
        };
        AdministrationRequest administration = new AdministrationRequest(new TenantId("universidad-uco"),
                ApplicationId.of("038428cd-88a7-416a-a9de-b25e5626c88c"), new UserId(UUID.randomUUID()),
                "david", Set.of());
        ServiceIdentityAdministrationAuditor auditor = new ServiceIdentityAdministrationAuditor(unavailable,
                UUID::randomUUID, () -> Instant.parse("2026-11-01T18:00:00Z"));

        StepVerifier.create(auditor.record(administration, AdministrationOperation.SERVICE_ACCESS_REQUESTED,
                        AdministrationOutcome.ALLOWED))
                .verifyComplete();
    }
}
