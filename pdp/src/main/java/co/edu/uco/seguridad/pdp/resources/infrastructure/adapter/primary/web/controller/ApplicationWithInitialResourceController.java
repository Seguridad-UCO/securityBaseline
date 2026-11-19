package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ApplicationWithInitialResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterApplicationWithInitialResourceInteractor;
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
 * Adaptador primario HTTP de la saga de registro (HU-010). Vive en {@code resources}, no en
 * {@code applications}: es el módulo que ya tiene permiso de mirar al otro (PLAN-HU-010.md §0).
 * Comparte el prefijo {@code /api/v1/applications} con {@code ApplicationController}, de
 * {@code applications} — las rutas no colisionan.
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationWithInitialResourceController {

    private final RegisterApplicationWithInitialResourceInteractor interactor;

    ApplicationWithInitialResourceController(RegisterApplicationWithInitialResourceInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    @PostMapping("/with-initial-resource")
    Mono<ResponseEntity<ApiResponse<ApplicationWithInitialResourceWebResponse>>> register(
            @RequestBody RegisterApplicationWithInitialResourceRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return interactor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_WITH_INITIAL_RESOURCE_REGISTERED",
                                WebContractMessages.successApplicationRegisteredWithInitialResource(), response,
                                context)));
    }
}
