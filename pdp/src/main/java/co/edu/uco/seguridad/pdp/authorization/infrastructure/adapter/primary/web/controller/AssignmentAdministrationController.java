package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AssignmentAdministrationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentCreationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerAssignmentRevocationInteractor;
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
 * Adaptador primario HTTP de las escrituras administrativas del catálogo de asignaciones (HU-018).
 * Vive en {@code authorization}, no en {@code assignments} — mismo motivo que
 * {@code RoleAdministrationController} (HU-016). Mismas rutas, verbos y códigos de éxito que antes
 * exponía {@code AssignmentController} — ver PLAN-HU-018.md §6.
 */
@RestController
@RequestMapping("/api/v1/roles/{roleId}/assignments")
final class AssignmentAdministrationController {

    private final AdministerAssignmentCreationInteractor createInteractor;
    private final AdministerAssignmentRevocationInteractor revokeInteractor;

    AssignmentAdministrationController(AdministerAssignmentCreationInteractor createInteractor,
                                       AdministerAssignmentRevocationInteractor revokeInteractor) {
        this.createInteractor = Objects.requireNonNull(createInteractor);
        this.revokeInteractor = Objects.requireNonNull(revokeInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<AssignmentAdministrationWebResponse>>> assign(@PathVariable String roleId,
                                                                                  @RequestBody AssignRoleRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return createInteractor.execute(new AssignRoleRawRequest(roleId, body.userId(), body.applicationId()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("ASSIGNMENT_CREATED", WebContractMessages.successAssignmentCreated(), response, context)));
    }

    @DeleteMapping("/{assignmentId}")
    Mono<ResponseEntity<ApiResponse<Void>>> revoke(@PathVariable String roleId, @PathVariable String assignmentId,
                                                   ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return revokeInteractor.execute(new RevokeAssignmentRawRequest(assignmentId))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success(
                        "ASSIGNMENT_REVOKED", WebContractMessages.successAssignmentRevoked(), null, context))));
    }
}
