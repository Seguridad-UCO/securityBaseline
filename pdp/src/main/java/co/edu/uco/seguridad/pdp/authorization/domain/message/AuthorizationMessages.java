package co.edu.uco.seguridad.pdp.authorization.domain.message;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;

/**
 * Catálogo de mensajes de negocio del slice {@code authorization}, en español (HU-009).
 */
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

    // HU-019 — DefineProfile se gatea desde este módulo (ProfileAdministrationController); el
    // perfil espeja las mismas dos barreras de alcance que el rol.
    public static String globalProfileScopeNotAdministrableYet() {
        return "los perfiles globales no se administran por este canal todavía";
    }

    public static String applicationIdNotApplicableForProfileTenantScope() {
        return "un perfil de alcance TENANT no admite applicationId";
    }

    // HU-024 — MFA como step-up: distinguible de notAuthorizedToAdminister (rol insuficiente vs.
    // sesión sin segundo factor).
    public static String mfaEvidenceRequired(ApplicationId applicationId) {
        return "El sujeto no tiene evidencia de MFA para administrar la aplicación " + applicationId.value();
    }

    // HU-036 — escrituras de catálogo de alcance TENANT: el actor es quien administra al menos una
    // aplicación del inquilino (ADR-036).
    public static String notAuthorizedToAdministerTenantCatalog() {
        return "El sujeto no está autorizado a administrar el catálogo de alcance inquilino: debe administrar "
                + "al menos una aplicación del inquilino";
    }

    public static String currentUserApplicationsAccessDenied() {
        return "El sujeto autenticado no puede consultar sus aplicaciones";
    }
}
