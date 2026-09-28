package co.edu.uco.seguridad.shared.web.session;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalBffSessionTokenControllerTests {

    @Test
    void resolves_the_authorized_client_with_the_bff_principal_not_the_technical_pep_identity() {
        ServerOAuth2AuthorizedClientRepository clients = mock(ServerOAuth2AuthorizedClientRepository.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/v1/bff-session-token"));
        Authentication bffPrincipal = new UsernamePasswordAuthenticationToken("bff-user", null, List.of());
        new WebSessionServerSecurityContextRepository().save(exchange, new SecurityContextImpl(bffPrincipal)).block();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "bff-access-token",
                Instant.now(), Instant.now().plusSeconds(60));
        when(clients.loadAuthorizedClient(eq("keycloak"), any(Authentication.class), eq(exchange)))
                .thenReturn(Mono.just(new OAuth2AuthorizedClient(registration(), "bff-user", accessToken)));

        var response = new InternalBffSessionTokenController(clients).token(exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<Authentication> principal = ArgumentCaptor.forClass(Authentication.class);
        verify(clients).loadAuthorizedClient(eq("keycloak"), principal.capture(), eq(exchange));
        assertThat(principal.getValue().getName()).isEqualTo("bff-user");
    }

    private static ClientRegistration registration() {
        Builder builder = ClientRegistration.withRegistrationId("keycloak");
        return builder.clientId("security-baseline-bff").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/authorize").tokenUri("http://localhost/token")
                .build();
    }
}
