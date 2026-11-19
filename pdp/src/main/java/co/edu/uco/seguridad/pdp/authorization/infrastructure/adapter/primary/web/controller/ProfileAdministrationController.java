package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.BatchProfileRolesRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileDefinitionInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerProfileRoleAdditionInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileRolesLookupValidator;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.stream.Collectors;
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
    private final ProfileRolesLookupValidator profileRoles;

    @Autowired
    ProfileAdministrationController(AdministerProfileDefinitionInteractor defineInteractor,
                                    AdministerProfileRoleAdditionInteractor addRoleInteractor,
                                    ProfileRolesLookupValidator profileRoles) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor);
        this.addRoleInteractor = Objects.requireNonNull(addRoleInteractor);
        this.profileRoles = Objects.requireNonNull(profileRoles);
    }

    /** Conserva el constructor usado por pruebas de las operaciones individuales. */
    ProfileAdministrationController(AdministerProfileDefinitionInteractor defineInteractor,
                                    AdministerProfileRoleAdditionInteractor addRoleInteractor) {
        this(defineInteractor, addRoleInteractor,
                ignored -> Mono.error(new IllegalStateException("La gestión masiva no está configurada")));
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

    /** Agrega roles sin retirar los ya asociados, igual que la concesión masiva de recursos de un rol. */
    @PostMapping("/{profileId}/roles/batch")
    Mono<ResponseEntity<ApiResponse<ProfileAdministrationWebResponse>>> addRoles(@PathVariable String profileId,
                                                                                  @RequestBody BatchProfileRolesRawRequest body,
                                                                                  ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return Flux.fromIterable(body.roleIds()).distinct()
                .concatMap(roleId -> addRoleInteractor.execute(new AddRoleToProfileRawRequest(profileId, roleId)))
                .last()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("roleIds no puede estar vacío para asignar roles")))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLES_ADDED",
                        "Los roles fueron agregados al perfil", response, context)));
    }

    /** Reemplaza el conjunto completo de roles del perfil con operaciones idempotentes y serializadas. */
    @PutMapping("/{profileId}/roles")
    Mono<ResponseEntity<ApiResponse<ProfileAdministrationWebResponse>>> synchronizeRoles(@PathVariable String profileId,
                                                                                           @RequestBody BatchProfileRolesRawRequest body,
                                                                                           ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        Set<RoleId> requested = body.roleIds().stream().map(RoleId::of).collect(Collectors.toUnmodifiableSet());
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> profileRoles.execute(new ProfileOwnershipQuery(principal.tenantId(), ProfileId.of(profileId))))
                .flatMap(current -> {
                    Set<RoleId> toAdd = requested.stream().filter(role -> !current.contains(role)).collect(Collectors.toUnmodifiableSet());
                    Set<RoleId> toRemove = current.stream().filter(role -> !requested.contains(role)).collect(Collectors.toUnmodifiableSet());
                    return Flux.concat(
                                    Flux.fromIterable(toAdd).concatMap(role -> addRoleInteractor.execute(new AddRoleToProfileRawRequest(profileId, role.value().toString()))),
                                    Flux.fromIterable(toRemove).concatMap(role -> addRoleInteractor.remove(new AddRoleToProfileRawRequest(profileId, role.value().toString()))))
                            .takeLast(1).next();
                })
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLES_SYNCHRONIZED",
                        "Los roles del perfil fueron sincronizados", response, context)))
                .switchIfEmpty(Mono.fromSupplier(() -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLES_SYNCHRONIZED",
                        "Los roles del perfil ya estaban así", (ProfileAdministrationWebResponse) null, context))));
    }

    @DeleteMapping("/{profileId}/roles/{roleId}")
    Mono<ResponseEntity<ApiResponse<ProfileAdministrationWebResponse>>> removeRole(@PathVariable String profileId,
                                                                                   @PathVariable String roleId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return addRoleInteractor.remove(new AddRoleToProfileRawRequest(profileId, roleId))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ROLE_REMOVED",
                        "El rol fue retirado del perfil", response, context)));
    }
}
