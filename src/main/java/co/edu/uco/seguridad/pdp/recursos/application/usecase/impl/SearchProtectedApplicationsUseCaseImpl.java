package co.edu.uco.seguridad.pdp.recursos.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.mapper.ProtectedResourceCatalogMapper;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.application.rulesvalidator.SearchProtectedApplicationsRulesValidator;
import co.edu.uco.seguridad.shared.observability.ReactiveLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Valida la consulta, pasa los criterios al repositorio sin cambios, y proyecta el resultado.
 *
 * <p>Nunca inspecciona los criterios para construir una consulta por sí mismo: interpretar la especificación es
 * trabajo del adaptador, que es lo que permite que el dummy escanee un mapa mientras SurrealDB traducirá el mismo
 * objeto a SurrealQL.</p>
 */
public final class SearchProtectedApplicationsUseCaseImpl implements SearchProtectedApplicationsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SearchProtectedApplicationsUseCaseImpl.class);

    private final SearchProtectedApplicationsRulesValidator rules;
    private final ProtectedResourceRepository resources;

    public SearchProtectedApplicationsUseCaseImpl(SearchProtectedApplicationsRulesValidator rules,
                                                   ProtectedResourceRepository resources) {
        this.rules = Objects.requireNonNull(rules, "se requiere validador de reglas");
        this.resources = Objects.requireNonNull(resources, "se requiere repositorio de recurso protegido");
    }

    @Override
    public Mono<ResultPage<ProtectedApplicationResponse>> execute(SearchProtectedApplicationsRequest dto) {
        return rules.execute(dto)
                .then(resources.findBy(dto.criteria(), dto.window()))
                .map(page -> page.map(ProtectedResourceCatalogMapper::toResponse))
                .transform(ReactiveLogContext.withContext(LOG, "protected_application.search"));
    }
}
