package co.edu.uco.seguridad.shared.config;

/**
 * Paquete raíz del proyecto, en un único lugar para que el escaneo de componentes de
 * {@code PdpApplication} no dependa de un literal repetido. Debe coincidir con
 * {@code logging.level.co.edu.uco.seguridad} en cada {@code application*.properties} — un archivo
 * de propiedades no puede referenciar esta constante, así que esa coincidencia se mantiene a mano.
 */
public final class ProjectPackages {

    public static final String BASE = "co.edu.uco.seguridad";

    private ProjectPackages() {
    }
}
