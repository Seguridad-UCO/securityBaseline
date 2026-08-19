package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adaptador primario HTTP. Solo recibe el payload, ejecuta el interactor y envuelve la respuesta.
 * El mapeo vive en el interactor.
 */
@RestController
@RequestMapping("/api/v1/protected-applications")
final class ProtectedApplicationController {

    private final RegisterProtectedApplicationInteractor registerInteractor;
    private final SearchProtectedApplicationsInteractor searchInteractor;

    ProtectedApplicationController(RegisterProtectedApplicationInteractor registerInteractor,
                                   SearchProtectedApplicationsInteractor searchInteractor) {
        this.registerInteractor = registerInteractor;
        this.searchInteractor = searchInteractor;
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProtectedApplicationResponse>>> register(
            @RequestBody RegisterProtectedApplicationRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return registerInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ProtectedApplicationResponse>>>> search(
            @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) String resourceContains,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        SearchProtectedApplicationsRawRequest raw = new SearchProtectedApplicationsRawRequest(
                nameContains, resourceContains, page, size, offset, limit);
        return searchInteractor.execute(raw)
                .map(response -> ResponseEntity.ok(ApiResponse.success("CATALOG_QUERIED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
