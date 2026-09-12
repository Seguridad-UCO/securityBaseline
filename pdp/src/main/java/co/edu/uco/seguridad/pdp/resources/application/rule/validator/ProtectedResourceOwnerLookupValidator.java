package co.edu.uco.seguridad.pdp.resources.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve qué aplicación es dueña de un recurso protegido, a partir solo de su identificador — a
 * diferencia de {@link ProtectedResourceMustExistValidator}, que verifica un endpoint ya conocido
 * (aplicación + ruta + método). Rechaza con {@code ProtectedResourceNotFoundException} si el recurso
 * no existe (HU-004, regla R4). Publicado en {@code resources :: rule} para que {@code roles} lo consuma.
 */
public interface ProtectedResourceOwnerLookupValidator extends ReactiveOperation<ResourceId, ApplicationId> {
}
