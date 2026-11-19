package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AdministerRoleDefinitionUseCase} (HU-016, HU-036): la solicitud de definición de rol,
 * más el gate que exige su alcance: administrar la aplicación del rol ({@code APPLICATION}) o administrar
 * al menos una aplicación del inquilino ({@code TENANT}). Ya no existe el estado «sin gate».
 */
public record AdministerRoleDefinitionRequest(CatalogAdministration administration, DefineRoleRequest role) {

    public AdministerRoleDefinitionRequest {
        Objects.requireNonNull(administration, RequiredArgumentMessages.CATALOG_ADMINISTRATION);
        Objects.requireNonNull(role, RequiredArgumentMessages.DEFINE_ROLE_REQUEST);
    }
}
