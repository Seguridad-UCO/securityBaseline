package co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request;

import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.commons.PageWindow;

/**
 * Entrada tipada del caso de uso de consulta: el criterio ya validado y la ventana ya resuelta.
 * Cuando llega aquí, no queda nada que rechazar.
 *
 * <p>Esqueleto de la SPEC de HU-001 — sin lógica todavía.
 */
public record ListApplicationsRequest(ApplicationCriteria criteria, PageWindow window) {

    public ListApplicationsRequest {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }
}
