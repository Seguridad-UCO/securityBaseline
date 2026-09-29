package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.Validity;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.ProfileAssignmentEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fila a dominio. Los value objects validan aquí. Espejo de AssignmentPersistenceMapper.
 */
public final class ProfileAssignmentPersistenceMapper {

    private ProfileAssignmentPersistenceMapper() {
    }

    public static ProfileAssignment toDomain(ProfileAssignmentEntity entity) {
        Validity validity = new Validity(Instant.parse(entity.validFrom()),
                Optional.ofNullable(entity.validUntil()).map(Instant::parse));
        Set<AssignmentId> generatedAssignmentIds = entity.generatedAssignmentIds().stream()
                .map(AssignmentId::of)
                .collect(Collectors.toSet());
        return new ProfileAssignment(ProfileAssignmentId.of(entity.id()), UserId.of(entity.userId()),
                new TenantId(entity.tenantId()), ApplicationId.of(entity.applicationId()),
                ProfileId.of(entity.profileId()), generatedAssignmentIds, validity);
    }
}
