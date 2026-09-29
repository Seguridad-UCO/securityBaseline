package co.edu.uco.seguridad.pdp.resources.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

public record UpdateProtectedResourceRequest(TenantId tenantId, ResourceId resourceId, ResourcePath path,
        HttpVerb method) { }
