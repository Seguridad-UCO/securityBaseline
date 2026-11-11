package co.edu.uco.seguridad.pdp.assignments.application.event;

/** Marca operaciones compuestas para que sus materializaciones no emitan hechos semánticos propios. */
public final class AssignmentEventContext {
    public static final String SUPPRESS_DIRECT_EVENT = AssignmentEventContext.class.getName() + ".suppress";
    private AssignmentEventContext() { }
}
