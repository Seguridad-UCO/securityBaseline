package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** DTO crudo: solo el identificador de la ruta. Sin invariantes propias — las valida el mapper. */
public record ApplicationAdministrationRawRequest(String applicationId) {
}
