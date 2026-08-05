package co.edu.uco.seguridad.pdp.recursos;
public record RegisterProtectedApplicationCommand(String tenantId, String applicationName, String resourceCode, String action) { }
