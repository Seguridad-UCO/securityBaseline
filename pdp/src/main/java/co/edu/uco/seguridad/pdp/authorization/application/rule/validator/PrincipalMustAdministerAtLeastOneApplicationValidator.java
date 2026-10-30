package co.edu.uco.seguridad.pdp.authorization.application.rule.validator;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Gate de las escrituras de catálogo de alcance {@code TENANT} (HU-036, ADR-036): el sujeto administra
 * al menos una aplicación de su inquilino. Emite la aplicación cuyo gate concedió el permiso (la
 * primera, en orden estable) — la auditoría la atribuye a ella — o termina en
 * {@code MfaEvidenceRequiredException} / {@code NotAuthorizedToAdministerTenantCatalogException}.
 * Es un actor intermedio: el administrador global de ADR-024 lo sustituirá sin cambiar este contrato.
 *
 * <p>No extiende {@link PrincipalMustBeApplicationAdministratorValidator} a propósito: son dos
 * contratos distintos y dos beans de tipos distintos, sin ambigüedad de inyección.</p>
 */
public interface PrincipalMustAdministerAtLeastOneApplicationValidator
        extends ReactiveOperation<TenantAdministrationRequest, ApplicationId> {
}
