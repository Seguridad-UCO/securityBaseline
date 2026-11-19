package co.edu.uco.seguridad.pdp.authorization.application.secondaryport;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AdministrationDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto de salida hacia el motor de políticas para decisiones de administración (HU-009). Mismo
 * patrón que {@code PolicyDecisionPort}, pero sin acoplarse al contrato {@code pdp-opa/v1}: una
 * decisión de "¿puede administrar?" no tiene {@code resource}/{@code action} que enviar.
 */
public interface AdministrationDecisionPort extends ReactiveOperation<AdministrationRequest, AdministrationDecision> {
}
