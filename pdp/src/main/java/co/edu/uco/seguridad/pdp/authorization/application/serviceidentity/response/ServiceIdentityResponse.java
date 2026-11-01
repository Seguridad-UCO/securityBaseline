package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.response;

import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentity;

/** El secreto es de revelación única y por ello no forma parte de la identidad almacenada. */
public record ServiceIdentityResponse(ServiceIdentity identity, String clientSecret) { }
