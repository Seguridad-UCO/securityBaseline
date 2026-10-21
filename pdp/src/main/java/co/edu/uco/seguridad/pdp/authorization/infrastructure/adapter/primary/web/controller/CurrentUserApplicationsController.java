package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.CurrentUserApplicationsWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ReadCurrentUserApplicationsInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/me/applications")
final class CurrentUserApplicationsController {
    private final ReadCurrentUserApplicationsInteractor interactor;
    CurrentUserApplicationsController(ReadCurrentUserApplicationsInteractor interactor) { this.interactor = interactor; }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<CurrentUserApplicationsWebResponse>>> list(ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return interactor.execute().map(response -> ResponseEntity.ok(ApiResponse.success("CURRENT_USER_APPLICATIONS_READ",
                "Aplicaciones y roles del usuario consultados", response, context)));
    }
}
