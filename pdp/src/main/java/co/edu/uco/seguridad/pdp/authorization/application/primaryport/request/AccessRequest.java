package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;

/**
 * Entrada normalizada del PEP a evaluar (corresponde a {@code SolicitudAcceso} del dominio
 * aceptado). El sujeto y el inquilino llegan del principal autenticado, nunca del cuerpo.
 * {@code subjectUserId} es el identificador interno ya resuelto del sujeto. El canal BFF lo toma
 * de la sesión local y el canal PEP lo resuelve desde {@code issuer + sub}. {@code subjectRoles}
 * lo llena {@code AuthorizeUseCaseImpl} a partir de
 * {@code subjectUserId} antes de llamar a {@code PolicyDecisionPort} — arranca vacío.
 */
public record AccessRequest(TenantId tenantId, String subject, ApplicationId applicationId,
        ResourcePath resourcePath, HttpVerb action, String requestId, String correlationId,
        Optional<UserId> subjectUserId, Set<String> subjectRoles, Instant timestamp, String environment,
        HttpVerb contextMethod, String channel) {

    public AccessRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(resourcePath, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(action, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(requestId, RequiredArgumentMessages.REQUEST_ID);
        Objects.requireNonNull(correlationId, RequiredArgumentMessages.CORRELATION_ID);
        Objects.requireNonNull(subjectUserId, RequiredArgumentMessages.PRINCIPAL_USER_ID);
        subjectRoles = Set.copyOf(Objects.requireNonNull(subjectRoles, RequiredArgumentMessages.SUBJECT_ROLES));
    }

    public AccessRequest(TenantId tenantId, String subject, ApplicationId applicationId, ResourcePath resourcePath,
            HttpVerb action, String requestId, String correlationId, Optional<UserId> subjectUserId,
            Set<String> subjectRoles) {
        this(tenantId, subject, applicationId, resourcePath, action, requestId, correlationId, subjectUserId,
                subjectRoles, null, null, null, null);
    }

    /** Nuevo {@code AccessRequest} con los roles resueltos — el resto de los campos no cambia. */
    public AccessRequest withSubjectRoles(Set<String> roles) {
        return new AccessRequest(tenantId, subject, applicationId, resourcePath, action, requestId, correlationId,
                subjectUserId, roles, timestamp, environment, contextMethod, channel);
    }
}
