package co.edu.uco.seguridad.shared.auth.service;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Valida retornos post-login contra orígenes de aplicaciones registradas para evitar open redirects. */
@Service
public final class OidcReturnTargetPolicy {
    private final List<String> allowedOrigins;

    public OidcReturnTargetPolicy(@Value("${pdp.security.login.allowed-return-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public String validate(String value) {
        URI target;
        try {
            target = URI.create(value);
        } catch (IllegalArgumentException error) {
            throw invalidTarget();
        }
        if (!target.isAbsolute() || target.getRawUserInfo() != null || target.getRawFragment() != null
                || !allowedOrigins.contains(target.getScheme() + "://" + target.getAuthority())) {
            throw invalidTarget();
        }
        return target.toString();
    }

    private static ResponseStatusException invalidTarget() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El destino de retorno no está autorizado.");
    }
}
