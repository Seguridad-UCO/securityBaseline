package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decoder del perfil de desarrollo (HMAC, {@code SecurityConfiguration}) coherente con el canal
 * Keycloak (HU-038): siempre elimina cualquier claim interno entrante y, si el {@code sub} ya tiene
 * identidad local, lo sella con su {@code UserId}. Es "suave": no aprovisiona ni rechaza por falta
 * de identidad (los tokens HMAC de desarrollo y de las pruebas HTTP no traen correo ni revocación).
 */
public final class LocalIdentityStampingJwtDecoder implements ReactiveJwtDecoder {

    private final ReactiveJwtDecoder delegate;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public LocalIdentityStampingJwtDecoder(ReactiveJwtDecoder delegate, SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.delegate = Objects.requireNonNull(delegate, RequiredArgumentMessages.REACTIVE_JWT_DECODER_DELEGATE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return delegate.decode(token).flatMap(jwt -> subjectUserIdLookup.execute(jwt.getSubject())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .onErrorMap(LocalIdentityStampingJwtDecoder::isLookupFailure, error -> lookupFailedException())
                .map(userId -> BearerIdentityClaim.stamp(jwt, userId)));
    }

    private static boolean isLookupFailure(Throwable error) {
        return error instanceof Exception && !(error instanceof JwtException);
    }

    private static JwtValidationException lookupFailedException() {
        String reason = "no se pudo resolver la identidad local del token";
        return new JwtValidationException(reason, List.of(new OAuth2Error("invalid_token", reason, null)));
    }
}
