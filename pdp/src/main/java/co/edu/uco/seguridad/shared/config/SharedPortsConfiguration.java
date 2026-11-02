package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.port.CredentialHasher;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.SecretGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.Sha256CredentialHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    // SU-02 — SHA-256 para secretos de 256 bits; acepta y migra los BCrypt existentes.
    @Bean
    CredentialHasher credentialHasher() {
        return new Sha256CredentialHasher();
    }
}
