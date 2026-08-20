package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: recibe el payload crudo, mapea, ejecuta el caso de uso y proyecta la
 * respuesta HTTP. Vive en {@code infrastructure}, no en {@code application} — traduce un formato de
 * transporte concreto, así que es una responsabilidad del adaptador web, no del núcleo.
 */
public interface CreateTenantInteractor extends ReactiveOperation<CreateTenantRawRequest, TenantWebResponse> {
}
