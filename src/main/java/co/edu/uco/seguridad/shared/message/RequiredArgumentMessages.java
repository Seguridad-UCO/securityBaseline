package co.edu.uco.seguridad.shared.message;

/**
 * Catálogo central de mensajes de {@code Objects.requireNonNull} usados en constructores de
 * registros, casos de uso, interactores, reglas y adaptadores.
 *
 * <p>Son guardas internas, no mensajes que lleguen al cliente HTTP (una violación cae en el handler
 * genérico de 500 de {@code ApiErrorHandler}). Se centralizan aquí para que la misma idea —"se
 * requiere id de inquilino", repetida en una docena de constructores— tenga una sola redacción,
 * en vez de una copia ligeramente distinta por clase.</p>
 */
public final class RequiredArgumentMessages {

    // Identificadores y value objects de dominio, compartidos entre DTOs, entidades y eventos.
    public static final String TENANT_ID = "se requiere id de inquilino";
    public static final String TENANT_NAME = "se requiere nombre de inquilino";
    public static final String APPLICATION_ID = "se requiere id de aplicación";
    public static final String RESOURCE_ID = "se requiere id de recurso";
    public static final String APPLICATION_NAME = "se requiere nombre de aplicación";
    public static final String APPLICATION_BASE_URL = "se requiere la URL base de la aplicación";
    public static final String RESOURCE_PATH = "se requiere la ruta del recurso";
    public static final String HTTP_METHOD = "se requiere el método HTTP";
    public static final String USER_ID = "se requiere id de usuario";
    public static final String USER_EMAIL = "se requiere correo de usuario";
    public static final String EXTERNAL_IDENTITY_ISSUER = "se requiere el issuer de la identidad externa";
    public static final String EXTERNAL_IDENTITY_SUBJECT = "se requiere el subject de la identidad externa";
    public static final String EXTERNAL_IDENTITY_PROVIDER = "se requiere el proveedor de la identidad externa";
    public static final String RESOURCE_CODE = "se requiere código de recurso";
    public static final String ACTION_CODE = "se requiere código de acción";
    public static final String REGISTERED_AT = "se requiere instante de registro";
    public static final String EVENT_OCCURRED_ON = "se requiere instante del evento";
    public static final String USER_REPOSITORY = "se requiere el repositorio de usuarios";
    public static final String USER_RULE = "se requiere la regla de usuario";
    public static final String APPLICATION_RULE = "se requiere la regla de aplicación";
    public static final String NAME_FILTER = "se requiere el filtro de nombre";
    public static final String APPLICATION_CRITERIA = "se requiere el criterio de consulta";
    public static final String PAGE_WINDOW = "se requiere la ventana de paginación";
    public static final String TENANT_STATUS = "se requiere estado del inquilino";
    public static final String SUBJECT = "se requiere sujeto";
    public static final String SEARCH_CRITERIA = "se requieren criterios de búsqueda";
    public static final String RESULT_WINDOW = "se requiere ventana de resultado";
    public static final String DTO = "se requiere dto";
    public static final String REGISTERED_APPLICATION = "se requiere aplicación registrada";

    // Puertos y colaboradores inyectados.
    public static final String RULES_VALIDATOR = "se requiere validador de reglas";
    public static final String PROTECTED_RESOURCE_REPOSITORY = "se requiere repositorio de recurso protegido";
    public static final String APPLICATION_REPOSITORY = "se requiere repositorio de aplicación";
    public static final String TENANT_REPOSITORY = "se requiere repositorio de inquilino";
    public static final String DOMAIN_EVENT_PUBLISHER = "se requiere publicador de eventos de dominio";
    public static final String SPRING_EVENT_PUBLISHER = "se requiere el publicador de eventos de Spring";
    public static final String TIME_PROVIDER = "se requiere proveedor de tiempo";
    public static final String IDENTIFIER_GENERATOR = "se requiere generador de identificadores";
    public static final String TENANT_RULE = "se requiere regla de inquilino";
    public static final String ACTIVE_STATUS_RULE = "se requiere regla de estado activo";
    public static final String REGISTER_USE_CASE = "se requiere caso de uso de registro";
    public static final String SEARCH_USE_CASE = "se requiere caso de uso de búsqueda";
    public static final String REMOVE_USE_CASE = "se requiere caso de uso de eliminación";
    public static final String RESERVED_NAMES = "se requieren nombres reservados";
    public static final String TENANT_CATALOG = "se requiere el catálogo de tenants";

    // Cliente y configuración de SurrealDB.
    public static final String SURREALDB_CLIENT = "se requiere el cliente de SurrealDB";
    public static final String SURREALDB_WEBCLIENT = "se requiere el WebClient de SurrealDB";
    public static final String SURREALDB_PROPERTIES = "se requiere la configuración de SurrealDB";
    public static final String SURREALDB_NAMESPACE = "se requiere el namespace de SurrealDB";
    public static final String SURREALDB_DATABASE = "se requiere la database de SurrealDB";
    public static final String SURREALDB_USERNAME = "se requiere el usuario de SurrealDB";
    public static final String SURREALDB_SECRET = "se requiere la contraseña de SurrealDB";
    public static final String SURREALDB_URL = "se requiere la URL de SurrealDB (pdp.persistence.surrealdb.url)";
    public static final String OBJECT_MAPPER = "se requiere ObjectMapper";

    // Seguridad JWT.
    public static final String JWT_ISSUER = "se requiere el emisor esperado (pdp.security.jwt.issuer)";
    public static final String JWT_EXACTLY_ONE_MODE =
            "se requiere exactamente uno de pdp.security.jwt.secret (HMAC, pruebas) o "
                    + "pdp.security.jwt.jwk-set-uri (JWKS de Keycloak, ambientes reales) — nunca ambos ni ninguno";
    public static final String JWT_AUDIENCE = "se requiere la audiencia esperada (pdp.security.jwt.audience)";
    public static final String JWT_ID = "se requiere el identificador del token (jti)";

    // Eventos de dominio y excepciones (pdp/commons, shared/event).
    public static final String DOMAIN_EVENT = "se requiere el evento de dominio";
    public static final String DOMAIN_EVENTS_LIST = "se requiere la lista de eventos de dominio";
    public static final String AGGREGATE_ENTITY = "se requiere la entidad del agregado";
    public static final String DOMAIN_EXCEPTION_MESSAGE = "se requiere mensaje de excepción de dominio";
    public static final String DOMAIN_EXCEPTION_CODE = "se requiere código de excepción de dominio";

    private RequiredArgumentMessages() {
    }
}
