package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationAdministratorsRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RemoveApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorAssignmentInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorListInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerApplicationAdministratorRemovalInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Autoservicio de administradores de aplicación (HU-020): agregar, quitar y listar sin pasar por el
 * canal interno mTLS de {@code InternalApplicationAdministratorController} (HU-015). Gateado
 * incondicionalmente por {@code PrincipalMustBeApplicationAdministratorValidator} (HU-009).
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/administrators")
final class ApplicationAdministratorController {

    private final AdministerApplicationAdministratorAssignmentInteractor assignInteractor;
    private final AdministerApplicationAdministratorRemovalInteractor removeInteractor;
    private final AdministerApplicationAdministratorListInteractor listInteractor;

    ApplicationAdministratorController(AdministerApplicationAdministratorAssignmentInteractor assignInteractor,
                                       AdministerApplicationAdministratorRemovalInteractor removeInteractor,
                                       AdministerApplicationAdministratorListInteractor listInteractor) {
        this.assignInteractor = Objects.requireNonNull(assignInteractor);
        this.removeInteractor = Objects.requireNonNull(removeInteractor);
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ApplicationAdministratorWebResponse>>> assign(@PathVariable String applicationId,
                                                                                  @RequestBody AssignApplicationAdministratorRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return assignInteractor.execute(new AssignApplicationAdministratorRawRequest(applicationId, body.userId()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_ADMINISTRATOR_ASSIGNED",
                                WebContractMessages.successApplicationAdministratorAssigned(), response, context)));
    }

    @DeleteMapping("/{userId}")
    Mono<ResponseEntity<ApiResponse<Void>>> remove(@PathVariable String applicationId, @PathVariable String userId,
                                                   ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return removeInteractor.execute(new RemoveApplicationAdministratorRawRequest(applicationId, userId))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("APPLICATION_ADMINISTRATOR_REMOVED",
                        WebContractMessages.successApplicationAdministratorRemoved(), null, context))));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<ApplicationAdministratorWebResponse>>>> list(
            @PathVariable String applicationId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListApplicationAdministratorsRawRequest(applicationId))
                .map(responses -> ResponseEntity.ok(ApiResponse.success("APPLICATION_ADMINISTRATORS_LISTED",
                        WebContractMessages.successApplicationAdministratorsListed(), responses, context)));
    }
}
