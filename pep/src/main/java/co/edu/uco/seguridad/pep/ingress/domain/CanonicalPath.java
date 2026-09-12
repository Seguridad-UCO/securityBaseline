package co.edu.uco.seguridad.pep.ingress.domain;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.INVALID_REQUEST;

/**
 * One conservative path representation for both authorization and forwarding.
 */
public record CanonicalPath(String value) {
    public CanonicalPath {
        if (value == null || !value.matches("/[A-Za-z0-9/._~-]*") || value.contains("//")) invalid();
        for (String segment : value.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) invalid();
        }
    }

    private static void invalid() {
        throw new EnforcementFailure(INVALID_REQUEST, "AMBIGUOUS_PATH");
    }
}

