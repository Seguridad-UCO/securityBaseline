package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationRoleAssignmentsRequest; import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationRoleAssignmentsUseCase extends ReactiveOperation<ListApplicationRoleAssignmentsRequest,ResultPage<AssignmentResponse>> { }
