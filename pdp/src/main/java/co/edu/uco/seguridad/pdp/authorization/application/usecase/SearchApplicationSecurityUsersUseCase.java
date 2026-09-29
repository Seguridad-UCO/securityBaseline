package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.SearchApplicationSecurityUsersRequest; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface SearchApplicationSecurityUsersUseCase extends ReactiveOperation<SearchApplicationSecurityUsersRequest,ResultPage<UserResponse>> { }
