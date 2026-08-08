package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.ApplicationsModuleApi;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.recursos.application.exception.ResourceTenantMismatchException;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.AuditPort;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.impl.RegisterProtectedApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit.InMemoryAuditAdapter;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository.InMemoryProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.transaction.SnapshotReactiveTransactionAdapter;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso de uso con cada colaborador sustituido por un dummy y sin contexto de Spring, que es la
 * evidencia de que la orquestación no depende del marco.
 */
class RegisterProtectedApplicationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-07T12:00:00Z");

    private InMemoryProtectedResourceRepository resources;
    private RecordingApplicationsModule applications;
    private InMemoryAuditAdapter audit;

    @BeforeEach
    void setUp() {
        resources = new InMemoryProtectedResourceRepository();
        applications = new RecordingApplicationsModule();
        audit = new InMemoryAuditAdapter();
    }

    @Test
    void registers_the_application_and_its_first_resource() {
        StepVerifier.create(service(audit).execute(dto("estudiantes", "consultar")))
                .assertNext(entry -> {
                    assertThat(entry.tenantId()).isEqualTo(TENANT);
                    assertThat(entry.applicationName()).isEqualTo(new ApplicationName("gestion-academica"));
                    assertThat(entry.resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
                    assertThat(entry.action()).isEqualTo(new ActionCode("consultar"));
                    assertThat(entry.registeredAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(storedResources()).hasSize(1);
        assertThat(audit.recorded()).hasSize(1);
    }

    @Test
    void rolls_back_the_saved_resource_and_removes_the_application_when_audit_fails() {
        AuditPort failing = resource -> Mono.error(new IllegalStateException("audit backend unavailable"));

        StepVerifier.create(service(failing).execute(dto("estudiantes", "consultar")))
                .expectError(IllegalStateException.class)
                .verify();

        assertThat(storedResources())
                .as("the transaction port must restore the snapshot taken before the work")
                .isEmpty();
        assertThat(applications.removed)
                .as("the application lives in another module, so it is undone by explicit compensation")
                .hasSize(1);
    }

    @Test
    void refuses_a_grant_that_is_already_registered_and_leaves_nothing_behind() {
        RegisterProtectedApplicationUseCaseImpl service = service(audit);

        StepVerifier.create(service.execute(dto("estudiantes", "consultar"))).expectNextCount(1).verifyComplete();
        applications.reuseLastId = true;

        StepVerifier.create(service.execute(dto("estudiantes", "consultar")))
                .expectError(DuplicateProtectedResourceException.class)
                .verify();

        assertThat(storedResources()).hasSize(1);
    }

    @Test
    void refuses_a_registration_whose_application_belongs_to_another_tenant() {
        applications.tenantOverride = new TenantId("otra-universidad");

        StepVerifier.create(service(audit).execute(dto("estudiantes", "consultar")))
                .expectError(ResourceTenantMismatchException.class)
                .verify();

        assertThat(storedResources()).isEmpty();
    }

    private RegisterProtectedApplicationUseCaseImpl service(AuditPort auditPort) {
        TimeProvider time = () -> NOW;
        IdentifierGenerator identifiers = UUID::randomUUID;
        return new RegisterProtectedApplicationUseCaseImpl(
                applications,
                new RegisterProtectedApplicationRulesValidatorImpl(
                        new ProtectedResourceMustBelongToApplicationTenantRuleImpl(),
                        new ProtectedResourceMustBeUniqueRuleImpl(resources)),
                resources,
                auditPort,
                new SnapshotReactiveTransactionAdapter(resources),
                identifiers,
                time);
    }

    private List<?> storedResources() {
        return resources.findBy(ProtectedApplicationCriteria.unfiltered(), PageWindow.defaultWindow())
                .block()
                .content();
    }

    private static RegisterProtectedApplicationRequest dto(String resourceCode, String action) {
        return new RegisterProtectedApplicationRequest(
                TENANT,
                new ApplicationName("gestion-academica"),
                new ResourceCode(resourceCode),
                new ActionCode(action));
    }

    /** Representa el módulo Aplicaciones y registra las eliminaciones compensatorias que recibe. */
    private static final class RecordingApplicationsModule implements ApplicationsModuleApi {

        private final List<ApplicationId> removed = new ArrayList<>();
        private TenantId tenantOverride;
        private boolean reuseLastId;
        private ApplicationId lastId;

        @Override
        public Mono<RegisteredApplicationResponse> register(RegisterApplicationRequest dto) {
            ApplicationId id = reuseLastId && lastId != null ? lastId : new ApplicationId(UUID.randomUUID());
            lastId = id;
            TenantId tenant = tenantOverride == null ? dto.tenantId() : tenantOverride;
            return Mono.just(new RegisteredApplicationResponse(id, tenant, dto.name(), NOW));
        }

        @Override
        public Mono<Void> remove(ApplicationId applicationId) {
            return Mono.fromRunnable(() -> removed.add(applicationId));
        }
    }
}
