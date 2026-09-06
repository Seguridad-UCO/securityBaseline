package co.edu.uco.seguridad.pdp.tenants.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantName;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** DTO de entrada del puerto primario: intención tipada de crear un tenant. */
public record CreateTenantRequest(TenantId id, TenantName name) {

    public CreateTenantRequest {
        Objects.requireNonNull(id, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.TENANT_NAME);
    }
}
