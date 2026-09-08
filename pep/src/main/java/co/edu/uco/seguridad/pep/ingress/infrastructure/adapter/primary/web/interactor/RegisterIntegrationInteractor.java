package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pep.application.contract.ReactiveOperation;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw.RegisterIntegrationRawRequest;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.response.IntegrationWebResponse;

/** Adaptador primario: mapea, ejecuta el caso de uso y proyecta la respuesta HTTP. */
public interface RegisterIntegrationInteractor
        extends ReactiveOperation<RegisterIntegrationRawRequest, IntegrationWebResponse> {
}
