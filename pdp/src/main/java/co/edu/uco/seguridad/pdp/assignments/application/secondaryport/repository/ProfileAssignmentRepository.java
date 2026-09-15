package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/** Puerto secundario del catálogo de asignaciones de perfil, expresado solo en tipos de dominio. */
public interface ProfileAssignmentRepository {

    /** Si ya hay una asignación de ese perfil activa para (usuario, aplicación) a la fecha {@code now}. */
    Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId, ProfileId profileId,
            Instant now);

    /** Vacío si la asignación no existe o no es de ese tenant. */
    Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId);

    Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window);

    /** Identificadores de perfiles asignados y vigentes para el sujeto y la aplicación. */
    default Mono<Set<ProfileId>> findActiveProfileIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
        return Mono.just(Set.of());
    }

    Mono<ProfileAssignment> save(ProfileAssignment profileAssignment);
}
