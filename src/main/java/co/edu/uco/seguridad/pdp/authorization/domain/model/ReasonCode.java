package co.edu.uco.seguridad.pdp.authorization.domain.model;

/** Catalogo cerrado de motivos de decision: estables, en mayusculas, sin datos sensibles. */
public enum ReasonCode {

    NO_APPLICABLE_POLICY,
    POLICY_DENY,
    TENANT_MISMATCH,
    TOKEN_INVALID,
    CONTEXT_UNAVAILABLE
}
