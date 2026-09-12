package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.auth.service.OidcAuthenticationFailureHandler;
import co.edu.uco.seguridad.shared.auth.service.OidcAuthenticationSuccessHandler;
import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
import co.edu.uco.seguridad.shared.security.CorsProperties;
import co.edu.uco.seguridad.shared.security.KeycloakSessionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** BFF reactivo: Keycloak autentica, WebFlux conserva una sesión HttpOnly y el navegador nunca recibe el JWT. */
@Configuration
@Profile("keycloak")
@EnableWebFluxSecurity
@EnableConfigurationProperties({CorsProperties.class, KeycloakSessionProperties.class})
class KeycloakSecurityConfiguration {

    @Bean
    SecurityWebFilterChain keycloakSecurityWebFilterChain(ServerHttpSecurity http,
            ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler deniedHandler,
            CorsConfigurationSource corsConfigurationSource,
            OidcAuthenticationSuccessHandler successHandler,
            OidcAuthenticationFailureHandler failureHandler,
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
        CookieServerCsrfTokenRepository csrf = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        return http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(spec -> spec.csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .accessDeniedHandler(deniedHandler)
                        .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                                CsrfWebFilter.DEFAULT_CSRF_MATCHER, bffSessionRequest())))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/session/logout").permitAll()
                        .pathMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .oauth2Login(login -> login
                        .authenticationMatcher(new PathPatternParserServerWebExchangeMatcher("/login/oauth2/code/{registrationId}"))
                        .authorizationRequestResolver(new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations,
                                new PathPatternParserServerWebExchangeMatcher("/internal/oauth2/authorization/{registrationId}")))
                        .authorizationRequestRepository(authorizationRequestRepository)
                        .authenticationSuccessHandler(successHandler)
                        .authenticationFailureHandler(failureHandler))
                .build();
    }

    private static ServerWebExchangeMatcher bffSessionRequest() {
        return exchange -> exchange.getRequest().getCookies().containsKey("SECURITY_BASELINE_SESSION")
                ? ServerWebExchangeMatcher.MatchResult.match()
                : ServerWebExchangeMatcher.MatchResult.notMatch();
    }

    @Bean
    CorsConfigurationSource keycloakCorsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Correlation-Id", "traceparent", "tracestate"));
        configuration.setExposedHeaders(List.of("X-Request-Id", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    CookieWebSessionIdResolver keycloakSessionIdResolver(KeycloakSessionProperties properties) {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("SECURITY_BASELINE_SESSION");
        resolver.addCookieInitializer(cookie -> cookie.path("/").httpOnly(true)
                .sameSite("Lax").secure(properties.secureCookies()));
        return resolver;
    }

    @Bean
    ApiAuthenticationEntryPoint keycloakApiAuthenticationEntryPoint(ObjectMapper mapper) {
        return new ApiAuthenticationEntryPoint(mapper);
    }

    @Bean
    ApiAccessDeniedHandler keycloakApiAccessDeniedHandler(ObjectMapper mapper) {
        return new ApiAccessDeniedHandler(mapper);
    }

    @Bean
    ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> keycloakAuthorizationRequestRepository() {
        return new WebSessionOAuth2ServerAuthorizationRequestRepository();
    }
}
