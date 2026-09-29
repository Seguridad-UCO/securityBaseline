package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-019: a diferencia de {@code AdministerProfileDefinitionInteractorImpl}, el applicationId no
 * viene en la petición — se resuelve del perfil ya existente vía {@link ProfileApplicationLookupValidator}.
 */
class AdministerProfileRoleAdditionInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ProfileId PROFILE_ID = new ProfileId(UUID.randomUUID());
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void builds_administration_present_when_the_profile_belongs_to_an_application() {
        List<AdministerProfileRoleAdditionRequest> received = new ArrayList<>();
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Docentes de matematicas"),
                RoleScope.ofApplication(new TenantId(TENANT), APPLICATION), Set.of(ROLE_ID), REGISTERED_AT);
        AdministerProfileRoleAdditionInteractorImpl interactor = new AdministerProfileRoleAdditionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup(), profileApplicationLookup(Optional.of(APPLICATION)));
        AddRoleToProfileRawRequest raw = new AddRoleToProfileRawRequest(PROFILE_ID.value().toString(), ROLE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(PROFILE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        Optional<AdministrationRequest> administration = received.getFirst().administration();
        assertThat(administration).isPresent();
        assertThat(administration.orElseThrow().applicationId()).isEqualTo(APPLICATION);
        assertThat(administration.orElseThrow().subjectUserId()).isEqualTo(USER);
    }

    @Test
    void builds_administration_empty_when_the_profile_is_tenant_scoped() {
        List<AdministerProfileRoleAdditionRequest> received = new ArrayList<>();
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinadores"),
                RoleScope.ofTenant(new TenantId(TENANT)), Set.of(ROLE_ID), REGISTERED_AT);
        AdministerProfileRoleAdditionInteractorImpl interactor = new AdministerProfileRoleAdditionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup(), profileApplicationLookup(Optional.empty()));
        AddRoleToProfileRawRequest raw = new AddRoleToProfileRawRequest(PROFILE_ID.value().toString(), ROLE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(PROFILE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration()).isEqualTo(Optional.empty());
    }

    @Test
    void propagates_profile_not_found_without_reaching_the_use_case() {
        AdministerProfileRoleAdditionUseCase useCase = input -> {
            throw new AssertionError("must not reach AdministerProfileRoleAdditionUseCase");
        };
        ProfileApplicationLookupValidator failingLookup = query -> Mono.error(new ProfileNotFoundException(query.profileId()));
        AdministerProfileRoleAdditionInteractorImpl interactor = new AdministerProfileRoleAdditionInteractorImpl(
                useCase, subjectUserIdLookup(), failingLookup);
        AddRoleToProfileRawRequest raw = new AddRoleToProfileRawRequest(PROFILE_ID.value().toString(), ROLE_ID.value().toString());

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .expectError(ProfileNotFoundException.class)
                .verify();
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static ProfileApplicationLookupValidator profileApplicationLookup(Optional<ApplicationId> result) {
        return (ProfileOwnershipQuery query) -> Mono.just(result);
    }

    private static AdministerProfileRoleAdditionUseCase useCaseCapturing(List<AdministerProfileRoleAdditionRequest> received,
                                                                         ProfileResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
