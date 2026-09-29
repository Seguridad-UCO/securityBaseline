package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListProfileAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.ListProfileAssignmentsInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de asignaciones de perfil. Solo lectura — alta y revocación
 * viven en {@code ProfileAssignmentAdministrationController} (módulo {@code authorization}), gateadas
 * desde HU-019. Antes de este ajuste no existía consulta alguna: el frontend guardaba las
 * asignaciones de perfil solo en memoria de sesión del navegador, sin sobrevivir a un refresh — mismo
 * criterio que {@code AssignmentController} aplica a las asignaciones de rol.
 */
@RestController
@RequestMapping("/api/v1/profiles/{profileId}/assignments")
final class ProfileAssignmentController {

    private final ListProfileAssignmentsInteractor listInteractor;

    ProfileAssignmentController(ListProfileAssignmentsInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor,
                RequiredArgumentMessages.LIST_PROFILE_ASSIGNMENTS_INTERACTOR);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ProfileAssignmentWebResponse>>>> list(@PathVariable String profileId,
                                                                                       @RequestParam(required = false) String page, @RequestParam(required = false) String size,
                                                                                       @RequestParam(required = false) String offset, @RequestParam(required = false) String limit,
                                                                                       ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListProfileAssignmentsRawRequest(profileId, page, size, offset, limit))
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROFILE_ASSIGNMENTS_LISTED",
                        WebContractMessages.successProfileAssignmentsListed(), response, context)));
    }
}
