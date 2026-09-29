package co.edu.uco.seguridad.pep.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("security.pep.registration")
public record PepRegistrationProperties(boolean enabled, URI pepUrl, String applicationId, String environment,
                                        URI backendUrl, String audience, String token, boolean allowInsecureHttp) {
    void validate() {
        if (pepUrl == null || applicationId == null || applicationId.isBlank() || environment == null || environment.isBlank()
                || backendUrl == null || audience == null || audience.isBlank() || token == null || token.isBlank()) {
            throw new IllegalArgumentException("PEP registration requires URL, application id, environment, backend URL, audience and token");
        }
        if (pepUrl.getHost() == null || (!"https".equals(pepUrl.getScheme())
                && !(allowInsecureHttp && "http".equals(pepUrl.getScheme()))) || backendUrl.getHost() == null
                || backendUrl.getRawUserInfo() != null || backendUrl.getRawQuery() != null || backendUrl.getRawFragment() != null
                || backendUrl.getRawPath() != null && !backendUrl.getRawPath().isEmpty()) {
            throw new IllegalArgumentException("PEP URL must be HTTPS (HTTP only with explicit development setting) and backend URL must be an origin");
        }
    }
}
