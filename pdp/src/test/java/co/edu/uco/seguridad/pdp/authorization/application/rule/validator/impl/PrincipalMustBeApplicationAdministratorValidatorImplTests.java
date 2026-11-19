package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

/**
 * El validador no decide: traduce lo que ya decidió {@code AuthorizeAdministrationUseCase}. Fail
 * closed también ante {@code INDETERMINATE} — no solo ante {@code DENY}.
 */
class PrincipalMustBeApplicationAdministratorValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());

    @Test
    void completes_when_the_use_case_allows() {
        PrincipalMustBeApplicationAdministratorValidatorImpl validator = new PrincipalMustBeApplicationAdministratorValidatorImpl(
                request -> Mono.just(new AdministrationDecision(DecisionState.ALLOW, ReasonCode.POLICY_ALLOWED)));

        StepVerifier.create(validator.execute(REQUEST)).verifyComplete();
    }

    @Test
    void rejects_when_the_use_case_denies() {
        PrincipalMustBeApplicationAdministratorValidatorImpl validator = new PrincipalMustBeApplicationAdministratorValidatorImpl(
                request -> Mono.just(new AdministrationDecision(DecisionState.DENY, ReasonCode.POLICY_DENY)));

        StepVerifier.create(validator.execute(REQUEST))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    @Test
    void rejects_when_the_use_case_is_indeterminate() {
        PrincipalMustBeApplicationAdministratorValidatorImpl validator = new PrincipalMustBeApplicationAdministratorValidatorImpl(
                request -> Mono.just(new AdministrationDecision(DecisionState.INDETERMINATE, ReasonCode.CONTEXT_UNAVAILABLE)));

        StepVerifier.create(validator.execute(REQUEST))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }
}
