package co.edu.uco.seguridad.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/** Llaves públicas rotables de Google para verificar ID tokens recibidos por el BFF. */
@Configuration
class GoogleTokenConfiguration {
    @Bean
    ReactiveJwtDecoder googleJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    }
}
