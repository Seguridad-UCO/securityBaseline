package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
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
}
