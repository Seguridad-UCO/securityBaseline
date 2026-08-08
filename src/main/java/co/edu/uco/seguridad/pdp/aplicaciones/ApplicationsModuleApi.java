package co.edu.uco.seguridad.pdp.aplicaciones;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import reactor.core.publisher.Mono;

/**
 * Lenguaje publicado de Aplicaciones. Los paquetes de implementación están intencionalmente ocultos, así que
 * un llamador solo puede expresar intención, nunca llegar al agregado o su almacén.
 *
 * <p>{@code register} devuelve la proyección registrada; {@code remove} devuelve {@code Mono<Void>}
 * porque la eliminación no tiene un resultado que valga la pena nombrar — es la operación compensatoria del módulo.</p>
 */
public interface ApplicationsModuleApi {

    Mono<RegisteredApplicationResponse> register(RegisterApplicationRequest dto);

    Mono<Void> remove(ApplicationId applicationId);
}
