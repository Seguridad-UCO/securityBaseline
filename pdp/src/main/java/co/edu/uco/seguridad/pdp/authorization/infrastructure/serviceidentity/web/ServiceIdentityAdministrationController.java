package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** API de gobierno: crear identidad, pedir un recurso y aprobarlo son tres actos separados. */
@RestController
@RequestMapping("/api/v1")
final class ServiceIdentityAdministrationController {
    private final ServiceIdentityAdministrationInteractor interactor;
    ServiceIdentityAdministrationController(ServiceIdentityAdministrationInteractor interactor) { this.interactor=interactor; }
    @GetMapping("/applications/{applicationId}/service-identities")
    Mono<ResponseEntity<ApiResponse<java.util.List<ServiceIdentityWebResponse>>>> identities(@PathVariable String applicationId, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.identities(applicationId).collectList().map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_IDENTITIES_FOUND", "Identidades de servicio consultadas.", value, context)));
    }
    @GetMapping("/applications/{applicationId}/service-identities/{identityId}/access-requests")
    Mono<ResponseEntity<ApiResponse<java.util.List<ServiceAccessGrantWebResponse>>>> grants(@PathVariable String applicationId, @PathVariable String identityId, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.grants(applicationId, identityId).collectList().map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_ACCESS_REQUESTS_FOUND", "Solicitudes de acceso consultadas.", value, context)));
    }
    @GetMapping("/applications/{applicationId}/service-access-requests")
    Mono<ResponseEntity<ApiResponse<java.util.List<ServiceAccessGrantWebResponse>>>> targetGrants(@PathVariable String applicationId, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.targetGrants(applicationId).collectList().map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_ACCESS_REQUESTS_FOUND", "Solicitudes recibidas consultadas.", value, context)));
    }
    @GetMapping("/applications/{applicationId}/service-technical-roles")
    Mono<ResponseEntity<ApiResponse<co.edu.uco.seguridad.shared.web.PageResponse<ServiceTechnicalRoleSummaryWebResponse>>>> technicalRoles(@PathVariable String applicationId,@RequestParam(required=false) String page,@RequestParam(required=false) String size,@RequestParam(required=false) String offset,@RequestParam(required=false) String limit, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.technicalRoles(applicationId,page,size,offset,limit).map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_TECHNICAL_ROLES_FOUND", "Roles técnicos consultados.", value, context)));
    }
    @GetMapping("/applications/{applicationId}/service-technical-roles/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<co.edu.uco.seguridad.shared.web.PageResponse<ServiceTechnicalRoleResourceWebResponse>>>> technicalRoleResources(@PathVariable String applicationId,@PathVariable String roleId,@RequestParam(required=false) String page,@RequestParam(required=false) String size,@RequestParam(required=false) String offset,@RequestParam(required=false) String limit,ServerWebExchange exchange) { RequestContext context=CorrelationWebFilter.context(exchange); return interactor.technicalRoleResources(applicationId,roleId,page,size,offset,limit).map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_TECHNICAL_ROLE_RESOURCES_FOUND","Recursos del rol técnico consultados.",value,context))); }
    @PostMapping("/applications/{applicationId}/service-identities")
    Mono<ResponseEntity<ApiResponse<RegisteredServiceIdentityWebResponse>>> create(@PathVariable String applicationId, @RequestBody CreateServiceIdentityRawRequest body, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.create(applicationId,body).map(RegisteredServiceIdentityWebResponse::from).map(value -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SERVICE_IDENTITY_CREATED", "La identidad de servicio se creó; guarde la credencial ahora.", value, context)));
    }
    @PostMapping("/applications/{applicationId}/service-technical-roles")
    Mono<ResponseEntity<ApiResponse<ServiceTechnicalRoleWebResponse>>> createTechnicalRole(@PathVariable String applicationId, @RequestBody CreateServiceTechnicalRoleRawRequest body, ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.createTechnicalRole(applicationId,body).map(value -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SERVICE_TECHNICAL_ROLE_CREATED", "El rol técnico fue creado.",value,context)));
    }
    @PostMapping("/applications/{applicationId}/service-technical-roles/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<Void>>> addTechnicalRoleResource(@PathVariable String applicationId,@PathVariable String roleId,@RequestBody ChangeServiceTechnicalRoleResourceRawRequest body,ServerWebExchange exchange) { RequestContext context=CorrelationWebFilter.context(exchange); return interactor.addTechnicalRoleResource(applicationId,roleId,body).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SERVICE_TECHNICAL_ROLE_RESOURCE_ADDED","El recurso fue asociado al rol técnico.",null,context))); }
    @DeleteMapping("/applications/{applicationId}/service-technical-roles/{roleId}/resources/{resourceId}")
    Mono<ResponseEntity<ApiResponse<Void>>> removeTechnicalRoleResource(@PathVariable String applicationId,@PathVariable String roleId,@PathVariable String resourceId,ServerWebExchange exchange) { RequestContext context=CorrelationWebFilter.context(exchange); return interactor.removeTechnicalRoleResource(applicationId,roleId,resourceId).thenReturn(ResponseEntity.ok(ApiResponse.success("SERVICE_TECHNICAL_ROLE_RESOURCE_REMOVED","El recurso fue retirado del rol técnico.",null,context))); }
    @PostMapping("/applications/{applicationId}/service-identities/{identityId}/access-requests")
    Mono<ResponseEntity<ApiResponse<ServiceAccessGrantWebResponse>>> request(@PathVariable String applicationId,@PathVariable String identityId,@RequestBody RequestServiceAccessRawRequest body,ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.request(applicationId,identityId,body).map(ServiceAccessGrantWebResponse::from).map(value -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SERVICE_ACCESS_REQUESTED", "La solicitud quedó pendiente de aprobación.",value,context)));
    }
    @PostMapping("/applications/{applicationId}/service-access-requests/{grantId}/decision")
    Mono<ResponseEntity<ApiResponse<ServiceAccessGrantWebResponse>>> decide(@PathVariable String applicationId,@PathVariable String grantId,@RequestBody DecideServiceAccessRawRequest body,ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.decide(applicationId,grantId,body).map(ServiceAccessGrantWebResponse::from).map(value -> ResponseEntity.ok(ApiResponse.success(body.approve()?"SERVICE_ACCESS_APPROVED":"SERVICE_ACCESS_REJECTED",body.approve()?"El acceso de servicio fue aprobado.":"El acceso de servicio fue rechazado.",value,context)));
    }
    @PostMapping("/applications/{applicationId}/service-access-requests/{grantId}/revocation")
    Mono<ResponseEntity<ApiResponse<ServiceAccessGrantWebResponse>>> revoke(@PathVariable String applicationId,@PathVariable String grantId,ServerWebExchange exchange) {
        RequestContext context=CorrelationWebFilter.context(exchange);
        return interactor.revoke(applicationId,grantId).map(ServiceAccessGrantWebResponse::from).map(value -> ResponseEntity.ok(ApiResponse.success("SERVICE_ACCESS_REVOKED","El permiso de servicio fue revocado.",value,context)));
    }
}
