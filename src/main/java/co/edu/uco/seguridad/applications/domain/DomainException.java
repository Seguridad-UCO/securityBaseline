package co.edu.uco.seguridad.applications.domain;

/** Base exception for rule violations; it deliberately has no framework dependency. */
public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
}
