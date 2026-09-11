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

    public static String mustBeInteger() {
        return "debe ser un número entero";
    }

    public static String requireErrorCode() {
        return "se requiere código de error de contrato";
    }

    public static String requireOffendingField() {
        return "se requiere campo ofensor";
    }

    private WebContractMessages() {
    }
}
