package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterApplicationWithInitialResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.ApplicationWithInitialResourceRegistrationResponse;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterApplicationWithInitialResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationWithInitialResourceUseCase} (HU-010). Pendiente:
 * registrar la aplicación con {@link RegisterApplicationUseCase}; si el registro del recurso con
 * {@link RegisterProtectedResourceUseCase} falla, compensar con {@link RemoveApplicationUseCase} y
 * propagar el error original del recurso (nunca el de la compensación); si la compensación también
 * falla, registrarlo por log — nunca en silencio, y sin cambiar el error que llega al cliente.
 */
public final class RegisterApplicationWithInitialResourceUseCaseImpl
        implements RegisterApplicationWithInitialResourceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterApplicationWithInitialResourceUseCaseImpl.class);

    private final RegisterApplicationUseCase registerApplication;
    private final RegisterProtectedResourceUseCase registerResource;
    private final RemoveApplicationUseCase removeApplication;

    public RegisterApplicationWithInitialResourceUseCaseImpl(RegisterApplicationUseCase registerApplication,
            RegisterProtectedResourceUseCase registerResource, RemoveApplicationUseCase removeApplication) {
        this.registerApplication = Objects.requireNonNull(registerApplication,
                RequiredArgumentMessages.REGISTER_APPLICATION_USE_CASE);
        this.registerResource = Objects.requireNonNull(registerResource,
                RequiredArgumentMessages.REGISTER_PROTECTED_RESOURCE_USE_CASE);
        this.removeApplication = Objects.requireNonNull(removeApplication,
                RequiredArgumentMessages.REMOVE_APPLICATION_USE_CASE);
    }

    @Override
    public Mono<ApplicationWithInitialResourceRegistrationResponse> execute(
            RegisterApplicationWithInitialResourceRequest input) {
        return registerApplication.execute(new RegisterApplicationRequest(input.tenantId(), input.name(),
                        input.description(), input.baseUrl()))
                .flatMap(applicationRegistration -> registerResource(input, applicationRegistration));
    }

    private Mono<ApplicationWithInitialResourceRegistrationResponse> registerResource(
            RegisterApplicationWithInitialResourceRequest input, ApplicationRegistrationResponse applicationRegistration) {
        ApplicationId applicationId = applicationRegistration.application().id();
        return registerResource.execute(new RegisterProtectedResourceRequest(input.tenantId(), applicationId,
                        input.resourcePath(), input.resourceMethod()))
                .map(resource -> new ApplicationWithInitialResourceRegistrationResponse(applicationRegistration, resource))
                .onErrorResume(resourceError -> compensate(applicationId, resourceError));
    }

    private Mono<ApplicationWithInitialResourceRegistrationResponse> compensate(ApplicationId applicationId,
            Throwable resourceError) {
        return removeApplication.execute(applicationId)
                .onErrorResume(compensationError -> {
                    LOG.error("no se pudo compensar el registro huérfano de la aplicación {}",
                            applicationId.value(), compensationError);
                    return Mono.empty();
                })
                .then(Mono.error(resourceError));
    }
}
