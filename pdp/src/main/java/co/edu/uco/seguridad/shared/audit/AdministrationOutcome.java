package co.edu.uco.seguridad.shared.audit;

/**
 * Resultado de una operación administrativa auditada (HU-021). Sin {@code ReasonCode}: si se
 * rechaza, la razón siempre es "no administra la aplicación" hasta que exista una segunda.
 */
public enum AdministrationOutcome {
    ALLOWED,
    DENIED
}
