package co.edu.uco.seguridad.pdp.profiles.domain.message;

/** Catálogo de mensajes de las excepciones de negocio del módulo {@code profiles}. Espejo de {@code RolesMessages}. */
public final class ProfilesMessages {

    public static String profileNameTaken(String name, String scope) {
        return "Ya existe un perfil llamado '" + name + "' en el alcance " + scope;
    }

    public static String profileNotFound(String profileId) {
        return "No existe el perfil " + profileId + " para este inquilino";
    }

    public static String globalScopeNotAdministrableYet() {
        return "los perfiles globales no se administran por este canal todavía";
    }

    public static String applicationIdNotApplicableForTenantScope() {
        return "un perfil de alcance TENANT no admite applicationId";
    }

    private ProfilesMessages() {
    }
}
