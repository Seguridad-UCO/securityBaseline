package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceGrantInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerRoleDefinitionInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP de las escrituras administrativas del catálogo de roles (HU-016). Vive en
 * {@code authorization}, no en {@code roles} — mismo motivo que {@code ApplicationAdministrationController}
 * (HU-015, PLAN-HU-015.md §0): es el módulo que ya depende de {@code roles} y consume
 * {@code PrincipalMustBeApplicationAdministratorValidator}. Mismas rutas, verbos y códigos de éxito
 * que antes exponía {@code RoleController} — ver PLAN-HU-016.md §6.
 */
@RestController
@RequestMapping("/api/v1/roles")
final class RoleAdministrationController {

    private final AdministerRoleDefinitionInteractor defineInteractor;
    private final AdministerResourceGrantInteractor grantInteractor;

    RoleAdministrationController(AdministerRoleDefinitionInteractor defineInteractor,
                                 AdministerResourceGrantInteractor grantInteractor) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor);
        this.grantInteractor = Objects.requireNonNull(grantInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<RoleAdministrationWebResponse>>> define(@RequestBody DefineRoleRawRequest body,
                                                                            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return defineInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("ROLE_DEFINED", WebContractMessages.successRoleDefined(), response, context)));
    }

    @PostMapping("/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<RoleAdministrationWebResponse>>> grantResource(@PathVariable String roleId,
                                                                                   @RequestBody GrantResourceRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return grantInteractor.execute(new GrantResourceRawRequest(roleId, body.resourceId()))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCE_GRANTED",
                        WebContractMessages.successRoleResourceGranted(), response, context)));
    }

    @DeleteMapping("/{roleId}/resources/{resourceId}")
    Mono<ResponseEntity<ApiResponse<RoleAdministrationWebResponse>>> revokeResource(@PathVariable String roleId,
                                                                                    @PathVariable String resourceId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return grantInteractor.revoke(new GrantResourceRawRequest(roleId, resourceId))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCE_REVOKED",
                        "El recurso fue retirado del rol", response, context)));
    }
}
