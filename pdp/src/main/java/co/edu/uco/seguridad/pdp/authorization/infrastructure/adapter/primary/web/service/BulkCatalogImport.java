package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import reactor.core.publisher.Mono;

/**
 * Una modalidad de carga masiva del catálogo. Cada modalidad se registra en {@link BulkCatalogImports}: agregar una
 * nueva no obliga a modificar las existentes ni el controlador.
 */
public interface BulkCatalogImport {

    /**
     * Valida el archivo completo sin persistir nada.
     */
    Mono<BulkResourceValidationResponse> validate(byte[] bytes, PdpPrincipal principal, UserId userId);

    /**
     * Revalida y, solo si el archivo está listo, persiste todo o nada.
     */
    Mono<BulkResourceValidationResponse> commit(byte[] bytes, PdpPrincipal principal, UserId userId);

    /**
     * Expone una modalidad ({@code roles} o {@code profiles}) de {@link BulkCatalogImportService}.
     */
    static BulkCatalogImport of(BulkCatalogImportService service, String mode) {
        return new BulkCatalogImport() {
            @Override
            public Mono<BulkResourceValidationResponse> validate(byte[] bytes, PdpPrincipal principal, UserId userId) {
                return service.validate(mode, bytes, principal, userId).map(BulkCatalogImportService.Result::response);
            }

            @Override
            public Mono<BulkResourceValidationResponse> commit(byte[] bytes, PdpPrincipal principal, UserId userId) {
                return service.commit(mode, bytes, principal, userId).map(BulkCatalogImportService.Result::response);
            }
        };
    }
}
