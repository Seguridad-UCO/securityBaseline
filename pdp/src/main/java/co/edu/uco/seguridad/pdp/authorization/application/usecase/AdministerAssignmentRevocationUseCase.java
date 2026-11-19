package co.edu.uco.seguridad.pdp.authorization.application.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentRevocationRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface AdministerAssignmentRevocationUseCase
        extends ReactiveOperation<AdministerAssignmentRevocationRequest, Void> {
}
