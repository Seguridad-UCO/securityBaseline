package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ListApplicationsInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de aplicaciones. Solo recibe el payload, ejecuta el
 * interactor y envuelve la respuesta. El mapeo (incluyendo derivar el tenant de la sesión) vive en
 * el interactor.
 *
 * <p>Desde HU-015, este controlador ya no sirve {@code POST} (registro) ni
 * {@code POST .../credential-rotations} (rotación): el registro se trasladó a
 * {@code ApplicationRegistrationController} de {@code assignments} (para poder dar de alta al primer
 * administrador) y la rotación a {@code ApplicationAdministrationController} de {@code authorization}
 * (para poder exigir que solo un administrador la ejecute) — ver PLAN-HU-015.md §0. La URL pública no
 * cambia para ningún cliente existente; solo cambió qué módulo la atiende.</p>
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationController {

    private final ListApplicationsInteractor listInteractor;

    ApplicationController(ListApplicationsInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationWebResponse>>>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(new ListApplicationsRawRequest(name, page, size, offset, limit))
                .map(response -> ResponseEntity.ok(ApiResponse.success("APPLICATIONS_LISTED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
