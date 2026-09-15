package co.edu.uco.seguridad.pep.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/** Configuración mínima que identifica una aplicación ante el PEP desde el starter embebido. */
@ConfigurationProperties("security.pep.enforcement")
public record PepEnforcementProperties(boolean enabled, URI pepUrl, String applicationName, String environment,
                                       String applicationCredential, List<String> publicPaths,
                                       boolean allowInsecureHttp) {
    public PepEnforcementProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }

    void validate() {
        if (pepUrl == null || pepUrl.getHost() == null || applicationName == null || applicationName.isBlank()
                || environment == null || environment.isBlank() || applicationCredential == null
                || applicationCredential.isBlank()) {
            throw new IllegalArgumentException("PEP enforcement requires URL, application name, environment and application credential");
        }
        if (!"https".equals(pepUrl.getScheme()) && !(allowInsecureHttp && "http".equals(pepUrl.getScheme()))) {
            throw new IllegalArgumentException("PEP URL must use HTTPS (HTTP only with explicit development setting)");
        }
        if (pepUrl.getRawUserInfo() != null || pepUrl.getRawQuery() != null || pepUrl.getRawFragment() != null) {
            throw new IllegalArgumentException("PEP URL must not include user info, query or fragment");
        }
        if (publicPaths.stream().anyMatch(path -> path == null || path.isBlank() || !path.startsWith("/"))) {
            throw new IllegalArgumentException("Each public path must start with '/'");
        }
    }
}
