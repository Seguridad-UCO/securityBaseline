package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/**
 * Puerto secundario del catálogo de asignaciones de perfil, expresado solo en tipos de dominio.
 */
public interface ProfileAssignmentRepository {

    /**
     * Si ya hay una asignación de ese perfil activa para (usuario, aplicación) a la fecha {@code now}.
     */
    Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId, ProfileId profileId,
                                                       Instant now);

    /**
     * Vacío si la asignación no existe o no es de ese tenant.
     */
    Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId);

    Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window);

    /**
     * Página de asignaciones directamente acotada por tenant y aplicación; evita el agregado en memoria.
     */
    default Mono<ResultPage<ProfileAssignment>> findPageByApplication(TenantId tenantId, ApplicationId applicationId,
                                                                      PageWindow window) {
        return Mono.error(new UnsupportedOperationException("La consulta paginada no está implementada"));
    }
    default Mono<Long> countByApplication(TenantId tenantId, ApplicationId applicationId) { return Mono.error(new UnsupportedOperationException()); }

    default Mono<ResultPage<ProfileAssignment>> findActivePageByProfileAndApplication(ProfileId profileId, TenantId tenantId,
            ApplicationId applicationId, Instant now, PageWindow window) { return Mono.error(new UnsupportedOperationException()); }
    default Mono<ResultPage<ProfileAssignment>> findActivePageByUserAndApplication(UserId userId, TenantId tenantId,
            ApplicationId applicationId, Instant now, PageWindow window) { return Mono.error(new UnsupportedOperationException()); }

    /**
     * Identificadores de perfiles asignados y vigentes para el sujeto y la aplicación.
     */
    default Mono<Set<ProfileId>> findActiveProfileIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
        return Mono.just(Set.of());
    }

    Mono<ProfileAssignment> save(ProfileAssignment profileAssignment);

    default Mono<Boolean> existsActiveByProfileId(ProfileId profileId, Instant now) {
        return Mono.just(false);
    }

    /**
     * True si hay asignaciones de perfiles, vigentes o históricas, para la aplicación.
     */
    default Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return Mono.just(false);
    }
}
