package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementaciones por defecto de los puertos transversales. Declaradas aquí una sola vez para que ningún caso de uso
 * llegue a {@code Instant.now()} o {@code UUID.randomUUID()} y cada uno de ellos pueda ser fijado en una
 * prueba intercambiando un bean.
 */
@Configuration
class SharedPortsConfiguration {

    @Bean
    TimeProvider timeProvider() {
        Clock clock = Clock.systemUTC();
        return clock::instant;
    }

    @Bean
    IdentifierGenerator identifierGenerator() {
        return UUID::randomUUID;
    }

    // HU-012 — credencial de aplicación: capacidades transversales, mismo patrón que las de arriba.
    @Bean
    SecretGenerator secretGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        };
    }

    @Bean
    CredentialHasher credentialHasher() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder::encode;
    }
}
