package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.interactor.AssignApplicationAdministratorInteractor;
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
 * Backfill manual del primer administrador de una aplicación ya existente (HU-015). Vive bajo
 * {@code /internal/v1/**}: hereda automáticamente la cadena mTLS + evidencia JWT de
 * {@code InternalSecurityConfiguration} (HU-003) — no requiere seguridad propia. Vive en
 * {@code assignments}, no en {@code applications}: es el módulo que ya depende de {@code applications}
 * y de {@code roles} (ver PLAN-HU-015.md §0).
 */
@RestController
@RequestMapping("/internal/v1/applications")
final class InternalApplicationAdministratorController {

    private final AssignApplicationAdministratorInteractor interactor;

    InternalApplicationAdministratorController(AssignApplicationAdministratorInteractor interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    @PostMapping("/{applicationId}/administrators")
    Mono<ResponseEntity<ApiResponse<AssignmentWebResponse>>> assign(@PathVariable String applicationId,
            @RequestBody AssignApplicationAdministratorRawRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return interactor.execute(new AssignApplicationAdministratorRawRequest(applicationId, body.userId()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_ADMINISTRATOR_ASSIGNED",
                                WebContractMessages.successApplicationAdministratorAssigned(), response, context)));
    }
}
