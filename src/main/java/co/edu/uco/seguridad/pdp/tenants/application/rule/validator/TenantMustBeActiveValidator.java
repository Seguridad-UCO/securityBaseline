package co.edu.uco.seguridad.pdp.tenants.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/**
 * Contrato publicado a los demás módulos: un inquilino existe y admite operar.
 *
 * <p>Es un validador y no una regla porque hace E/S: resuelve el estado contra el puerto y luego
 * aplica las reglas puras del dominio. Los módulos que lo consumen —{@code applications},
 * {@code identity}— inyectan esta única implementación en vez de copiar la comprobación, que es lo
 * que impide que la decisión diverja entre módulos.</p>
 *
 * <p>No devuelve el inquilino: ningún consumidor lo usaba, y devolverlo obligaba al puerto a cargar
 * el agregado completo para responder una pregunta de sí o no.</p>
 */
public interface TenantMustBeActiveValidator extends ReactiveOperationWithoutResult<TenantId> {
}
