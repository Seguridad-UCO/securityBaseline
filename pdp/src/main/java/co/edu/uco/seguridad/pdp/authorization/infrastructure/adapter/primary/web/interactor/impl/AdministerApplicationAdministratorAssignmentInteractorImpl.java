package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorAssignmentRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorAssignmentInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
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
 * Implementación de {@link AdministerApplicationAdministratorAssignmentInteractor} (HU-020).
 * Resuelve {@code tenantId} vía {@link ApplicationOwnerLookupValidator} desde el
 * {@code applicationId} de la ruta — nunca del principal ni del cuerpo, mismo patrón que
 * {@code AssignApplicationAdministratorInteractorImpl} (HU-015).
 */
public final class AdministerApplicationAdministratorAssignmentInteractorImpl
        implements AdministerApplicationAdministratorAssignmentInteractor {

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final AdministerApplicationAdministratorAssignmentUseCase useCase;

    public AdministerApplicationAdministratorAssignmentInteractorImpl(ApplicationOwnerLookupValidator ownerLookup,
            SubjectUserIdLookupValidator subjectUserIdLookup, AdministerApplicationAdministratorAssignmentUseCase useCase) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.useCase = Objects.requireNonNull(useCase,
                RequiredArgumentMessages.ADMINISTER_APPLICATION_ADMINISTRATOR_ASSIGNMENT_USE_CASE);
    }

    @Override
    public Mono<ApplicationAdministratorWebResponse> execute(AssignApplicationAdministratorRawRequest raw) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        UserId newAdministratorId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> Mono.zip(ownerLookup.execute(applicationId), resolveUserId(principal))
                        .map(tuple -> new AdministerApplicationAdministratorAssignmentRequest(
                                new AdministrationRequest(tuple.getT1(), applicationId, tuple.getT2(), principal.subject(),
                                        Set.of()),
                                new AssignApplicationAdministratorRequest(tuple.getT1(), applicationId, newAdministratorId))))
                .flatMap(useCase::execute)
                .map(AdministerApplicationAdministratorAssignmentInteractorImpl::toWebResponse);
    }

    private static ApplicationAdministratorWebResponse toWebResponse(AssignmentResponse response) {
        return new ApplicationAdministratorWebResponse(response.userId().value().toString(),
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
