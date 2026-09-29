package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationProfileAssignmentsRequest; import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationProfileAssignmentsUseCase extends ReactiveOperation<ListApplicationProfileAssignmentsRequest,ResultPage<ProfileAssignmentResponse>> { }
