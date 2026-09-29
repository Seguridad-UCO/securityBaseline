package co.edu.uco.seguridad.pdp.resources.application.usecase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ListProtectedResourcesPageRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListProtectedResourcesPageUseCase extends ReactiveOperation<ListProtectedResourcesPageRequest, ResultPage<RegisteredProtectedResourceResponse>> { }
