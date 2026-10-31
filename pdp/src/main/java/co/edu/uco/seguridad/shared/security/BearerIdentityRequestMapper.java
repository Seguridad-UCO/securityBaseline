package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ResolveBearerIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.domain.exception.InvalidEmailException;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.Locale;
import java.util.Optional;

/**
 * Traduce los claims de un {@code Jwt} Bearer ya validado a la intención tipada de aprovisionar
 * (HU-038). Claim {@code tenant} ausente o inválido lanza la excepción del VO {@code TenantId};
 * {@code email} inválido o ausente se traduce a {@code Optional.empty()}.
 */
public final class BearerIdentityRequestMapper {

    private static final String TENANT_CLAIM = "tenant";
    private static final String EMAIL_CLAIM = "email";
    private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    private static final String NAME_CLAIM = "name";
    private static final String USERNAME_CLAIM = "preferred_username";
    private static final String IDENTITY_PROVIDER_CLAIM = "identity_provider";
    private static final String DEFAULT_PROVIDER = "keycloak-local";

    private BearerIdentityRequestMapper() {
    }

    public static ResolveBearerIdentityRequest toRequest(Jwt jwt) {
        return new ResolveBearerIdentityRequest(
                jwt.getClaimAsString(JwtClaimNames.ISS),
                jwt.getSubject(),
                new TenantId(jwt.getClaimAsString(TENANT_CLAIM)),
                email(jwt),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM)),
                displayName(jwt),
                provider(jwt));
    }

    private static Optional<Email> email(Jwt jwt) {
        try {
            return Optional.ofNullable(jwt.getClaimAsString(EMAIL_CLAIM)).map(Email::new);
        } catch (InvalidEmailException invalid) {
            return Optional.empty();
        }
    }

    /** Nombre para mostrar: {@code name}; si el cliente no lo emite, {@code preferred_username}; si no, vacío. */
    private static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString(NAME_CLAIM);
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        return username == null ? null : username.trim();
    }

    private static String provider(Jwt jwt) {
        String provider = jwt.getClaimAsString(IDENTITY_PROVIDER_CLAIM);
        return provider == null || provider.isBlank() ? DEFAULT_PROVIDER : provider.trim().toLowerCase(Locale.ROOT);
    }
}
