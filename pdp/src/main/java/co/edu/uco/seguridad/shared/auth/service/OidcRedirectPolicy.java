package co.edu.uco.seguridad.shared.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public final class OidcRedirectPolicy {

    private final String frontendOrigin;

    public OidcRedirectPolicy(@Value("${pdp.frontend.origin}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    public String frontendHome() {
        return UriComponentsBuilder.fromUriString(frontendOrigin)
                .replaceQuery(null)
                .build(true)
                .toUriString();
    }

    public String frontendAuthenticationError() {
        return UriComponentsBuilder.fromUriString(frontendOrigin)
                .replaceQuery(null)
                .queryParam("error", "authentication")
                .build(true)
                .toUriString();
    }

    public String frontendRegistrationReady() {
        return UriComponentsBuilder.fromUriString(frontendOrigin)
                .replaceQuery(null)
                .queryParam("registered", "success")
                .build(true)
                .toUriString();
    }
}
