package co.edu.uco.seguridad.applications.infrastructure.web;

import co.edu.uco.seguridad.applications.application.port.in.*;
import co.edu.uco.seguridad.applications.application.port.out.PageWindow;
import co.edu.uco.seguridad.applications.domain.*;
import co.edu.uco.seguridad.shared.web.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/protected-applications")
final class ProtectedApplicationController {
    private final RegisterProtectedApplicationUseCase register;
    private final SearchProtectedApplicationsUseCase search;
    ProtectedApplicationController(RegisterProtectedApplicationUseCase register, SearchProtectedApplicationsUseCase search) { this.register = register; this.search = search; }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProtectedApplicationResponse>>> create(@Valid @RequestBody RegisterProtectedApplicationRequest request, ServerWebExchange exchange) {
        var context = CorrelationWebFilter.context(exchange);
        return register.register(ProtectedApplicationMapper.command(request))
            .map(ProtectedApplicationMapper::response)
            .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("APPLICATION_REGISTERED", "Protected application registered", body, context)));
    }

    @GetMapping
    Mono<ApiResponse<PageResponse<ProtectedApplicationResponse>>> list(
            @RequestParam(required = false) String tenantId, @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) String resourceContains, @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size, @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit, ServerWebExchange exchange) {
        PageWindow window = window(page, size, offset, limit);
        var criteria = new ProtectedApplicationCriteria(optionalTenant(tenantId), Optional.ofNullable(nameContains), Optional.ofNullable(resourceContains));
        var context = CorrelationWebFilter.context(exchange);
        return search.search(criteria, window).map(result -> {
            var content = result.content().stream().map(ProtectedApplicationMapper::response).toList();
            return ApiResponse.success("APPLICATIONS_FOUND", "Protected applications retrieved", new PageResponse<>(content, result.total(), result.offset(), result.limit()), context);
        });
    }
    private Optional<TenantId> optionalTenant(String tenantId) { return tenantId == null || tenantId.isBlank() ? Optional.empty() : Optional.of(new TenantId(tenantId)); }
    private PageWindow window(Integer page, Integer size, Integer offset, Integer limit) {
        if ((offset == null) != (limit == null)) throw new IllegalArgumentException("offset and limit must be supplied together");
        if (offset != null) return new PageWindow(offset, limit);
        return PageWindow.page(page, size);
    }
}
