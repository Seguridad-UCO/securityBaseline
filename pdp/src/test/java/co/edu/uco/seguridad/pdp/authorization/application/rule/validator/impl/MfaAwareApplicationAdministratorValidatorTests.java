package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import co.edu.uco.seguridad.shared.security.mfa.MfaEvidenceProperties;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-HU-024.md §9, fila 2 — unitaria, sin contexto de Spring: {@link PrincipalMustBeApplicationAdministratorValidator}
 * delegado y {@link MfaEvidenceProperties} son ambos dobles (sb-testing) — el delegado, una lambda;
 * las propiedades, el propio {@code record} con valores fijos, sin necesidad de doblarlo. Orden:
 * autorización primero — el delegado que rechaza nunca deja evaluar MFA (PLAN-HU-024.md §1.2).
 */
class MfaAwareApplicationAdministratorValidatorTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final MfaEvidenceProperties SATISFIED_BY_OTP = new MfaEvidenceProperties("acr", Set.of("urn:mfa:otp"));

    @Test
    void propagates_the_delegate_rejection_without_ever_evaluating_mfa_evidence() {
        AdministrationRequest request = requestWith(AuthenticationContextEvidence.NONE);
        MfaAwareApplicationAdministratorValidator validator = new MfaAwareApplicationAdministratorValidator(
                delegateRejecting(), SATISFIED_BY_OTP);

        StepVerifier.create(validator.execute(request))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    @Test
    void completes_when_the_delegate_allows_and_the_authentication_context_satisfies_mfa() {
        AdministrationRequest request = requestWith(new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of()));
        MfaAwareApplicationAdministratorValidator validator = new MfaAwareApplicationAdministratorValidator(
                delegateAllowing(), SATISFIED_BY_OTP);

        StepVerifier.create(validator.execute(request)).verifyComplete();
    }

    @Test
    void rejects_with_mfa_evidence_required_when_the_delegate_allows_but_the_authentication_context_does_not_satisfy_mfa() {
        AdministrationRequest request = requestWith(AuthenticationContextEvidence.NONE);
        MfaAwareApplicationAdministratorValidator validator = new MfaAwareApplicationAdministratorValidator(
                delegateAllowing(), SATISFIED_BY_OTP);

        StepVerifier.create(validator.execute(request))
                .expectErrorSatisfies(error -> {
                    var mfaRequired = (MfaEvidenceRequiredException) error;
                    assertThat(mfaRequired.code()).isEqualTo("MFA_REQUIRED");
                })
                .verify();
    }

    private static AdministrationRequest requestWith(AuthenticationContextEvidence authenticationContext) {
        return new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of(), authenticationContext);
    }

    private static PrincipalMustBeApplicationAdministratorValidator delegateAllowing() {
        return input -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator delegateRejecting() {
        return input -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }
}
