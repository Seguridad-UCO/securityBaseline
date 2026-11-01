package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogImportService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkResourceImportService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkResourceWorkbookService;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.UnsupportedBulkModeException;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Contrato de archivo de la carga masiva. La confirmación se habilita únicamente tras validación limpia.
 */
@RestController
@RequestMapping("/api/v1/resources/bulk")
final class BulkResourceController {
    private final BulkResourceWorkbookService workbooks;
    private final BulkResourceImportService imports;
    private final SubjectUserIdLookupValidator subjectUsers;
    private final BulkCatalogWorkbookService catalogWorkbooks;
    private final BulkCatalogImportService catalogImports;

    BulkResourceController(BulkResourceWorkbookService workbooks, BulkResourceImportService imports, SubjectUserIdLookupValidator subjectUsers, BulkCatalogWorkbookService catalogWorkbooks, BulkCatalogImportService catalogImports) {
        this.workbooks = workbooks;
        this.imports = imports;
        this.subjectUsers = subjectUsers;
        this.catalogWorkbooks = catalogWorkbooks;
        this.catalogImports = catalogImports;
    }

    @PostMapping(value = "/catalog/{mode}/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<ApiResponse<BulkResourceValidationResponse>>> validateCatalog(@PathVariable String mode, @RequestPart("file") FilePart file, ServerWebExchange exchange) {
        return bytes(file).flatMap(bytes -> withUser(user -> catalogImports.validate(mode, bytes, user.principal(), user.id())))
                .onErrorMap(UnsupportedBulkModeException.class, BulkResourceController::malformedMode)
                .map(BulkCatalogImportService.Result::response)
                .map(result -> ResponseEntity.ok(ApiResponse.success("BULK_CATALOG_VALIDATED", "Archivo validado", result, CorrelationWebFilter.context(exchange))));
    }

    @PostMapping(value = "/catalog/{mode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<ApiResponse<BulkResourceValidationResponse>>> commitCatalog(@PathVariable String mode, @RequestPart("file") FilePart file, ServerWebExchange exchange) {
        return bytes(file).flatMap(bytes -> withUser(user -> catalogImports.commit(mode, bytes, user.principal(), user.id())))
                .onErrorMap(UnsupportedBulkModeException.class, BulkResourceController::malformedMode)
                .map(BulkCatalogImportService.Result::response)
                .map(result -> ResponseEntity.ok(ApiResponse.success("BULK_CATALOG_IMPORTED", "Carga masiva procesada", result, CorrelationWebFilter.context(exchange))));
    }

    @PostMapping(value = "/catalog/{mode}/error-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<byte[]>> catalogErrorReport(@PathVariable String mode, @RequestPart("file") FilePart file) {
        return bytes(file).flatMap(bytes -> withUser(user -> catalogImports.validate(mode, bytes, user.principal(), user.id())
                        .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-errores.xlsx").build().toString()).body(catalogWorkbooks.errorReport(bytes, result.response().errors())))))
                .onErrorMap(UnsupportedBulkModeException.class, BulkResourceController::malformedMode);
    }

    @GetMapping("/catalog-template/{mode}")
    Mono<ResponseEntity<byte[]>> catalogTemplate(@PathVariable String mode) {
        try {
            return Mono.just(ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("plantilla-" + mode + ".xlsx").build().toString())
                    .body(catalogWorkbooks.template(mode)));
        } catch (UnsupportedBulkModeException error) {
            return Mono.error(malformedMode(error));
        }
    }

    private static MalformedRequestFieldException malformedMode(UnsupportedBulkModeException error) {
        return new MalformedRequestFieldException("mode", error.getMessage());
    }

    @GetMapping("/template")
    Mono<ResponseEntity<byte[]>> template(@RequestParam(defaultValue = "xlsx") String format) {
        if (!"xlsx".equalsIgnoreCase(format) && !"xls".equalsIgnoreCase(format))
            return Mono.just(ResponseEntity.badRequest().build());
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        String extension = xlsx ? "xlsx" : "xls";
        MediaType type = xlsx ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.parseMediaType("application/vnd.ms-excel");
        return Mono.just(ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("plantilla-recursos." + extension).build().toString()).body(workbooks.template(xlsx)));
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<ApiResponse<BulkResourceValidationResponse>>> validate(@RequestPart("file") FilePart file,
                                                                               ServerWebExchange exchange) {
        return bytes(file).flatMap(bytes -> withUser(userId -> imports.validate(bytes, userId.principal(), userId.id())))
                .map(BulkResourceImportService.Result::response)
                .map(result -> ResponseEntity.ok(ApiResponse.success("BULK_RESOURCE_VALIDATED",
                        "Archivo validado", result, CorrelationWebFilter.context(exchange))));
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<ApiResponse<BulkResourceValidationResponse>>> commit(@RequestPart("file") FilePart file,
                                                                             ServerWebExchange exchange) {
        return bytes(file).flatMap(bytes -> withUser(userId -> imports.commit(bytes, userId.principal(), userId.id())))
                .map(BulkResourceImportService.Result::response)
                .map(result -> ResponseEntity.ok(ApiResponse.success("BULK_RESOURCE_IMPORTED", "Carga masiva procesada", result,
                        CorrelationWebFilter.context(exchange))));
    }

    /* Devuelve el mismo libro con las filas anotadas; no persiste archivos en el servidor. */
    @PostMapping(value = "/error-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<ResponseEntity<byte[]>> errorReport(@RequestPart("file") FilePart file) {
        return bytes(file).flatMap(bytes -> withUser(user -> imports.validate(bytes, user.principal(), user.id())
                .map(result -> ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-errores.xlsx").build().toString())
                        .body(workbooks.annotate(bytes, result.response().errors())))));
    }

    private Mono<byte[]> bytes(FilePart file) {
        return DataBufferUtils.join(file.content()).map(buffer -> {
            byte[] result = new byte[buffer.readableByteCount()];
            buffer.read(result);
            DataBufferUtils.release(buffer);
            return result;
        });
    }

    private <T> Mono<T> withUser(java.util.function.Function<PrincipalUser, Mono<T>> action) {
        return SecurityContext.currentPrincipal().flatMap(principal -> principal.userId().map(Mono::just)
                .orElseGet(() -> subjectUsers.execute(principal.subject()))
                .switchIfEmpty(Mono.error(new IllegalStateException("No fue posible resolver el usuario autenticado")))
                .flatMap(user -> action.apply(new PrincipalUser(principal, user))));
    }

    private record PrincipalUser(co.edu.uco.seguridad.shared.security.PdpPrincipal principal, UserId id) {
    }
}
