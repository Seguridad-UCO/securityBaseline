package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdpPrincipalSecurityContextTests {
    @Test
    void creates_principals_from_jwt_and_oidc_claims_with_sid_fallback() {
        var jwtPrincipal = PdpPrincipal.from(TestJwtSupport.jwt("universidad-uco", "jwt-subject"));
        var token = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", "oidc-subject", "tenant", "estudiantes-uco", "jti", "jwt-id"));
        var oidcPrincipal = PdpPrincipal.from(new DefaultOidcUser(List.of(), token));

        assertThat(jwtPrincipal.tenantId().value()).isEqualTo("universidad-uco");
        assertThat(jwtPrincipal.subject()).isEqualTo("jwt-subject");
        assertThat(jwtPrincipal.userId()).isEmpty();
        assertThat(oidcPrincipal).extracting("tenantId.value", "subject", "tokenId")
                .containsExactly("estudiantes-uco", "oidc-subject", "jwt-id");
        assertThat(oidcPrincipal.userId()).isEmpty();
    }

    @Test
    void resolves_authentication_context_evidence_from_a_jwt_when_acr_and_amr_are_present() {
        var jwt = TestJwtSupport.jwtWithAuthenticationContext("universidad-uco", "jwt-subject", "urn:mfa:otp",
                List.of("otp", "pwd"));

        var principal = PdpPrincipal.from(jwt);

        assertThat(principal.authenticationContext().acr()).contains("urn:mfa:otp");
        assertThat(principal.authenticationContext().amr()).containsExactly("otp", "pwd");
    }

    @Test
    void resolves_empty_authentication_context_evidence_from_a_jwt_without_acr_or_amr() {
        var jwt = TestJwtSupport.jwt("universidad-uco", "jwt-subject");

        var principal = PdpPrincipal.from(jwt);

        assertThat(principal.authenticationContext().acr()).isEmpty();
        assertThat(principal.authenticationContext().amr()).isEmpty();
    }

    @Test
    void rejects_missing_required_principal_values_and_resolves_supported_session_identities() {
        assertThatThrownBy(() -> new PdpPrincipal(null, "subject", "token", Optional.empty(), AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PdpPrincipal(new TenantId("universidad-uco"), null, "token", Optional.empty(), AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PdpPrincipal(new TenantId("universidad-uco"), "subject", null, Optional.empty(), AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PdpPrincipal(new TenantId("universidad-uco"), "subject", "token", null, AuthenticationContextEvidence.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PdpPrincipal(new TenantId("universidad-uco"), "subject", "token", Optional.empty(), null))
                .isInstanceOf(NullPointerException.class);

        UUID localUserIdValue = UUID.randomUUID();
        var authenticationContext = new AuthenticationContextEvidence(Optional.of("urn:mfa:otp"), List.of("otp"));
        var local = new LocalUserPrincipal(localUserIdValue.toString(), "local-subject",
                new TenantId("universidad-uco"), "user@uco.edu", "Usuario", authenticationContext);
        var resolved = SecurityContext.currentPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken(local, null, List.of()))).block();

        assertThat(resolved).extracting("tenantId.value", "subject", "tokenId")
                .containsExactly("universidad-uco", "local-subject", localUserIdValue.toString());
        assertThat(resolved.userId()).contains(new UserId(localUserIdValue));
        assertThat(resolved.authenticationContext()).isEqualTo(authenticationContext);
        assertThatThrownBy(() -> SecurityContext.currentPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken("unsupported", null, List.of())))
                .block()).isInstanceOf(IllegalStateException.class).hasMessageContaining("no soportado");
    }
}
