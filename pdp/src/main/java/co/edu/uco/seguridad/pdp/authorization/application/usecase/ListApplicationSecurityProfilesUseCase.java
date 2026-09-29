package co.edu.uco.seguridad.pdp.authorization.application.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityProfilesRequest; import co.edu.uco.seguridad.pdp.commons.model.ResultPage; import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse; import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface ListApplicationSecurityProfilesUseCase extends ReactiveOperation<ListApplicationSecurityProfilesRequest, ResultPage<ProfileResponse>> { }
