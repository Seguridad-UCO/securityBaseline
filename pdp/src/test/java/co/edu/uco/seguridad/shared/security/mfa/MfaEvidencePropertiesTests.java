package co.edu.uco.seguridad.shared.security.mfa;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-HU-024.md §9, fila 1 — unitaria, sin contexto de Spring: seis casos sobre
 * {@link MfaEvidenceProperties#satisfiedBy(AuthenticationContextEvidence)}, incluido el
 * fail-closed del criterio de aceptación 5 con {@code claim}/{@code acceptedValues} sin configurar.
 */
class MfaEvidencePropertiesTests {

    @Test
    void accepts_an_acr_claim_present_and_within_the_accepted_values() {
        var properties = new MfaEvidenceProperties(true, "acr", Set.of("urn:mfa:otp"));
        var evidence = new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of());

        assertThat(properties.satisfiedBy(evidence)).isTrue();
    }

    @Test
    void rejects_an_acr_claim_present_but_outside_the_accepted_values() {
        var properties = new MfaEvidenceProperties(true, "acr", Set.of("urn:mfa:otp"));
        var evidence = new AuthenticationContextEvidence(Optional.of("urn:mfa:password-only"), List.of());

        assertThat(properties.satisfiedBy(evidence)).isFalse();
    }

    @Test
    void rejects_an_absent_acr_claim() {
        var properties = new MfaEvidenceProperties(true, "acr", Set.of("urn:mfa:otp"));
        var evidence = new AuthenticationContextEvidence(Optional.empty(), List.of());

        assertThat(properties.satisfiedBy(evidence)).isFalse();
    }

    @Test
    void accepts_an_amr_claim_when_any_of_its_values_is_within_the_accepted_values() {
        var properties = new MfaEvidenceProperties(true, "amr", Set.of("otp"));
        var evidence = new AuthenticationContextEvidence(Optional.empty(), List.of("pwd", "otp"));

        assertThat(properties.satisfiedBy(evidence)).isTrue();
    }

    @Test
    void rejects_an_amr_claim_that_is_empty_or_has_no_intersection_with_the_accepted_values() {
        var properties = new MfaEvidenceProperties(true, "amr", Set.of("otp"));

        assertThat(properties.satisfiedBy(new AuthenticationContextEvidence(Optional.empty(), List.of()))).isFalse();
        assertThat(properties.satisfiedBy(new AuthenticationContextEvidence(Optional.empty(), List.of("pwd"))))
                .isFalse();
    }

    @Test
    void is_always_unsatisfied_when_the_claim_or_the_accepted_values_are_not_configured_even_with_evidence_present() {
        var evidence = new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of("otp"));

        assertThat(new MfaEvidenceProperties(true, "", Set.of()).satisfiedBy(evidence)).isFalse();
        assertThat(new MfaEvidenceProperties(true, null, Set.of("urn:mfa:otp")).satisfiedBy(evidence)).isFalse();
        assertThat(new MfaEvidenceProperties(true, "acr", Set.of()).satisfiedBy(evidence)).isFalse();
        assertThat(new MfaEvidenceProperties(true, null, null).satisfiedBy(evidence)).isFalse();
    }

    @Test
    void allows_administration_when_mfa_is_explicitly_disabled_for_the_local_environment() {
        assertThat(new MfaEvidenceProperties(false, null, null)
                .satisfiedBy(AuthenticationContextEvidence.NONE)).isTrue();
    }
}
