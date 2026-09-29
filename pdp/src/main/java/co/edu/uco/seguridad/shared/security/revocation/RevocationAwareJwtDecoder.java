package co.edu.uco.seguridad.shared.security.revocation;

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

/**
 * Decora un {@link ReactiveJwtDecoder} delegado con la verificación de revocación (HU-022, ADR-026)
 * — se ejecuta dentro de la cadena de autenticación, después de que el delegado valida
 * firma/issuer/claims y antes de que el {@code Jwt} se resuelva en {@code PdpPrincipal}. Envuelve
 * {@code internalEvidenceJwtDecoder} de {@code InternalSecurityConfiguration} (PLAN-HU-022.md §1 —
 * el canal interno PEP→PDP es el que protege el flujo BFF real, no el modo dev/HMAC).
 *
 * <p>Resuelve el {@code UserId} del {@code sub} vía {@link SubjectUserIdLookupValidator} y consulta
 * {@link TokenRevocationPort#isRevoked}. Fail-closed en tres casos — revocado, sin {@code UserId}
 * resuelto (PLAN-HU-022.md §11 punto 4) y falla de la consulta misma (Redis caído, §3 regla 2) —,
 * todos con {@link JwtValidationException} (subclase de {@code JwtException}) para que
 * {@code ApiAuthenticationEntryPoint} responda 401 sin tocar {@code ApiErrorHandler}. Un fallo del
 * {@code delegate} (firma/issuer inválidos) se propaga sin envolver: nunca llega a consultar el
 * lookup de {@code UserId} ni la revocación.</p>
 */
public final class RevocationAwareJwtDecoder implements ReactiveJwtDecoder {

    private final ReactiveJwtDecoder delegate;
    private final TokenRevocationPort revocation;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final String technicalClientId;

    public RevocationAwareJwtDecoder(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
                                     SubjectUserIdLookupValidator subjectUserIdLookup) {
        this(delegate, revocation, subjectUserIdLookup, "");
    }

    /**
     * El token de client-credentials del PEP solo autentica el canal técnico: ya pasó firma,
     * emisor y audiencia en {@code delegate}, pero no representa a una persona del catálogo de
     * identidades y por eso no participa en su lista de revocación.
     */
    public RevocationAwareJwtDecoder(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
                                     SubjectUserIdLookupValidator subjectUserIdLookup, String technicalClientId) {
        this.delegate = Objects.requireNonNull(delegate, RequiredArgumentMessages.REACTIVE_JWT_DECODER_DELEGATE);
        this.revocation = Objects.requireNonNull(revocation, RequiredArgumentMessages.TOKEN_REVOCATION_PORT);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.technicalClientId = Objects.requireNonNull(technicalClientId, "technicalClientId");
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return delegate.decode(token).flatMap(jwt -> isTechnicalPepToken(jwt) ? Mono.just(jwt) : subjectUserIdLookup.execute(jwt.getSubject())
                .switchIfEmpty(Mono.error(() -> revocationCheckFailedException(
                        "no existe una identidad registrada para el subject del token; no se puede verificar su revocación")))
                .flatMap(userId -> revocation.isRevoked(userId, jwt.getIssuedAt()))
                .onErrorMap(error -> !(error instanceof JwtException), error -> revocationCheckFailedException(
                        "no se pudo verificar el estado de revocación del token"))
                .flatMap(revoked -> revoked ? Mono.<Jwt>error(revokedException()) : Mono.just(jwt)));
    }

    private boolean isTechnicalPepToken(Jwt jwt) {
        return !technicalClientId.isBlank() && technicalClientId.equals(jwt.getClaimAsString("azp"));
    }

    private static JwtValidationException revocationCheckFailedException(String reason) {
        return new JwtValidationException(reason, List.of(new OAuth2Error("invalid_token", reason, null)));
    }

    private static JwtValidationException revokedException() {
        return revocationCheckFailedException("el token fue emitido antes de la última revocación del sujeto");
    }
}
