package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ListApplicationsInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ListApplicationsRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Traduce la consulta HTTP y la proyecta de vuelta.
 *
 * <p>El inquilino sale del principal autenticado y se inyecta en el criterio aquí, de modo que
 * ningún parámetro de la petición pueda influir en qué inquilino se consulta.
 */
public final class ListApplicationsInteractorImpl implements ListApplicationsInteractor {

    private final ListApplicationsUseCase useCase;

    public ListApplicationsInteractorImpl(ListApplicationsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.SEARCH_USE_CASE);
    }

    @Override
    public Mono<PageResponse<ApplicationWebResponse>> execute(ListApplicationsRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> ListApplicationsRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(page -> new PageResponse<>(
                        page.content().stream().map(ApplicationResponseMapper::toResponse).toList(),
                        page.total(),
                        page.window().page(),
                        page.window().offset(),
                        page.window().limit()));
    }
}
