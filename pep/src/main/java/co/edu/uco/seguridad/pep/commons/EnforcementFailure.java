package co.edu.uco.seguridad.pep.commons;

/**
 * Technical enforcement outcomes; no policy logic or HTTP dependency.
 */
public final class EnforcementFailure extends RuntimeException {
    public enum Kind {
        INVALID_REQUEST, UNKNOWN_ROUTE, UNAUTHENTICATED, DENIED, UNAVAILABLE,
        TOO_LARGE, RATE_LIMITED, BAD_GATEWAY, GATEWAY_TIMEOUT, UNSUPPORTED
    }

    private final Kind kind;
    private final String code;
    private final String decisionId;

    public EnforcementFailure(Kind kind, String code) {
        this(kind, code, null);
    }

    public EnforcementFailure(Kind kind, String code, String decisionId) {
        super(code);
        this.kind = kind;
        this.code = code;
        this.decisionId = decisionId;
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public String decisionId() {
        return decisionId;
    }
}
