package co.edu.uco.seguridad.pep.commons;

import java.util.Objects;

/**
 * Opaque evidence; domain code neither parses nor logs provider claims.
 */
public record IdentityEvidence(String bearer) {
    public IdentityEvidence(String bearer) {
        this.bearer = Objects.requireNonNull(bearer);
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}

