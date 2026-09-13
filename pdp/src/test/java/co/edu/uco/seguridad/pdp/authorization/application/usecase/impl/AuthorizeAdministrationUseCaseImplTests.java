package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso de uso no decide: resuelve los roles activos del sujeto y traduce lo que ya decidió
 * {@code AdministrationDecisionPort}. Mismo espíritu que {@code AuthorizeUseCaseImplTests}: cada
 * caso comprueba una traducción, nunca una regla de negocio.
 */
class AuthorizeAdministrationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());

    @Test
    void resolves_roles_and_delegates_to_the_policy_port_returning_an_allow_decision() {
        AdministrationDecision expected = new AdministrationDecision(DecisionState.ALLOW, ReasonCode.POLICY_ALLOWED);
        List<AdministrationRequest> received = new ArrayList<>();
        AuthorizeAdministrationUseCaseImpl useCase = new AuthorizeAdministrationUseCaseImpl(
                request -> {
                    assertThat(request).isEqualTo(new ResolveActiveRolesRequest(USER, APPLICATION));
                    return Mono.just(Set.of("Administrador de aplicación"));
                },
                request -> {
                    received.add(request);
                    return Mono.just(expected);
                });

        StepVerifier.create(useCase.execute(REQUEST)).expectNext(expected).verifyComplete();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst().subjectRoles()).containsExactly("Administrador de aplicación");
    }

    @Test
    void delegates_to_the_policy_port_returning_a_deny_decision() {
        AdministrationDecision expected = new AdministrationDecision(DecisionState.DENY, ReasonCode.POLICY_DENY);
        AuthorizeAdministrationUseCaseImpl useCase = new AuthorizeAdministrationUseCaseImpl(
                request -> Mono.just(Set.of()), request -> Mono.just(expected));

        StepVerifier.create(useCase.execute(REQUEST)).expectNext(expected).verifyComplete();
    }

    @Test
    void reports_indeterminate_when_the_policy_port_fails() {
        AuthorizeAdministrationUseCaseImpl useCase = new AuthorizeAdministrationUseCaseImpl(
                request -> Mono.just(Set.of()), request -> Mono.error(new RuntimeException("OPA unreachable")));

        StepVerifier.create(useCase.execute(REQUEST))
                .assertNext(decision -> {
                    assertThat(decision.state()).isEqualTo(DecisionState.INDETERMINATE);
                    assertThat(decision.reasonCode()).isEqualTo(ReasonCode.CONTEXT_UNAVAILABLE);
                })
                .verifyComplete();
    }
}
