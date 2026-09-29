package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignProfileRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.ProfileAssignmentMustNotDuplicateActiveRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveProfileAssignmentAvailability;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Hace la E/S para las reglas de asignación de perfil: el perfil existe y devuelve sus roles
 * (prestado de {@code profiles}), y no hay ya una asignación activa de ese perfil para (usuario,
 * aplicación). El resto (existencia de usuario/aplicación, cobertura de alcance por rol) lo aplica
 * {@code AssignRoleUseCase} al materializar cada rol — no se repite aquí.
 */
public final class AssignProfileRulesValidatorImpl implements AssignProfileRulesValidator {

    private final ProfileRolesLookupValidator profileRolesLookup;
    private final ProfileAssignmentRepository repository;
    private final ProfileAssignmentMustNotDuplicateActiveRule mustNotDuplicate;
    private final TimeProvider time;

    public AssignProfileRulesValidatorImpl(ProfileRolesLookupValidator profileRolesLookup,
                                           ProfileAssignmentRepository repository, ProfileAssignmentMustNotDuplicateActiveRule mustNotDuplicate,
                                           TimeProvider time) {
        this.profileRolesLookup = Objects.requireNonNull(profileRolesLookup,
                RequiredArgumentMessages.PROFILE_ROLES_LOOKUP_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.mustNotDuplicate = Objects.requireNonNull(mustNotDuplicate,
                RequiredArgumentMessages.PROFILE_ASSIGNMENT_NOT_DUPLICATE_RULE);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Set<RoleId>> execute(AssignProfileRequest input) {
        return profileRolesLookup.execute(new ProfileOwnershipQuery(input.tenantId(), input.profileId()))
                .flatMap(roleIds -> repository
                        .existsActiveByUserApplicationProfile(input.userId(), input.applicationId(), input.profileId(),
                                time.now())
                        .doOnNext(taken -> mustNotDuplicate.execute(
                                new ActiveProfileAssignmentAvailability(input.userId(), input.applicationId(),
                                        input.profileId(), taken)))
                        .thenReturn(roleIds));
    }
}
