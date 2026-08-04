package co.edu.uco.seguridad.applications.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

public record ProtectedApplicationResponse(UUID id, String name, String tenantId, String resource, String status, Instant registeredAt) { }
