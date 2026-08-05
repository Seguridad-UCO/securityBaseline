package co.edu.uco.seguridad.pdp.recursos.infrastructure.web;
import jakarta.validation.constraints.*;
public record RegisterProtectedApplicationRequest(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}") String tenantId,
    @NotBlank @Size(min = 3, max = 100) String applicationName,
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{1,63}") String resourceCode,
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{1,63}") String action) { }
