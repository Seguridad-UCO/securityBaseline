package co.edu.uco.seguridad.pep.ingress;

import co.edu.uco.seguridad.pep.commons.IdentityEvidence;
import co.edu.uco.seguridad.pep.commons.ProxyTarget;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;

public record CapturedAccess(NormalizeAccessRequest metadata, IdentityEvidence evidence, ProxyTarget target) {
}
