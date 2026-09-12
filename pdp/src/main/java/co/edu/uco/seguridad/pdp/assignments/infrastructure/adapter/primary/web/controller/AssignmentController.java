package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignRoleInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RevokeAssignmentInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Adaptador primario HTTP del catálogo de asignaciones (HU-005). Solo recibe, obtiene el contexto,
 * delega al interactor y envuelve. El roleId de la ruta entra al raw request para que el interactor
 * reciba un único payload, como exige ReactiveOperation (mismo patrón que RoleController de HU-004).
 */
@RestController
@RequestMapping("/api/v1/roles/{roleId}/assignments")
final class AssignmentController {

    private final AssignRoleInteractor assignInteractor;
    private final RevokeAssignmentInteractor revokeInteractor;
    private final ListAssignmentsInteractor listInteractor;

    AssignmentController(AssignRoleInteractor assignInteractor, RevokeAssignmentInteractor revokeInteractor,
            ListAssignmentsInteractor listInteractor) {
        this.assignInteractor = Objects.requireNonNull(assignInteractor, RequiredArgumentMessages.ASSIGN_ROLE_INTERACTOR);
        this.revokeInteractor = Objects.requireNonNull(revokeInteractor, RequiredArgumentMessages.REVOKE_ASSIGNMENT_INTERACTOR);
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_ASSIGNMENTS_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<AssignmentWebResponse>>> assign(@PathVariable String roleId,
            @RequestBody AssignRoleRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return assignInteractor.execute(new AssignRoleRawRequest(roleId, body.userId(), body.applicationId()))
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

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<AssignmentWebResponse>>>> list(@PathVariable String roleId,
            @RequestParam(required = false) String page, @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset, @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListAssignmentsRawRequest(roleId, page, size, offset, limit))
                .map(response -> ResponseEntity.ok(ApiResponse.success("ASSIGNMENTS_LISTED",
                        WebContractMessages.successAssignmentsListed(), response, context)));
    }
}
