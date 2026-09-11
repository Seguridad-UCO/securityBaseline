package co.edu.uco.seguridad.pep.commons;

import java.net.URI;

public record ProxyTarget(URI origin, String path, boolean forwardBearer, boolean forwardCookies) {
}

