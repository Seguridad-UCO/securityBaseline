package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterProtectedResourceUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void refuses_to_register_a_resource_under_an_application_that_does_not_belong_to_the_tenant() {
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        RegisterProtectedResourceUseCaseImpl useCase = new RegisterProtectedResourceUseCaseImpl(
                query -> Mono.error(new ApplicationNotFoundException(query.applicationId())),
                dto -> Mono.empty(), unreachableResourceRepository(), events(),
                UUID::randomUUID, () -> NOW);

        RegisterProtectedResourceRequest request = new RegisterProtectedResourceRequest(
                TENANT, applicationId, new ResourcePath("/estudiantes"), HttpVerb.GET);

        StepVerifier.create(useCase.execute(request))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void registers_the_resource_and_publishes_the_registration_event() {
        Application application = Application.register(new ApplicationId(UUID.randomUUID()), TENANT,
                new ApplicationName("gestion-academica"), "", new ApplicationBaseUrl("https://example.com"), NOW);
        List<ProtectedResource> saved = new ArrayList<>();
        ProtectedResourceRepository resources = new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
                    HttpVerb method) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                saved.add(resource);
                return Mono.just(resource);
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }
        };
        List<DomainEvent> published = new ArrayList<>();
        UUID fixedId = UUID.randomUUID();
        RegisterProtectedResourceUseCaseImpl useCase = new RegisterProtectedResourceUseCaseImpl(
                query -> Mono.empty(), dto -> Mono.empty(), resources, event -> {
                    published.add(event);
                    return Mono.empty();
                }, () -> fixedId, () -> NOW);

        RegisterProtectedResourceRequest request = new RegisterProtectedResourceRequest(
                TENANT, application.id(), new ResourcePath("/estudiantes"), HttpVerb.GET);

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(new ResourceId(fixedId));
                    assertThat(response.applicationId()).isEqualTo(application.id());
                    assertThat(response.path()).isEqualTo(new ResourcePath("/estudiantes"));
                    assertThat(response.method()).isEqualTo(HttpVerb.GET);
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(published).hasSize(1);
    }


    private static ProtectedResourceRepository unreachableResourceRepository() {
        return new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
                    HttpVerb method) {
                throw new AssertionError("must not be reached when the application lookup fails");
            }

            @Override
            public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProtectedResource> save(ProtectedResource resource) {
                throw new AssertionError("must not be reached when the application lookup fails");
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DomainEventPublisher events() {
        return event -> Mono.empty();
    }
}
