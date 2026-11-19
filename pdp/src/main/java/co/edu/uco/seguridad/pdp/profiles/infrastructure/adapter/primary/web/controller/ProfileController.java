package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.ListProfilesInteractor;
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
 * Adaptador primario HTTP del catálogo de perfiles (HU-011). Desde HU-019, solo lista: definir y
 * agregar rol se movieron a {@code ProfileAdministrationController} (`authorization`) — mismo
 * criterio que HU-016 aplicó a {@code RoleController}.
 */
@RestController
@RequestMapping("/api/v1/profiles")
final class ProfileController {

    private final ListProfilesInteractor listInteractor;

    ProfileController(ListProfilesInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_PROFILES_INTERACTOR);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ProfileWebResponse>>>> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String query,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListProfilesRawRequest(page, size, offset, limit, query))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILES_LISTED",
                        WebContractMessages.successProfilesListed(), response, context)));
    }

    Mono<ResponseEntity<ApiResponse<PageResponse<ProfileWebResponse>>>> list(String page, String size, String offset,
                                                                               String limit, ServerWebExchange exchange) {
        return list(page, size, offset, limit, null, exchange);
    }
}
