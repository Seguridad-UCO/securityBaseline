package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.ListProfilesRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.AddRoleToProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.DefineProfileInteractor;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.interactor.ListProfilesInteractor;
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
 * Adaptador primario HTTP del catálogo de perfiles (HU-011). Solo recibe, obtiene el contexto,
 * delega al interactor y envuelve — espejo exacto de {@code RoleController}.
 */
@RestController
@RequestMapping("/api/v1/profiles")
final class ProfileController {

    private final DefineProfileInteractor defineInteractor;
    private final AddRoleToProfileInteractor addRoleInteractor;
    private final ListProfilesInteractor listInteractor;

    ProfileController(DefineProfileInteractor defineInteractor, AddRoleToProfileInteractor addRoleInteractor,
            ListProfilesInteractor listInteractor) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor, RequiredArgumentMessages.DEFINE_PROFILE_INTERACTOR);
        this.addRoleInteractor = Objects.requireNonNull(addRoleInteractor,
                RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_INTERACTOR);
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_PROFILES_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProfileWebResponse>>> define(@RequestBody DefineProfileRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return defineInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("PROFILE_DEFINED", WebContractMessages.successProfileDefined(), response, context)));
    }

    @PostMapping("/{profileId}/roles")
    Mono<ResponseEntity<ApiResponse<ProfileWebResponse>>> addRole(@PathVariable String profileId,
            @RequestBody AddRoleToProfileRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return addRoleInteractor.execute(new AddRoleToProfileRawRequest(profileId, body.roleId()))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLE_ADDED",
                        WebContractMessages.successProfileRoleAdded(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ProfileWebResponse>>>> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListProfilesRawRequest(page, size, offset, limit))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILES_LISTED",
                        WebContractMessages.successProfilesListed(), response, context)));
    }
}
