package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

/**
 * Ciclo de una solicitud de acceso interaplicación; la ausencia nunca significa aprobación.
 */
public enum ServiceAccessStatus {PENDING, APPROVED, REJECTED, REVOKED}
