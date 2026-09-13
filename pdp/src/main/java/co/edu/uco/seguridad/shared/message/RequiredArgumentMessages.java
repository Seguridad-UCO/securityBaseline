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
    public static final String NAME_FILTER = "se requiere el filtro de nombre";
    public static final String APPLICATION_CRITERIA = "se requiere el criterio de consulta";
    public static final String PAGE_WINDOW = "se requiere la ventana de paginación";
    public static final String TENANT_STATUS = "se requiere estado del inquilino";
    public static final String SUBJECT = "se requiere sujeto";
    public static final String PRINCIPAL_USER_ID = "se requiere el identificador interno del principal (puede estar ausente)";
    public static final String SUBJECT_ROLES = "se requieren los roles del sujeto (puede ser vacío)";
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
    // Reglas puras de dominio, inyectadas en los validadores y casos de uso que resuelven su entrada.
    public static final String ACTIVE_STATUS_RULE = "se requiere regla de estado activo";
    public static final String TENANT_EXISTS_RULE = "se requiere la regla de existencia de inquilino";
    public static final String APPLICATION_EXISTS_RULE = "se requiere la regla de existencia de aplicación";
    public static final String USER_RULE = "se requiere la regla de usuario";
    public static final String UNIQUE_RULE = "se requiere la regla de unicidad";
    public static final String RESERVED_NAME_RULE = "se requiere la regla de nombre reservado";

    // Validadores reactivos publicados entre módulos.
    public static final String TENANT_ACTIVE_VALIDATOR = "se requiere el validador de inquilino activo";
    public static final String APPLICATION_EXISTS_VALIDATOR = "se requiere el validador de existencia de aplicación";
    public static final String PROTECTED_RESOURCE_EXISTS_RULE = "se requiere la regla de existencia de recurso protegido";
    public static final String PROTECTED_RESOURCE_EXISTS_VALIDATOR = "se requiere el validador de existencia de recurso protegido";
    public static final String POLICY_DECISION_PORT = "se requiere el puerto de decisión de políticas";
    public static final String AUTHORIZE_USE_CASE = "se requiere el caso de uso de autorización";
    // HU-003 — canal interno para el PEP.
    public static final String APPLICATION_OWNER_LOOKUP_VALIDATOR = "se requiere el validador de dueño de aplicación";
    public static final String EVALUATE_INTERNAL_ACCESS_USE_CASE = "se requiere el caso de uso de acceso interno";
    public static final String INTERNAL_ACCESS_DECISION_INTERACTOR = "se requiere el interactor de decisión interna";
    public static final String AUTHORIZE_INTERACTOR = "se requiere el interactor de autorización";
    // HU-004 — catálogo de roles.
    public static final String ROLE_ID = "se requiere id de rol";
    public static final String ROLE_NAME = "se requiere nombre de rol";
    public static final String ROLE_SCOPE = "se requiere el alcance del rol";
    public static final String ROLE_SCOPE_LEVEL = "se requiere el nivel de alcance del rol";
    public static final String ROLE_SCOPE_TENANT = "se requiere el inquilino del alcance";
    public static final String ROLE_SCOPE_APPLICATION = "se requiere la aplicación del alcance";
    public static final String ROLE_RESOURCES = "se requiere el conjunto de recursos del rol";
    public static final String ROLE = "se requiere el rol";
    public static final String ROLE_CRITERIA = "se requiere el criterio de consulta de roles";
    public static final String ROLE_REPOSITORY = "se requiere el repositorio de roles";
    public static final String ROLE_NAME_UNIQUE_RULE = "se requiere la regla de nombre de rol único";
    public static final String ROLE_EXISTS_RULE = "se requiere la regla de existencia de rol";
    public static final String ROLE_SCOPE_COVERS_RULE = "se requiere la regla de cobertura del alcance";
    public static final String DEFINE_ROLE_RULES_VALIDATOR = "se requiere el validador de definición de rol";
    public static final String GRANT_RESOURCE_RULES_VALIDATOR = "se requiere el validador de concesión de recurso";
    public static final String PROTECTED_RESOURCE_OWNER_LOOKUP_VALIDATOR = "se requiere el validador de dueño de recurso";
    public static final String DEFINE_ROLE_USE_CASE = "se requiere el caso de uso de definición de rol";
    public static final String GRANT_RESOURCE_TO_ROLE_USE_CASE = "se requiere el caso de uso de concesión de recurso";
    public static final String LIST_ROLES_USE_CASE = "se requiere el caso de uso de consulta de roles";
    public static final String DEFINE_ROLE_INTERACTOR = "se requiere el interactor de definición de rol";
    public static final String GRANT_RESOURCE_TO_ROLE_INTERACTOR = "se requiere el interactor de concesión de recurso";
    public static final String LIST_ROLES_INTERACTOR = "se requiere el interactor de consulta de roles";
    // HU-005 — asignaciones vigentes.
    public static final String ROLE_SCOPE_COVERS_APPLICATION_RULE = "se requiere la regla de cobertura de aplicación del alcance";
    public static final String ASSIGNMENT_ID = "se requiere id de asignación";
    public static final String ASSIGNMENT = "se requiere la asignación";
    public static final String ASSIGNMENT_CRITERIA = "se requiere el criterio de consulta de asignaciones";
    public static final String ASSIGNMENT_REPOSITORY = "se requiere el repositorio de asignaciones";
    public static final String ASSIGNMENT_NOT_DUPLICATE_RULE = "se requiere la regla de no duplicidad de asignación activa";
    public static final String ASSIGNMENT_EXISTS_RULE = "se requiere la regla de existencia de asignación";
    public static final String VALIDITY = "se requiere la vigencia";
    public static final String VALID_FROM = "se requiere el inicio de vigencia";
    public static final String VALID_UNTIL = "se requiere el fin de vigencia";
    public static final String ACTIVE_ROLE_IDS = "se requiere el conjunto de roles activos";
    public static final String USER_MUST_EXIST_VALIDATOR = "se requiere el validador de existencia de usuario";
    public static final String ROLE_SCOPE_COVERS_APPLICATION_VALIDATOR = "se requiere el validador de cobertura de aplicación del alcance de rol";
    public static final String ASSIGN_ROLE_RULES_VALIDATOR = "se requiere el validador de asignación de rol";
    public static final String REVOKE_ASSIGNMENT_RULES_VALIDATOR = "se requiere el validador de revocación de asignación";
    public static final String ASSIGN_ROLE_USE_CASE = "se requiere el caso de uso de asignación de rol";
    public static final String REVOKE_ASSIGNMENT_USE_CASE = "se requiere el caso de uso de revocación de asignación";
    public static final String LIST_ASSIGNMENTS_USE_CASE = "se requiere el caso de uso de consulta de asignaciones";
    public static final String RESOLVE_ACTIVE_ROLES_USE_CASE = "se requiere el caso de uso de resolución de roles activos";
    public static final String ROLE_NAMES_LOOKUP_VALIDATOR = "se requiere el validador de nombres de rol";
    public static final String ACTIVE_ROLE_NAMES_LOOKUP_VALIDATOR = "se requiere el validador de nombres de roles activos";
    public static final String ASSIGN_ROLE_INTERACTOR = "se requiere el interactor de asignación de rol";
    public static final String REVOKE_ASSIGNMENT_INTERACTOR = "se requiere el interactor de revocación de asignación";
    public static final String LIST_ASSIGNMENTS_INTERACTOR = "se requiere el interactor de consulta de asignaciones";
    public static final String INTERNAL_MTLS_PROPERTIES = "se requiere la configuración mTLS del canal interno";
    public static final String INTERNAL_MTLS_TRUST_CERTIFICATE = "se requiere la ruta al certificado de confianza mTLS";
    public static final String INTERNAL_MTLS_ALLOWED_SUBJECTS = "se requiere la lista de sujetos admitidos por mTLS";
    public static final String INTERNAL_EVIDENCE_JWK_SET_URI = "se requiere la URL JWKS de la evidencia del canal interno";
    public static final String INTERNAL_EVIDENCE_ISSUER = "se requiere el emisor esperado de la evidencia del canal interno";
    public static final String INTERNAL_EVIDENCE_AUDIENCE = "se requiere la audiencia esperada de la evidencia del canal interno";

    // Componentes de AccessRequest / AccessDecision (autorizacion).
    public static final String REQUEST_ID = "se requiere el identificador de la solicitud";
    public static final String CORRELATION_ID = "se requiere el identificador de correlación";
    public static final String DECISION_ID = "se requiere el identificador de la decisión";
    public static final String DECISION_STATE = "se requiere el estado de la decisión";
    public static final String REASON_CODE = "se requiere el código de motivo";
    public static final String POLICY_REFERENCES = "se requiere la lista de referencias de política";
    public static final String DECIDED_AT = "se requiere el instante de la decisión";
    public static final String POLICY_ID = "se requiere el identificador de la política";
    public static final String POLICY_VERSION = "se requiere la versión de la política";
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

    // Cliente y configuración de OPA (HU-006).
    public static final String OPA_WEB_CLIENT = "se requiere el WebClient de OPA";
    public static final String OPA_PROPERTIES = "se requieren las propiedades de OPA";
    public static final String OPA_BASE_URL = "se requiere la URL de OPA (pdp.opa.base-url)";
    public static final String OPA_DECISION_PATH = "se requiere la ruta de decisión de OPA (pdp.opa.decision-path)";
    public static final String OPA_TIMEOUT = "se requiere el timeout de OPA (pdp.opa.timeout)";
    public static final String OPA_ADMINISTRATION_DECISION_PATH =
            "se requiere la ruta de decisión de administración de OPA (pdp.opa.administration-decision-path)";

    // Mecanismo de administración por aplicación (HU-009).
    public static final String ADMINISTRATION_DECISION_PORT = "se requiere el puerto de decisión de administración";
    public static final String AUTHORIZE_ADMINISTRATION_USE_CASE =
            "se requiere el caso de uso de autorización de administración";
    public static final String PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR =
            "se requiere el validador de administrador de aplicación";

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

    // HU-007 — evidencia de auditoría (AccessEvent).
    public static final String EVENT_ID = "se requiere el identificador del evento";
    public static final String ACCESS_AUDIT_REPOSITORY = "se requiere el repositorio de evidencia de auditoría";

    // HU-011 — catálogo de perfiles (profiles).
    public static final String PROFILE_ID = "se requiere id de perfil";
    public static final String PROFILE_NAME = "se requiere nombre de perfil";
    public static final String PROFILE_SCOPE = "se requiere el alcance del perfil";
    public static final String PROFILE_ROLES = "se requiere el conjunto de roles del perfil";
    public static final String PROFILE = "se requiere el perfil";
    public static final String PROFILE_CRITERIA = "se requiere el criterio de consulta de perfiles";
    public static final String PROFILE_REPOSITORY = "se requiere el repositorio de perfiles";
    public static final String PROFILE_NAME_UNIQUE_RULE = "se requiere la regla de nombre de perfil único";
    public static final String PROFILE_EXISTS_RULE = "se requiere la regla de existencia de perfil";
    public static final String DEFINE_PROFILE_RULES_VALIDATOR = "se requiere el validador de definición de perfil";
    public static final String ADD_ROLE_TO_PROFILE_RULES_VALIDATOR = "se requiere el validador de agregar rol a perfil";
    public static final String PROFILE_ROLES_LOOKUP_VALIDATOR = "se requiere el validador de roles de perfil";
    public static final String DEFINE_PROFILE_USE_CASE = "se requiere el caso de uso de definición de perfil";
    public static final String ADD_ROLE_TO_PROFILE_USE_CASE = "se requiere el caso de uso de agregar rol a perfil";
    public static final String LIST_PROFILES_USE_CASE = "se requiere el caso de uso de consulta de perfiles";
    public static final String DEFINE_PROFILE_INTERACTOR = "se requiere el interactor de definición de perfil";
    public static final String ADD_ROLE_TO_PROFILE_INTERACTOR = "se requiere el interactor de agregar rol a perfil";
    public static final String LIST_PROFILES_INTERACTOR = "se requiere el interactor de consulta de perfiles";
    public static final String ROLE_MUST_EXIST_FOR_TENANT_VALIDATOR = "se requiere el validador de existencia de rol";

    // HU-011 — asignación de perfiles (assignments).
    public static final String PROFILE_ASSIGNMENT_ID = "se requiere id de asignación de perfil";
    public static final String PROFILE_ASSIGNMENT = "se requiere la asignación de perfil";
    public static final String PROFILE_ASSIGNMENT_REPOSITORY = "se requiere el repositorio de asignaciones de perfil";
    public static final String GENERATED_ASSIGNMENT_IDS = "se requiere el conjunto de asignaciones generadas";
    public static final String PROFILE_ASSIGNMENT_NOT_DUPLICATE_RULE =
            "se requiere la regla de no duplicidad de asignación de perfil activa";
    public static final String PROFILE_ASSIGNMENT_EXISTS_RULE = "se requiere la regla de existencia de asignación de perfil";
    public static final String ASSIGN_PROFILE_RULES_VALIDATOR = "se requiere el validador de asignación de perfil";
    public static final String REVOKE_PROFILE_ASSIGNMENT_RULES_VALIDATOR =
            "se requiere el validador de revocación de asignación de perfil";
    public static final String ASSIGN_PROFILE_USE_CASE = "se requiere el caso de uso de asignación de perfil";
    public static final String REVOKE_PROFILE_ASSIGNMENT_USE_CASE =
            "se requiere el caso de uso de revocación de asignación de perfil";
    public static final String ASSIGN_PROFILE_INTERACTOR = "se requiere el interactor de asignación de perfil";
    public static final String REVOKE_PROFILE_ASSIGNMENT_INTERACTOR =
            "se requiere el interactor de revocación de asignación de perfil";

    // HU-012 — credencial de aplicación.
    public static final String SECRET_GENERATOR = "se requiere el generador de secretos";
    public static final String CREDENTIAL_HASHER = "se requiere el hasher de credenciales";
    public static final String APPLICATION_CREDENTIAL_HASH = "se requiere el hash de la credencial de la aplicación";
    public static final String REGISTERED_APPLICATION_RESPONSE = "se requiere la respuesta de aplicación registrada";
    public static final String APPLICATION_CREDENTIAL = "se requiere la credencial de la aplicación";

    // HU-013 — validación de credencial de aplicación.
    public static final String APPLICATION_CREDENTIAL_MUST_BE_VALID_RULE =
            "se requiere la regla de validez de credencial de aplicación";
    public static final String VALIDATE_APPLICATION_CREDENTIAL_USE_CASE =
            "se requiere el caso de uso de validación de credencial de aplicación";
    public static final String VALIDATE_APPLICATION_CREDENTIAL_INTERACTOR =
            "se requiere el interactor de validación de credencial de aplicación";

    // HU-014 — rotación de credencial de aplicación.
    public static final String ROTATE_APPLICATION_CREDENTIAL_USE_CASE =
            "se requiere el caso de uso de rotación de credencial de aplicación";
    public static final String ROTATE_APPLICATION_CREDENTIAL_INTERACTOR =
            "se requiere el interactor de rotación de credencial de aplicación";

    private RequiredArgumentMessages() {
    }
}
