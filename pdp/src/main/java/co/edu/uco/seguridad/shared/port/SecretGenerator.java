package co.edu.uco.seguridad.shared.port;

/**
 * Genera secretos aleatorios de alta entropía para que los casos de uso nunca compongan uno en
 * línea (HU-012). Distinto de {@link IdentifierGenerator}: un identificador puede repetirse en
 * texto (no es secreto), un secreto de aplicación no debe ser adivinable.
 */
@FunctionalInterface
public interface SecretGenerator {

    String next();
}
