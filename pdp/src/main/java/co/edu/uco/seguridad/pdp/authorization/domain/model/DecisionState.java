package co.edu.uco.seguridad.pdp.authorization.domain.model;

/**
 * Resultado tri-estado de una evaluacion de acceso (INV-POL-04): nunca un booleano desnudo.
 */
public enum DecisionState {

    ALLOW, DENY, INDETERMINATE;

    public boolean isAllow() {
        return this == ALLOW;
    }
}
