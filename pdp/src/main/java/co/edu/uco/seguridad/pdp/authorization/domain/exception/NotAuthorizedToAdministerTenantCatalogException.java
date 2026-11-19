package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Optional;

/**
 * El sujeto no administra ninguna aplicación de su inquilino, así que no puede crear ni editar roles o
 * perfiles de alcance {@code TENANT} (HU-036, ADR-036). Distinta de {@link NotAuthorizedToAdministerException}
 * a propósito: aquí no hay una aplicación que se haya intentado administrar. {@code evaluatedApplication}
 * es la primera aplicación contra la que se evaluó al sujeto (vacía si no tiene ninguna): la auditoría
 * atribuye a ella el evento {@code DENIED}; no implica que la operación apuntara a esa aplicación.
 * {@code tenantId} viaja como en las demás excepciones de este paquete, para la traza.
 */
public final class NotAuthorizedToAdministerTenantCatalogException extends BusinessRuleViolationException {

    private final transient Optional<ApplicationId> evaluatedApplication;

    public NotAuthorizedToAdministerTenantCatalogException(TenantId tenantId, Optional<ApplicationId> evaluatedApplication) {
        super("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG", AuthorizationMessages.notAuthorizedToAdministerTenantCatalog());
        this.evaluatedApplication = evaluatedApplication;
    }

    public Optional<ApplicationId> evaluatedApplication() {
        return evaluatedApplication;
    }
}
