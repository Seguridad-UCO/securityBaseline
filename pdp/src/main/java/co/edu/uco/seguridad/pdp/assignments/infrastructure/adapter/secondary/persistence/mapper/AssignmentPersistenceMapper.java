package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.Validity;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.AssignmentEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

import java.time.Instant;
import java.util.Optional;

/** Fila a dominio. Los value objects validan aquí. */
public final class AssignmentPersistenceMapper {

    private AssignmentPersistenceMapper() {
    }

    public static Assignment toDomain(AssignmentEntity entity) {
        Validity validity = new Validity(Instant.parse(entity.validFrom()),
                Optional.ofNullable(entity.validUntil()).map(Instant::parse));
        return new Assignment(AssignmentId.of(entity.id()), UserId.of(entity.userId()), new TenantId(entity.tenantId()),
                ApplicationId.of(entity.applicationId()), RoleId.of(entity.roleId()), validity);
    }
}
