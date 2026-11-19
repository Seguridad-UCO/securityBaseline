package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RoleDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

public final class RoleDeletionDependencyValidatorImpl implements RoleDeletionDependencyValidator {
    private final AssignmentRepository repository;
    private final TimeProvider time;

    public RoleDeletionDependencyValidatorImpl(AssignmentRepository repository, TimeProvider time) {
        this.repository = repository;
        this.time = time;
    }

    @Override
    public Mono<Void> execute(RoleId id) {
        return repository.existsActiveByRoleId(id, time.now()).flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("el rol")) : Mono.empty());
    }
}
