package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceBodyRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.ListProtectedResourcesInteractor;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.RegisterProtectedResourceInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador primario HTTP de endpoints protegidos. Solo recibe el payload, ejecuta el interactor y
 * envuelve la respuesta. El mapeo (incluyendo derivar el tenant de la sesión) vive en el interactor.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/resources")
final class ProtectedResourceController {

    private final RegisterProtectedResourceInteractor registerInteractor;
    private final ListProtectedResourcesInteractor listInteractor;

    ProtectedResourceController(RegisterProtectedResourceInteractor registerInteractor,
            ListProtectedResourcesInteractor listInteractor) {
        this.registerInteractor = Objects.requireNonNull(registerInteractor);
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProtectedResourceWebResponse>>> register(@PathVariable String applicationId,
            @RequestBody RegisterProtectedResourceBodyRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        RegisterProtectedResourceRawRequest raw =
                new RegisterProtectedResourceRawRequest(applicationId, body.path(), body.method());
        return registerInteractor.execute(raw)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("PROTECTED_RESOURCE_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<ProtectedResourceWebResponse>>>> list(@PathVariable String applicationId,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(applicationId)
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROTECTED_RESOURCES_LISTED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
