package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/** Consulta mínima, tenant-scoped, requerida por importaciones administrativas. */
public interface ApplicationNameForTenantLookupValidator extends ReactiveOperation<ApplicationNameForTenantQuery, RegisteredApplicationResponse> { }
