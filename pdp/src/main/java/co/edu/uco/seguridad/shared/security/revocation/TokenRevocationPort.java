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

    /** Marca como revocado todo token del sujeto emitido antes o en {@code since}. */
    Mono<Void> revokeAllSince(UserId subject, Instant since);

    /** {@code true} si {@code issuedAt} es anterior o igual al último {@code revokeAllSince} del sujeto. */
    Mono<Boolean> isRevoked(UserId subject, Instant issuedAt);
}
