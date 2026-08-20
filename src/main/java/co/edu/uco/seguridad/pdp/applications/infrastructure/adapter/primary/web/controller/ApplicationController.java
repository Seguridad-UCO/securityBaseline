package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.usecase.ListApplicationsUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador primario del catálogo de aplicaciones. El tenant se deriva de la sesión autenticada
 * (ADR-0003), nunca del cuerpo de la petición.
 */
@RestController
@RequestMapping("/api/v1/applications")
final class ApplicationController {

    record RegisterApplicationRawRequest(String name, String description, String baseUrl) {
    }

    private final RegisterApplicationUseCase registerApplication;
    private final ListApplicationsUseCase listApplications;

    ApplicationController(RegisterApplicationUseCase registerApplication, ListApplicationsUseCase listApplications) {
        this.registerApplication = Objects.requireNonNull(registerApplication);
        this.listApplications = Objects.requireNonNull(listApplications);
    }

    @PostMapping
    Mono<?> register(@RequestBody RegisterApplicationRawRequest raw, ServerWebExchange exchange) {
        return SecurityContext.currentPrincipal()
                .flatMap(principal -> registerApplication.execute(new RegisterApplicationRequest(
                        principal.tenantId(), new ApplicationName(raw.name()), raw.description(),
                        new ApplicationBaseUrl(raw.baseUrl()))))
                .map(registered -> ApiResponse.success("APPLICATION_REGISTERED",
                        WebContractMessages.successApplicationRegistered(), registered,
                        CorrelationWebFilter.context(exchange)));
    }

    @GetMapping
    Mono<?> list(ServerWebExchange exchange) {
        return SecurityContext.currentPrincipal()
                .flatMapMany(principal -> listApplications.execute(principal.tenantId()))
                .collectList()
                .map(applications -> ApiResponse.success("APPLICATIONS_LISTED",
                        WebContractMessages.successCatalogQueried(), applications,
                        CorrelationWebFilter.context(exchange)));
    }
}
