package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentCreationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AssignmentAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentCreationInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerAssignmentCreationInteractor} (HU-018). Resuelve el principal
 * y su {@code UserId} (mismo patrón {@code resolveUserId} que
 * {@code AdministerResourceRegistrationInteractorImpl}), mapea el raw a {@code AssignRoleRequest} y
 * construye el {@code AdministrationRequest} directamente desde {@code applicationId} — sin
 * {@code Optional}: siempre presente.
 */
public final class AdministerAssignmentCreationInteractorImpl implements AdministerAssignmentCreationInteractor {

    private final AdministerAssignmentCreationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public AdministerAssignmentCreationInteractorImpl(AdministerAssignmentCreationUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_ASSIGNMENT_CREATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<AssignmentAdministrationWebResponse> execute(AssignRoleRawRequest input) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal).map(userId -> toAdministerRequest(input, principal, userId)))
                .flatMap(useCase::execute)
                .map(AdministerAssignmentCreationInteractorImpl::toWebResponse);
    }

    private static AdministerAssignmentCreationRequest toAdministerRequest(AssignRoleRawRequest raw,
            PdpPrincipal principal, UserId subjectUserId) {
        RoleId roleId = RequestFieldParser.parse("roleId", raw.roleId(), RoleId::of);
        UserId userId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        AssignRoleRequest assignment = new AssignRoleRequest(principal.tenantId(), userId, applicationId, roleId);
        AdministrationRequest administration = new AdministrationRequest(principal.tenantId(), applicationId,
                subjectUserId, principal.subject(), Set.of(), principal.authenticationContext());
        return new AdministerAssignmentCreationRequest(administration, assignment);
    }

    private static AssignmentAdministrationWebResponse toWebResponse(AssignmentResponse response) {
        return new AssignmentAdministrationWebResponse(response.id().value().toString(),
                response.userId().value().toString(), response.tenantId().value(),
                response.applicationId().value().toString(), response.roleId().value().toString(),
                response.validFrom().toString(), response.validUntil().map(Object::toString).orElse(null));
    }

    private Mono<UserId> resolveUserId(PdpPrincipal principal) {
        return principal.userId()
                .map(Mono::just)
                .orElseGet(() -> subjectUserIdLookup.execute(principal.subject()))
                .switchIfEmpty(Mono.error(() -> new IllegalStateException(
                        "No fue posible resolver el UserId del llamador: el principal no lo trae y no hay "
                                + "ninguna identidad externa vinculada a su subject")));
    }
}
