package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ProtectedResourceIdLookupValidator extends ReactiveOperation<ProtectedResourceLookup, ResourceId> {
}
