package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceBodyRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredResourceWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AdministerResourceRegistrationInteractor;
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
 * Adaptador primario HTTP del registro administrativo de recursos protegidos (HU-017). Vive en
 * {@code authorization}, no en {@code resources} — mismo motivo que
 * {@code ApplicationAdministrationController} (HU-015) y {@code RoleAdministrationController}
 * (HU-016): es el módulo que ya depende de {@code resources} y consume
 * {@code PrincipalMustBeApplicationAdministratorValidator}. Misma ruta, verbo y código de éxito que
 * antes exponía {@code ProtectedResourceController} — ver PLAN-HU-017.md §6.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/resources")
final class ResourceAdministrationController {

    private final AdministerResourceRegistrationInteractor registerInteractor;

    ResourceAdministrationController(AdministerResourceRegistrationInteractor registerInteractor) {
        this.registerInteractor = Objects.requireNonNull(registerInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<AdministeredResourceWebResponse>>> register(@PathVariable String applicationId,
                                                                                @RequestBody RegisterProtectedResourceBodyRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        RegisterProtectedResourceRawRequest raw =
                new RegisterProtectedResourceRawRequest(applicationId, body.path(), body.method());
        return registerInteractor.execute(raw)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("PROTECTED_RESOURCE_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }
}
