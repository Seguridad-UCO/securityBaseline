package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;

import co.edu.uco.seguridad.pdp.identity.domain.Email;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** DTO de entrada del puerto primario: intención tipada de provisionar (o reconocer) un usuario. */
public record ProvisionIdentityRequest(String issuer, String subject, Email email, String name, String provider) {

    public ProvisionIdentityRequest {
        Objects.requireNonNull(issuer, RequiredArgumentMessages.EXTERNAL_IDENTITY_ISSUER);
        Objects.requireNonNull(subject, RequiredArgumentMessages.EXTERNAL_IDENTITY_SUBJECT);
        Objects.requireNonNull(email, RequiredArgumentMessages.USER_EMAIL);
        Objects.requireNonNull(provider, RequiredArgumentMessages.EXTERNAL_IDENTITY_PROVIDER);
        name = name == null ? "" : name.trim();
    }
}
