package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/**
 * Puerto secundario del catálogo de asignaciones, expresado solo en tipos de dominio.
 */
public interface AssignmentRepository {

    /**
     * Si ya hay una asignación activa para esa tripleta a la fecha {@code now}.
     */
    Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId, Instant now);

    /**
     * Vacío si la asignación no existe o no es de ese tenant.
     */
    Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId);

    Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window);

    /**
     * Página de asignaciones directamente acotada por tenant y aplicación; nunca compone páginas por rol.
     */
    default Mono<ResultPage<Assignment>> findPageByApplication(TenantId tenantId, ApplicationId applicationId,
                                                               PageWindow window) {
        return Mono.error(new UnsupportedOperationException("La consulta paginada no está implementada"));
    }
    default Mono<Long> countByApplication(TenantId tenantId, ApplicationId applicationId) { return Mono.error(new UnsupportedOperationException()); }

    default Mono<ResultPage<Assignment>> findActivePageByRoleAndApplication(RoleId roleId, TenantId tenantId,
            ApplicationId applicationId, Instant now, PageWindow window) {
        return Mono.error(new UnsupportedOperationException("La consulta paginada no está implementada"));
    }
    default Mono<Long> countActiveByRoleAndApplication(RoleId roleId, TenantId tenantId, ApplicationId applicationId,
            Instant now) { return Mono.error(new UnsupportedOperationException()); }

    /**
     * Los identificadores de rol con asignación activa para (usuario, aplicación) a la fecha {@code now}.
     */
    Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now);

    Mono<Assignment> save(Assignment assignment);

    default Mono<Boolean> existsActiveByRoleId(RoleId roleId, Instant now) {
        return Mono.just(false);
    }

    /**
     * True si hay asignaciones de roles, vigentes o históricas, para la aplicación.
     */
    default Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return Mono.just(false);
    }
}
