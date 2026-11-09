package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.ProtectedResourceMustExistValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustExistRuleImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ProtectedResourceMustExistValidatorTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourcePath PATH = new ResourcePath("/estudiantes");

    @Test
    void completes_when_the_resource_is_registered() {
        StepVerifier.create(validator(true).execute(lookup())).verifyComplete();
    }

    @Test
    void refuses_a_resource_that_is_not_registered() {
        StepVerifier.create(validator(false).execute(lookup()))
                .expectError(ProtectedResourceNotFoundException.class)
                .verify();
    }

    private static ProtectedResourceMustExistValidator validator(boolean registered) {
        return new ProtectedResourceMustExistValidatorImpl(repositoryReporting(registered),
                new ProtectedResourceMustExistRuleImpl());
    }

    private static ProtectedResourceLookup lookup() {
        return new ProtectedResourceLookup(APPLICATION, PATH, HttpVerb.GET);
    }

    private static ProtectedResourceRepository repositoryReporting(boolean exists) {
        return new ProtectedResourceRepository() {
            @Override
            public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
                                                                  HttpVerb method) {
                return Mono.just(exists);
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
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
