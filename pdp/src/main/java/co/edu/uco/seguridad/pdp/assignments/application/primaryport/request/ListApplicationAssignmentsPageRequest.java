package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;
import co.edu.uco.seguridad.pdp.commons.model.*; import java.util.Objects;
public record ListApplicationAssignmentsPageRequest(TenantId tenantId, ApplicationId applicationId, PageWindow window) { public ListApplicationAssignmentsPageRequest { Objects.requireNonNull(tenantId); Objects.requireNonNull(applicationId); Objects.requireNonNull(window); } }
