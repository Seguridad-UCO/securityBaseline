package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentRevocationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerProfileAssignmentRevocationUseCase
        extends ReactiveOperation<AdministerProfileAssignmentRevocationRequest, Void> {
}
