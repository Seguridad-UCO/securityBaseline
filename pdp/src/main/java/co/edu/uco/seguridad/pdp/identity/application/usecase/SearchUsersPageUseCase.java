package co.edu.uco.seguridad.pdp.identity.application.usecase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.SearchUsersPageRequest; import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface SearchUsersPageUseCase extends ReactiveOperation<SearchUsersPageRequest,ResultPage<UserResponse>> { }
