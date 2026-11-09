package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Caché de lecturas de alta frecuencia y baja volatilidad relativa (HU-023, ADR-026) — hoy solo
 * roles activos de un sujeto por aplicación. Puerto separado de {@code TokenRevocationPort}
 * (HU-022): uno es rendimiento, el otro seguridad, y ADR-026 los mantiene sin mezclar.
 *
 * <p><b>Las tres operaciones son fail-open por contrato: ninguna propaga un error de Redis.</b>
 * {@code get} resuelve {@code Mono.empty()} (se trata igual que un miss) si Redis no responde;
 * {@code put}/{@code evict} siempre completan. Contraste deliberado con {@code TokenRevocationPort},
 * que es fail-closed en las dos direcciones porque protege una decisión de seguridad, no de
 * rendimiento (PLAN-HU-023.md §7).</p>
 */
public interface DistributedCachePort {

    Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId);

    Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds);

    Mono<Void> evict(UserId subject, ApplicationId applicationId);
}
