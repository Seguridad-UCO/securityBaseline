package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerUserTenantAssignmentRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.authorization.domain.rule.impl.UserTenantAssignmentMustStayInActorTenantRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindUserByIdUseCase;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.audit.TestAdministrationAuditRepositories;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SU-01: reasignar el inquilino de un usuario exige ser actor de inquilino y nunca cruza inquilinos.
 * La regla real, no un doble: la prueba confirma que regla y caso de uso encajan.
 */
class AdministerUserTenantAssignmentUseCaseImplTests {

    private static final TenantId ACTOR_TENANT = new TenantId("universidad-uco");
    private static final TenantId OTHER_TENANT = new TenantId("colegio-otro");
    private static final ApplicationId GRANTED_BY = new ApplicationId(UUID.randomUUID());
    private static final UserId TARGET_USER = new UserId(UUID.randomUUID());
    private static final TenantAdministrationRequest ACTOR = new TenantAdministrationRequest(ACTOR_TENANT,
            new UserId(UUID.randomUUID()), "actor-subject", AuthenticationContextEvidence.NONE);

    private final List<AdministrationEvent> audited = new ArrayList<>();
    private final AtomicInteger assignments = new AtomicInteger();

    @Test
    void a_tenant_actor_reassigns_a_user_of_their_tenant_and_it_is_audited_as_allowed() {
        StepVerifier.create(useCase(allowed(), userIn(ACTOR_TENANT)).execute(request(ACTOR_TENANT)))
                .assertNext(user -> assertThat(user.tenantId()).isEqualTo(ACTOR_TENANT))
                .verifyComplete();

        assertThat(assignments).hasValue(1);
        assertThat(audited).singleElement().satisfies(event -> {
            assertThat(event.operation()).isEqualTo(AdministrationOperation.USER_TENANT_ASSIGNED);
            assertThat(event.outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
            assertThat(event.applicationId()).isEqualTo(GRANTED_BY);
        });
    }

    @Test
    void a_principal_that_administers_no_application_is_rejected_without_touching_the_user() {
        StepVerifier.create(useCase(deniedEvaluating(GRANTED_BY), userIn(ACTOR_TENANT)).execute(request(ACTOR_TENANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertThat(assignments).hasValue(0);
        assertThat(audited).singleElement()
                .satisfies(event -> assertThat(event.outcome()).isEqualTo(AdministrationOutcome.DENIED));
    }

    @Test
    void moving_a_user_to_another_tenant_is_rejected() {
        StepVerifier.create(useCase(allowed(), userIn(ACTOR_TENANT)).execute(request(OTHER_TENANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertThat(assignments).hasValue(0);
        assertThat(audited).singleElement()
                .satisfies(event -> assertThat(event.outcome()).isEqualTo(AdministrationOutcome.DENIED));
    }

    @Test
    void a_user_that_belongs_to_another_tenant_cannot_be_touched() {
        StepVerifier.create(useCase(allowed(), userIn(OTHER_TENANT)).execute(request(ACTOR_TENANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();

        assertThat(assignments).hasValue(0);
    }

    @Test
    void a_failing_audit_never_changes_an_allowed_result() {
        AdministerUserTenantAssignmentUseCaseImpl useCase = new AdministerUserTenantAssignmentUseCaseImpl(allowed(),
                userIn(ACTOR_TENANT), new UserTenantAssignmentMustStayInActorTenantRuleImpl(), assign(),
                TestAdministrationAuditRepositories.failing(), UUID::randomUUID, Instant::now);

        StepVerifier.create(useCase.execute(request(ACTOR_TENANT))).expectNextCount(1).verifyComplete();
    }

    private AdministerUserTenantAssignmentUseCaseImpl useCase(PrincipalMustAdministerAtLeastOneApplicationValidator gate,
                                                              FindUserByIdUseCase users) {
        AdministrationAuditRepository audit = TestAdministrationAuditRepositories.capturing(audited);
        return new AdministerUserTenantAssignmentUseCaseImpl(gate, users,
                new UserTenantAssignmentMustStayInActorTenantRuleImpl(), assign(), audit, UUID::randomUUID, Instant::now);
    }

    private AssignTenantUseCase assign() {
        return request -> Mono.fromCallable(() -> {
            assignments.incrementAndGet();
            return user(request.tenantId());
        });
    }

    private static PrincipalMustAdministerAtLeastOneApplicationValidator allowed() {
        return actor -> Mono.just(GRANTED_BY);
    }

    private static PrincipalMustAdministerAtLeastOneApplicationValidator deniedEvaluating(ApplicationId evaluated) {
        return actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(actor.tenantId(), Optional.of(evaluated)));
    }

    private static FindUserByIdUseCase userIn(TenantId tenant) {
        return id -> Mono.just(user(tenant));
    }

    private static UserResponse user(TenantId tenant) {
        return new UserResponse(TARGET_USER, "persona@uco.test", "Persona", "keycloak", tenant, Instant.EPOCH, Instant.EPOCH);
    }

    private static AdministerUserTenantAssignmentRequest request(TenantId target) {
        return new AdministerUserTenantAssignmentRequest(ACTOR, new AssignTenantRequest(TARGET_USER, target));
    }
}
