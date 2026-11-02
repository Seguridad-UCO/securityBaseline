package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbHealthIndicator;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbProperties;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealTokenAuthentication;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Única clase consciente de Spring para la conexión a SurrealDB (ADR-0004). El {@link WebClient} se
 * arma una vez, con el namespace/database en las cabeceras por defecto y la autenticación por token
 * ({@link SurrealTokenAuthentication}) como filtro, para que ningún adaptador de repositorio repita
 * ese cableado.
 */
@Configuration
@EnableConfigurationProperties(SurrealDbProperties.class)
class SurrealDbConfiguration {

    @Bean
    SurrealDbClient surrealDbClient(SurrealDbProperties properties, ObjectMapper objectMapper, WebClient.Builder builder,
                                    TimeProvider timeProvider) {
        // Token de sesión en vez de autenticación básica: con básica, SurrealDB verifica la contraseña
        // (Argon2) en cada consulta y eso dominaba la latencia de cada decisión.
        WebClient signinClient = builder.clone().baseUrl(properties.url()).build();
        WebClient webClient = builder.clone()
                .baseUrl(properties.url())
                .defaultHeaders(headers -> {
                    headers.set("surreal-ns", properties.namespace());
                    headers.set("surreal-db", properties.database());
                })
                .filter(new SurrealTokenAuthentication(signinClient, properties, objectMapper, timeProvider))
                .build();
        return new SurrealDbClient(webClient, objectMapper, properties);
    }

    /**
     * Publica el estado de SurrealDB en {@code /actuator/health}. Sin esto, una base caída se
     * manifestaba como un 503 sin explicación y había que adivinar la causa.
     */
    @Bean
    SurrealDbHealthIndicator surrealDbHealthIndicator(SurrealDbClient client, SurrealDbProperties properties) {
        return new SurrealDbHealthIndicator(client, properties);
    }
}
