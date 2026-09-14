package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationCredentialRotationInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ApplicationRemovalInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP de las escrituras administrativas del catálogo de aplicaciones (HU-015).
 * Vive en {@code authorization}, no en {@code applications} — ver PLAN-HU-015.md §0: es el módulo
 * que ya depende de {@code applications} y consume {@code PrincipalMustBeApplicationAdministratorValidator}.
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationAdministrationController {

    private final ApplicationRemovalInteractor removalInteractor;
    private final ApplicationCredentialRotationInteractor rotationInteractor;

    ApplicationAdministrationController(ApplicationRemovalInteractor removalInteractor,
            ApplicationCredentialRotationInteractor rotationInteractor) {
        this.removalInteractor = Objects.requireNonNull(removalInteractor);
        this.rotationInteractor = Objects.requireNonNull(rotationInteractor);
    }

    @DeleteMapping("/{applicationId}")
    Mono<ResponseEntity<ApiResponse<Void>>> remove(@PathVariable String applicationId, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return removalInteractor.execute(new ApplicationAdministrationRawRequest(applicationId))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("APPLICATION_REMOVED",
                        WebContractMessages.successApplicationRemoved(), null, context))));
    }

    @PostMapping("/{applicationId}/credential-rotations")
    Mono<ResponseEntity<ApiResponse<AdministeredApplicationWebResponse>>> rotate(@PathVariable String applicationId,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return rotationInteractor.execute(new ApplicationAdministrationRawRequest(applicationId))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_CREDENTIAL_ROTATED",
                                WebContractMessages.successApplicationCredentialRotated(), response, context)));
    }
}
