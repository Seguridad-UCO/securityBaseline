package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RegisterApplicationWithFirstAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithFirstAdministratorInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.ApplicationRegisteredResponseMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.RegisterApplicationWithFirstAdministratorRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Pendiente de cablear a un {@code @PostMapping} (HU-015): la ruta {@code POST /api/v1/applications}
 * sigue siendo de {@code ApplicationController} (applications) hasta que el implementador la retire
 * de allí en el mismo cambio que active este interactor aquí (ver PLAN-HU-015.md árbol §8). El
 * {@code UserId} del registrador sale de {@code principal.userId()} si ya está resuelto
 * ({@code LocalUserPrincipal}, login real); si no (JWT crudo — el único modo que ejercitan las
 * pruebas HTTP del proyecto), se resuelve vía {@code identity} por {@code subject}
 * (PLAN-HU-015.md §14).
 */
public final class RegisterApplicationWithFirstAdministratorInteractorImpl
        implements RegisterApplicationWithFirstAdministratorInteractor {

    private final RegisterApplicationWithFirstAdministratorUseCase useCase;
    private final SubjectUserIdLookupValidator subjectUserIdLookup;

    public RegisterApplicationWithFirstAdministratorInteractorImpl(RegisterApplicationWithFirstAdministratorUseCase useCase,
                                                                   SubjectUserIdLookupValidator subjectUserIdLookup) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_APPLICATION_USE_CASE);
        this.subjectUserIdLookup = Objects.requireNonNull(subjectUserIdLookup,
                RequiredArgumentMessages.SUBJECT_USER_ID_LOOKUP_VALIDATOR);
    }

    @Override
    public Mono<ApplicationRegisteredWebResponse> execute(RegisterApplicationWithFirstAdministratorRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> resolveUserId(principal)
                        .map(userId -> RegisterApplicationWithFirstAdministratorRequestMapper.toRequest(raw,
                                principal.tenantId(), userId)))
                .flatMap(useCase::execute)
                .map(ApplicationRegisteredResponseMapper::toWebResponse);
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
