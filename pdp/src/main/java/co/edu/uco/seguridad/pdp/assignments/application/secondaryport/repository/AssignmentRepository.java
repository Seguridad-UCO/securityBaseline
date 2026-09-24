package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/** Puerto secundario del catálogo de asignaciones, expresado solo en tipos de dominio. */
public interface AssignmentRepository {

    /** Si ya hay una asignación activa para esa tripleta a la fecha {@code now}. */
    Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId, Instant now);

    /** Vacío si la asignación no existe o no es de ese tenant. */
    Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId);

    Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window);

    /** Los identificadores de rol con asignación activa para (usuario, aplicación) a la fecha {@code now}. */
    Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now);

    Mono<Assignment> save(Assignment assignment);

    default Mono<Boolean> existsActiveByRoleId(RoleId roleId, Instant now) { return Mono.just(false); }
}
