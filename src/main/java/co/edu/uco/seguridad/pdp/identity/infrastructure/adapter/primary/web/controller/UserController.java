package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.application.usecase.AssignTenantUseCase;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ListUsersUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Adaptador primario del catálogo de usuarios. Operación administrativa. */
@RestController
@RequestMapping("/api/v1/users")
final class UserController {

    record TenantAssignmentRawRequest(String tenantCode) {
    }

    private final ListUsersUseCase listUsers;
    private final AssignTenantUseCase assignTenant;

    UserController(ListUsersUseCase listUsers, AssignTenantUseCase assignTenant) {
        this.listUsers = Objects.requireNonNull(listUsers);
        this.assignTenant = Objects.requireNonNull(assignTenant);
    }

    @GetMapping
    Mono<?> list(ServerWebExchange exchange) {
        return listUsers.execute()
                .map(users -> ApiResponse.success("USERS_LISTED", WebContractMessages.successCatalogQueried(), users,
                        CorrelationWebFilter.context(exchange)));
    }

    @PutMapping("/{id}/tenant")
    Mono<?> assign(@PathVariable String id, @RequestBody TenantAssignmentRawRequest raw, ServerWebExchange exchange) {
        return assignTenant.execute(new AssignTenantRequest(UserId.of(id), new TenantId(raw.tenantCode())))
                .map(updated -> ApiResponse.success("USER_TENANT_ASSIGNED", WebContractMessages.successApplicationRegistered(),
                        updated, CorrelationWebFilter.context(exchange)));
    }
}
