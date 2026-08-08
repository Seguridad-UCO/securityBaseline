package co.edu.uco.seguridad.crosscutting.messages;

/**
 * Catálogo central de mensajes de error de los objetos de valor.
 *
 * <p>Ningún objeto de valor, entidad de dominio ni excepción debe contener literales de texto
 * orientados al usuario directamente en su cuerpo. Todos los mensajes se definen aquí, agrupados
 * en clases internas estáticas por concepto, de modo que un cambio de redacción impacte un único
 * sitio y sea trivialmente rastreable con las herramientas del IDE.</p>
 *
 * <p>Incluye tanto fragmentos de razón (usados por los constructores de objetos de valor) como
 * plantillas de mensaje completo (usadas por las excepciones {@code Invalid*}), de forma que esas
 * clases de excepción no contengan literales en español.</p>
 */
public final class ValueObjectMessages {

    /** Razón genérica cuando el campo llega nulo o vacío. Usada por todos los objetos de valor. */
    public static final String VALUE_REQUIRED = "se requiere un valor";

    public static final class ApplicationName {
        public static final String LENGTH = "debe contener de 3 a 100 caracteres";

        private ApplicationName() {
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

    private ValueObjectMessages() {
    }
}
