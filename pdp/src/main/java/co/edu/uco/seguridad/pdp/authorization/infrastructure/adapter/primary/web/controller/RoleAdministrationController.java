package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.BatchRoleResourcesRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceGrantInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerRoleDefinitionInteractor;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleResourcesLookupValidator;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ResourceOutsideRoleScopeException;
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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final RoleResourcesLookupValidator roleResources;
    private final ProtectedResourceOwnerLookupValidator resourceOwner;
    private final ApplicationOwnerLookupValidator applicationOwner;

    @Autowired
    RoleAdministrationController(AdministerRoleDefinitionInteractor defineInteractor,
                                 AdministerResourceGrantInteractor grantInteractor,
                                 RoleResourcesLookupValidator roleResources,
                                 ProtectedResourceOwnerLookupValidator resourceOwner,
                                 ApplicationOwnerLookupValidator applicationOwner) {
        this.defineInteractor = Objects.requireNonNull(defineInteractor);
        this.grantInteractor = Objects.requireNonNull(grantInteractor);
        this.roleResources = Objects.requireNonNull(roleResources);
        this.resourceOwner = Objects.requireNonNull(resourceOwner);
        this.applicationOwner = Objects.requireNonNull(applicationOwner);
    }

    /**
     * Conserva la firma de construcción de HU-016 para los adaptadores y pruebas que solo ejercen
     * las operaciones individuales. Las dependencias del lote se requieren únicamente cuando se
     * invocan las rutas masivas y la configuración de Spring siempre usa el constructor completo.
     */
    RoleAdministrationController(AdministerRoleDefinitionInteractor defineInteractor,
                                 AdministerResourceGrantInteractor grantInteractor) {
        this(defineInteractor, grantInteractor,
                ignored -> Mono.error(new IllegalStateException("La gestión masiva no está configurada")),
                ignored -> Mono.error(new IllegalStateException("La gestión masiva no está configurada")),
                ignored -> Mono.error(new IllegalStateException("La gestión masiva no está configurada")));
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

    /**
     * Concesión idempotente en bloque. La validación de visibilidad, alcance y administración se
     * conserva en el interactor de concesión para cada recurso; el controlador solo adapta el
     * contrato HTTP masivo.
     */
    @PostMapping("/{roleId}/resources/batch")
    Mono<ResponseEntity<ApiResponse<RoleAdministrationWebResponse>>> grantResources(@PathVariable String roleId,
                                                                                     @RequestBody BatchRoleResourcesRawRequest body,
                                                                                     ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return validateRequestedApplication(body)
                .thenMany(Flux.fromIterable(body.resourceIds()).distinct())
                .concatMap(resourceId -> grantInteractor.execute(new GrantResourceRawRequest(roleId, resourceId)))
                .last()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("resourceIds no puede estar vacío para asignar recursos")))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCES_GRANTED",
                        "Los recursos fueron concedidos al rol", response, context)));
    }

    /** Sincroniza exclusivamente las asociaciones cuyo recurso pertenece a la aplicación indicada. */
    @PutMapping("/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<RoleAdministrationWebResponse>>> synchronizeResources(@PathVariable String roleId,
                                                                                            @RequestBody BatchRoleResourcesRawRequest body,
                                                                                            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        RoleId parsedRoleId = RoleId.of(roleId);
        Set<ResourceId> requested = body.resourceIds().stream().map(ResourceId::of).collect(Collectors.toUnmodifiableSet());
        return validateRequestedApplication(body)
                .then(roleResources.execute(Set.of(parsedRoleId)))
                .flatMap(current -> resourcesForApplication(current, ApplicationId.of(body.applicationId())).collect(Collectors.toSet()))
                .flatMap(currentInApplication -> {
                    Set<ResourceId> toGrant = requested.stream().filter(resource -> !currentInApplication.contains(resource)).collect(Collectors.toSet());
                    Set<ResourceId> toRevoke = currentInApplication.stream().filter(resource -> !requested.contains(resource)).collect(Collectors.toSet());
                    return Flux.concat(
                                    Flux.fromIterable(toGrant).concatMap(resource -> grantInteractor.execute(new GrantResourceRawRequest(roleId, resource.value().toString()))),
                                    Flux.fromIterable(toRevoke).concatMap(resource -> grantInteractor.revoke(new GrantResourceRawRequest(roleId, resource.value().toString()))))
                            // takeLast(1).next() y no last(): sin cambios el flujo queda vacío y last() lanzaría
                            // NoSuchElementException (500) al guardar dos veces lo mismo.
                            .takeLast(1).next()
                            // Sin cambios la operación es idempotente: se confirma con el estado actual del rol
                            // reconcediendo (idempotente) un recurso que ya tiene.
                            .switchIfEmpty(Mono.defer(() -> requested.stream().findFirst()
                                    .map(resource -> grantInteractor.execute(new GrantResourceRawRequest(roleId, resource.value().toString())))
                                    .orElseGet(Mono::empty)));
                })
                .map(response -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCES_SYNCHRONIZED",
                        "Los permisos de la aplicación fueron sincronizados", response, context)))
                // Ni cambios ni recursos pedidos: el rol ya no tiene permisos en esta aplicación y no hay nada que hacer.
                .switchIfEmpty(Mono.fromSupplier(() -> ResponseEntity.ok(ApiResponse.success("ROLE_RESOURCES_SYNCHRONIZED",
                        "Los permisos de la aplicación ya estaban así", (RoleAdministrationWebResponse) null, context))));
    }

    private Mono<Void> validateRequestedApplication(BatchRoleResourcesRawRequest body) {
        ApplicationId applicationId = ApplicationId.of(body.applicationId());
        return applicationOwner.execute(applicationId)
                .thenMany(Flux.fromIterable(body.resourceIds()).distinct().map(ResourceId::of)
                .flatMap(resourceId -> resourceOwner.execute(resourceId)
                        .filter(applicationId::equals)
                        .switchIfEmpty(Mono.error(new ResourceOutsideRoleScopeException(resourceId)))))
                .then();
    }

    private Flux<ResourceId> resourcesForApplication(Set<ResourceId> resources, ApplicationId applicationId) {
        return Flux.fromIterable(resources)
                .filterWhen(resourceId -> resourceOwner.execute(resourceId).map(applicationId::equals));
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
