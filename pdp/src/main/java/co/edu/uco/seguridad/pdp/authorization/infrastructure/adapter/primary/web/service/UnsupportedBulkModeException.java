package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

/**
 * La modalidad de carga masiva del catálogo (el segmento {@code {mode}} de la ruta) no es una de las
 * soportadas para la operación pedida (HU-035). Extiende {@link IllegalArgumentException} a propósito:
 * es el tipo que {@code BulkCatalogWorkbookService.template} ya anunciaba para este caso. El controlador
 * la traduce a {@code MalformedRequestFieldException("mode", ...)}, que {@code ApiErrorHandler} responde
 * como 400; nunca debe llegar al manejador genérico (500).
 */
public final class UnsupportedBulkModeException extends IllegalArgumentException {

    public UnsupportedBulkModeException(String message) {
        super(message);
    }
}
