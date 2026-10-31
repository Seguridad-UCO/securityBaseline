package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
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
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-019 + HU-036: el gate que se exige sale del alcance de la petición. Un perfil {@code APPLICATION} exige
 * administrar su aplicación (sale del propio scope, sin lookup); un perfil {@code TENANT} exige el actor de
 * inquilino — nunca una petición sin gate: el tipo {@link CatalogAdministration} no lo puede representar.
 */
class AdministerProfileDefinitionInteractorImplTests {

    private static final String TENANT = "universidad-uco";
    private static final String SUBJECT = "keycloak-subject-123";
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ProfileId PROFILE_ID = new ProfileId(UUID.randomUUID());
    private static final String APPLICATION_ID = UUID.randomUUID().toString();
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void builds_the_application_gate_for_an_application_scoped_profile() {
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
        assertThat(received.getFirst().administration()).isInstanceOfSatisfying(CatalogAdministration.OfApplication.class,
                gate -> {
                    assertThat(gate.administration().applicationId().value().toString()).isEqualTo(APPLICATION_ID);
                    assertThat(gate.administration().subjectUserId()).isEqualTo(USER);
                    assertThat(gate.administration().tenantId().value()).isEqualTo(TENANT);
                    assertThat(gate.administration().subject()).isEqualTo(SUBJECT);
                });
    }

    @Test
    void builds_the_tenant_gate_for_a_tenant_scoped_profile() {
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
        assertThat(received.getFirst().administration()).isInstanceOf(CatalogAdministration.OfTenant.class);
    }

    @Test
    void the_tenant_gate_carries_tenant_user_subject_and_authentication_of_the_principal() {
        List<AdministerProfileDefinitionRequest> received = new ArrayList<>();
        ProfileResponse response = new ProfileResponse(PROFILE_ID, new ProfileName("Coordinadores"),
                RoleScope.ofTenant(new TenantId(TENANT)), Set.of(), REGISTERED_AT);
        AdministerProfileDefinitionInteractorImpl interactor = new AdministerProfileDefinitionInteractorImpl(
                useCaseCapturing(received, response), subjectUserIdLookup());
        DefineProfileRawRequest raw = new DefineProfileRawRequest("Coordinadores", "TENANT", null);
        List<String> amr = List.of("otp");

        StepVerifier.create(interactor.execute(raw).contextWrite(principalWithEvidence("urn:mfa:otp", amr)))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(received.getFirst().administration()).isEqualTo(new CatalogAdministration.OfTenant(
                new TenantAdministrationRequest(new TenantId(TENANT), USER, SUBJECT,
                        new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), amr))));
    }

    private static Context principalWithEvidence(String acr, List<String> amr) {
        return ReactiveSecurityContextHolder.withAuthentication(new JwtAuthenticationToken(
                TestJwtSupport.jwtWithAuthenticationContext(TENANT, SUBJECT, acr, amr)));
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
