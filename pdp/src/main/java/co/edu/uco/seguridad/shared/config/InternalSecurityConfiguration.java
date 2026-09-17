package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.ApiAccessDeniedHandler;
import co.edu.uco.seguridad.shared.security.ApiAuthenticationEntryPoint;
import co.edu.uco.seguridad.shared.security.InternalEvidenceJwtProperties;
import co.edu.uco.seguridad.shared.security.InternalMtlsProperties;
import co.edu.uco.seguridad.shared.security.InternalMtlsWebFilter;
import co.edu.uco.seguridad.shared.security.revocation.RevocationAwareJwtDecoder;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
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
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Cadena de seguridad del canal interno para el PEP (HU-003, decisiones D2/D5 del HANDOFF):
 * {@code securityMatcher("/internal/v1/**")} — no {@code "/internal/**"}, porque
 * {@code /internal/oauth2/**} ya sirve el flujo OIDC del panel en {@code KeycloakSecurityConfiguration}
 * y no debe pasar por mTLS.
 *
 * <p>Sin {@code @Profile}: el canal interno existe independientemente del modo de autenticación del
 * BFF (HMAC local o Keycloak real). {@code SecurityConfiguration}/{@code KeycloakSecurityConfiguration}
 * siguen siendo "el único lugar que decide" — pero **por canal**, no en términos absolutos.</p>
 *
 * <p><b>{@link InternalMtlsWebFilter} no es un {@code @Bean} independiente, a propósito.</b> Spring
 * Boot registra automáticamente cualquier bean de tipo {@code WebFilter} en la cadena global de
 * WebFlux, además de dondequiera que se use explícitamente — exponerlo como bean aparte lo hacía
 * correr en **todas** las peticiones de la aplicación, no solo en {@code /internal/v1/**} (se
 * verificó así: rompía en rojo toda la suite HTTP existente — {@code /actuator/health}, el panel —
 * con 403, porque ninguna de esas peticiones trae certificado de cliente). Se construye con
 * {@code new} dentro del único método que lo usa, igual que cualquier otro colaborador de un
 * {@code {Slice}Configuration}.</p>
 *
 * <p>Dos capas, en orden: mTLS (falla cerrado con 403 antes de que Spring Security intente
 * autenticar nada) y, si pasa, evidencia JWT vía {@code oauth2ResourceServer}. Un JWT inválido,
 * expirado, de otro emisor/audiencia o sin {@code sub} responde 401 — sin exigir el claim
 * {@code tenant} que sí exige {@code PdpPrincipal} (T1): el tenant de este canal sale del catálogo
 * de aplicaciones (D3), nunca del token.</p>
 */
@Configuration
@EnableConfigurationProperties({InternalMtlsProperties.class, InternalEvidenceJwtProperties.class})
class InternalSecurityConfiguration {

    @Bean
    SecurityWebFilterChain internalAccessDecisionsSecurityWebFilterChain(ServerHttpSecurity http,
            InternalMtlsProperties mtlsProperties,
            @Qualifier("internalEvidenceJwtDecoder") ReactiveJwtDecoder internalEvidenceJwtDecoder,
            ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDeniedHandler) {
        return http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/internal/v1/**"))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(internalEvidenceJwtDecoder)))
                .addFilterBefore(new InternalMtlsWebFilter(mtlsProperties), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Solo JWKS (D5, confirmado con el usuario): sin el modo HMAC de {@code JwtSecurityProperties}, y
     * sin exigir {@code tenant}. {@code withJwkSetUri} no resuelve la URL al construir el bean —lo
     * hace de forma perezosa en el primer {@code decode()}— así que un valor por defecto que no
     * resuelve nada (ver {@code application.properties}) no impide arrancar el contexto; solo hace
     * que cualquier evidencia falle al validar, que es exactamente fallar cerrado.
     *
     * <p>HU-022 (PLAN-HU-022.md §1, §7): el decoder Nimbus queda como {@code delegate}, sin cambios —
     * {@link RevocationAwareJwtDecoder} lo envuelve para rechazar (fail-closed) un JWT criptográfica
     * y temporalmente válido pero emitido antes de la última revocación de su sujeto. Este es el
     * canal real que protege el flujo BFF (el modo dev/HMAC de {@code SecurityConfiguration} queda
     * fuera de esta historia).</p>
     */
    @Bean
    ReactiveJwtDecoder internalEvidenceJwtDecoder(InternalEvidenceJwtProperties properties,
            TokenRevocationPort revocation, SubjectUserIdLookupValidator subjectUserIdLookup) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                new JwtClaimValidator<String>("sub", StringUtils::hasText),
                new JwtClaimValidator<List<String>>("aud",
                        audiences -> audiences != null && audiences.contains(properties.audience()))));
        decoder.setJwtValidator(validator);
        return new RevocationAwareJwtDecoder(decoder, revocation, subjectUserIdLookup);
    }
}
