package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Claim interno por el que el decoder Bearer entrega al principal el {@code UserId} local ya
 * resuelto (HU-038, decisión 2). No es un claim del IdP: todo decoder Bearer lo <b>sobrescribe</b>
 * (o lo elimina si no hay identidad) y nunca se confía en un valor que venga del token original.
 */
public final class BearerIdentityClaim {

    /** Nombre del claim interno. */
    public static final String NAME = "pdp_user_id";

    private BearerIdentityClaim() {
    }

    /** Devuelve un {@code Jwt} cuyo claim interno es exactamente {@code userId} (o ausente si vacío). */
    public static Jwt stamp(Jwt jwt, Optional<UserId> userId) {
        if (userId.isEmpty() && !jwt.hasClaim(NAME)) {
            return jwt;
        }
        return Jwt.withTokenValue(jwt.getTokenValue())
                .headers(headers -> headers.putAll(jwt.getHeaders()))
                .claims(claims -> {
                    claims.putAll(jwt.getClaims());
                    claims.remove(NAME);
                    userId.ifPresent(id -> claims.put(NAME, id.value().toString()));
                })
                .build();
    }

    /** Lee el claim interno; vacío si no está. Un valor que no sea UUID lanza {@code InvalidIdentifierException}. */
    public static Optional<UserId> read(Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsString(NAME)).map(UserId::of);
    }
}
