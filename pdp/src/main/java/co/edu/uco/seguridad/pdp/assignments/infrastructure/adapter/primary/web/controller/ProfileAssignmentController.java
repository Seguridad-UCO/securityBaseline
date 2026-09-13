package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignProfileInteractor;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.RevokeProfileAssignmentInteractor;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP de las asignaciones de perfil (HU-011). No se toca {@code AssignmentController}
 * (asignaciones de rol, HU-005): esta es una capacidad nueva, aditiva, con su propio controller —
 * mismo patrón que separa {@code RoleController} de {@code AssignmentController}.
 */
@RestController
@RequestMapping("/api/v1/profiles/{profileId}/assignments")
final class ProfileAssignmentController {

    private final AssignProfileInteractor assignInteractor;
    private final RevokeProfileAssignmentInteractor revokeInteractor;

    ProfileAssignmentController(AssignProfileInteractor assignInteractor, RevokeProfileAssignmentInteractor revokeInteractor) {
        this.assignInteractor = Objects.requireNonNull(assignInteractor, RequiredArgumentMessages.ASSIGN_PROFILE_INTERACTOR);
        this.revokeInteractor = Objects.requireNonNull(revokeInteractor,
                RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_INTERACTOR);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProfileAssignmentWebResponse>>> assign(@PathVariable String profileId,
            @RequestBody AssignProfileRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return assignInteractor.execute(new AssignProfileRawRequest(profileId, body.userId(), body.applicationId()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("PROFILE_ASSIGNED", WebContractMessages.successProfileAssigned(), response, context)));
    }

    @DeleteMapping("/{profileAssignmentId}")
    Mono<ResponseEntity<ApiResponse<Void>>> revoke(@PathVariable String profileId,
            @PathVariable String profileAssignmentId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return revokeInteractor.execute(new RevokeProfileAssignmentRawRequest(profileAssignmentId))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success(
                        "PROFILE_ASSIGNMENT_REVOKED", WebContractMessages.successProfileAssignmentRevoked(), null, context))));
    }
}
