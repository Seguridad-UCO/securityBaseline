package co.edu.uco.seguridad.pdp.applications.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Resuelve qué inquilino es dueño de una aplicación, a partir solo de su identificador — a
 * diferencia de {@link ApplicationMustExistForTenantValidator}, que verifica una pertenencia ya
 * conocida. Rechaza con {@code ApplicationNotFoundException} si la aplicación no existe (HU-003,
 * decisión D3 de {@code docs/ai-harness/workspace/HANDOFF-INTEGRACION-PEP-OPA.md}).
 */
public interface ApplicationOwnerLookupValidator extends ReactiveOperation<ApplicationId, TenantId> {
}
