package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadApplicationSecuritySummaryRequest; import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.ApplicationSecuritySummaryResponse; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ReadApplicationSecuritySummaryUseCase extends ReactiveOperation<ReadApplicationSecuritySummaryRequest,ApplicationSecuritySummaryResponse>{}
