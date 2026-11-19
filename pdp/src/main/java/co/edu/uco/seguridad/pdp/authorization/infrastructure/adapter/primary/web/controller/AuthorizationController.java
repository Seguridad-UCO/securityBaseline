package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AuthorizeInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/authorize")
final class AuthorizationController {

    private final AuthorizeInteractor interactor;

    AuthorizationController(AuthorizeInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor, RequiredArgumentMessages.AUTHORIZE_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<AccessDecisionWebResponse>>> authorize(@RequestBody AuthorizeRawRequest body,
                                                                           ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return interactor.execute(body)
                .map(response -> ResponseEntity.ok(ApiResponse.success("ACCESS_EVALUATED",
                        WebContractMessages.successAccessEvaluated(), response, context)));
    }
}
