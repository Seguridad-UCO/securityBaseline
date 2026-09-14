package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.interactor.ListRolesInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de roles (HU-004). Solo recibe, obtiene el contexto, delega al
 * interactor y envuelve. Desde HU-016, solo expone la consulta: definir un rol y conceder un recurso
 * se movieron a {@code RoleAdministrationController} (módulo {@code authorization}), que gatea ambas
 * escrituras contra el mecanismo de administración por aplicación (ver PLAN-HU-016.md §6) — mismo
 * patrón que HU-015 aplicó a {@code applications}.
 */
@RestController
@RequestMapping("/api/v1/roles")
final class RoleController {

    private final ListRolesInteractor listInteractor;

    RoleController(ListRolesInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_ROLES_INTERACTOR);
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
