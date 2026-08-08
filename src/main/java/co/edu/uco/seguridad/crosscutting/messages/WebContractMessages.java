package co.edu.uco.seguridad.crosscutting.messages;

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

    public static String successApplicationRegistered() {
        return "Protected application and initial resource registered";
    }

    public static String successCatalogQueried() {
        return "Protected application catalog queried";
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
