package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.auth.service.BffSessionTokenRenewer;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.SessionTokenRenewalProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

/**
 * Cableado de la renovación del token BFF (HU-030). El manager no es un bean: es un detalle interno
 * del renovador y así no compite con ningún otro {@link ReactiveOAuth2AuthorizedClientManager}.
 */
@Configuration
@Profile("keycloak")
@EnableConfigurationProperties(SessionTokenRenewalProperties.class)
class SessionTokenRenewalConfiguration {

    @Bean
    BffSessionTokenRenewer bffSessionTokenRenewer(ReactiveClientRegistrationRepository registrations,
                                                  ServerOAuth2AuthorizedClientRepository authorizedClients,
                                                  SessionTokenRenewalProperties properties,
                                                  TimeProvider timeProvider) {
        ReactiveOAuth2AuthorizedClientProvider provider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken(refresh -> refresh.clockSkew(properties.tokenRenewalSkew()))
                .build();
        DefaultReactiveOAuth2AuthorizedClientManager manager =
                new DefaultReactiveOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(provider);
        return new BffSessionTokenRenewer(manager, timeProvider, properties.tokenRenewalTimeout());
    }
}
