package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorListRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorListUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationAdministratorsRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityUserWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorListInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implementación de {@link AdministerApplicationAdministratorListInteractor} (HU-020).
 */
public final class AdministerApplicationAdministratorListInteractorImpl
        implements AdministerApplicationAdministratorListInteractor {

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final AdministerApplicationAdministratorListUseCase useCase;

    public AdministerApplicationAdministratorListInteractorImpl(ApplicationOwnerLookupValidator ownerLookup,
                                                                SubjectUserIdLookupValidator subjectUserIdLookup, AdministerApplicationAdministratorListUseCase useCase) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ADMINISTER_APPLICATION_ADMINISTRATOR_LIST_USE_CASE);
    }

    @Override
    public Mono<List<ApplicationAdministratorWebResponse>> execute(ListApplicationAdministratorsRawRequest raw) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> Mono.zip(ownerLookup.execute(applicationId), resolveUserId(principal))
                        .map(tuple -> new AdministerApplicationAdministratorListRequest(
                                new AdministrationRequest(tuple.getT1(), applicationId, tuple.getT2(), principal.subject(),
                                        Set.of(), principal.authenticationContext()),
                                new ListApplicationAdministratorsRequest(tuple.getT1(), applicationId))))
                .flatMap(useCase::execute)
                .map(responses -> responses.stream()
                        .map(AdministerApplicationAdministratorListInteractorImpl::toWebResponse)
                        .toList());
    }

    private static ApplicationAdministratorWebResponse toWebResponse(AssignmentResponse response) {
        return new ApplicationAdministratorWebResponse(new ApplicationSecurityUserWebResponse(response.userId().value().toString(), "", ""),
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
