package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Puerto secundario de revocación de tokens (HU-022, ADR-026). No extiende un contrato de una sola
 * operación de {@code shared/contract} a propósito: tiene dos operaciones distintas, mismo criterio
 * que un puerto de repositorio con varios métodos.
 *
 * <p>El modelo es "todo lo anterior a T" por sujeto, no un conjunto de {@code jti} — decisión
 * confirmada con Sebastián (PLAN-HU-022.md §11): revocar invalida todas las sesiones activas del
 * sujeto, no una puntual.
 */
public interface TokenRevocationPort {

    /**
     * Marca como revocado todo token del sujeto emitido antes o en {@code since}.
     */
    Mono<Void> revokeAllSince(UserId subject, Instant since);

    /**
     * Revoca únicamente el token identificado por su {@code jti} hasta que expire.
     */
    Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt);

    /**
     * {@code true} si {@code issuedAt} es anterior o igual al último {@code revokeAllSince} del sujeto.
     */
    Mono<Boolean> isRevoked(UserId subject, Instant issuedAt);

    /**
     * {@code true} si el token identificado por su {@code jti} fue revocado explícitamente.
     */
    Mono<Boolean> isTokenRevoked(UserId subject, String tokenId);

    /**
     * HU-028 / ADR-034 [N]: marca como revocado todo token del sujeto emitido al cliente
     * {@code clientId} ({@code azp}) antes o en {@code since}. No afecta a otros clientes del mismo
     * sujeto.
     */
    Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since);

    /**
     * HU-028 / ADR-034 [N]: {@code true} si {@code issuedAt} es anterior o igual al último
     * {@link #revokeClientSince} del par (sujeto, cliente). Fail-closed igual que el resto.
     */
    Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt);
}
