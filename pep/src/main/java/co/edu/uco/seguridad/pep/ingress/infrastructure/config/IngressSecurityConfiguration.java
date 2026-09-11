package co.edu.uco.seguridad.pep.ingress.infrastructure.config;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.ProblemWriter;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.RouteResolver;
import co.edu.uco.seguridad.pep.ingress.infrastructure.integration.IntegrationProperties;
import co.edu.uco.seguridad.pep.ingress.infrastructure.properties.IngressProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({IngressProperties.class, IntegrationProperties.class})
class IngressSecurityConfiguration {

    @Bean
    ReactiveJwtDecoder jwtDecoder(IngressProperties properties) {
        WebClient jwks = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().followRedirect(false)
                                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                                .responseTimeout(properties.jwksTimeout())))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(65536)).build();
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwksUri().toString())
                .webClient(jwks).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new JwtClaimValidator<String>("sub", v -> v != null && !v.isBlank()),
                new JwtClaimValidator<Object>("exp", v -> v != null)));
        return token -> decoder.decode(token).timeout(properties.jwksTimeout())
                .onErrorMap(IngressSecurityConfiguration::isRemoteFailure,
                        error -> new AuthenticationServiceException("IDENTITY_UNAVAILABLE"));
    }

    private static boolean isRemoteFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.io.IOException || cause instanceof java.util.concurrent.TimeoutException
                    || cause instanceof org.springframework.web.reactive.function.client.WebClientException)
                return true;
        }
        return false;
    }

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http, ReactiveJwtDecoder decoder,
                                    IngressProperties properties, RouteResolver routes, ProblemWriter problems) {
        return http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .cors(cors -> cors.configurationSource(exchange -> {
                    try {
                        routes.resolve(exchange.getRequest().getURI().getRawPath());
                    } catch (EnforcementFailure ignored) {
                        return null;
                    }
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(properties.allowedOrigins());
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
                    config.setExposedHeaders(List.of("X-Request-Id", "X-Correlation-Id", "X-Decision-Id"));
                    config.setAllowCredentials(false);
                    config.setMaxAge(600L);
                    return config;
                }))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/internal/v1/integrations/*/*").permitAll()
                        .pathMatchers("/actuator/**").denyAll()
                        .anyExchange().access((authentication, context) -> authentication
                                .<org.springframework.security.authorization.AuthorizationResult>map(auth -> {
                                    var route = routes.resolve(context.getExchange().getRequest().getURI().getRawPath());
                                    boolean valid = auth instanceof JwtAuthenticationToken jwt && auth.isAuthenticated()
                                            && jwt.getToken().getAudience().stream().anyMatch(route.audiences()::contains);
                                    if (!valid) throw new EnforcementFailure(
                                            EnforcementFailure.Kind.UNAUTHENTICATED, "TOKEN_INVALID");
                                    return new AuthorizationDecision(true);
                                })
                                .defaultIfEmpty(new AuthorizationDecision(false))))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((exchange, error) -> problems.write(exchange,
                                new EnforcementFailure(error instanceof AuthenticationServiceException
                                        ? EnforcementFailure.Kind.UNAVAILABLE : EnforcementFailure.Kind.UNAUTHENTICATED,
                                        error instanceof AuthenticationServiceException ? "IDENTITY_UNAVAILABLE" : "TOKEN_INVALID")))
                        .accessDeniedHandler((exchange, error) -> problems.write(exchange,
                                new EnforcementFailure(EnforcementFailure.Kind.DENIED, "ACCESS_DENIED"))))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtDecoder(decoder))
                        .authenticationEntryPoint((exchange, error) -> problems.write(exchange,
                                new EnforcementFailure(error instanceof AuthenticationServiceException
                                        ? EnforcementFailure.Kind.UNAVAILABLE : EnforcementFailure.Kind.UNAUTHENTICATED,
                                        error instanceof AuthenticationServiceException ? "IDENTITY_UNAVAILABLE" : "TOKEN_INVALID"))))
                .build();
    }
}
