package co.edu.uco.seguridad.pep.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** Optional control-plane self-registration with the remote PEP. */
@AutoConfiguration
@EnableConfigurationProperties(PepRegistrationProperties.class)
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "security.pep.registration", name = "enabled", havingValue = "true")
public class PepRegistrationAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PepRegistrationAutoConfiguration.class);

    @Bean
    ApplicationRunner pepRegistrationRunner(PepRegistrationProperties properties, WebClient.Builder builder) {
        return ignored -> {
            try {
                properties.validate();
            } catch (IllegalArgumentException error) {
                LOG.error("PEP registration is disabled by invalid local configuration: {}", error.getMessage());
                return;
            }
            register(properties, builder).retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(60)).filter(error -> !(error instanceof RegistrationRejectedException)))
                    .doOnSuccess(response -> LOG.info("PEP integration active: publicBaseUrl={}", response.publicBaseUrl()))
                    .doOnError(error -> LOG.error("PEP integration registration rejected; correct configuration and restart: {}",
                            error.getMessage()))
                    .subscribe();
        };
    }

    private Mono<RegistrationResponse> register(PepRegistrationProperties properties, WebClient.Builder builder) {
        URI endpoint = URI.create(properties.pepUrl().toString().replaceAll("/$", "") + "/internal/v1/integrations/"
                + properties.applicationId() + "/" + properties.environment());
        return builder.clone().build().put().uri(endpoint).headers(headers -> headers.setBearerAuth(properties.token()))
                .bodyValue(Map.of("backendUrl", properties.backendUrl().toString(), "audience", properties.audience()))
                .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                        ? response.bodyToMono(RegistrationResponse.class)
                        : response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body -> Mono.error(
                                response.statusCode().is4xxClientError() ? new RegistrationRejectedException(response.statusCode())
                                        : new RegistrationUnavailableException(response.statusCode()))));
    }

    record RegistrationResponse(String applicationId, String environment, String prefix, URI publicBaseUrl, String status) {}
    static final class RegistrationRejectedException extends RuntimeException {
        RegistrationRejectedException(HttpStatusCode status) { super("PEP returned " + status.value()); }
    }
    static final class RegistrationUnavailableException extends RuntimeException {
        RegistrationUnavailableException(HttpStatusCode status) { super("PEP returned " + status.value()); }
    }
}
