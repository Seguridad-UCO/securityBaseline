package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListAssignmentsInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de asignaciones (HU-005). Desde HU-018, solo lista: crear y
 * revocar se movieron a {@code AssignmentAdministrationController} (`authorization`) — mismo
 * criterio que HU-016 aplicó a {@code RoleController}.
 */
@RestController
@RequestMapping("/api/v1/roles/{roleId}/assignments")
final class AssignmentController {

    private final ListAssignmentsInteractor listInteractor;

    AssignmentController(ListAssignmentsInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor, RequiredArgumentMessages.LIST_ASSIGNMENTS_INTERACTOR);
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
