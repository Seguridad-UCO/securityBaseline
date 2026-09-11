package co.edu.uco.seguridad.shared.web.session;

/** Contexto mínimo que el BFF expone al SPA; no incluye ningún JWT ni secreto. */
public record SessionResponse(String subject, String name, String email, String tenantId) {
}
