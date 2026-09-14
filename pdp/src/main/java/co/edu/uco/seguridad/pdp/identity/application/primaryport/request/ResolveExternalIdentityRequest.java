package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Identidad que un proveedor externo ya autenticó y que debe vincularse a un usuario local. */
public record ResolveExternalIdentityRequest(String issuer, String subject) {

    public ResolveExternalIdentityRequest {
        Objects.requireNonNull(issuer, RequiredArgumentMessages.EXTERNAL_IDENTITY_ISSUER);
        Objects.requireNonNull(subject, RequiredArgumentMessages.EXTERNAL_IDENTITY_SUBJECT);
    }
}
