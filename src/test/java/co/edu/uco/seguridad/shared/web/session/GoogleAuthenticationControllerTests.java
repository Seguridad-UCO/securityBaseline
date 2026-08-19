package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleAuthenticationControllerTests {
    private static final String CLIENT_ID = "security-baseline.apps.googleusercontent.com";

    @Test
    void validates_google_credential_provisions_the_identity_and_creates_a_local_session() {
        PlatformAdministrationService users = mock(PlatformAdministrationService.class);
        when(users.provision(any(), any(), any(), any())).thenReturn(Mono.just(
                new LocalUserPrincipal("user-1", "google-subject", new TenantId("universidad-uco"), "david@uco.edu", "David Alzate")));
        ReactiveJwtDecoder decoder = credential -> Mono.just(validGoogleJwt());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/google").build());

        new GoogleAuthenticationController(decoder, users, CLIENT_ID)
                .google(new GoogleAuthenticationController.GoogleCredentialRequest("signed-google-token"), exchange)
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Object savedContext = exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(savedContext).isNotNull();
    }

    @Test
    void rejects_empty_configuration_or_an_unverified_google_token() {
        ReactiveJwtDecoder decoder = credential -> Mono.just(validGoogleJwt());
        PlatformAdministrationService users = mock(PlatformAdministrationService.class);
        when(users.provision(any(), any(), any(), any())).thenReturn(Mono.just(
                new LocalUserPrincipal("user-1", "google-subject", new TenantId("universidad-uco"), "david@uco.edu", "David Alzate")));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/google").build());

        assertThatThrownBy(() -> new GoogleAuthenticationController(decoder, users, "")
                .google(new GoogleAuthenticationController.GoogleCredentialRequest("token"), exchange).block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Google no está configurado");

        Jwt unverified = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject("google-subject")
                .audience(List.of(CLIENT_ID))
                .claim("email", "david@uco.edu")
                .claim("name", "David Alzate")
                .claim("email_verified", false)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();

        assertThatThrownBy(() -> new GoogleAuthenticationController(credential -> Mono.just(unverified), users, CLIENT_ID)
                .google(new GoogleAuthenticationController.GoogleCredentialRequest("token"), exchange).block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no es válida");

        for (Jwt invalid : List.of(jwt("https://issuer.invalid", List.of(CLIENT_ID), true, "david@uco.edu"),
                jwt("https://accounts.google.com", List.of("another-client"), true, "david@uco.edu"),
                jwt("https://accounts.google.com", List.of(CLIENT_ID), true, null))) {
            assertThatThrownBy(() -> new GoogleAuthenticationController(credential -> Mono.just(invalid), users, CLIENT_ID)
                    .google(new GoogleAuthenticationController.GoogleCredentialRequest("token"), exchange).block())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no es válida");
        }
    }

    private static Jwt validGoogleJwt() {
        return jwt("https://accounts.google.com", List.of(CLIENT_ID), true, "david@uco.edu");
    }

    private static Jwt jwt(String issuer, List<String> audience, boolean verified, String email) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("google-subject")
                .audience(audience)
                .claim("name", "David Alzate")
                .claim("email_verified", verified)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
        if (email != null) builder.claim("email", email);
        return builder.build();
    }
}
