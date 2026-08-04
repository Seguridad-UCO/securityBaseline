package co.edu.uco.seguridad.applications.infrastructure;

import co.edu.uco.seguridad.applications.application.port.out.*;
import co.edu.uco.seguridad.applications.application.service.ProtectedApplicationService;
import co.edu.uco.seguridad.applications.infrastructure.audit.dummy.InMemoryAuditAdapter;
import co.edu.uco.seguridad.applications.infrastructure.persistence.dummy.*;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
class ApplicationConfiguration {
    @Bean InMemoryProtectedApplicationRepository protectedApplicationRepository() { return new InMemoryProtectedApplicationRepository(); }
    @Bean InMemoryAuditAdapter auditAdapter() { return new InMemoryAuditAdapter(); }
    @Bean ReactiveTransactionPort transactionPort(InMemoryProtectedApplicationRepository repository) { return new SnapshotReactiveTransactionAdapter(List.of(repository)); }
    @Bean ApplicationIdGenerator applicationIdGenerator() { return () -> new co.edu.uco.seguridad.applications.domain.ProtectedApplicationId(UUID.randomUUID()); }
    @Bean TimeProvider timeProvider() { return Instant::now; }
    @Bean ProtectedApplicationService protectedApplicationService(InMemoryProtectedApplicationRepository repository, InMemoryAuditAdapter audit,
            ReactiveTransactionPort transaction, ApplicationIdGenerator ids, TimeProvider clock, ObservationRegistry observations) {
        return new ProtectedApplicationService(repository, audit, transaction, ids, clock, observations);
    }
}
