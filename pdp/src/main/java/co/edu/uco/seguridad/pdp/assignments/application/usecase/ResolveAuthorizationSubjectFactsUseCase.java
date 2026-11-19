package co.edu.uco.seguridad.pdp.assignments.application.usecase;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveAuthorizationSubjectFactsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AuthorizationSubjectFactsResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface ResolveAuthorizationSubjectFactsUseCase extends ReactiveOperation<ResolveAuthorizationSubjectFactsRequest, AuthorizationSubjectFactsResponse> {
}
