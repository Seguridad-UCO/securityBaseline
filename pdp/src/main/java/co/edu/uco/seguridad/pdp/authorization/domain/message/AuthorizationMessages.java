package co.edu.uco.seguridad.pdp.authorization.domain.message;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;

/** Catálogo de mensajes de negocio del slice {@code authorization}, en español (HU-009). */
public final class AuthorizationMessages {

    private AuthorizationMessages() {
    }

    public static String notAuthorizedToAdminister(ApplicationId applicationId) {
        return "El sujeto no está autorizado a administrar la aplicación " + applicationId.value();
    }

    // HU-016 — movidos desde RolesMessages: DefineRoleRequestMapper ahora vive en este módulo
    // (RoleAdministrationController gatea la escritura que antes exponía RoleController).
    public static String globalScopeNotAdministrableYet() {
        return "los roles globales no se administran por este canal todavía";
    }

    public static String applicationIdNotApplicableForTenantScope() {
        return "un rol de alcance TENANT no admite applicationId";
    }
}
