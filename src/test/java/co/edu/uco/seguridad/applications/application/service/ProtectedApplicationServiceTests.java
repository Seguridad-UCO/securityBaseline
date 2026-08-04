package co.edu.uco.seguridad.applications.application.service;

import co.edu.uco.seguridad.applications.application.port.in.RegisterProtectedApplicationCommand;
import co.edu.uco.seguridad.applications.application.port.out.*;
import co.edu.uco.seguridad.applications.domain.*;
import co.edu.uco.seguridad.applications.infrastructure.persistence.dummy.*;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

class ProtectedApplicationServiceTests {
    private final InMemoryProtectedApplicationRepository repository = new InMemoryProtectedApplicationRepository();
    private final ApplicationIdGenerator ids = () -> new ProtectedApplicationId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final TimeProvider time = () -> Instant.parse("2026-08-03T00:00:00Z");
    private RegisterProtectedApplicationCommand command() { return new RegisterProtectedApplicationCommand(new ApplicationName("Billing API"), new TenantId("tenant-a"), new ResourceIdentifier("/invoices")); }
    private ProtectedApplicationService service(AuditPort audit) {
        return new ProtectedApplicationService(repository, audit, new SnapshotReactiveTransactionAdapter(List.of(repository)), ids, time, ObservationRegistry.create());
    }

    @Test void registers_an_aggregate_with_one_resource_and_audit() {
        StepVerifier.create(service(app -> Mono.empty()).register(command()))
            .assertNext(app -> { org.junit.jupiter.api.Assertions.assertEquals("tenant-a", app.tenantId().value()); org.junit.jupiter.api.Assertions.assertEquals(1, app.resources().size()); })
            .verifyComplete();
    }
    @Test void rejects_duplicate_name_inside_the_same_tenant() {
        var useCase = service(app -> Mono.empty());
        StepVerifier.create(useCase.register(command()).then(useCase.register(command())))
            .expectError(DuplicateProtectedApplicationException.class).verify();
    }
    @Test void rolls_back_save_when_audit_fails() {
        StepVerifier.create(service(app -> Mono.error(new IllegalStateException("audit unavailable"))).register(command()))
            .expectErrorMessage("audit unavailable").verify();
        StepVerifier.create(repository.findBy(new ProtectedApplicationCriteria(null, null, null), new PageWindow(0, 10)))
            .assertNext(page -> org.junit.jupiter.api.Assertions.assertEquals(0, page.total())).verifyComplete();
    }
}
