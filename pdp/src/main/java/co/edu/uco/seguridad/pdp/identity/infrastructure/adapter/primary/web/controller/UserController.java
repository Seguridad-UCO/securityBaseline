package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantBodyRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.AssignTenantInteractor;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor.ListUsersInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/** Adaptador primario del catálogo de usuarios. Operación administrativa. */
@RestController
@RequestMapping("/api/v1/users")
final class UserController {

    private final ListUsersInteractor listInteractor;
    private final AssignTenantInteractor assignInteractor;

    UserController(ListUsersInteractor listInteractor, AssignTenantInteractor assignInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor);
        this.assignInteractor = Objects.requireNonNull(assignInteractor);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<UserWebResponse>>>> list(ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute()
                .map(users -> ResponseEntity.ok(ApiResponse.success("USERS_LISTED",
                        WebContractMessages.successCatalogQueried(), users, context)));
    }

    @PutMapping("/{id}/tenant")
    Mono<ResponseEntity<ApiResponse<UserWebResponse>>> assign(@PathVariable String id,
            @RequestBody AssignTenantBodyRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        AssignTenantRawRequest raw = new AssignTenantRawRequest(id, body.tenantCode());
        return assignInteractor.execute(raw)
                .map(updated -> ResponseEntity.ok(ApiResponse.success("USER_TENANT_ASSIGNED",
                        WebContractMessages.successApplicationRegistered(), updated, context)));
    }
}
