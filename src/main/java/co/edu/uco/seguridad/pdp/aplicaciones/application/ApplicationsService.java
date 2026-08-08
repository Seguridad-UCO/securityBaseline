package co.edu.uco.seguridad.pdp.aplicaciones.application;

import co.edu.uco.seguridad.pdp.aplicaciones.ApplicationsModuleApi;
import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Fachada del módulo {@code aplicaciones}: implementa {@link ApplicationsModuleApi} delegando cada
 * operación al caso de uso individual que le corresponde.
 *
 * <p>No contiene lógica de negocio propia. Su única responsabilidad es traducir el lenguaje
 * publicado del módulo ({@code register}/{@code remove}) al contrato interno de puerto primario
 * ({@code execute}), manteniendo el límite del módulo completamente opaco para los llamadores.</p>
 */
public final class ApplicationsService implements ApplicationsModuleApi {

    private final RegisterApplicationUseCase registerUseCase;
    private final RemoveApplicationUseCase removeUseCase;

    public ApplicationsService(RegisterApplicationUseCase registerUseCase,
                               RemoveApplicationUseCase removeUseCase) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase, "se requiere caso de uso de registro");
        this.removeUseCase = Objects.requireNonNull(removeUseCase, "se requiere caso de uso de eliminación");
    }

    @Override
    public Mono<RegisteredApplicationResponse> register(RegisterApplicationRequest dto) {
        return registerUseCase.execute(dto);
    }

    @Override
    public Mono<Void> remove(ApplicationId applicationId) {
        return removeUseCase.execute(applicationId);
    }
}
