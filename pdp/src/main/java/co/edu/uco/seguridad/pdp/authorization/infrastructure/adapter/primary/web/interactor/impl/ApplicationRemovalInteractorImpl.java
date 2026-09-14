package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationRemovalUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationRemovalInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationAdministrationRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * {@code UserId} del llamador: viene de {@code principal.userId()} si ya está resuelto
 * ({@code LocalUserPrincipal}), o se resuelve vía {@code identity} por {@code subject} si no
 * (PLAN-HU-015.md §14).
 */
public final class ApplicationRemovalInteractorImpl implements ApplicationRemovalInteractor {

    private final AdministerApplicationRemovalUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public ApplicationRemovalInteractorImpl(AdministerApplicationRemovalUseCase useCase,
            SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REMOVE_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<Void> execute(ApplicationAdministrationRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .map(userId -> ApplicationAdministrationRequestMapper.toAdministrationRequest(raw, principal, userId)))
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
