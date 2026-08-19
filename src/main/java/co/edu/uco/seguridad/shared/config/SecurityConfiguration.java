package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
import co.edu.uco.seguridad.shared.security.CorsProperties;
import co.edu.uco.seguridad.shared.security.JwtSecurityProperties;
import co.edu.uco.seguridad.shared.security.KeycloakSessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import reactor.core.publisher.Mono;

/**
 * Frontera PEP reactiva (ADR-018, ADR-020). Único lugar del proyecto donde se decide qué ruta
 * necesita un token y cómo se valida ese token; ningún módulo de negocio importa una clase de
 * Spring Security.
 */
@Configuration
@Profile("!keycloak")
@EnableWebFluxSecurity
@EnableConfigurationProperties({JwtSecurityProperties.class, CorsProperties.class, KeycloakSessionProperties.class})
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, @Qualifier("jwtDecoder") ReactiveJwtDecoder jwtDecoder,
            ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource) {
        CookieServerCsrfTokenRepository csrf = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        return http
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .csrf(spec -> spec.csrfTokenRepository(csrf)
                        // La SPA reenvía el valor crudo de XSRF-TOKEN como header. El handler
                        // por defecto lo espera en formato XOR, por lo que rechaza la petición.
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .accessDeniedHandler(accessDeniedHandler)
                        .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                                CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                                new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/api/v1/auth/google")))))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                .build();
    }

    /**
     * Orígenes explícitos únicamente (ADR-022) — el token viaja en el header {@code Authorization},
     * nunca en cookie, así que no hace falta {@code allowCredentials}. Un ambiente sin
     * {@code pdp.security.cors.allowed-origins} configurado no permite ningún origen: ninguna
     * llamada de navegador entra, pero curl/Postman/servidor-a-servidor no usan CORS y no se ven
     * afectados.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    CookieWebSessionIdResolver sessionIdResolver(KeycloakSessionProperties properties) {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("SECURITY_BASELINE_SESSION");
        resolver.addCookieInitializer(cookie -> cookie.path("/").httpOnly(true).sameSite("Lax").secure(properties.secureCookies()));
        return resolver;
    }

    /**
     * Dos modos (ADR-020): {@code jwkSetUri} presente → RS256 contra las llaves públicas de
     * Keycloak, el modo real de to-do ambiente desplegado. Ausente → HMAC con {@code secret}, el
     * emisor propio original (ADR-018), vivo solo para que pruebas y desarrollo local no dependan
     * de un Keycloak real corriendo. El validador exige {@code sub}, {@code tenant} y {@code jti}
     * presentes, y {@code aud} igual a la audiencia configurada, para rechazar un token incompleto
     * o emitido para otra aplicación con 401 antes de que llegue al dominio. Validar {@code jti}
     * aquí no revoca nada todavía — solo garantiza que to-do token aceptado ya trae el identificador
     * que una futura revocación (Redis) necesitará.
     */
    @Bean
    @Primary
    ReactiveJwtDecoder jwtDecoder(JwtSecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = properties.usesJwks()
                ? NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build()
                : NimbusReactiveJwtDecoder.withSecretKey(hmacKey(properties.secret())).build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                new JwtClaimValidator<String>("sub", StringUtils::hasText),
                new JwtClaimValidator<String>("tenant", StringUtils::hasText),
                new JwtClaimValidator<String>("jti", StringUtils::hasText),
                new JwtClaimValidator<List<String>>("aud",
                        audiences -> audiences != null && audiences.contains(properties.audience()))));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private static SecretKeySpec hmacKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper mapper) {
        return new ApiAuthenticationEntryPoint(mapper);
    }

    @Bean
    ApiAccessDeniedHandler apiAccessDeniedHandler(ObjectMapper mapper) {
        return new ApiAccessDeniedHandler(mapper);
    }
}
