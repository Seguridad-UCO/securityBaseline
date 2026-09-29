package co.edu.uco.seguridad.pdp.profiles.application.usecase;

import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ApplicationProfileCountRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

public interface CountApplicationProfilesUseCase extends ReactiveOperation<ApplicationProfileCountRequest, Long> {
}
