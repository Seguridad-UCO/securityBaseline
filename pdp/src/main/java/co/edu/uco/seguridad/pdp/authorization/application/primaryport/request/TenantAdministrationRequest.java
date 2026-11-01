package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;

import java.util.Objects;

/**
 * Entrada de {@code PrincipalMustAdministerAtLeastOneApplicationValidator} (HU-036): quién pregunta y
 * bajo qué inquilino, sin aplicación — el validador la deduce de las asignaciones del sujeto. Es el
 * actor de las escrituras de catálogo de alcance {@code TENANT}. {@code authenticationContext} es la
 * evidencia de MFA que evalúa el gate por aplicación (ADR-027).
 */
public record TenantAdministrationRequest(TenantId tenantId, UserId subjectUserId, String subject,
                                          AuthenticationContextEvidence authenticationContext) {

    public TenantAdministrationRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(subjectUserId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(authenticationContext, RequiredArgumentMessages.AUTHENTICATION_CONTEXT_EVIDENCE);
    }
}
