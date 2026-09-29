package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListRoleResourcesRequest;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ListRoleResourcesUseCase extends ReactiveOperation<ListRoleResourcesRequest, ResultPage<RegisteredProtectedResourceResponse>> { }
