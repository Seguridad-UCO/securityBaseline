package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileDefinitionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileRoleAdditionInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP de las escrituras administrativas del catálogo de perfiles (HU-019). Vive
 * en {@code authorization}, no en {@code profiles} — mismo motivo que
 * {@code RoleAdministrationController} (HU-016). Mismas rutas, verbos y códigos de éxito que antes
 * exponía {@code ProfileController} — ver PLAN-HU-019.md §6.
 */
@RestController
@RequestMapping("/api/v1/profiles")
final class ProfileAdministrationController {

    private final AdministerProfileDefinitionInteractor defineInteractor;
    private final AdministerProfileRoleAdditionInteractor addRoleInteractor;

    ProfileAdministrationController(AdministerProfileDefinitionInteractor defineInteractor,
            AdministerProfileRoleAdditionInteractor addRoleInteractor) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor);
        this.addRoleInteractor = Objects.requireNonNull(addRoleInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProfileAdministrationWebResponse>>> define(@RequestBody DefineProfileRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return defineInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("PROFILE_DEFINED", WebContractMessages.successProfileDefined(), response, context)));
    }

    @PostMapping("/{profileId}/roles")
    Mono<ResponseEntity<ApiResponse<ProfileAdministrationWebResponse>>> addRole(@PathVariable String profileId,
            @RequestBody AddRoleToProfileRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return addRoleInteractor.execute(new AddRoleToProfileRawRequest(profileId, body.roleId()))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLE_ADDED",
                        WebContractMessages.successProfileRoleAdded(), response, context)));
    }
}
