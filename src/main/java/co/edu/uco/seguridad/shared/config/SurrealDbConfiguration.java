package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Única clase consciente de Spring para la conexión a SurrealDB (ADR-0004). El {@link WebClient} se
 * arma una vez, con la autenticación y el namespace/database ya fijados en las cabeceras por
 * defecto, para que ningún adaptador de repositorio repita ese cableado.
 */
@Configuration
@EnableConfigurationProperties(SurrealDbProperties.class)
class SurrealDbConfiguration {

    @Bean
    SurrealDbClient surrealDbClient(SurrealDbProperties properties, ObjectMapper objectMapper) {
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.url())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.username(), properties.password());
                    headers.set("surreal-ns", properties.namespace());
                    headers.set("surreal-db", properties.database());
                })
                .build();
        return new SurrealDbClient(webClient, objectMapper, properties);
    }
}
