package co.edu.uco.seguridad.shared.web.message;

/**
 * Catálogo de mensajes del contrato HTTP compartido (excepciones de frontera, parser y controlador).
 */
public final class WebContractMessages {

    public static String missingField(String field) {
        return "El campo '" + field + "' es requerido";
    }

    public static String malformedField(String field, String reason) {
        return "El campo '" + field + "' es inválido: " + reason;
    }

    public static String conflictingPagingModes() {
        return "Use either page/size or offset/limit, not both";
    }

    public static String offsetLimitTogether() {
        return "offset y limit deben suministrarse juntos";
    }

    public static String unreadableBody() {
        return "El cuerpo de la solicitud no pudo ser leído";
    }

    public static String internalError() {
        return "La solicitud no pudo ser completada";
    }

    public static String unauthorized() {
        return "Token ausente, inválido o expirado";
    }

    public static String forbidden() {
        return "El token es válido pero no autoriza esta operación";
    }

    public static String successApplicationRegistered() {
        return "Aplicación protegida y recurso inicial registrados";
    }

    public static String successCatalogQueried() {
        return "Catálogo de aplicaciones protegidas consultado";
    }

    public static String successAccessEvaluated() {
        return "Solicitud de acceso evaluada";
    }

    public static String successRoleDefined() {
        return "Rol definido";
    }

    public static String successRoleResourceGranted() {
        return "Recurso concedido al rol";
    }

    public static String successRolesListed() {
        return "Catálogo de roles consultado";
    }

    public static String successAssignmentCreated() {
        return "Rol asignado";
    }

    public static String successAssignmentRevoked() {
        return "Asignación revocada";
    }

    public static String successAssignmentsListed() {
        return "Asignaciones del rol consultadas";
    }

    public static String successProfileDefined() {
        return "Perfil definido";
    }

    public static String successProfileRoleAdded() {
        return "Rol agregado al perfil";
    }

    public static String successProfilesListed() {
        return "Catálogo de perfiles consultado";
    }

    public static String successProfileAssigned() {
        return "Perfil asignado";
    }

    public static String successProfileAssignmentRevoked() {
        return "Asignación de perfil revocada";
    }

    public static String mustBeInteger() {
        return "debe ser un número entero";
    }

    public static String mustBeVersion1() {
        return "debe ser \"1\"";
    }

    public static String mustBeIso8601() {
        return "debe ser una fecha en formato ISO 8601";
    }

    public static String headerBodyMismatch(String field) {
        return "El valor de '" + field + "' en la cabecera no coincide con el del cuerpo";
    }

    public static String requireErrorCode() {
        return "se requiere código de error de contrato";
    }

    public static String requireOffendingField() {
        return "se requiere campo ofensor";
    }

    // HU-014 — rotación de credencial de aplicación.
    public static String successApplicationCredentialRotated() {
        return "La credencial de la aplicación se rotó correctamente";
    }

    // HU-010 — registro de aplicación con recurso inicial, con compensación explícita.
    public static String successApplicationRegisteredWithInitialResource() {
        return "Aplicación y recurso inicial registrados correctamente";
    }

    // HU-015 — administración del catálogo de aplicaciones.
    public static String successApplicationRemoved() {
        return "La aplicación se eliminó correctamente";
    }

    public static String successApplicationAdministratorAssigned() {
        return "El administrador de la aplicación se asignó correctamente";
    }

    private WebContractMessages() {
    }
}
