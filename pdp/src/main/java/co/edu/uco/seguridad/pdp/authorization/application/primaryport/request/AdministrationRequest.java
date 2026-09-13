package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Set;

/**
 * Entrada de {@code AuthorizeAdministrationUseCase} (HU-009): quién pregunta, por qué aplicación, y
 * bajo qué inquilino. {@code subjectRoles} lo llena {@code AuthorizeAdministrationUseCaseImpl} a
 * partir de {@code subjectUserId} antes de llamar a {@code AdministrationDecisionPort} — arranca
 * vacío, igual que {@code AccessRequest.subjectRoles}.
 */
public record AdministrationRequest(TenantId tenantId, ApplicationId applicationId, UserId subjectUserId,
        String subject, Set<String> subjectRoles) {

    public AdministrationRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(subjectUserId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        subjectRoles = Set.copyOf(Objects.requireNonNull(subjectRoles, RequiredArgumentMessages.SUBJECT_ROLES));
    }

    /** Nuevo {@code AdministrationRequest} con los roles resueltos — el resto no cambia. */
    public AdministrationRequest withSubjectRoles(Set<String> roles) {
        return new AdministrationRequest(tenantId, applicationId, subjectUserId, subject, roles);
    }
}
