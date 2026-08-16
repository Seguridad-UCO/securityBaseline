package co.edu.uco.seguridad.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Orígenes permitidos para llamar la API desde un navegador (ADR-022). Vacía por defecto — un
 * ambiente sin frontend conocido no permite ninguno, nunca {@code *}.
 *
 * @param allowedOrigins orígenes exactos (esquema+host+puerto) autorizados a hacer CORS
 */
@ConfigurationProperties(prefix = "pdp.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
