package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

/**
 * Adaptador primario HTTP: lista los endpoints protegidos de una aplicación. Entrada: el id de la ruta.
 */
public interface ListProtectedResourcesInteractor
        extends ReactiveOperation<String, List<ProtectedResourceWebResponse>> {
}
