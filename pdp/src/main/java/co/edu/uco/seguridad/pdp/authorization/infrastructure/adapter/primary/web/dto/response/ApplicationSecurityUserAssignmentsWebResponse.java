package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import co.edu.uco.seguridad.shared.web.PageResponse;

/** Complete access detail for one already tenant-scoped user. */
public record ApplicationSecurityUserAssignmentsWebResponse(ApplicationSecurityUserWebResponse user,
        PageResponse<ApplicationSecurityRoleAssignmentWebResponse> roleAssignments,
        PageResponse<ApplicationSecurityProfileAssignmentWebResponse> profileAssignments) {}
