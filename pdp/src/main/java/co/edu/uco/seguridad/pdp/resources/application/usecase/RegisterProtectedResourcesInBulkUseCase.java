package co.edu.uco.seguridad.pdp.resources.application.usecase;

import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.List;

/**
 * Puerto primario: persistir de forma atómica un lote de recursos ya validados y publicar
 * {@code ProtectedResourceRegistered} por cada uno.
 */
public interface RegisterProtectedResourcesInBulkUseCase extends ReactiveOperation<List<ProtectedResource>, Void> {
}
