package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
import co.edu.uco.seguridad.shared.security.CorsProperties;
import co.edu.uco.seguridad.shared.security.JwtSecurityProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Frontera PEP reactiva (ADR-018, ADR-020). Único lugar del proyecto donde se decide qué ruta
 * necesita un token y cómo se valida ese token; ningún módulo de negocio importa una clase de
 * Spring Security.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({JwtSecurityProperties.class, CorsProperties.class})
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder,
            ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
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
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Dos modos (ADR-020): {@code jwkSetUri} presente → RS256 contra las llaves públicas de
     * Keycloak, el modo real de todo ambiente desplegado. Ausente → HMAC con {@code secret}, el
     * emisor propio original (ADR-018), vivo solo para que pruebas y desarrollo local no dependan
     * de un Keycloak real corriendo. El validador exige {@code sub}, {@code tenant} y {@code jti}
     * presentes, y {@code aud} igual a la audiencia configurada, para rechazar un token incompleto
     * o emitido para otra aplicación con 401 antes de que llegue al dominio. Validar {@code jti}
     * aquí no revoca nada todavía — solo garantiza que todo token aceptado ya trae el identificador
     * que una futura revocación (Redis) necesitará.
     */
    @Bean
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
