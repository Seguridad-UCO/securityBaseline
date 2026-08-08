package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
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

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Frontera PEP reactiva (ADR-0003). Único lugar del proyecto donde se decide qué ruta necesita un
 * token y cómo se valida ese token; ningún módulo de negocio importa una clase de Spring Security.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder,
            ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDeniedHandler) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
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
     * HMAC simétrico porque el emisor es propio (ver {@link JwtSecurityProperties}). El validador
     * exige, además de expiración e emisor, que {@code sub} y {@code tenant} estén presentes: si
     * faltaran, {@link co.edu.uco.seguridad.shared.security.PdpPrincipal#from} fallaría con una
     * excepción de dominio en vez de un 401 — la frontera de seguridad debe rechazar un token
     * incompleto antes de que llegue tan lejos.
     */
    @Bean
    ReactiveJwtDecoder jwtDecoder(JwtSecurityProperties properties) {
        SecretKeySpec key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key).build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                new JwtClaimValidator<String>("sub", StringUtils::hasText),
                new JwtClaimValidator<String>("tenant", StringUtils::hasText)));
        decoder.setJwtValidator(validator);
        return decoder;
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
