package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.RequestServiceAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
public interface RequestServiceAccessUseCase extends ReactiveOperation<RequestServiceAccessRequest, ServiceAccessGrant> { }
