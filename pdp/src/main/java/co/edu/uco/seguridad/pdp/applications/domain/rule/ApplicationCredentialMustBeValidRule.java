package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationCredentialValidity;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * La aplicación existe y su secreto coincide con el hash guardado. Pura y síncrona: el validador
 * resuelve el booleano contra el puerto y {@code CredentialHasher}; esta regla solo decide (HU-013).
 */
public interface ApplicationCredentialMustBeValidRule extends OperationWithoutResult<ApplicationCredentialValidity> {
}
