package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.commons.exception.CatalogItemInUseException;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;
public final class ProfileDeletionDependencyValidatorImpl implements ProfileDeletionDependencyValidator {
 private final ProfileAssignmentRepository repository; private final TimeProvider time;
 public ProfileDeletionDependencyValidatorImpl(ProfileAssignmentRepository repository, TimeProvider time) { this.repository=repository; this.time=time; }
 @Override public Mono<Void> execute(ProfileId id) { return repository.existsActiveByProfileId(id,time.now()).flatMap(inUse -> inUse ? Mono.error(new CatalogItemInUseException("el perfil")) : Mono.empty()); }
}
