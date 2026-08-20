package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ResourcePath;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario de endpoints protegidos. El tenant se deriva de la sesión autenticada, nunca
 * del cuerpo o la ruta.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/resources")
final class ProtectedResourceController {

    record RegisterProtectedResourceRawRequest(String path, String method) {
    }

    private final RegisterProtectedResourceUseCase registerResource;
    private final ListProtectedResourcesUseCase listResources;

    ProtectedResourceController(RegisterProtectedResourceUseCase registerResource,
            ListProtectedResourcesUseCase listResources) {
        this.registerResource = Objects.requireNonNull(registerResource);
        this.listResources = Objects.requireNonNull(listResources);
    }

    @PostMapping
    Mono<?> register(@PathVariable String applicationId, @RequestBody RegisterProtectedResourceRawRequest raw,
            ServerWebExchange exchange) {
        ApplicationId application = new ApplicationId(UUID.fromString(applicationId));
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> registerResource.execute(new RegisterProtectedResourceRequest(
                        principal.tenantId(), application, new ResourcePath(raw.path()), HttpVerb.parse(raw.method()))))
                .map(registered -> ApiResponse.success("PROTECTED_RESOURCE_REGISTERED",
                        WebContractMessages.successApplicationRegistered(), registered,
                        CorrelationWebFilter.context(exchange)));
    }

    @GetMapping
    Mono<?> list(@PathVariable String applicationId, ServerWebExchange exchange) {
        ApplicationId application = new ApplicationId(UUID.fromString(applicationId));
        return listResources.execute(application)
                .collectList()
                .map(resources -> ApiResponse.success("PROTECTED_RESOURCES_LISTED",
                        WebContractMessages.successCatalogQueried(), resources,
                        CorrelationWebFilter.context(exchange)));
    }
}
