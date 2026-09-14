package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor.ListProtectedResourcesInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador primario HTTP de endpoints protegidos. Solo recibe el payload, ejecuta el interactor y
 * envuelve la respuesta. Desde HU-017, solo expone la consulta: registrar un recurso se movió a
 * {@code ResourceAdministrationController} (módulo {@code authorization}), que gatea la escritura
 * contra el mecanismo de administración por aplicación (ver PLAN-HU-017.md §6) — mismo patrón que
 * HU-015/HU-016 aplicaron a {@code applications}/{@code roles}.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/resources")
final class ProtectedResourceController {

    private final ListProtectedResourcesInteractor listInteractor;

    ProtectedResourceController(ListProtectedResourcesInteractor listInteractor) {
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<ProtectedResourceWebResponse>>>> list(@PathVariable String applicationId,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute(applicationId)
                .map(response -> ResponseEntity.ok(ApiResponse.success("PROTECTED_RESOURCES_LISTED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
