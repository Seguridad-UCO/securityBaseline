package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.CreateTenantUseCase;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.ListTenantsUseCase;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
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

/** Adaptador primario del catálogo de tenants. Operación administrativa: no depende del tenant del llamador. */
@RestController
@RequestMapping("/api/v1/tenants")
final class TenantController {

    record CreateTenantRawRequest(String code, String name) {
    }

    private final CreateTenantUseCase createTenant;
    private final ListTenantsUseCase listTenants;

    TenantController(CreateTenantUseCase createTenant, ListTenantsUseCase listTenants) {
        this.createTenant = Objects.requireNonNull(createTenant);
        this.listTenants = Objects.requireNonNull(listTenants);
    }

    @PostMapping
    Mono<?> create(@RequestBody CreateTenantRawRequest raw, ServerWebExchange exchange) {
        return createTenant.execute(new CreateTenantRequest(new TenantId(raw.code()), new TenantName(raw.name())))
                .map(tenant -> ApiResponse.success("TENANT_CREATED", WebContractMessages.successApplicationRegistered(),
                        tenant, CorrelationWebFilter.context(exchange)));
    }

    @GetMapping
    Mono<?> list(ServerWebExchange exchange) {
        return listTenants.execute()
                .map(tenants -> ApiResponse.success("TENANTS_LISTED", WebContractMessages.successCatalogQueried(),
                        tenants, CorrelationWebFilter.context(exchange)));
    }
}
