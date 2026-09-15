package co.edu.uco.seguridad.pdp.assignments.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para {@code LastAdministratorMustNotBeRevokedRule} (HU-020). Trae
 * {@code applicationId} porque {@code CannotRemoveLastAdministratorException} lo necesita — la SPEC
 * original del plan lo omitía (corregido en FASE 1 de {@code 2-tester-spec}, ver cierre).
 */
public record AdministratorRevocationEligibility(ApplicationId applicationId, int activeAdministratorCount) {

    public AdministratorRevocationEligibility {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
