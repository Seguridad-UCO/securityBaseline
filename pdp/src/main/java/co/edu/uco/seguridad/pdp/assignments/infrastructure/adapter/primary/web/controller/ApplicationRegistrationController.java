package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithFirstAdministratorInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Registro de aplicaciones con alta automática del primer administrador (HU-015). Vive en
 * {@code assignments}, no en {@code applications} — es el único módulo que ya depende a la vez de
 * {@code applications} y de {@code roles} (PLAN-HU-015.md §0). {@code applications} conserva
 * {@code GET /api/v1/applications} en su propio {@code ApplicationController}; este controlador solo
 * añade el {@code POST} sobre la misma ruta base — mismo patrón que
 * {@code ApplicationWithInitialResourceController} de {@code resources} (HU-010) ya usaba para una
 * ruta hermana.
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationRegistrationController {

    private final RegisterApplicationWithFirstAdministratorInteractor interactor;

    ApplicationRegistrationController(RegisterApplicationWithFirstAdministratorInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ApplicationRegisteredWebResponse>>> register(
            @RequestBody RegisterApplicationWithFirstAdministratorRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return interactor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }
}
