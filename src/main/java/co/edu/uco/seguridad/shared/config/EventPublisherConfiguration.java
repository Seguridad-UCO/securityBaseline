package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.event.DomainEventPublisher;
import co.edu.uco.seguridad.shared.event.SpringDomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Implementación por defecto del puerto de publicación de eventos de dominio (ADR-0002). Separada de
 * {@link SharedPortsConfiguration} porque, a diferencia de {@code TimeProvider}/{@code IdentifierGenerator},
 * depende de un bean que Spring ya provee ({@link ApplicationEventPublisher}), no de una fábrica en línea.
 */
@Configuration
class EventPublisherConfiguration {

    @Bean
    DomainEventPublisher domainEventPublisher(ApplicationEventPublisher events) {
        return new SpringDomainEventPublisher(events);
    }
}
