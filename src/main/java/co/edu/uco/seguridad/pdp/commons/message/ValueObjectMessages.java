package co.edu.uco.seguridad.pdp.commons.message;

/**
 * Catálogo central de mensajes de error de los objetos de valor, agrupados por concepto en clases
 * internas estáticas — ningún objeto de valor ni excepción {@code Invalid*} debe contener literales.
 */
public final class ValueObjectMessages {

    /** Razón genérica cuando el campo llega nulo o vacío. Usada por todos los objetos de valor. */
    public static final String VALUE_REQUIRED = "se requiere un valor";

    public static final class ApplicationName {
        public static final String LENGTH = "debe contener de 3 a 100 caracteres";

        private ApplicationName() {
        }
    }

    public static final class TenantName {
        public static final String LENGTH = "debe contener de 3 a 100 caracteres";

        private TenantName() {
        }
    }

    public static final class ApplicationBaseUrl {
        public static final String FORMAT = "debe ser una URL absoluta (con esquema y host)";

        private ApplicationBaseUrl() {
        }
    }

    public static final class ResourcePath {
        public static final String FORMAT =
                "debe iniciar con / y no contener espacios, barras dobles, query ni fragment";

        private ResourcePath() {
        }
    }

    public static final class HttpMethod {
        public static final String UNSUPPORTED = "debe ser uno de GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS";

        private HttpMethod() {
        }
    }

    public static final class TenantId {
        public static final String FORMAT =
                "debe tener de 2 a 64 caracteres alfanuméricos, guiones o guiones bajos";

        private TenantId() {
        }
    }

    public static final class Identifier {
        public static final String NULL = "se requiere un identificador";

        private Identifier() {
        }
    }

    public static final class PageWindow {
        public static final String NEGATIVE_OFFSET = "el desplazamiento no debe ser negativo";
        public static final String LIMIT_RANGE = "el límite debe estar entre 1 y 100";
        public static final String NEGATIVE_PAGE = "la página no debe ser negativa";
        public static final String SIZE_RANGE = "el tamaño debe estar entre 1 y 100";

        private PageWindow() {
        }
    }

    public static final class ResourceCode {
        public static final String FORMAT = "debe ser kebab-case en minúsculas de 2 a 64 caracteres";

        private ResourceCode() {
        }
    }

    public static final class ActionCode {
        public static final String FORMAT = "debe ser kebab-case en minúsculas de 2 a 64 caracteres";

        private ActionCode() {
        }
    }

    public static final class ResultPage {
        public static final String NEGATIVE_TOTAL = "el total no debe ser negativo";
        public static final String WINDOW_REQUIRED = "se requiere la ventana de resultado";
        public static final String CONTENT_REQUIRED = "se requiere contenido de página";

        private ResultPage() {
        }
    }

    public static String invalidTenantId(String reason) {
        return "El id del inquilino es inválido: " + reason;
    }

    public static String invalidApplicationName(String reason) {
        return "El nombre de la aplicación es inválido: " + reason;
    }

    public static String invalidPageWindow(String reason) {
        return "La ventana de resultado es inválida: " + reason;
    }

    public static String invalidActionCode(String reason) {
        return "El código de acción es inválido: " + reason;
    }

    public static String invalidResourceCode(String reason) {
        return "El código de recurso es inválido: " + reason;
    }

    public static String invalidIdentifier(String identifierName, String rejectedValue) {
        return "El valor no es un válido " + identifierName.toLowerCase().replace('_', ' ')
                + ": " + rejectedValue;
    }

    public static String invalidTenantName(String reason) {
        return "El nombre del tenant es inválido: " + reason;
    }

    public static String invalidApplicationBaseUrl(String reason) {
        return "La URL base de la aplicación es inválida: " + reason;
    }

    public static String invalidResourcePath(String reason) {
        return "La ruta del recurso es inválida: " + reason;
    }

    public static String unsupportedHttpMethod(String reason) {
        return "El método HTTP es inválido: " + reason;
    }

    private ValueObjectMessages() {
    }
}
