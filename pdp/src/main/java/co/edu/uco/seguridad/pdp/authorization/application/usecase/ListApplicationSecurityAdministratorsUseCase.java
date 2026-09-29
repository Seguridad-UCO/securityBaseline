package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityAdministratorsRequest; import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationSecurityAdministratorsUseCase extends ReactiveOperation<ListApplicationSecurityAdministratorsRequest,ResultPage<AssignmentResponse>> { }
