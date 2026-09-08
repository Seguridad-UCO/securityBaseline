package co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request;

import co.edu.uco.seguridad.pep.commons.AccessRequest;
import co.edu.uco.seguridad.pep.commons.IdentityEvidence;

/** Entrada atómica para evaluar una solicitud ya normalizada y su evidencia de identidad. */
public record EnforceAccessRequest(AccessRequest accessRequest, IdentityEvidence identityEvidence) {
}
