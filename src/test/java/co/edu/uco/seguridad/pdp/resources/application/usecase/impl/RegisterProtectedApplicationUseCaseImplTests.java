package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.application.exception.ResourceTenantMismatchException;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.application.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rule.impl.ProtectedResourceMustBelongToApplicationTenantRuleImpl;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.impl.RegisterProtectedApplicationRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.pdp.resources.testsupport.FakeProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
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
 * El caso de uso con cada colaborador sustituido por un dummy y sin contexto de Spring.
 */
class RegisterProtectedApplicationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-07T12:00:00Z");

    private FakeProtectedResourceRepository resources;
    private RecordingRegisterApplicationUseCase registerApplication;
    private RecordingRemoveApplicationUseCase removeApplication;
    private RecordingEventPublisher events;
    private TenantMustBeActiveRule activeTenant;

    @BeforeEach
    void setUp() {
        resources = new FakeProtectedResourceRepository();
        registerApplication = new RecordingRegisterApplicationUseCase();
        removeApplication = new RecordingRemoveApplicationUseCase();
        events = new RecordingEventPublisher();
        activeTenant = tenantId -> TENANT.equals(tenantId)
                ? Mono.just(new TenantResponse(tenantId, TenantStatus.ACTIVE))
                : Mono.error(new TenantNotFoundException(tenantId));
    }

    @Test
    void registers_the_application_and_publishes_its_registration_event() {
        StepVerifier.create(service(events).execute(dto("estudiantes", "consultar")))
                .assertNext(entry -> {
                    assertThat(entry.tenantId()).isEqualTo(TENANT);
                    assertThat(entry.applicationName()).isEqualTo(new ApplicationName("gestion-academica"));
                    assertThat(entry.code()).isEqualTo(new ResourceCode("estudiantes"));
                    assertThat(entry.action()).isEqualTo(new ActionCode("consultar"));
                    assertThat(entry.registeredAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(storedResources()).hasSize(1);
        assertThat(events.published).hasSize(1);
        assertThat(events.published.get(0)).isInstanceOfSatisfying(ProtectedResourceRegistered.class, event -> {
            assertThat(event.tenantId()).isEqualTo(TENANT);
            assertThat(event.resourceCode()).isEqualTo(new ResourceCode("estudiantes"));
            assertThat(event.action()).isEqualTo(new ActionCode("consultar"));
        });
    }

    @Test
    void rolls_back_the_saved_resource_and_removes_the_application_when_event_publication_fails() {
        RecordingEventPublisher failing = new RecordingEventPublisher();
        failing.failWith(new IllegalStateException("event publication unavailable"));

        StepVerifier.create(service(failing).execute(dto("estudiantes", "consultar")))
                .expectError(IllegalStateException.class)
                .verify();

        assertThat(storedResources())
                .as("a failure after the save must trigger the explicit compensating delete")
                .isEmpty();
        assertThat(removeApplication.removed)
                .as("the application lives in another module, so it is undone by explicit compensation")
                .hasSize(1);
    }

    @Test
    void refuses_a_grant_that_is_already_registered_and_leaves_nothing_behind() {
        RegisterProtectedApplicationUseCaseImpl service = service(events);

        StepVerifier.create(service.execute(dto("estudiantes", "consultar"))).expectNextCount(1).verifyComplete();
        registerApplication.reuseLastId = true;

        StepVerifier.create(service.execute(dto("estudiantes", "consultar")))
                .expectError(DuplicateProtectedResourceException.class)
                .verify();

        assertThat(storedResources()).hasSize(1);
    }

    @Test
    void refuses_a_registration_whose_application_belongs_to_another_tenant() {
        registerApplication.tenantOverride = new TenantId("otra-universidad");

        StepVerifier.create(service(events).execute(dto("estudiantes", "consultar")))
                .expectError(ResourceTenantMismatchException.class)
                .verify();

        assertThat(storedResources()).isEmpty();
    }

    @Test
    void surfaces_the_original_failure_even_when_compensation_itself_fails() {
        RecordingEventPublisher failing = new RecordingEventPublisher();
        IllegalStateException originalFailure = new IllegalStateException("event publication unavailable");
        failing.failWith(originalFailure);
        removeApplication.failWith(new IllegalStateException("compensation is down too"));

        StepVerifier.create(service(failing).execute(dto("estudiantes", "consultar")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .as("the client must see why registration failed, not why cleanup failed")
                        .isSameAs(originalFailure))
                .verify();
    }

    @Test
    void refuses_to_register_a_protected_resource_for_a_tenant_that_is_not_active() {
        activeTenant = tenantId -> Mono.error(new TenantNotActiveException(tenantId, TenantStatus.SUSPENDED));

        StepVerifier.create(service(events).execute(dto("estudiantes", "consultar")))
                .expectError(TenantNotActiveException.class)
                .verify();

        assertThat(storedResources())
                .as("the resource must not be created before the tenant-active rule runs")
                .isEmpty();
    }

    private RegisterProtectedApplicationUseCaseImpl service(DomainEventPublisher eventPublisher) {
        TimeProvider time = () -> NOW;
        IdentifierGenerator identifiers = UUID::randomUUID;
        return new RegisterProtectedApplicationUseCaseImpl(
                registerApplication,
                removeApplication,
                new RegisterProtectedApplicationRulesValidatorImpl(
                        activeTenant,
                        new ProtectedResourceMustBelongToApplicationTenantRuleImpl(),
                        new ProtectedResourceMustBeUniqueRuleImpl(resources)),
                resources,
                eventPublisher,
                identifiers,
                time);
    }

    private List<?> storedResources() {
        return resources.findBy(ProtectedApplicationCriteria.scopedTo(TENANT), PageWindow.defaultWindow())
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

    private static final class RecordingRegisterApplicationUseCase implements RegisterApplicationUseCase {

        private TenantId tenantOverride;
        private boolean reuseLastId;
        private ApplicationId lastId;

        @Override
        public Mono<RegisteredApplicationResponse> execute(RegisterApplicationRequest dto) {
            ApplicationId id = reuseLastId && lastId != null ? lastId : new ApplicationId(UUID.randomUUID());
            lastId = id;
            TenantId tenant = tenantOverride == null ? dto.tenantId() : tenantOverride;
            return Mono.just(new RegisteredApplicationResponse(id, tenant, dto.name(), NOW));
        }
    }

    private static final class RecordingRemoveApplicationUseCase implements RemoveApplicationUseCase {

        private final List<ApplicationId> removed = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException error) {
            this.failure = error;
        }

        @Override
        public Mono<Void> execute(ApplicationId applicationId) {
            if (failure != null) {
                return Mono.error(failure);
            }
            return Mono.fromRunnable(() -> removed.add(applicationId));
        }
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException error) {
            this.failure = error;
        }

        @Override
        public Mono<Void> publish(DomainEvent event) {
            if (failure != null) {
                return Mono.error(failure);
            }
            published.add(event);
            return Mono.empty();
        }
    }
}
