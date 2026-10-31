package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * DTO de entrada (HU-038): lo que el canal Bearer sabe del sujeto ya autenticado por el IdP, para
 * reconocer —o aprovisionar— su identidad local. {@code tenantId} sale del claim {@code tenant} del
 * token, nunca del tenant por defecto del panel. {@code email} viene vacío cuando el token no lo
 * trae o no es un correo válido; {@code emailVerified} refleja {@code email_verified}.
 */
public record ResolveBearerIdentityRequest(String issuer, String subject, TenantId tenantId,
                                           Optional<Email> email, boolean emailVerified, String name,
                                           String provider) {

    public ResolveBearerIdentityRequest {
        Objects.requireNonNull(issuer, RequiredArgumentMessages.EXTERNAL_IDENTITY_ISSUER);
        Objects.requireNonNull(subject, RequiredArgumentMessages.EXTERNAL_IDENTITY_SUBJECT);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(email, RequiredArgumentMessages.USER_EMAIL);
        Objects.requireNonNull(provider, RequiredArgumentMessages.EXTERNAL_IDENTITY_PROVIDER);
        name = name == null ? "" : name;
    }
}
