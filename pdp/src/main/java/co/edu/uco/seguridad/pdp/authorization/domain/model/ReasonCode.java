package co.edu.uco.seguridad.pdp.authorization.domain.model;

/**
 * Por qué se decidió lo que se decidió.
 *
 * <p>Este enum es la implementación en el PDP del vocabulario único acordado entre los tres
 * componentes; la definición está en {@code contracts/reason-codes.md} y añadir un valor aquí sin
 * acordarlo allí es introducir deriva. El enum es cerrado a propósito: si el motor de políticas
 * emite un código que no está en esta lista, el mapeo falla en vez de propagar una errata.
 */
public enum ReasonCode {

    // --- Los produce la política -------------------------------------------------------------

    /**
     * Una política de aplicación concedió el acceso.
     */
    POLICY_ALLOWED,
    /**
     * Una política denegó explícitamente.
     */
    POLICY_DENY,
    /**
     * Ninguna política aplicó a la petición. Es la {@code NotApplicable} de XACML.
     */
    NO_APPLICABLE_POLICY,
    /**
     * El guard de tenant rechazó: acceso cruzado sin evidencia que lo respalde.
     */
    TENANT_ISOLATION_FAILED,

    // --- El motor no pudo decidir: son defectos, no denegaciones -----------------------------

    /**
     * El conjunto de hechos enviado al motor no pasó su validación. Acusa un defecto del PDP.
     */
    INVALID_INPUT,
    /**
     * Más de un candidato ALLOW para la misma petición.
     */
    POLICY_AMBIGUITY,
    /**
     * Una política emitió un candidato mal formado, p. ej. una obligación de tipo desconocido.
     */
    POLICY_OUTPUT_INVALID,

    // --- Los produce el PDP, antes o alrededor de la política --------------------------------

    /**
     * La aplicación no pertenece al tenant del principal, según el catálogo.
     */
    TENANT_MISMATCH,
    /**
     * La evidencia JWT no es válida. Se responde 401; el código existe para la traza.
     */
    TOKEN_INVALID,
    /**
     * El motor de políticas no respondió, expiró o falló.
     */
    CONTEXT_UNAVAILABLE
}
