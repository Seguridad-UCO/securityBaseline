package co.edu.uco.seguridad.shared.auth.port;

import reactor.core.publisher.Mono;

import java.time.Instant;

public interface RefreshTokenStorePort {
    Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt);

    Mono<String> find(String sessionId);

    Mono<Void> delete(String sessionId);
}
