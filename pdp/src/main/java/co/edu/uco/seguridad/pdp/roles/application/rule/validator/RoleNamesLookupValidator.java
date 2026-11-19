package co.edu.uco.seguridad.pdp.roles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

import java.util.Set;

/**
 * Traduce identificadores de rol a sus nombres (HU-008): lo que un consumidor externo —hoy solo
 * {@code authorization}— necesita para construir el {@code subject.roles} que recibe OPA. Un id que
 * ya no existe se omite, nunca rechaza: es un enriquecimiento, no una regla de negocio.
 */
public interface RoleNamesLookupValidator extends ReactiveOperation<Set<RoleId>, Set<String>> {
}
