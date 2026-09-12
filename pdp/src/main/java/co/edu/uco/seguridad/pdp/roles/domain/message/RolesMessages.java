package co.edu.uco.seguridad.pdp.roles.domain.message;

/**
 * Catálogo de mensajes de las excepciones de negocio del módulo {@code roles}. Los métodos reciben
 * {@code String} para que el catálogo no arrastre tipos de otros módulos.
 */
public final class RolesMessages {

    public static String roleNameTaken(String name, String scope) {
        return "Ya existe un rol llamado '" + name + "' en el alcance " + scope;
    }

    public static String roleNotFound(String roleId) {
        return "No existe el rol " + roleId + " para este inquilino";
    }

    public static String resourceOutsideRoleScope(String resourceId) {
        return "El recurso " + resourceId + " está fuera del alcance del rol";
    }

    public static String resourceOutsideRoleScopeForApplication(String applicationId) {
        return "El recurso pertenece a la aplicación " + applicationId + ", fuera del alcance del rol";
    }

    public static String globalScopeNotAdministrableYet() {
        return "los roles globales no se administran por este canal todavía";
    }

    public static String applicationIdNotApplicableForTenantScope() {
        return "un rol de alcance TENANT no admite applicationId";
    }

    private RolesMessages() {
    }
}
