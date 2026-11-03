package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignApplicationAdministratorInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper.AssignmentResponseMapper;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AssignApplicationAdministratorInteractor} (HU-015, backfill manual).
 * Pendiente: resolver {@code tenantId} vía {@link ApplicationOwnerLookupValidator} a partir del
 * {@code applicationId} (el tenant nunca viaja en el cuerpo, ni siquiera en el canal interno),
 * construir {@link AssignApplicationAdministratorRequest} y delegar en el caso de uso.
 */
public final class AssignApplicationAdministratorInteractorImpl implements AssignApplicationAdministratorInteractor {

    private final ApplicationOwnerLookupValidator ownerLookup;
    private final AssignApplicationAdministratorUseCase useCase;

    public AssignApplicationAdministratorInteractorImpl(ApplicationOwnerLookupValidator ownerLookup,
                                                        AssignApplicationAdministratorUseCase useCase) {
        this.ownerLookup = Objects.requireNonNull(ownerLookup, RequiredArgumentMessages.APPLICATION_OWNER_LOOKUP_VALIDATOR);
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<AssignmentWebResponse> execute(AssignApplicationAdministratorRawRequest raw) {
        return Mono.fromCallable(() -> RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of))
                .flatMap(applicationId -> ownerLookup.execute(applicationId)
                        .map(tenantId -> new AssignApplicationAdministratorRequest(tenantId, applicationId,
                                RequestFieldParser.parse("userId", raw.userId(), UserId::of))))
                .flatMap(useCase::execute)
                .map(AssignmentResponseMapper::toResponse);
    }
}
