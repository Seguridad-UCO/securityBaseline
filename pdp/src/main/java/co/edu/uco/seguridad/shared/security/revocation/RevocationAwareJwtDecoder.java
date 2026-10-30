package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ResolveBearerIdentityUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.BearerIdentityClaim;
import co.edu.uco.seguridad.shared.security.BearerIdentityRequestMapper;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private final Function<Jwt, Mono<UserId>> userIdResolver;
    private final BiFunction<Jwt, UserId, Jwt> delivery;
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
        this(delegate, revocation, lookupOnly(Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR)), (jwt, userId) -> jwt,
                Objects.requireNonNull(technicalClientId, "technicalClientId"));
    }

    private RevocationAwareJwtDecoder(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
                                      Function<Jwt, Mono<UserId>> userIdResolver,
                                      BiFunction<Jwt, UserId, Jwt> delivery, String technicalClientId) {
        this.delegate = Objects.requireNonNull(delegate, RequiredArgumentMessages.REACTIVE_JWT_DECODER_DELEGATE);
        this.revocation = Objects.requireNonNull(revocation, RequiredArgumentMessages.TOKEN_REVOCATION_PORT);
        this.userIdResolver = userIdResolver;
        this.delivery = delivery;
        this.technicalClientId = technicalClientId;
    }

    /**
     * Ruta Bearer con aprovisionamiento (HU-038): resuelve —o crea de forma idempotente— la identidad
     * local del {@code sub} en el tenant del claim <b>antes</b> de consultar la revocación, y entrega
     * el {@code Jwt} sellado con el {@code UserId} resuelto (se sobrescribe cualquier claim interno
     * entrante). Cualquier fallo de la resolución es fail-closed (401).
     */
    public static RevocationAwareJwtDecoder provisioning(ReactiveJwtDecoder delegate, TokenRevocationPort revocation,
                                                         ResolveBearerIdentityUseCase identities) {
        Objects.requireNonNull(identities, RequiredArgumentMessages.RESOLVE_BEARER_IDENTITY_USE_CASE);
        return new RevocationAwareJwtDecoder(delegate, revocation,
                jwt -> Mono.defer(() -> identities.execute(BearerIdentityRequestMapper.toRequest(jwt))),
                (jwt, userId) -> BearerIdentityClaim.stamp(jwt, Optional.of(userId)), "");
    }

    @Override
    public Mono<Jwt> decode(String token) {
        return delegate.decode(token).flatMap(jwt -> isTechnicalPepToken(jwt) ? Mono.just(jwt) : userIdResolver.apply(jwt)
                .flatMap(userId -> revoked(userId, jwt)
                        .flatMap(revoked -> revoked ? Mono.<Jwt>error(revokedException()) : Mono.just(delivery.apply(jwt, userId))))
                .onErrorMap(RevocationAwareJwtDecoder::isCheckFailure, error -> revocationCheckFailedException(
                        "no se pudo verificar el estado de revocación del token")));
    }

    /** Solo una {@code Exception} es fail-closed: un {@code Error} de la JVM no se disfraza de 401. */
    private static boolean isCheckFailure(Throwable error) {
        return error instanceof Exception && !(error instanceof JwtException);
    }

    private static Function<Jwt, Mono<UserId>> lookupOnly(SubjectUserIdLookupValidator subjectUserIdLookup) {
        return jwt -> subjectUserIdLookup.execute(jwt.getSubject())
                .switchIfEmpty(Mono.error(() -> revocationCheckFailedException(
                        "no existe una identidad registrada para el subject del token; no se puede verificar su revocación")));
    }

    private Mono<Boolean> revoked(UserId userId, Jwt jwt) {
        return Mono.zip(
                        revocation.isRevoked(userId, jwt.getIssuedAt()),
                        revocation.isTokenRevoked(userId, jwt.getId()),
                        clientRevoked(userId, jwt))
                .map(revocationState -> revocationState.getT1() || revocationState.getT2() || revocationState.getT3());
    }

    /** HU-028 / ADR-034: solo consulta la revocación por cliente cuando el token trae {@code azp}. */
    private Mono<Boolean> clientRevoked(UserId userId, Jwt jwt) {
        String clientId = jwt.getClaimAsString("azp");
        return clientId == null || clientId.isBlank()
                ? Mono.just(false)
                : revocation.isClientRevoked(userId, clientId, jwt.getIssuedAt());
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
