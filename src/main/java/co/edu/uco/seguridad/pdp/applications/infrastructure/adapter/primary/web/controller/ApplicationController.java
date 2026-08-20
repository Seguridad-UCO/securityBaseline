package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ListApplicationsInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de aplicaciones. Solo recibe el payload, ejecuta el
 * interactor y envuelve la respuesta. El mapeo (incluyendo derivar el tenant de la sesión) vive en
 * el interactor.
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationController {

    private final RegisterApplicationInteractor registerInteractor;
    private final ListApplicationsInteractor listInteractor;

    ApplicationController(RegisterApplicationInteractor registerInteractor, ListApplicationsInteractor listInteractor) {
        this.registerInteractor = Objects.requireNonNull(registerInteractor);
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ApplicationWebResponse>>> register(@RequestBody RegisterApplicationRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return registerInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<ApplicationWebResponse>>>> list(ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute()
                .map(response -> ResponseEntity.ok(ApiResponse.success("APPLICATIONS_LISTED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
