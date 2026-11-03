package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ApplicationDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import reactor.core.publisher.Mono;

public final class ApplicationDeletionDependencyValidatorImpl implements ApplicationDeletionDependencyValidator {
    private final AssignmentRepository assignments;
    private final ProfileAssignmentRepository profileAssignments;

    public ApplicationDeletionDependencyValidatorImpl(AssignmentRepository assignments,
                                                      ProfileAssignmentRepository profileAssignments) {
        this.assignments = assignments;
        this.profileAssignments = profileAssignments;
    }

    @Override
    public Mono<Void> execute(ApplicationId id) {
        return assignments.existsByApplicationId(id)
                .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("la aplicación"))
                        : profileAssignments.existsByApplicationId(id))
                .flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("la aplicación")) : Mono.empty());
    }
}
