package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.pdp.resources.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Valida la consulta y consulta el repositorio. Devuelve dominio; el interactor proyecta al DTO.
 */
public final class SearchProtectedApplicationsUseCaseImpl implements SearchProtectedApplicationsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SearchProtectedApplicationsUseCaseImpl.class);

    private final SearchProtectedApplicationsRulesValidator rules;
    private final ProtectedResourceRepository resources;

    public SearchProtectedApplicationsUseCaseImpl(SearchProtectedApplicationsRulesValidator rules,
                                                   ProtectedResourceRepository resources) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.RULES_VALIDATOR);
        this.resources = Objects.requireNonNull(resources, RequiredArgumentMessages.PROTECTED_RESOURCE_REPOSITORY);
    }

    @Override
    public Mono<ResultPage<ProtectedResource>> execute(SearchProtectedApplicationsRequest dto) {
        return rules.execute(dto)
                .then(resources.findBy(dto.criteria(), dto.window()))
                .transform(ReactiveLogContext.withContext(LOG, "protected_application.search"));
    }
}
