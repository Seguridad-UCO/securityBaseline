package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl.RegisterProtectedResourceRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.domain.rule.impl.ProtectedResourceMustBeUniqueRuleImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterProtectedResourceRulesValidatorTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void completes_when_no_resource_matches_the_endpoint_yet() {
        StepVerifier.create(validator(false).execute(dto())).verifyComplete();
    }

    @Test
    void fails_when_the_endpoint_is_already_registered() {
        StepVerifier.create(validator(true).execute(dto()))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(DuplicateProtectedResourceException.class)
                        .hasMessageContaining("/estudiantes"))
                .verify();
    }

    private static RegisterProtectedResourceRulesValidator validator(boolean registered) {
        return new RegisterProtectedResourceRulesValidatorImpl(new ProtectedResourceMustBeUniqueRuleImpl(),
                repositoryReporting(registered));
    }

    private static RegisterProtectedResourceRequest dto() {
        return new RegisterProtectedResourceRequest(TENANT, APPLICATION, new ResourcePath("/estudiantes"), HttpVerb.GET);
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
