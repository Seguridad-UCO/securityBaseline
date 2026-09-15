package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorRemovalRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationAdministratorRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RemoveApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorRemovalInteractor;
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

/** Implementación de {@link AdministerApplicationAdministratorRemovalInteractor} (HU-020). */
public final class AdministerApplicationAdministratorRemovalInteractorImpl
        implements AdministerApplicationAdministratorRemovalInteractor {

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;
    private final AdministerApplicationAdministratorRemovalUseCase useCase;

    public AdministerApplicationAdministratorRemovalInteractorImpl(ApplicationOwnerLookupValidator ownerLookup,
            SubjectUserIdLookupValidator subjectUserIdLookup, AdministerApplicationAdministratorRemovalUseCase useCase) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
        this.useCase = Objects.requireNonNull(useCase,
                RequiredArgumentMessages.ADMINISTER_APPLICATION_ADMINISTRATOR_REMOVAL_USE_CASE);
    }

    @Override
    public Mono<Void> execute(RemoveApplicationAdministratorRawRequest raw) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        UserId targetUserId = RequestFieldParser.parse("userId", raw.userId(), UserId::of);
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> Mono.zip(ownerLookup.execute(applicationId), resolveUserId(principal))
                        .map(tuple -> new AdministerApplicationAdministratorRemovalRequest(
                                new AdministrationRequest(tuple.getT1(), applicationId, tuple.getT2(), principal.subject(),
                                        Set.of()),
                                new RemoveApplicationAdministratorRequest(tuple.getT1(), applicationId, targetUserId))))
                .flatMap(useCase::execute);
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
