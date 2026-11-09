package co.edu.uco.seguridad.pdp.identity.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve el {@code UserId} propio de quien tiene una identidad externa con ese {@code subject},
 * sin conocer el emisor (HU-015, enmienda de contrato §14): el principal de un JWT crudo —el único
 * modo que ejercitan las pruebas HTTP del proyecto, `TestJwtSupport`— no trae el {@code UserId} ya
 * resuelto como sí lo trae {@code LocalUserPrincipal} tras un login real. Vacío si no hay ninguna
 * identidad externa con ese subject.
 */
public interface SubjectUserIdLookupValidator extends ReactiveOperation<String, UserId> {
}
