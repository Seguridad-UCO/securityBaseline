package co.edu.uco.seguridad.pdp.resources.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.exception.DuplicateProtectedResourceException;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class ProtectedResourceMustBeUniqueRuleImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());

    @Test
    void completes_when_no_resource_matches_the_endpoint_yet() {
        ProtectedResourceMustBeUniqueRuleImpl rule = new ProtectedResourceMustBeUniqueRuleImpl(repositoryReporting(false));

        StepVerifier.create(rule.execute(dto())).verifyComplete();
    }

    @Test
    void fails_when_the_endpoint_is_already_registered() {
        ProtectedResourceMustBeUniqueRuleImpl rule = new ProtectedResourceMustBeUniqueRuleImpl(repositoryReporting(true));

        StepVerifier.create(rule.execute(dto()))
                .expectErrorSatisfies(error -> org.assertj.core.api.Assertions.assertThat(error)
                        .isInstanceOf(DuplicateProtectedResourceException.class)
                        .hasMessageContaining("/estudiantes"))
                .verify();
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
            public Mono<Void> deleteById(co.edu.uco.seguridad.pdp.commons.ResourceId resourceId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
