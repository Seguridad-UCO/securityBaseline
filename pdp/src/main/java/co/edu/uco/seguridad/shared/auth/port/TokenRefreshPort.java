package co.edu.uco.seguridad.shared.auth.port;

import co.edu.uco.seguridad.shared.auth.model.TokenRefreshResult;
import reactor.core.publisher.Mono;

public interface TokenRefreshPort {
    Mono<TokenRefreshResult> refreshToken(String sessionId);
}
