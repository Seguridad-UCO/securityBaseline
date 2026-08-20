package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
import co.edu.uco.seguridad.shared.security.CorsProperties;
import co.edu.uco.seguridad.shared.security.KeycloakSessionProperties;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
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
            CorsConfigurationSource corsConfigurationSource, PlatformAdministrationService users,
            @Value("${pdp.frontend.origin}") String frontendOrigin) {
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
                        .pathMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .oauth2Login(login -> login.authenticationSuccessHandler((exchange, authentication) -> {
                    OidcUser oidc = (OidcUser) authentication.getPrincipal();
                    String email = oidc.getEmail();
                    if (email == null || email.isBlank()) return reactor.core.publisher.Mono.error(new IllegalArgumentException("Keycloak no entregó un correo verificable."));
                    String name = oidc.getFullName() == null || oidc.getFullName().isBlank() ? oidc.getGivenName() : oidc.getFullName();
                    String provider = authProvider(oidc);
                    var redirect = new RedirectServerAuthenticationSuccessHandler(frontendOrigin);
                    return users.provision(oidc.getIdToken().getIssuer().toString(), oidc.getSubject(), email, name, provider)
                            .flatMap(local -> saveLocalSession(exchange.getExchange(), local))
                            .then(redirect.onAuthenticationSuccess(exchange, authentication));
                }))
                .build();
    }

    private static String authProvider(OidcUser oidc) {
        String broker = oidc.getClaimAsString("identity_provider");
        return broker == null || broker.isBlank() ? "keycloak-local" : broker.toLowerCase(Locale.ROOT);
    }

    private static ServerWebExchangeMatcher bffSessionRequest() {
        return exchange -> exchange.getRequest().getCookies().containsKey("SECURITY_BASELINE_SESSION")
                ? ServerWebExchangeMatcher.MatchResult.match()
                : ServerWebExchangeMatcher.MatchResult.notMatch();
    }

    private reactor.core.publisher.Mono<Void> saveLocalSession(org.springframework.web.server.ServerWebExchange exchange, LocalUserPrincipal user) {
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        return new WebSessionServerSecurityContextRepository().save(exchange, new SecurityContextImpl(authentication));
    }

    @Bean
    CorsConfigurationSource keycloakCorsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
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
}
