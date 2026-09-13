package co.edu.uco.seguridad.pdp.assignments.domain.message;

/** Catálogo de mensajes de las excepciones de negocio del módulo {@code assignments}. */
public final class AssignmentsMessages {

    public static String assignmentAlreadyActive(String userId, String applicationId, String roleId) {
        return "El usuario " + userId + " ya tiene una asignación activa del rol " + roleId + " en la aplicación "
                + applicationId;
    }

    public static String assignmentNotFound(String assignmentId) {
        return "No existe la asignación " + assignmentId + " para este inquilino";
    }

    public static String invalidValidity(String reason) {
        return "La vigencia es inválida: " + reason;
    }

    public static String profileAssignmentAlreadyActive(String userId, String applicationId, String profileId) {
        return "El usuario " + userId + " ya tiene una asignación activa del perfil " + profileId
                + " en la aplicación " + applicationId;
    }

    public static String profileAssignmentNotFound(String profileAssignmentId) {
        return "No existe la asignación de perfil " + profileAssignmentId + " para este inquilino";
    }

    private AssignmentsMessages() {
    }
}
