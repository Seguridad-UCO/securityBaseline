package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceOwnerLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ProtectedResourceOwnerLookupValidatorImplTests {

    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final ApplicationId OWNER = new ApplicationId(UUID.randomUUID());

    @Test
    void resolves_the_application_that_owns_an_existing_resource() {
        ProtectedResourceOwnerLookupValidatorImpl validator =
                new ProtectedResourceOwnerLookupValidatorImpl(repositoryReturning(Mono.just(OWNER)));

        StepVerifier.create(validator.execute(RESOURCE)).expectNext(OWNER).verifyComplete();
    }

    @Test
    void reports_not_found_when_no_application_owns_the_resource() {
        ProtectedResourceOwnerLookupValidatorImpl validator =
                new ProtectedResourceOwnerLookupValidatorImpl(repositoryReturning(Mono.empty()));

        StepVerifier.create(validator.execute(RESOURCE))
                .expectError(ProtectedResourceNotFoundException.class)
                .verify();
    }

    private static ProtectedResourceRepository repositoryReturning(Mono<ApplicationId> result) {
        return new ProtectedResourceRepository() {
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
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> deleteById(ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ApplicationId> findApplicationIdById(ResourceId resourceId) {
                return result;
            }

            @Override
            public Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
