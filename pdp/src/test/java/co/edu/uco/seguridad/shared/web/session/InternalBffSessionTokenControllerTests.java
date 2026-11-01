package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.auth.service.BffSessionTokenRenewer;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-030: el canal interno PEP->PDP entrega el token vigente de la sesion BFF (renovandolo si hace falta)
 * y es fail-closed: 401 si la sesion murio, 503 si Keycloak no responde, nunca un token vencido.
 */
class InternalBffSessionTokenControllerTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    @Test
    void resolves_the_token_with_the_bff_principal_not_the_technical_pep_identity() {
        AtomicReference<String> principalName = new AtomicReference<>();
        MockServerWebExchange exchange = exchangeWithBffPrincipal(new UsernamePasswordAuthenticationToken("bff-user", null, List.of()));
        var controller = controller(request -> {
            principalName.set(request.getPrincipal().getName());
            return Mono.just(client("bff-access-token", NOW.plusSeconds(120)));
        });

        StepVerifier.create(controller.token(exchange))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().accessToken()).isEqualTo("bff-access-token");
                })
                .verifyComplete();

        assertThat(principalName.get()).isEqualTo("bff-user");
    }

    @Test
    void resolves_the_token_with_the_keycloak_subject_after_the_session_uses_a_local_principal() {
        AtomicReference<String> principalName = new AtomicReference<>();
        LocalUserPrincipal localUser = new LocalUserPrincipal("local-id", "keycloak-subject", new TenantId("universidad-uco"),
                "user@example.edu", "Usuario", AuthenticationContextEvidence.NONE);
        MockServerWebExchange exchange = exchangeWithBffPrincipal(new UsernamePasswordAuthenticationToken(localUser, null, List.of()));
        var controller = controller(request -> {
            principalName.set(request.getPrincipal().getName());
            return Mono.just(client("bff-access-token", NOW.plusSeconds(120)));
        });

        StepVerifier.create(controller.token(exchange))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        assertThat(principalName.get()).isEqualTo("keycloak-subject");
    }

    @Test
    void answers_401_when_the_renewal_says_the_session_expired_and_the_message_carries_no_token() {
        MockServerWebExchange exchange = exchangeWithBffPrincipal(new UsernamePasswordAuthenticationToken("bff-user", null, List.of()));
        var controller = controller(request -> Mono.error(
                new ClientAuthorizationException(new OAuth2Error("invalid_grant", "refresh-secret-value", null), "keycloak")));

        StepVerifier.create(controller.token(exchange))
                .expectErrorSatisfies(error -> {
                    assertThat(statusOf(error)).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(((ResponseStatusException) error).getReason()).isEqualTo("Sesión BFF no activa.");
                    assertThat(String.valueOf(error.getMessage())).doesNotContain("refresh-secret-value");
                })
                .verify();
    }

    @Test
    void answers_503_when_keycloak_is_unavailable_and_the_message_carries_no_token() {
        MockServerWebExchange exchange = exchangeWithBffPrincipal(new UsernamePasswordAuthenticationToken("bff-user", null, List.of()));
        var controller = controller(request -> Mono.error(new IllegalStateException("Connection refused: expired-access-token")));

        StepVerifier.create(controller.token(exchange))
                .expectErrorSatisfies(error -> {
                    assertThat(statusOf(error)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(String.valueOf(error.getMessage())).doesNotContain("expired-access-token");
                })
                .verify();
    }

    @Test
    void answers_401_without_asking_for_a_renewal_when_there_is_no_bff_session() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/v1/bff-session-token").build());
        var controller = controller(request -> Mono.error(new AssertionError("sin sesion BFF no debe pedir renovacion")));

        StepVerifier.create(controller.token(exchange))
                .expectErrorSatisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
    }

    private static InternalBffSessionTokenController controller(
            Function<OAuth2AuthorizeRequest, Mono<OAuth2AuthorizedClient>> manager) {
        ReactiveOAuth2AuthorizedClientManager fakeManager = manager::apply;
        return new InternalBffSessionTokenController(
                new BffSessionTokenRenewer(fakeManager, () -> NOW, Duration.ofSeconds(5)));
    }

    private static MockServerWebExchange exchangeWithBffPrincipal(Authentication bffPrincipal) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/v1/bff-session-token").build());
        new WebSessionServerSecurityContextRepository().save(exchange, new SecurityContextImpl(bffPrincipal)).block();
        return exchange;
    }

    private static HttpStatusCode statusOf(Throwable error) {
        assertThat(error).isInstanceOf(ResponseStatusException.class);
        return ((ResponseStatusException) error).getStatusCode();
    }

    private static OAuth2AuthorizedClient client(String accessToken, Instant expiresAt) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/authorize").tokenUri("http://localhost/token")
                .build();
        return new OAuth2AuthorizedClient(registration, "keycloak-subject",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, expiresAt.minusSeconds(300), expiresAt));
    }
}
