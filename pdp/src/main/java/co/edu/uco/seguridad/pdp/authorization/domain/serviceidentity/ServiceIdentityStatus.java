package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

/**
 * Una identidad suspendida deja de producir decisiones ALLOW aunque conserve grants históricos.
 */
public enum ServiceIdentityStatus {ACTIVE, SUSPENDED, REVOKED}
