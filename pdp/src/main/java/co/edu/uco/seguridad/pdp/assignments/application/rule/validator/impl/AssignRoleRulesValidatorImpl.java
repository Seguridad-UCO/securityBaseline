package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationOwnershipQuery;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignRoleRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustNotDuplicateActiveRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveAssignmentAvailability;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.UserMustExistValidator;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleCoverageQuery;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleScopeMustCoverApplicationValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Hace la E/S para las reglas de asignación, en orden de coste: el usuario existe (R1, prestada de
 * {@code identity}), la aplicación existe y es del tenant (R2, prestada de {@code applications}), el
 * rol existe y cubre la aplicación (R3, prestada de {@code roles}), y por último no hay ya una
 * asignación activa para la tripleta (R4, la más cara porque toca la tabla propia).
 *
 * <p>Corrección sobre PLAN-HU-005 §7: el constructor original no fijaba de dónde salía el "ahora"
 * para {@code AssignmentRepository.existsActiveByUserApplicationRole}, que sí lo exige. Se añadió
 * {@code TimeProvider} al detectarlo al escribir las pruebas — hueco del plan, no cambio de negocio.
 */
public final class AssignRoleRulesValidatorImpl implements AssignRoleRulesValidator {

    private final UserMustExistValidator userMustExist;
    private final ApplicationMustExistForTenantValidator applicationMustExist;
    private final RoleScopeMustCoverApplicationValidator roleScopeMustCoverApplication;
    private final AssignmentMustNotDuplicateActiveRule mustNotDuplicate;
    private final AssignmentRepository repository;
    private final TimeProvider time;

    public AssignRoleRulesValidatorImpl(UserMustExistValidator userMustExist,
                                        ApplicationMustExistForTenantValidator applicationMustExist,
                                        RoleScopeMustCoverApplicationValidator roleScopeMustCoverApplication,
                                        AssignmentMustNotDuplicateActiveRule mustNotDuplicate, AssignmentRepository repository, TimeProvider time) {
        this.userMustExist = Objects.requireNonNull(userMustExist, RequiredArgumentMessages.USER_MUST_EXIST_VALIDATOR);
        this.applicationMustExist = Objects.requireNonNull(applicationMustExist,
                RequiredArgumentMessages.APPLICATION_EXISTS_VALIDATOR);
        this.roleScopeMustCoverApplication = Objects.requireNonNull(roleScopeMustCoverApplication,
                RequiredArgumentMessages.ROLE_SCOPE_COVERS_APPLICATION_VALIDATOR);
        this.mustNotDuplicate = Objects.requireNonNull(mustNotDuplicate, RequiredArgumentMessages.ASSIGNMENT_NOT_DUPLICATE_RULE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(AssignRoleRequest input) {
        return userMustExist.execute(input.userId())
                .then(Mono.defer(() -> applicationMustExist.execute(
                        new ApplicationOwnershipQuery(input.tenantId(), input.applicationId()))))
                .then(Mono.defer(() -> roleScopeMustCoverApplication.execute(
                        new RoleCoverageQuery(input.roleId(), input.tenantId(), input.applicationId()))))
                .then(Mono.defer(() -> repository
                        .existsActiveByUserApplicationRole(input.userId(), input.applicationId(), input.roleId(), time.now())
                        .doOnNext(taken -> mustNotDuplicate.execute(
                                new ActiveAssignmentAvailability(input.userId(), input.applicationId(), input.roleId(), taken)))
                        .then()));
    }
}
