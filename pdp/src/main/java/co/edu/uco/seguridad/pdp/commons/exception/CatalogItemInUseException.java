package co.edu.uco.seguridad.pdp.commons.exception;

/**
 * Impide borrar un elemento de catálogo mientras otra relación activa aún lo referencia.
 */
public final class CatalogItemInUseException extends ConflictBusinessRuleException {
    public CatalogItemInUseException(String item) {
        super("CATALOG_ITEM_IN_USE", "No se puede eliminar " + item + " porque tiene dependencias activas.");
    }
}
