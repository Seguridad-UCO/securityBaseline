package co.edu.uco.seguridad.pdp.authorization.application.service.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AuthorizationSubjectFactsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveAuthorizationSubjectFactsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationContextResolverImplTests {

    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void projects_the_registered_template_and_its_entitlement_for_a_concrete_path() {
        ResolveAuthorizationSubjectFactsUseCase facts = mock(ResolveAuthorizationSubjectFactsUseCase.class);
        ProtectedResourceRepository resources = mock(ProtectedResourceRepository.class);
        ProtectedResource registered = ProtectedResource.register(RESOURCE, APPLICATION, TENANT,
                new ResourcePath("/api/v1/empleados/{id}/tipo/{tipo}"), HttpVerb.GET, Instant.now());
        when(facts.execute(any())).thenReturn(Mono.just(new AuthorizationSubjectFactsResponse(
                Set.of("LECTOR"), Set.of("Profesor"), Set.of(RESOURCE))));
        when(resources.findByApplicationPathAndMethod(any(), any(), any())).thenReturn(Mono.just(registered));

        AccessRequest request = new AccessRequest(TENANT, "subject", APPLICATION,
                new ResourcePath("/api/v1/empleados/42/tipo/contratista"), HttpVerb.GET,
                "request", "correlation", Optional.of(USER), Set.of(), Instant.now(), "local", HttpVerb.GET, "pep");

        StepVerifier.create(new AuthorizationContextResolverImpl(facts, resources).execute(request))
                .assertNext(input -> {
                    assertThat(input.resourceId()).isEqualTo("/api/v1/empleados/{id}/tipo/{tipo}");
                    assertThat(input.resourceAttributes()).containsEntry("path", "/api/v1/empleados/42/tipo/contratista")
                            .containsEntry("pathTemplate", "/api/v1/empleados/{id}/tipo/{tipo}")
                            .containsEntry("entitlementKey", "resource:" + RESOURCE.value());
                    assertThat(input.entitlements()).containsExactly("resource:" + RESOURCE.value());
                })
                .verifyComplete();
    }
}
