package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

/**
 * Consulta paginada de asignaciones vigentes dentro de una aplicación, filtradas por una clave
 * (rol, perfil o usuario según el caso de uso).
 */
public record ListActiveAssignmentsPageRequest<K>(K key, TenantId tenantId, ApplicationId applicationId,
                                                  PageWindow window) {
    public ListActiveAssignmentsPageRequest {
        Objects.requireNonNull(key);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(window);
    }
}
