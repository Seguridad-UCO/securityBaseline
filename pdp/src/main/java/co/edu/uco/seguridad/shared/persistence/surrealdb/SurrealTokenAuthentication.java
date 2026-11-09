package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Autentica cada consulta a SurrealDB con un token de sesión en vez de usuario y contraseña.
 *
 * <p><strong>Por qué.</strong> Con autenticación básica, SurrealDB verifica la contraseña con Argon2
 * en <em>cada</em> petición: ~70 ms por consulta, y en ráfaga las verificaciones compiten entre sí.
 * Una decisión del PDP hace varias consultas seguidas, así que ese costo se multiplicaba hasta superar
 * el timeout del PEP. Con el token, la misma consulta tarda pocos milisegundos (medido contra
 * SurrealDB v3.3.0 el 2026-11-02).</p>
 *
 * <p>Se inicia sesión una sola vez ({@code POST /signin}) y el token se reutiliza hasta poco antes de
 * su {@code exp}. Varias peticiones simultáneas sin token válido comparten un único inicio de sesión.
 * Si SurrealDB responde {@code 401} (token revocado o vencido antes de lo previsto), se descarta el
 * token, se inicia sesión otra vez y la petición se repite <em>una</em> vez; un segundo {@code 401} se
 * entrega tal cual, sin bucles.</p>
 */
public final class SurrealTokenAuthentication implements ExchangeFilterFunction {

    private static final Duration RENEWAL_MARGIN = Duration.ofSeconds(60);
    private static final Duration LIFETIME_WITHOUT_EXP = Duration.ofMinutes(5);
    private static final String SIGNIN_FAILED_MESSAGE = "SurrealDB rechazó el inicio de sesión del PDP: HTTP %d";
    private static final String SIGNIN_WITHOUT_TOKEN_MESSAGE = "SurrealDB no devolvió un token al iniciar sesión";

    private final WebClient signinClient;
    private final SurrealDbProperties properties;
    private final ObjectMapper objectMapper;
    private final TimeProvider time;
    private final AtomicReference<SessionToken> current = new AtomicReference<>();
    private final AtomicReference<Mono<SessionToken>> inFlight = new AtomicReference<>();

    /**
     * @param signinClient un {@link WebClient} <em>sin</em> este filtro, apuntando a la URL de SurrealDB
     */
    public SurrealTokenAuthentication(WebClient signinClient, SurrealDbProperties properties,
                                      ObjectMapper objectMapper, TimeProvider time) {
        this.signinClient = Objects.requireNonNull(signinClient, RequiredArgumentMessages.SURREALDB_WEBCLIENT);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.SURREALDB_PROPERTIES);
        this.objectMapper = Objects.requireNonNull(objectMapper, RequiredArgumentMessages.OBJECT_MAPPER);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return token().flatMap(token -> next.exchange(authenticated(request, token))
                .flatMap(response -> response.statusCode().value() == HttpStatus.UNAUTHORIZED.value()
                        ? response.releaseBody().then(Mono.defer(() -> retryWithFreshToken(request, next, token)))
                        : Mono.just(response)));
    }

    private Mono<ClientResponse> retryWithFreshToken(ClientRequest request, ExchangeFunction next, SessionToken rejected) {
        current.compareAndSet(rejected, null);
        return token().flatMap(fresh -> next.exchange(authenticated(request, fresh)));
    }

    private Mono<SessionToken> token() {
        return Mono.defer(() -> {
            SessionToken token = current.get();
            if (token != null && time.now().isBefore(token.renewAt())) {
                return Mono.just(token);
            }
            return sharedSignin();
        });
    }

    private Mono<SessionToken> sharedSignin() {
        AtomicReference<Mono<SessionToken>> self = new AtomicReference<>();
        Mono<SessionToken> candidate = signin()
                .doOnNext(current::set)
                .doOnTerminate(() -> inFlight.compareAndSet(self.get(), null))
                .doOnCancel(() -> inFlight.compareAndSet(self.get(), null))
                .cache();
        self.set(candidate);
        return inFlight.compareAndSet(null, candidate) ? candidate : Objects.requireNonNullElse(inFlight.get(), candidate);
    }

    private Mono<SessionToken> signin() {
        return signinClient.post()
                .uri("/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("user", properties.username(), "pass", properties.password()))
                .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                        ? response.bodyToMono(String.class).map(this::sessionToken)
                        : response.releaseBody().then(Mono.error(new SurrealDbException(
                        SIGNIN_FAILED_MESSAGE.formatted(response.statusCode().value())))));
    }

    private SessionToken sessionToken(String body) {
        String value = objectMapper.readTree(body).path("token").asString("");
        if (value.isBlank()) {
            throw new SurrealDbException(SIGNIN_WITHOUT_TOKEN_MESSAGE);
        }
        Instant now = time.now();
        Instant expiresAt = expiration(value).orElse(now.plus(LIFETIME_WITHOUT_EXP));
        Duration lifetime = Duration.between(now, expiresAt);
        Duration margin = lifetime.compareTo(RENEWAL_MARGIN.multipliedBy(2)) < 0 ? lifetime.dividedBy(2) : RENEWAL_MARGIN;
        return new SessionToken(value, expiresAt.minus(margin));
    }

    private Optional<Instant> expiration(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            JsonNode claims = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8));
            long exp = claims.path("exp").asLong(0);
            return exp > 0 ? Optional.of(Instant.ofEpochSecond(exp)) : Optional.empty();
        } catch (IllegalArgumentException | JacksonException malformed) {
            return Optional.empty();
        }
    }

    private static ClientRequest authenticated(ClientRequest request, SessionToken token) {
        return ClientRequest.from(request).headers(headers -> headers.setBearerAuth(token.value())).build();
    }

    private record SessionToken(String value, Instant renewAt) {
    }
}
