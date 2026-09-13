package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationRemovalInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP de las escrituras administrativas del catálogo de aplicaciones (HU-015).
 * Vive en {@code authorization}, no en {@code applications} — ver PLAN-HU-015.md §0: es el módulo
 * que ya depende de {@code applications} y consume {@code PrincipalMustBeApplicationAdministratorValidator}.
 *
 * <p>Solo trae {@code DELETE} por ahora. {@code POST .../credential-rotations} sigue en
 * {@code ApplicationController} de {@code applications} — el implementador la traslada aquí en el
 * mismo cambio que la retira de allí (dos controladores no pueden mapear la misma ruta a la vez).</p>
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationAdministrationController {

    private final ApplicationRemovalInteractor removalInteractor;

    ApplicationAdministrationController(ApplicationRemovalInteractor removalInteractor) {
        this.removalInteractor = Objects.requireNonNull(removalInteractor);
    }

    @DeleteMapping("/{applicationId}")
    Mono<ResponseEntity<ApiResponse<Void>>> remove(@PathVariable String applicationId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return removalInteractor.execute(new ApplicationAdministrationRawRequest(applicationId))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("APPLICATION_REMOVED",
                        WebContractMessages.successApplicationRemoved(), null, context))));
    }
}
