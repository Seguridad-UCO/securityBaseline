package co.edu.uco.seguridad.pdp.recursos.infrastructure.web;

import co.edu.uco.seguridad.pdp.recursos.*;
import co.edu.uco.seguridad.shared.web.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/protected-applications")
final class ProtectedApplicationController {
    private final RegisterProtectedApplicationUseCase register;
    ProtectedApplicationController(RegisterProtectedApplicationUseCase register) { this.register = register; }
    @PostMapping Mono<ResponseEntity<ApiResponse<ProtectedApplicationCatalogEntry>>> create(@Valid @RequestBody RegisterProtectedApplicationRequest body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return register.register(new RegisterProtectedApplicationCommand(body.tenantId(), body.applicationName(), body.resourceCode(), body.action()))
            .map(entry -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("APPLICATION_REGISTERED", "Protected application and initial resource registered", entry, context)));
    }
}
