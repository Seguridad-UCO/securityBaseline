package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.DefineRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.GrantResourceToRoleInteractor;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.ListRolesInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de roles (HU-004). Solo recibe, obtiene el contexto, delega al
 * interactor y envuelve. El roleId de la ruta entra al raw request para que el interactor reciba un
 * único payload, como exige ReactiveOperation.
 */
@RestController
@RequestMapping("/api/v1/roles")
final class RoleController {

    private final DefineRoleInteractor defineInteractor;
    private final GrantResourceToRoleInteractor grantInteractor;
    private final ListRolesInteractor listInteractor;

    RoleController(DefineRoleInteractor defineInteractor, GrantResourceToRoleInteractor grantInteractor,
            ListRolesInteractor listInteractor) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor, RequiredArgumentMessages.DEFINE_ROLE_INTERACTOR);
        this.grantInteractor = Objects.requireNonNull(grantInteractor, RequiredArgumentMessages.GRANT_RESOURCE_TO_ROLE_INTERACTOR);
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_ROLES_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<RoleWebResponse>>> define(@RequestBody DefineRoleRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return defineInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("ROLE_DEFINED", WebContractMessages.successRoleDefined(), response, context)));
    }

    @PostMapping("/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<RoleWebResponse>>> grantResource(@PathVariable String roleId,
            @RequestBody GrantResourceRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return grantInteractor.execute(new GrantResourceRawRequest(roleId, body.resourceId()))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCE_GRANTED",
                        WebContractMessages.successRoleResourceGranted(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<RoleWebResponse>>>> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListRolesRawRequest(page, size, offset, limit))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLES_LISTED",
                        WebContractMessages.successRolesListed(), response, context)));
    }
}
