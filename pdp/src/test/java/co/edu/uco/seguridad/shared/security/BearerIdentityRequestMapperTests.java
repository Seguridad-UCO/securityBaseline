package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidTenantIdException;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerIdentityRequestMapperTests {

    private static final String ISSUER = "https://idp.test/realms/uco";

    @Test
    void refuses_a_token_without_the_tenant_claim() {
        assertThatThrownBy(() -> BearerIdentityRequestMapper.toRequest(builder().build()))
                .isInstanceOf(InvalidTenantIdException.class);
    }

    @Test
    void refuses_a_malformed_tenant_claim() {
        assertThatThrownBy(() -> BearerIdentityRequestMapper.toRequest(builder().claim("tenant", "x y!").build()))
                .isInstanceOf(InvalidTenantIdException.class);
    }

    @Test
    void maps_issuer_subject_and_tenant() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").build());

        assertThat(request.issuer()).isEqualTo(ISSUER);
        assertThat(request.subject()).isEqualTo("sub-1");
        assertThat(request.tenantId().value()).isEqualTo("universidad-uco");
    }

    @Test
    void maps_a_valid_verified_email() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("email", "David@UCO.edu.co")
                .claim("email_verified", true).build());

        assertThat(request.email()).contains(new Email("david@uco.edu.co"));
        assertThat(request.emailVerified()).isTrue();
    }

    @Test
    void maps_an_invalid_email_to_empty() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("email", "no-es-correo").build());

        assertThat(request.email()).isEmpty();
    }

    @Test
    void maps_a_missing_email_to_empty_and_unverified() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").build());

        assertThat(request.email()).isEmpty();
        assertThat(request.emailVerified()).isFalse();
    }

    @Test
    void treats_a_missing_email_verified_claim_as_false() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("email", "david@uco.edu.co").build());

        assertThat(request.emailVerified()).isFalse();
    }

    @Test
    void uses_the_name_claim_when_present() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("name", "  Ana Pérez ").claim("preferred_username", "ana").build());

        assertThat(request.name()).isEqualTo("Ana Pérez");
    }

    @Test
    void falls_back_to_preferred_username_when_the_client_does_not_emit_name() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("preferred_username", "ana").build());

        assertThat(request.name()).isEqualTo("ana");
    }

    @Test
    void leaves_the_name_empty_when_the_token_has_neither_claim() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").build());

        assertThat(request.name()).isEmpty();
    }

    @Test
    void lowercases_the_identity_provider_claim() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(builder()
                .claim("tenant", "universidad-uco").claim("identity_provider", "Google").build());

        assertThat(request.provider()).isEqualTo("google");
    }

    @Test
    void defaults_the_provider_to_keycloak_local() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").build());

        assertThat(request.provider()).isEqualTo("keycloak-local");
    }

    @Test
    void defaults_a_missing_name_to_empty_text() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").build());

        assertThat(request.name()).isEmpty();
    }

    @Test
    void maps_the_name_claim() {
        ResolveBearerIdentityRequest request = BearerIdentityRequestMapper.toRequest(
                builder().claim("tenant", "universidad-uco").claim("name", "David Alzate").build());

        assertThat(request.name()).isEqualTo("David Alzate");
    }

    private static Jwt.Builder builder() {
        Instant iat = Instant.parse("2026-10-30T10:00:00Z");
        return Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .subject("sub-1")
                .issuedAt(iat)
                .expiresAt(iat.plusSeconds(300));
    }
}
