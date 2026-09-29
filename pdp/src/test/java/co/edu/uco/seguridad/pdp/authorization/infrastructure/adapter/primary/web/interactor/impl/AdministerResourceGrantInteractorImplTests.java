package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceGrantUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-016: a diferencia de {@code AdministerRoleDefinitionInteractorImpl}, el applicationId no viene
 * en la petición — se resuelve del rol ya existente vía {@link RoleApplicationLookupValidator}.
 */
class AdministerResourceGrantInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final ResourceId RESOURCE_ID = new ResourceId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    void builds_administration_present_when_the_role_belongs_to_an_application() {
        List<AdministerResourceGrantRequest> received = new ArrayList<>();
        RoleResponse response = new RoleResponse(ROLE_ID, new RoleName("Docente"),
                RoleScope.ofApplication(new TenantId(TENANT), APPLICATION), Set.of(RESOURCE_ID), REGISTERED_AT);
        AdministerResourceGrantInteractorImpl interactor = new AdministerResourceGrantInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup(), roleApplicationLookup(Optional.of(APPLICATION)));
        GrantResourceRawRequest raw = new GrantResourceRawRequest(ROLE_ID.value().toString(), RESOURCE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(ROLE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        Optional<AdministrationRequest> administration = received.getFirst().administration();
        assertThat(administration).isPresent();
        assertThat(administration.orElseThrow().applicationId()).isEqualTo(APPLICATION);
        assertThat(administration.orElseThrow().subjectUserId()).isEqualTo(USER);
    }

    @Test
    void builds_administration_empty_when_the_role_is_tenant_scoped() {
        List<AdministerResourceGrantRequest> received = new ArrayList<>();
        RoleResponse response = new RoleResponse(ROLE_ID, new RoleName("Coordinador"),
                RoleScope.ofTenant(new TenantId(TENANT)), Set.of(RESOURCE_ID), REGISTERED_AT);
        AdministerResourceGrantInteractorImpl interactor = new AdministerResourceGrantInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup(), roleApplicationLookup(Optional.empty()));
        GrantResourceRawRequest raw = new GrantResourceRawRequest(ROLE_ID.value().toString(), RESOURCE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(ROLE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration()).isEqualTo(Optional.empty());
    }

    @Test
    void propagates_role_not_found_without_reaching_the_use_case() {
        AdministerResourceGrantUseCase useCase = input -> {
            throw new AssertionError("must not reach AdministerResourceGrantUseCase");
        };
        RoleApplicationLookupValidator failingLookup = query -> Mono.error(new RoleNotFoundException(query.roleId()));
        AdministerResourceGrantInteractorImpl interactor = new AdministerResourceGrantInteractorImpl(
                useCase, subjectUserIdLookup(), failingLookup);
        GrantResourceRawRequest raw = new GrantResourceRawRequest(ROLE_ID.value().toString(), RESOURCE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .expectError(RoleNotFoundException.class)
                .verify();
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static RoleApplicationLookupValidator roleApplicationLookup(Optional<ApplicationId> result) {
        return (RoleOwnershipQuery query) -> Mono.just(result);
    }

    private static AdministerResourceGrantUseCase useCaseCapturing(List<AdministerResourceGrantRequest> received,
                                                                   RoleResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
