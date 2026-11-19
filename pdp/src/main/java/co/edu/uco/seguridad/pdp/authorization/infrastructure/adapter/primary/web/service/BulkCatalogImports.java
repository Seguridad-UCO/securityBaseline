package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registro de modalidades de carga masiva del catálogo, indexadas por el {@code mode} de la ruta.
 *
 * <p>Abierto a extensión, cerrado a modificación: una modalidad nueva se registra en la configuración; el
 * controlador y las modalidades existentes no cambian. Una modalidad no registrada es
 * {@link UnsupportedBulkModeException} (400 en el campo {@code mode}).</p>
 */
public final class BulkCatalogImports {
    private final Map<String, BulkCatalogImport> modes;

    public BulkCatalogImports(Map<String, BulkCatalogImport> modes) {
        this.modes = new LinkedHashMap<>(Objects.requireNonNull(modes));
    }

    public BulkCatalogImport forMode(String mode) {
        BulkCatalogImport found = mode == null ? null : modes.get(mode);
        if (found == null) {
            throw new UnsupportedBulkModeException("Modalidad de carga no soportada: use " + String.join(", ", modes.keySet()));
        }
        return found;
    }
}
