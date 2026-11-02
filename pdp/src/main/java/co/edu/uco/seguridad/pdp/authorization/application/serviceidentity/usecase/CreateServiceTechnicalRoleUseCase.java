package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.CreateServiceTechnicalRoleRequest;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface CreateServiceTechnicalRoleUseCase extends ReactiveOperation<CreateServiceTechnicalRoleRequest, ServiceTechnicalRoleDefinition> {
}
