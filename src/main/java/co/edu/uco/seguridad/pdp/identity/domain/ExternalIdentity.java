package co.edu.uco.seguridad.pdp.identity.domain;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Vínculo entre un usuario propio y la identidad que el IdP le asignó. {@code provider} distingue el
 * broker real ({@code google}) del login local de Keycloak ({@code keycloak-local}).
 */
public record ExternalIdentity(UserId userId, String issuer, String subject, String provider) {

    public ExternalIdentity {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(issuer, RequiredArgumentMessages.EXTERNAL_IDENTITY_ISSUER);
        Objects.requireNonNull(subject, RequiredArgumentMessages.EXTERNAL_IDENTITY_SUBJECT);
        Objects.requireNonNull(provider, RequiredArgumentMessages.EXTERNAL_IDENTITY_PROVIDER);
    }
}
