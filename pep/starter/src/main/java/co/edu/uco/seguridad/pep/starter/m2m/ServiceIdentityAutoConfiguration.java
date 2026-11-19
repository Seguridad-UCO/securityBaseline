package co.edu.uco.seguridad.pep.starter.m2m;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableConfigurationProperties(ServiceIdentityProperties.class)
@ConditionalOnProperty(prefix = "security.pep.service-identity", name = "enabled", havingValue = "true")
public class ServiceIdentityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ServiceAccessTokenProvider serviceAccessTokenProvider(ServiceIdentityProperties properties) {
        return new KeycloakServiceAccessTokenProvider(properties, WebClient.builder().build());
    }
}
