package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response;

import java.time.Instant;

/**
 * El contrato HTTP de salida, propiedad del adaptador web.
 *
 * <p>Plano y primitivo a propósito: serializar la entrada del catálogo directamente publicaría la
 * forma de {@code TenantId}, {@code ResourceCode} y todos los demás objetos de valor, y renombrar un
 * campo dentro del dominio se convertiría silenciosamente en un cambio disruptivo de la API.</p>
 */
public record ProtectedApplicationResponse(String applicationId,
                                           String resourceId,
                                           String tenantId,
                                           String applicationName,
                                           String resourceCode,
                                           String action,
                                           Instant registeredAt) {
}
