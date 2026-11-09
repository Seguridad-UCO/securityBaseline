package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.CreateTenantInteractor;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor.ListTenantsInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador primario HTTP del catálogo de tenants. Solo recibe el payload, ejecuta el interactor y
 * envuelve la respuesta. El mapeo vive en el interactor.
 */
@RestController
@RequestMapping("/api/v1/tenants")
final class TenantController {

    private final CreateTenantInteractor createInteractor;
    private final ListTenantsInteractor listInteractor;

    TenantController(CreateTenantInteractor createInteractor, ListTenantsInteractor listInteractor) {
        this.createInteractor = Objects.requireNonNull(createInteractor);
        this.listInteractor = Objects.requireNonNull(listInteractor);
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<TenantWebResponse>>> create(@RequestBody CreateTenantRawRequest body,
                                                                ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return createInteractor.execute(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("TENANT_CREATED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<List<TenantWebResponse>>>> list(ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return listInteractor.execute()
                .map(response -> ResponseEntity.ok(ApiResponse.success("TENANTS_LISTED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
