package co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request;

import java.time.Instant;

/**
 * DTO de entrada del caso de uso de normalización; no contiene tipos HTTP ni credenciales.
 */
public record NormalizeAccessRequest(String requestId, String correlationId, Instant timestamp,
                                     String applicationName, String environment, String path, String method) {
}
