package co.edu.uco.seguridad.pdp.authorization.domain.model;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Referencia a la politica evaluada: identificador y version (vacia hasta HU-004).
 */
public record PolicyReference(String policyId, String version) {

    public PolicyReference {
        Objects.requireNonNull(policyId, RequiredArgumentMessages.POLICY_ID);
        Objects.requireNonNull(version, RequiredArgumentMessages.POLICY_VERSION);
    }
}
