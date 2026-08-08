package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.RegisterProtectedApplicationInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.ProtectedApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.RegisterProtectedApplicationRequestMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.SearchProtectedApplicationsRequestMapper;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adaptador primario HTTP (Infrastructure Input/Primary).
 *
 * <p>Responsabilidades exclusivas de esta capa:</p>
 * <ol>
 *   <li>Recibir la solicitud HTTP y construir el DTO crudo (todos {@code String}).</li>
 *   <li>Mapear el DTO crudo al DTO tipado de aplicación (via mappers web).</li>
 *   <li>Invocar el interactor de aplicación ({@code execute}).</li>
 *   <li>Proyectar el resultado de aplicación al DTO de respuesta HTTP.</li>
 *   <li>Envolver en {@link ResponseEntity} con el código de estado correcto.</li>
 * </ol>
 *
 * <p>No contiene ninguna regla de negocio ni de validación de dominio. Cada parámetro de consulta
 * se declara {@code required = false} y tipado {@code String}: el enlace nunca debe fallar, porque
 * un valor que el framework rechaza nunca podría ser reportado por nuestro propio contrato de error.</p>
 */
@RestController
@RequestMapping("/api/v1/protected-applications")
final class ProtectedApplicationController {

    private final RegisterProtectedApplicationInteractor registerInteractor;
    private final SearchProtectedApplicationsInteractor searchInteractor;

    ProtectedApplicationController(RegisterProtectedApplicationInteractor registerInteractor,
                                   SearchProtectedApplicationsInteractor searchInteractor) {
        this.registerInteractor = registerInteractor;
        this.searchInteractor = searchInteractor;
    }

    @PostMapping
    Mono<ResponseEntity<ApiResponse<ProtectedApplicationResponse>>> register(
            @RequestBody RegisterProtectedApplicationRawRequest body,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return Mono.fromSupplier(() ->
                        RegisterProtectedApplicationRequestMapper.toRequest(
                                RegisterProtectedApplicationRequestMapper.toValidatedRequest(body)))
                .flatMap(registerInteractor::execute)
                .map(ProtectedApplicationResponseMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("APPLICATION_REGISTERED",
                                WebContractMessages.successApplicationRegistered(), response, context)));
    }

    @GetMapping
    Mono<ResponseEntity<ApiResponse<PageResponse<ProtectedApplicationResponse>>>> search(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) String resourceContains,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        SearchProtectedApplicationsRawRequest raw = new SearchProtectedApplicationsRawRequest(
                tenantId, nameContains, resourceContains, page, size, offset, limit);
        return Mono.fromSupplier(() ->
                        SearchProtectedApplicationsRequestMapper.toRequest(
                                SearchProtectedApplicationsRequestMapper.toValidatedRequest(raw)))
                .flatMap(searchInteractor::execute)
                .map(ProtectedApplicationResponseMapper::toPageResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.success("CATALOG_QUERIED",
                        WebContractMessages.successCatalogQueried(), response, context)));
    }
}
