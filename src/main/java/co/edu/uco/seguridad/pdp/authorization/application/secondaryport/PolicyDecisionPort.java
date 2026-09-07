package co.edu.uco.seguridad.pdp.authorization.application.secondaryport;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AccessRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto de salida hacia el motor de politicas. La frontera por donde entrara OPA en HU-004 sin
 * tocar el contrato HTTP: hoy la unica implementacion deniega por defecto (ADR-012).
 */
public interface PolicyDecisionPort extends ReactiveOperation<AccessRequest, AccessDecision> {
}
