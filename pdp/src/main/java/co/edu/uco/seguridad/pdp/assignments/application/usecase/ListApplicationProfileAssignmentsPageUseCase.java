package co.edu.uco.seguridad.pdp.assignments.application.usecase;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAssignmentsPageRequest; import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationProfileAssignmentsPageUseCase extends ReactiveOperation<ListApplicationAssignmentsPageRequest, ResultPage<ProfileAssignmentResponse>> { }
