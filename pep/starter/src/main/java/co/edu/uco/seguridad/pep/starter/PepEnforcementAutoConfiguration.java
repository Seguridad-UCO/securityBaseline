package co.edu.uco.seguridad.pep.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Activa la protección local de rutas para toda aplicación que habilite {@code security.enabled}. */
@AutoConfiguration
@EnableConfigurationProperties(PepEnforcementProperties.class)
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "security.pep.enforcement", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PepEnforcementAutoConfiguration {

    @Bean
    PepEnforcementWebFilter pepEnforcementWebFilter(PepEnforcementProperties properties) {
        properties.validate();
        return new PepEnforcementWebFilter(properties,
                org.springframework.web.reactive.function.client.WebClient.builder()
                        .baseUrl(properties.pepUrl().toString()).build());
    }
}
