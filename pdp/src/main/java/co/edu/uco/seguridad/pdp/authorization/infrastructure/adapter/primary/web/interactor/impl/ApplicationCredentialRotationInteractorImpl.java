package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationCredentialRotationUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationCredentialRotationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.AdministeredApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationAdministrationRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * {@code UserId} del llamador resuelto igual que {@link ApplicationRemovalInteractorImpl}
 * (PLAN-HU-015.md §14).
 */
public final class ApplicationCredentialRotationInteractorImpl implements ApplicationCredentialRotationInteractor {

    private final AdministerApplicationCredentialRotationUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public ApplicationCredentialRotationInteractorImpl(AdministerApplicationCredentialRotationUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<AdministeredApplicationWebResponse> execute(ApplicationAdministrationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .map(userId -> ApplicationAdministrationRequestMapper.toAdministrationRequest(raw, principal, userId)))
                .flatMap(useCase::execute)
                .map(AdministeredApplicationResponseMapper::toWebResponse);
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
