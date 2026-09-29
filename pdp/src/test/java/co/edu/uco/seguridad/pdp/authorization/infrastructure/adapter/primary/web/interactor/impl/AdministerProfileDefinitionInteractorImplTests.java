package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
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
 * HU-019: construye la solicitud de administración solo cuando el perfil es de alcance
 * APPLICATION — el applicationId sale del propio scope de la petición, sin lookup.
 */
class AdministerProfileDefinitionInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ProfileId PROFILE_ID = new ProfileId(UUID.randomUUID());
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void builds_administration_present_for_an_application_scoped_profile() {
        List<AdministerProfileDefinitionRequest> received = new ArrayList<>();
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Docentes de matematicas"),
                RoleScope.ofApplication(new TenantId(TENANT), ApplicationId.of(APPLICATION_ID)), Set.of(), REGISTERED_AT);
        AdministerProfileDefinitionInteractorImpl interactor = new AdministerProfileDefinitionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        DefineProfileRawRequest raw = new DefineProfileRawRequest("Docentes de matematicas", "APPLICATION", APPLICATION_ID);

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(PROFILE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        Optional<AdministrationRequest> administration = received.getFirst().administration();
        assertThat(administration).isPresent();
        assertThat(administration.orElseThrow().applicationId().value().toString()).isEqualTo(APPLICATION_ID);
        assertThat(administration.orElseThrow().subjectUserId()).isEqualTo(USER);
        assertThat(administration.orElseThrow().tenantId().value()).isEqualTo(TENANT);
    }

    @Test
    void builds_administration_empty_for_a_tenant_scoped_profile() {
        List<AdministerProfileDefinitionRequest> received = new ArrayList<>();
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinadores"),
                RoleScope.ofTenant(new TenantId(TENANT)), Set.of(), REGISTERED_AT);
        AdministerProfileDefinitionInteractorImpl interactor = new AdministerProfileDefinitionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        DefineProfileRawRequest raw = new DefineProfileRawRequest("Coordinadores", "TENANT", null);

        StepVerifier.create(interactor.execute(raw).contextWrite(TestJwtSupport.withPrincipal(TENANT, SUBJECT)))
                .assertNext(webResponse -> assertThat(webResponse.id()).isEqualTo(PROFILE_ID.value().toString()))
                .verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().administration()).isEqualTo(Optional.empty());
    }

    private static SubjectUserIdLookupValidator subjectUserIdLookup() {
        return subject -> Mono.just(USER);
    }

    private static AdministerProfileDefinitionUseCase useCaseCapturing(List<AdministerProfileDefinitionRequest> received,
                                                                       ProfileResponse response) {
        return input -> {
            received.add(input);
            return Mono.just(response);
        };
    }
}
