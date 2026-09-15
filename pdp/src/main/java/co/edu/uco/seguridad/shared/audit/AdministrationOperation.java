package co.edu.uco.seguridad.shared.audit;

/** Catálogo cerrado de operaciones administrativas auditables (HU-021). */
public enum AdministrationOperation {
    APPLICATION_REGISTERED,
    APPLICATION_REMOVED,
    APPLICATION_CREDENTIAL_ROTATED,
    ADMINISTRATOR_ASSIGNED,
    ADMINISTRATOR_REMOVED,
    ROLE_DEFINED,
    RESOURCE_REGISTERED,
    RESOURCE_GRANTED,
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    PROFILE_DEFINED,
    PROFILE_ROLE_ADDED,
    PROFILE_ASSIGNED,
    PROFILE_REVOKED
}
